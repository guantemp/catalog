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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;

import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQuerySpec;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.spec.*;
import io.netty.buffer.ByteBuf;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2026-04-12
 */
public class ESItemQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465", "slave.tooo.top:9200");
    }

    private static final ItemQuery query = new ESItemQuery();

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFindAsync() {
        Mono<ByteBuf>[] monos = new Mono[]{
                query.findAsync(55307473635765901L),
                query.findAsync(55307896123812197L),
                query.findAsync(55307820773139539L),
                query.findAsync(55307834488513967L)
        };
        ESItemQueryTest.printResult(monos);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFind() throws IOException {
        long[] ids = new long[]{55307473635765901L, 55307896123812197L, 55307820773139539L, 55307834488513967L};
        for (long id : ids) {
            try (InputStream is = query.find(id)) {
                System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test(invocationCount = 1, threadPoolSize = 1, expectedExceptions = IllegalArgumentException.class)
    public void testFindByBarcode() throws IOException {
        String[] barcodes = new String[]{"6900404523737", "6939006488885", "6940188805018", "6907469320189", "dsgf", "", null};
        for (String barcode : barcodes) {
            try (InputStream is = query.findByBarcode(barcode)) {
                System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFindByBarcodeAsync() {
        String[] barcodes = new String[]{"6900404523737", "6939006488885", "6940188805018", "6907469320189", "erghrh"};
        Mono<ByteBuf>[] monos = new Mono[barcodes.length];
        for (int i = 0, j = barcodes.length; i < j; i++) {
            monos[i] = query.findByBarcodeAsync(barcodes[i]);
        }
        ESItemQueryTest.printResult(monos);
    }

    @Test(invocationCount = 1, threadPoolSize = 1, priority = 2)
    public void testSearch() throws IOException {
        try (InputStream is = query.search(100, 30)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(0, 50, SortFieldEnum._BARCODE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("694")}, 0, 10, SortFieldEnum._BARCODE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("6934"), new CategorySpec(null)}, 10, 30, SortFieldEnum.BARCODE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("693"), new CategorySpec(new long[]{55307231199687601L}), new BrandSpec(-1L)}, 0, 10, SortFieldEnum._BARCODE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("692"), new RetailPriceSpec(1.1, 2), new LastReceiptPriceSpec(null, 1.2)}, 0, 9, SortFieldEnum._ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("693"), new CategorySpec(49680944612900409L)}, 50, 10, SortFieldEnum.ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("伊利")}, 10, 128, SortFieldEnum._RETAIL_PRICE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            System.out.println();
        }
    }

    @Test(invocationCount = 1, threadPoolSize = 2)
    public void testSearchAsync() {
        // 创建所有查询（不立即执行）
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync(100, 30),
                query.searchAsync(0, 50, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("694")}, 0, 10, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("6934"), new CategorySpec(null)}, 10, 30, SortFieldEnum._NAME),
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("693"), new CategorySpec(new long[]{55307231199687601L}), new BrandSpec(-1L)}, 0, 10, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("692"), new RetailPriceSpec(1.1, 2), new LastReceiptPriceSpec(null, 1.2)}, 0, 9, SortFieldEnum._ID),
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("693"), new CategorySpec(49680944612900409L)}, 50, 10, SortFieldEnum.ID),
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("伊利")}, 10, 128, SortFieldEnum._RETAIL_PRICE)
        };
        ESItemQueryTest.printResult(fluxes);
    }

    @Test(invocationCount = 1, threadPoolSize = 1, priority = 2)
    public void testSearchAfter() throws IOException {
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("6934")}, 60, null, SortFieldEnum._NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        /*
        try (InputStream is = query.search(50, "471019908050", SortFieldEnum.BARCODE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new CategorySpec(55307216538497678L)}, 50)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("693"), new CategorySpec(55307434890881701L)}, 20, null, SortFieldEnum._BARCODE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("6932"), new CategorySpec(55307434890881701L)}, 50)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("692"), new CategorySpec(new long[]{55307230903989168L}), new RetailPriceSpec(2.6, 25.5), new LastReceiptPriceSpec(1.1, 3)}, 15, null, SortFieldEnum._ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(new ItemQuerySpec[]{new KeywordSpec("伊利"), new KeywordSpec("690")}, 10, "258", SortFieldEnum._RETAIL_PRICE)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }

         */
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testSearchAfterAsync() {
        // 创建所有查询（不立即执行）
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("6934")}, 60, null, SortFieldEnum._NAME),
                //query.searchAsync(50, "471019908050", SortFieldEnum.BARCODE),
                //query.searchAsync(new ItemQuerySpec[]{new CategorySpec(55307216538497678L)}, 50),
                //query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("693"), new CategorySpec(55307434890881701L)}, 20, null, SortFieldEnum._BARCODE),
                //query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("6932"), new CategorySpec(55307434890881701L)}, 50),
                //query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("692"), new CategorySpec(new long[]{55307230903989168L}), new RetailPriceSpec(2.6, 25.5), new LastReceiptPriceSpec(1.1, 3)}, 15, null, SortFieldEnum._ID),
                //query.searchAsync(new ItemQuerySpec[]{new KeywordSpec("伊利"), new KeywordSpec("690")}, 10, "258", SortFieldEnum._RETAIL_PRICE)
        };
        ESItemQueryTest.printResult(fluxes);
    }

    private static void printResult(Flux<ByteBuf>[] fluxes) {
        int total = fluxes.length;
        CountDownLatch latch = new CountDownLatch(total);

        for (int i = 0; i < total; i++) {
            final int idx = i;
            fluxes[i].count()
                    .subscribeOn(Schedulers.parallel())
                    .subscribe(count -> System.out.println("\n[Query-" + idx + "]总共收到 " + count + " 个 ByteBuf\n"));
            fluxes[i]
                    // 👇 关键：加这一行 = 真正多线程并行执行
                    .subscribeOn(Schedulers.parallel())
                    .subscribe(
                            // 1. 正常数据
                            byteBuf -> {
                                try {
                                    String content = byteBuf.toString(StandardCharsets.UTF_8);
                                    System.out.println("[Query-" + idx + "] 接收数据：\n" + content);
                                } finally {
                                    byteBuf.release();
                                }
                            },
                            // 2. 异常回调（完整保留）
                            error -> {
                                System.err.println("[Query-" + idx + "] 异常：" + error.getClass().getSimpleName() + " - " + error.getMessage());
                                assert error instanceof IllegalArgumentException :
                                        "索引" + idx + " 必须抛出 IllegalArgumentException";
                                latch.countDown();
                            },
                            // 3. 完成回调（完整保留！）
                            () -> {
                                System.out.println("[Query-" + idx + "] 执行完成");
                                latch.countDown();
                            }
                    );
        }

        // 等待所有异步结束
        try {
            boolean done = latch.await(30, TimeUnit.SECONDS);
            if (!done) {
                System.err.println("异步请求超时！");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void printResult(Mono<ByteBuf>[] monos) {
        // 1. 使用 Mono.when 合并
        // 这会让数组里所有的 Mono 同时开始执行（并发）
        // 注意：Mono.when 会等待所有 Mono 完成，但只返回 Void (不返回具体数据)
        // 如果你想处理数据，需要用 Mono.zip 或者分别 subscribe
        // 这里为了演示简单且保留数据内容，我们改用 Flux.merge 或者分别订阅

        // 方案 A：如果只是想等它们都跑完（不管数据内容，或者数据内容已经在内部处理了）
        // Mono.when(monos).subscribe(() -> System.out.println("全部完成"));

        // 方案 B (推荐)：为了能看到每个 ID 的具体返回内容，我们利用 Flux.merge 把它们当成流处理
        // 或者简单地遍历订阅（这也是并发执行的）

        System.out.println(">>> 开始并发执行 " + monos.length + " 个 Mono 任务...");

        for (int i = 0; i < monos.length; i++) {
            final int index = i; // 用于日志标记
            monos[i].subscribe(
                    byteBuf -> {
                        // onNext
                        if (byteBuf != null) {
                            String content = byteBuf.toString(StandardCharsets.UTF_8);
                            System.out.println("[任务 " + index + "] 收到结果长度: " + content.length());
                            // System.out.println(content); // 如果需要看具体 JSON
                        }
                    },
                    error -> {
                        // onError
                        System.err.println("[任务 " + index + "] 发生错误: " + error.getMessage());
                    }
            );
        }
    }
}