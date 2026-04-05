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
    public void testFind() throws IOException, InterruptedException {
        try (InputStream is = query.find(495651176959596552L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(495651176959596602L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(-1L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(55307444711845017L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }

        try (InputStream is = query.find(817884324788650L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFindAsync() throws InterruptedException {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.findAsync(495651176959596552L),
                query.findAsync(495651176959596602L),
                query.findAsync(55307444711845017L),
                query.findAsync(-1L),
                query.findAsync(817884324788650L)
        };

        printResult(fluxes);
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

    @Test(priority = 2)
    public void testSearchAll() throws IOException {
        try (InputStream is = query.search(100, 15)) {
            System.out.println("offset=100,size=5");
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(100, 5, SortFieldEnum._NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(0, 256, SortFieldEnum._ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(64, null, SortFieldEnum.NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(128, "62078470412941622", SortFieldEnum.ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("", 8, "62078807563681609", SortFieldEnum.ID)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(priority = 3)
    public void testSearchName() throws IOException {
        try (InputStream is = query.search("天", 0, 20)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("白萝卜", 10, 5)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("天", 0, 20, SortFieldEnum.NAME)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}