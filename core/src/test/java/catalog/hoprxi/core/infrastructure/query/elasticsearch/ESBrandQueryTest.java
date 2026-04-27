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

import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import io.netty.buffer.ByteBuf;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-06-15
 */
public class ESBrandQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "129.28.29.105:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final BrandQuery query = new ESBrandQuery();

    @Test(priority = 1, invocationCount = 1, threadPoolSize = 1, expectedExceptions = SearchException.class)
    public void testFind() throws IOException {
        Long[] ids = new Long[]{495651176959596552L, 495651176959596602L, -1L, 55307444711845017L, 55308342812993069L, 19L};
        for (Long id : ids) {
            try (InputStream is = query.find(id)) {
                System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFindAsync() {
        Mono<ByteBuf>[] monos = new Mono[]{
                query.findAsync(495651176959596552L),
                query.findAsync(495651176959596602L),
                query.findAsync(55307444711845017L),
                query.findAsync(-1L),
                query.findAsync(55308342812993069L)
        };
        ESBrandQueryTest.printResult(monos);
    }

    @Test(priority = 2)
    public void testSearch() throws IOException {
        try (InputStream is = query.search(100, 15)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(10, 5, SortFieldEnum._NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(0, 64, SortFieldEnum._ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(64, null, SortFieldEnum.NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(128, "55307914039782154", SortFieldEnum.ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("", 8, "62078807563681609", SortFieldEnum.ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("天", 0, 20)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("天", 0, 20, SortFieldEnum.NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("白萝卜", 10, 5)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(priority = 3)
    public void testSearchAsync() {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync("", 100, 15, SortFieldEnum._ID),
                query.searchAsync(null, 100, 30, SortFieldEnum._NAME),
                query.searchAsync("", 0, 64, SortFieldEnum._ID),
                query.searchAsync("天", 0, 20, SortFieldEnum.NAME),
                query.searchAsync("天", 0, 20,SortFieldEnum._ID),
                query.searchAsync("白萝卜", 10, 5, SortFieldEnum.NAME),
                query.searchAsync("", 8, "62078807563681609", SortFieldEnum.ID),
                query.searchAsync("", 128, "495651176959596546", SortFieldEnum.ID),
                query.searchAsync("", 64, null, SortFieldEnum.NAME)
        };
        ESBrandQueryTest.printResult(fluxes);
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
                            System.out.println(content); // 如果需要看具体 JSON
                        }
                    },
                    error -> {
                        // onError
                        System.err.println("[任务 " + index + "] 发生错误: " + error.getMessage());
                    }
            );
        }
    }

    private static void printResult(Flux<ByteBuf>[] fluxes) {
        int total = fluxes.length;
        CountDownLatch latch = new CountDownLatch(total);

        // 2. 遍历订阅，使用 Reactor 异步机制，绝对不要手动 new Thread
        for (int i = 0; i < total; i++) {
            final int idx = i;
            fluxes[i]
                    // 关键：指定订阅线程池（避免阻塞主线程）
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .subscribe(
                            byteBuf -> {
                                // 正常处理数据
                                try {
                                    String content = byteBuf.toString(StandardCharsets.UTF_8);
                                    System.out.println("[Query-" + idx + "] 接收数据：" + content);
                                } finally {
                                    byteBuf.release(); // 必须释放
                                }
                            },
                            error -> {
                                System.err.println("[Query-" + idx + "] 异常：" + error.getMessage());
                                error.printStackTrace();
                                latch.countDown(); // 异常也要倒计时！
                            },
                            () -> {
                                System.out.println("[Query-" + idx + "] 执行完成");
                                latch.countDown(); // 只有完成才倒计时
                            }
                    );
        }

        // 3. 等待（必须加超时，防止永久卡死）
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("测试超时！存在未完成的异步请求");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待被中断", e);
        }
    }
}