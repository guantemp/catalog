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

import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.SearchException;
import io.netty.buffer.ByteBuf;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ESCategoryQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "129.28.29.105:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final CategoryQuery query = new ESCategoryQuery();

    @Test
    public void testRoot() throws IOException {
        try (InputStream is = query.root()) {
            System.out.println("root:\n" + new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testRootAsync() {
        Flux<ByteBuf> flux = query.rootAsync();
        PrintUtil.printFlux(new Flux[]{flux});
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

    @Test
    public void testFindAsync() {
        Mono<ByteBuf>[] monos = new Mono[]{
                query.findAsync(-1L),
                query.findAsync(143),
                query.findAsync(121),
                query.findAsync(496796322118291482L),
                query.findAsync(49679632211829142L),
        };
        PrintUtil.printMono(monos);
    }


    @Test
    public void testChildren() throws IOException {
        try (InputStream is = query.children(151)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.children(1514)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.children(1)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.children(711)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testChildrenAsync() throws IOException {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.childrenAsync(151),
                query.childrenAsync(1514),
                query.childrenAsync(1),
                query.childrenAsync(711),
        };
       PrintUtil.printFlux(fluxes);
    }


    @Test
    public void testDescendantsAsync() {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.descendantsAsync(1),
                query.descendantsAsync(14)
        };
        PrintUtil.printFlux(fluxes);
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
    public void testSearchAsync() {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync("酒"),
                query.searchAsync("白萝卜"),
                query.searchAsync("wine"),
                query.searchAsync("oil"),
                query.searchAsync("oil", 1, 2),
                query.searchAsync(null, 0, 1)
        };
        PrintUtil.printFlux(fluxes);
    }

    @Test
    public void testSearchSiblings() {
    }

    @Test
    public void testPath() throws IOException {
        try (InputStream is = query.path(1513)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream is = query.path(-1)) {
            System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testPathAsync() throws IOException {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.pathAsync(1513),
                query.pathAsync(-1),
                query.pathAsync(1513465)
        };
        PrintUtil.printFlux(fluxes);
    }

}