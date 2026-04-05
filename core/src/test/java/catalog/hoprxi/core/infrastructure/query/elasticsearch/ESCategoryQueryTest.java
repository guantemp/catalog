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

import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.NotFoundException;
import catalog.hoprxi.core.application.query.SearchException;
import io.netty.buffer.ByteBuf;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ESCategoryQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "129.28.29.105:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final CategoryQuery query = new ESCategoryQuery();

    @Test
    public void testRoot() throws IOException {
        InputStream is = query.root();
        System.out.println("root:\n" + new String(is.readAllBytes(), StandardCharsets.UTF_8));
        Flux<ByteBuf> flux = query.rootAsync();
        String json = flux
                .map(buf -> buf.toString(StandardCharsets.UTF_8))
                .collect(Collectors.joining())
                .block();
        System.out.println(json);
    }

    @Test(expectedExceptions = SearchException.class)
    public void testFind() throws IOException {
        try (InputStream is = query.find(-1L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(143)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(121)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(496796322118291482L)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.find(19)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testFindAsync() {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.findAsync(121),
                query.findAsync(496796322118291482L),
                query.findAsync(143),
                query.findAsync(-1L)
        };
        printresult(fluxes);
        query.findAsync(19L).blockFirst();
    }

    private static void printresult(Flux<ByteBuf>[] fluxes) {
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

    @Test
    public void testChildren() throws IOException {
        InputStream is = query.children(151);
        String s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
        is = query.children(1514);
        s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
        is = query.children(711);
        s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
        is = query.children(1);
        s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
    }

    @Test
    public void testDescendants() throws IOException {
        InputStream is = query.descendants(1);
        String s = inputStreamToString(is);
        System.out.println("Descendants:\n" + s);
        is = query.descendants(14);
        s = inputStreamToString(is);
        System.out.println("Descendants:\n" + s);
    }

    @Test
    public void testSearch() throws IOException {
        try (InputStream is = query.search("酒")) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("白萝卜")) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("wine")) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("oil")) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("oil", 1, 2)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search(null, 0, 1)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testSearchAysnc() {

    }

    @Test
    public void testSearchSiblings() {
    }

    @Test
    public void testPath() throws IOException {
        InputStream is = query.path(1514);
        String s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
        is = query.path(-1);
        s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192]; // 8KB 缓冲区
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
        }
        return sb.toString();
    }

}