/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package catalog.hoprxi.core.infrastructure.batch;


import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.barcode.InvalidBarcodeException;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/7/18
 */

public class BatchBarcodeHandler implements WorkHandler<ItemImportEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchBarcodeHandler.class);
    private static final Set<String> BARCODE_CACHE = ConcurrentHashMap.newKeySet(4960);
    static final Set<String> BARCODE_BLACKLIST = ConcurrentHashMap.newKeySet();
    // 配置参数
    private static final int BATCH_SIZE = 30;          // 批量查询阈值
    private static final long FLUSH_INTERVAL_MS = 500; // 超时刷新间隔（毫秒）

    // 队列：存放等待批量处理的事件和条码
    private final BlockingQueue<ItemImportEvent> queue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{7}$|^\\d{11,12}$");
    private static final AtomicInteger START;
    private static final String PREFIX;
    private static final String BARCODE_TYPE;
    private static final boolean complete = false;
    private static final boolean correct = false;

    // 共享的内部 Disruptor（单例）
    private static final Disruptor<ItemImportEvent> POST_DISRUPTOR;
    private static final RingBuffer<ItemImportEvent> RING_BUFFER;

    static {
        Config config = ConfigFactory.load("import");
        PREFIX = config.hasPath("barcode.prefix") ? config.getString("barcode.prefix") : "20";
        BARCODE_TYPE = config.hasPath("barcode.type") ? config.getString("barcode.type") : "ean_8";
        START = config.hasPath("barcode.start") ? new AtomicInteger(config.getInt("barcode.start")) : new AtomicInteger(1);

        // 初始化共享的内部 Disruptor
        POST_DISRUPTOR = new Disruptor<>(
                ItemImportEvent::new,
                2048,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,          // 多个生产者（多个 Worker 实例）
                new YieldingWaitStrategy()
        );
        POST_DISRUPTOR.setDefaultExceptionHandler(new ExceptionHandler<ItemImportEvent>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, ItemImportEvent event) {
                ex.printStackTrace(); // 打印到控制台
            }

            @Override
            public void handleOnStartException(Throwable ex) {
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
            }
        });
        // 消费者：多个 UploadHandler Worker，然后顺序执行 AssembleHandler 和 FailedValidationHandler
        POST_DISRUPTOR.handleEventsWithWorkerPool(
                        new UploadHandler(), new UploadHandler(), new UploadHandler(), new UploadHandler())
                .then(new AssembleHandler(), new FailedValidationHandler());
        POST_DISRUPTOR.start();
        RING_BUFFER = POST_DISRUPTOR.getRingBuffer();
    }

    public BatchBarcodeHandler() {
        // 启动定时刷新任务
        scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        //long t1 = System.nanoTime();
        // 不到BATCH_SIZE，最后一行， LAST_ROW 触发关闭
        if (event.map.get(ItemMapping.LAST_ROW) != null) {
            flush();
            publishToPostDisruptor(event);
            POST_DISRUPTOR.shutdown();
            return;
        }

        String rawBarcode = BatchBarcodeHandler.getOrGenerateBarcode(event);
        if (rawBarcode == null) {      // 校验失败已经 addWrong，直接转发给下游（下游会跳过有错误的事件）
            publishToPostDisruptor(event);
            return;
        }
        // ✅ 必须赋值，供后续批量查询使用
        event.barcode = rawBarcode;
        if (!BARCODE_CACHE.add(rawBarcode)) {
            System.out.println("❌ 缓存添加失败！原始=" + event.map.get(ItemMapping.BARCODE) + " → 最终=" + rawBarcode);
            event.addWrong(Verify.BARCODE_REPEAT, rawBarcode);
            BARCODE_BLACKLIST.add(rawBarcode);
            publishToPostDisruptor(event);  // ✅ 转发，让错误事件继续流向下游
            return;
        }
        // ----- 入队前复制事件对象 -----
        ItemImportEvent copy = new ItemImportEvent();
        copy.map = event.map;                          // 浅拷贝，后续不会改 map
        copy.wrong.putAll(event.wrong);                // 深拷贝错误集
        copy.generatedId = event.generatedId;
        copy.barcode = event.barcode;
        copy.madeInJson = event.madeInJson;
        copy.categoryId = event.categoryId;
        copy.brandId = event.brandId;
        copy.show = event.show;
        copy.basicInfo = event.basicInfo;              // record 不可变，直接引用
        // 如果有其他字段，一并复制
        queue.offer(copy);
        //long t2 = System.nanoTime();
        //System.out.println("barcode前置处理耗时 " + (t2 - t1) / 1_000_000 + " ms");
        if (queue.size() >= BATCH_SIZE) {
            scheduler.submit(this::flush);
        }
    }

    /**
     * 发布到内部 Disruptor（直接复制字段到新事件）
     */
    private void publishToPostDisruptor(ItemImportEvent event) {
        RING_BUFFER.publishEvent((e, sequence, arg) -> {
            // 1. 复制 map（浅拷贝，因为 map 内部是 String，不可变）
            e.map = arg.map; // 如果担心后续修改，可以 new HashMap<>(arg.map)，但一般不会修改 map

            // 2. 复制 wrong Map（必须深拷贝，因为后续会修改）
            e.wrong.clear();
            e.wrong.putAll(arg.wrong);

            // 3. 复制基本类型和不可变对象
            e.generatedId = arg.generatedId;
            e.barcode = arg.barcode;
            e.madeInJson = arg.madeInJson;
            e.categoryId = arg.categoryId;
            e.brandId = arg.brandId;
            e.show = arg.show;
            // 4. basicInfo 是 record，不可变，直接引用
            e.basicInfo = arg.basicInfo;
        }, event);
    }

    /**
     * 获取或生成标准条码（原始字符串，不带引号）。
     * 如果条码为空，则生成店内码；如果非空，则校验并补全。
     * 若无法补全，会添加错误并放入原始输入值，返回 null。
     */
    private static String getOrGenerateBarcode(ItemImportEvent event) {
        String barcode = event.map.get(ItemMapping.BARCODE);
        if (barcode == null || barcode.isBlank()) {
            // 生成店内码
            return BARCODE_TYPE.equals("ean_13")
                    ? BarcodeGenerateServices.inStoreEAN_13BarcodeGenerate(START.getAndIncrement(), PREFIX).barcode()
                    : BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(START.getAndIncrement(), PREFIX).barcode();
        }
        barcode = barcode.trim();
        // 非空条码：校验并如果是7,11,12位补全
        try {
            Barcode bar = BarcodeGenerateServices.createBarcode(barcode);
            return bar.barcode();
        } catch (InvalidBarcodeException e) {
            //System.out.println("校验和错误:" + barcode);
            Matcher matcher = BARCODE_PATTERN.matcher(barcode);
            if (!matcher.matches()) {
                event.addWrong(Verify.BARCODE_CHECK_SUM_ERROR, barcode);
                return null;
            }
            Barcode bar = BarcodeGenerateServices.createBarcodeCompleteChecksum(barcode);
            System.out.println("已补全:" + bar.barcode());
            return bar.barcode();
        }
    }

    /**
     * 批量查询并处理队列中的所有事件
     */
    private void flush() {
        //long t1 = System.nanoTime();
        if (queue.isEmpty()) return;
        // 取出当前队列中的所有事件
        List<ItemImportEvent> batch = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) return;

        // 提取合规条码（使用 event.barcode）
        List<String> barcodes = batch.stream()
                .map(e -> e.barcode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        //long t2 = System.nanoTime();
        //System.out.println("批量查询前: " + (t2 - t1) / 1_000_000 + " ms");
        // 批量查询数据库
        Set<String> existing = queryExistingBarcodes(barcodes);
        BARCODE_BLACKLIST.addAll(existing);
        //long t3 = System.nanoTime();
        //System.out.println("读取数据库耗时: " + (t3 - t2) / 1_000_000 + " ms");
        // 处理每个事件
        for (ItemImportEvent event : batch) {
            if (existing.contains(event.barcode)) {
                // 数据库已存在
                event.addWrong(Verify.BARCODE_REPEAT, event.barcode);
            } else {
                // 数据库不存在，设置带引号的条码供后续使用
                event.barcode = "'" + event.barcode + "'";
            }
            publishToPostDisruptor(event);
        }
        //long t4 = System.nanoTime();
        //System.out.println("barCODE读取数据库后处理耗时: " + (t4 - t3) / 1_000_000 + " ms");
    }

    /**
     * 执行批量 IN 查询，返回已存在的条码集合
     */
    private Set<String> queryExistingBarcodes(List<String> barcodes) {
       // long t1 = System.nanoTime();
        if (barcodes.isEmpty()) return Collections.emptySet();
        // 构建 IN 占位符
        String placeholders = barcodes.stream().map(b -> "?").collect(Collectors.joining(","));
        String sql = "SELECT barcode FROM item WHERE barcode IN (" + placeholders + ")";
        Set<String> existing = new HashSet<>();
        try (Connection conn = PsqlUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            //long t2 = System.nanoTime();
            //System.out.println("获取连接的时间: 获取连接的时间:获取连接的时间:" + (t2 - t1) / 1_000_000 + " ms");
            for (int i = 0; i < barcodes.size(); i++) {
                ps.setString(i + 1, barcodes.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getString("barcode"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Batch query failed", e);
            // 发生异常时，保守起见认为所有条码都存在，避免插入重复
            existing.addAll(barcodes);
        }
        return existing;
    }
}
