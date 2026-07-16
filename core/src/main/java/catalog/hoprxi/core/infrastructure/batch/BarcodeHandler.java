/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class BarcodeHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {
    private static final Set<String> BARCODE_CACHE = ConcurrentHashMap.newKeySet(5120);
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{7}$|^\\d{11,12}$");
    // 2. 记录所有发生过重复的条码（黑名单）
    static final Set<String> BARCODE_BLACKLIST = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger START;
    private static final String PREFIX;
    private static final String BARCODE_TYPE;
    private static final Logger LOGGER = LoggerFactory.getLogger(BarcodeHandler.class);

    static {
        Config config = ConfigFactory.load("import");
        PREFIX = config.hasPath("barcode.prefix") ? config.getString("barcode.prefix") : "20";
        BARCODE_TYPE = config.hasPath("barcode.type") ? config.getString("barcode.type") : "ean_8";
        START = config.hasPath("barcode.start") ? new AtomicInteger(config.getInt("barcode.start")) : new AtomicInteger(1);
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }

    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) {
        long start = System.nanoTime();
        // 1. 获取或生成标准条码或者原始值，不带引号
        String rawBarcode = BarcodeHandler.getOrGenerateBarcode(event);
        if (rawBarcode == null) return;

        // 2. 检查重复和数据库存在性，并处理
        boolean isDuplicateOrExist = BarcodeHandler.checkAndHandleDuplicateOrExist(event, rawBarcode);
        if (isDuplicateOrExist) return;

        event.barcode = "'" + rawBarcode + "'";
        //System.out.println("barcode:"+event.barcode);
        long elapsed = (System.nanoTime() - start) / 1_000_000; // 毫秒
        if (elapsed > 100) {
            System.out.printf("Handler %s slow, event seq=%d, cost=%dms%n",
                    this.getClass().getSimpleName(), l, elapsed);
        }
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
            System.out.println("InvalidBarcode:" + barcode);
            Matcher matcher = BARCODE_PATTERN.matcher(barcode);
            if (!matcher.matches()) {
                event.addWrong(Verify.BARCODE_CHECK_SUM_ERROR, barcode);
                return null;
            }
            System.out.println("补全:" + barcode);
            Barcode bar = BarcodeGenerateServices.createBarcodeCompleteChecksum(barcode);
            return bar.barcode();
        }
    }

    /**
     * 检查缓存和数据库，处理重复或已存在情况。
     * 若命中，添加错误，加入黑名单，并放入带引号的值，返回 true。
     * 否则返回 false。
     */
    private static boolean checkAndHandleDuplicateOrExist(ItemImportEvent event, String rawBarcode) {
        // 尝试原子性地将条码加入缓存（表示该条码正在被处理）
        // 若 add 返回 false，说明已有其他线程成功插入，当前线程应视为重复
        if (!BARCODE_CACHE.add(rawBarcode)) {
            event.addWrong(Verify.BARCODE_REPEAT, rawBarcode);
            BARCODE_BLACKLIST.add(rawBarcode);
            //event.barcode = "'" + rawBarcode + "'";   // 确保导出时有值
            return true;
        }

        // 当前线程成功获得缓存标记，现在查询数据库
        if (BarcodeHandler.isExist(rawBarcode)) {
            // 数据库已存在，报告错误，保留缓存标记（后续线程会因缓存命中而报重复）
            event.addWrong(Verify.BARCODE_EXIST, rawBarcode);
            BARCODE_BLACKLIST.add(rawBarcode);
            //event.barcode = "'" + rawBarcode + "'";
            return true;
        }
        // 数据库不存在，缓存标记已保留，用于防重复，返回 false 表示成功
        return false;
    }

    private static boolean isExist(String barcode) {
        long t1 = System.nanoTime();
        Connection conn = null;
        try {
            // 计时获取连接
            conn = PsqlUtil.getConnection();  // 假设使用 HikariCP
            long t2 = System.nanoTime();
            System.out.println("获取连接耗时: " + (t2 - t1) / 1_000_000 + " ms");

            PreparedStatement ps = conn.prepareStatement("select id from item where barcode = ?");
            ps.setString(1, barcode);
            long t3 = System.nanoTime();
            ResultSet rs = ps.executeQuery();
            long t4 = System.nanoTime();
            System.out.println("执行查询耗时: " + (t4 - t3) / 1_000_000 + " ms");

            boolean exists = rs.next();
            long t5 = System.nanoTime();
            System.out.println("结果集处理耗时: " + (t5 - t4) / 1_000_000 + " ms");
            return exists;
        } catch (SQLException e) {
            // ...
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
