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
    public void testFindAsync() throws InterruptedException {
        Mono<ByteBuf>[] monos = new Mono[]{
                query.findAsync(495651176959596552L),
                query.findAsync(495651176959596602L),
                query.findAsync(55307444711845017L),
                query.findAsync(-1L),
                query.findAsync(55308342812993069L)
        };
        PrintUtil.printMono(monos);
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
        try (InputStream is = query.search("天", 0, 200)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.search("天", 0, 30, SortFieldEnum.NAME)) {
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
        PrintUtil.printFlux(fluxes);
    }
}