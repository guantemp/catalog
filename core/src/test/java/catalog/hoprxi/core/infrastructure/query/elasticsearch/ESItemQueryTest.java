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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;

import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.filter.*;
import io.netty.buffer.ByteBuf;
import org.testng.Assert;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-08-24
 */
public class ESItemQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:9200"});
    }

    private static final ItemQuery query = new ESItemQuery();

    @Test(invocationCount = 512, threadPoolSize = 2, priority = 2)
    public void testFindAsync() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());

        long[] ids = {51746812605656589L, 51748312021100428L, 51748057162606289L};

        // 使用固定线程池或虚拟线程（Java 21+）
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 21+
        // 或：Executors.newFixedThreadPool(ids.length);
        CompletableFuture<Void>[] futures = new CompletableFuture[ids.length];

        for (int i = 0; i < ids.length; i++) {
            final long id = ids[i];
            futures[i] = CompletableFuture.runAsync(() -> {
                System.out.println("Starting request for ID: " + id);

                StringBuilder result = new StringBuilder();
                Throwable[] errorHolder = new Throwable[1];
                boolean[] completed = {false};

                // 使用 CountDownLatch 等待单个 Flux 完成
                var latch = new java.util.concurrent.CountDownLatch(1);

                Flux<ByteBuf> flux = query.findAsync(id); // 或 service.findAsynca(id)

                flux.subscribe(
                        byteBuf -> {
                            try {
                                String chunk = byteBuf.toString(StandardCharsets.UTF_8);
                                synchronized (result) {
                                    result.append(chunk);
                                }
                                System.out.println("[ID=" + id + "] Chunk: " + chunk);
                            } catch (Exception e) {
                                errorHolder[0] = e;
                                latch.countDown();
                            }
                        },
                        error -> {
                            errorHolder[0] = error;
                            System.err.println("[ID=" + id + "] Error: " + error.getMessage());
                            latch.countDown();
                        },
                        () -> {
                            completed[0] = true;
                            System.out.println("[ID=" + id + "] Completed.");
                            latch.countDown();
                        }
                );

                try {
                    boolean finished = latch.await(3, TimeUnit.SECONDS);
                    if (!finished) {
                        throw new RuntimeException("Timeout for ID " + id);
                    }

                    if (errorHolder[0] != null) {
                        throw new RuntimeException("Request failed for ID " + id, errorHolder[0]);
                    }

                    String fullOutput = result.toString();
                    System.out.println("\n[ID=" + id + "] === FULL OUTPUT ===");
                    System.out.println(fullOutput);
                    System.out.println("[ID=" + id + "] ===================\n");

                    // 基本验证
                    Assert.assertTrue(fullOutput.contains("_source") || fullOutput.startsWith("{"),
                            "Response should be valid JSON for ID " + id);
                    Assert.assertFalse(fullOutput.contains("_meta"),
                            "Response must NOT contain '_meta' for ID " + id);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for ID " + id, e);
                }
            }, executor);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures).get(3, TimeUnit.SECONDS);

        executor.shutdownNow();
    }

    @Test(invocationCount = 512, threadPoolSize = 5, priority = 2)
    public void testFind() throws IOException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());
        try (InputStream is = query.find(51746812605656589L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(51748312021100428L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(51748057162606289L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(invocationCount = 512, threadPoolSize = 6, priority = 2)
    public void testFindByBarcode() throws IOException {
        try (InputStream is = query.findByBarcode("6900404523737");) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.findByBarcode("6939006488885");) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.findByBarcode("6940188805018");) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }

        try (InputStream is = query.findByBarcode("690158611081"); InputStream is1 = query.findByBarcode("dsgf");
             InputStream is2 = query.findByBarcode(""); InputStream is3 = query.findByBarcode(null);) {
            Assert.fail("Expected exception but none thrown!"); // 未抛出异常时失败
        } catch (IllegalArgumentException e) {
            // 验证异常信息
            Assert.assertEquals(e.getMessage(), "Not valid barcode ctr");
        }
    }

    @Test(invocationCount = 512, threadPoolSize = 2, priority = 2)
    public void testFindByBarcodeAsync() throws InterruptedException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());
        StringBuilder result = new StringBuilder();
        String[] barcodes = {"6900404523737", "6939006488885", "69401888050182"};
        // Java 21+
        CompletableFuture<Void>[] futures = new CompletableFuture[barcodes.length];
        var latch = new java.util.concurrent.CountDownLatch(1);
        for (int i = 0; i < barcodes.length; i++) {
            final String barcode = barcodes[i];
            futures[i] = CompletableFuture.runAsync(() -> {
                Flux<ByteBuf> flux = query.findByBarcodeAsync(barcode);
                flux.doOnComplete(latch::countDown).subscribe(
                        byteBuf -> {
                            result.append(byteBuf.toString(StandardCharsets.UTF_8));
                        }, error -> {
                            System.err.println("[ barcode=" + barcode + "] Error: " + error.getMessage());

                        }, () -> {
                            System.out.println("[barCODE=" + barcode + "] Completed.");
                            System.out.println(result);
                        });
            });
        }
        latch.await();
    }

    @Test(invocationCount = 128, threadPoolSize = 2, priority = 2)
    public void testSearch() throws IOException {
        InputStream is = query.search(100, 30);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(0, 50, SortFieldEnum._BARCODE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("694")}, 0, 10, SortFieldEnum._BARCODE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));

        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilter(null)}, 10, 15, SortFieldEnum.BARCODE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilter(new long[]{49680933986631205L}), new BrandFilter(-1L)}, 0, 10, SortFieldEnum._BARCODE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("692"), new RetailPriceFilter(1.1, 2), new LastReceiptPriceFilter(null, 1.2)}, 0, 9, SortFieldEnum._ID);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryFilter(49680944612900409L)}, 50, 10, SortFieldEnum.ID);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("伊利")}, 10, 256, SortFieldEnum._RETAIL_PRICE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test(invocationCount = 128, threadPoolSize = 1)
    public void testSearchAsync() throws InterruptedException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());

        // 创建所有查询（不立即执行）
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync(100, 30),
                query.searchAsync(0, 50, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("694")}, 0, 10, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilter(null)}, 10, 15, SortFieldEnum.BARCODE),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilter(new long[]{49680933986631205L}), new BrandFilter(-1L)}, 0, 10, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("692"), new RetailPriceFilter(1.1, 2), new LastReceiptPriceFilter(null, 1.2)}, 0, 9, SortFieldEnum._ID),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryFilter(49680944612900409L)}, 50, 10, SortFieldEnum.ID),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("伊利")}, 10, 256, SortFieldEnum._RETAIL_PRICE)
        };

        CountDownLatch latch = new CountDownLatch(fluxes.length);
        // 为每个查询在独立线程中启动订阅
        for (int i = 0; i < fluxes.length; i++) {
            StringBuilder sb = new StringBuilder();
            final int queryIndex = i;
            new Thread(() -> {
                System.out.println("[Thread-" + Thread.currentThread().getId() + "] Starting query #" + queryIndex);
                fluxes[queryIndex].subscribe(
                        byteBuf -> {
                            try {
                                sb.append(byteBuf.toString(StandardCharsets.UTF_8));
                            } finally {
                                byteBuf.release(); // 安全释放
                            }
                        },
                        error -> {
                            System.err.println("[Thread-" + Thread.currentThread().getId() + "] Error: " + error.getMessage());
                            error.printStackTrace();
                        },
                        () -> {
                            System.out.println("[Thread-" + Thread.currentThread().getId() + "] Query #" + queryIndex + " completed");
                            System.out.println(sb);
                            latch.countDown();
                        }
                );
            }).start();
        }

        // 等待所有查询完成（30秒超时）
        latch.await();
    }

    @Test(invocationCount = 256, threadPoolSize = 2, priority = 2)
    public void testSearchAfter() throws IOException {
        InputStream is = query.search(50, "471019908050", SortFieldEnum.BARCODE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new CategoryFilter(49681151224315522L)}, 50);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilter(49680933986631205L)}, 1, null, SortFieldEnum._BARCODE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("6932"), new CategoryFilter(49680933986631205L)}, 50);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("692"), new CategoryFilter(new long[]{49680944612900409L}), new RetailPriceFilter(2.6, 25.5), new LastReceiptPriceFilter(1.1, 3)}, 5, null, SortFieldEnum._ID);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("伊利"), new KeywordFilter("690")}, 10, "258", SortFieldEnum._RETAIL_PRICE);
        System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test(invocationCount = 256, threadPoolSize = 1)
    public void testSearchAfterAsync() throws InterruptedException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());

        // 创建所有查询（不立即执行）
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync(50, "471019908050", SortFieldEnum.BARCODE),
                query.searchAsync(new ItemQueryFilter[]{new CategoryFilter(49681151224315522L)}, 50),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilter(49680933986631205L)}, 1, null, SortFieldEnum._BARCODE),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("6932"), new CategoryFilter(49680933986631205L)}, 50),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("692"), new CategoryFilter(new long[]{49680944612900409L}), new RetailPriceFilter(2.6, 25.5), new LastReceiptPriceFilter(1.1, 3)}, 5, null, SortFieldEnum._ID),
                query.searchAsync(new ItemQueryFilter[]{new KeywordFilter("伊利"), new KeywordFilter("690")}, 10, "258", SortFieldEnum._RETAIL_PRICE)
        };

        CountDownLatch latch = new CountDownLatch(fluxes.length);
        // 为每个查询在独立线程中启动订阅
        for (int i = 0; i < fluxes.length; i++) {
            StringBuilder sb = new StringBuilder();
            final int queryIndex = i;
            new Thread(() -> {
                System.out.println("[Thread-" + Thread.currentThread().getId() + "] Starting query #" + queryIndex);

                fluxes[queryIndex].subscribe(
                        byteBuf -> {
                            try {
                                sb.append(byteBuf.toString(StandardCharsets.UTF_8));
                            } finally {
                                byteBuf.release(); // 安全释放
                            }
                        },
                        error -> {
                            System.err.println("[Thread-" + Thread.currentThread().getId() + "] Error: " + error.getMessage());
                            error.printStackTrace();
                        },
                        () -> {
                            System.out.println("[Thread-" + Thread.currentThread().getId() + "] Query #" + queryIndex + " completed");
                            System.out.println(sb);
                            latch.countDown();
                        }
                );
            }).start();
        }

        // 等待所有查询完成（30秒超时）
        boolean completed = latch.await(30, TimeUnit.SECONDS);
    }
}