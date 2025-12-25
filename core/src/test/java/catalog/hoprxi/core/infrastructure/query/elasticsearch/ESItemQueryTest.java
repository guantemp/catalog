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
import org.testng.Assert;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    @Test(invocationCount = 256, threadPoolSize = 2, priority = 2)
    public void testFindAsync() throws ExecutionException, InterruptedException, IOException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());

        long[] ids = {51746812605656589L, 51748312021100428L, 51748057162606289L, 517480571626062891L};
        CompletableFuture<InputStream> f1 = query.findAsync(ids[0]);
        CompletableFuture<InputStream> f2 = query.findAsync(ids[1]);
        CompletableFuture<InputStream> f3 = query.findAsync(ids[2]);
        CompletableFuture<InputStream> f4 = query.findAsync(ids[3]);
        try {
            CompletableFuture.allOf(f1, f2, f3, f4).get(3, TimeUnit.SECONDS); // 可能抛出 ExecutionException
        } catch (Throwable t) {
            // 捕获异常，继续执行
        }

        try (InputStream is = f1.get()) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = f2.get()) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = f3.get()) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }

    }

    @Test(invocationCount = 512, threadPoolSize = 8, priority = 2)
    public void testFind() throws IOException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());
        try (InputStream is = query.find(51746812605656589L)) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = query.find(51748312021100428L)) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = query.find(51748057162606289L)) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
    }

    @Test
    public void testFindByBarcode() throws IOException {
        try (InputStream is = query.findByBarcode("6900404523737");) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = query.findByBarcode("6939006488885");) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = query.findByBarcode("6940188805018");) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }

        try (InputStream is = query.findByBarcode("690158611081"); InputStream is1 = query.findByBarcode("dsgf");
             InputStream is2 = query.findByBarcode(""); InputStream is3 = query.findByBarcode(null);) {
            Assert.fail("Expected exception but none thrown!"); // 未抛出异常时失败
        } catch (IllegalArgumentException e) {
            // 验证异常信息
            Assert.assertEquals(e.getMessage(), "Not valid barcode ctr");
        }
    }

    @Test(invocationCount = 256, threadPoolSize = 2, priority = 2)
    public void testFindByBarcodeAsync() throws IOException, ExecutionException, InterruptedException {
        System.out.println("➡️ Started on thread: " + Thread.currentThread().getName());
        String[] barcodes = {"6900404523737", "6939006488885", "6940188805018"};
        CompletableFuture<InputStream> f1 = query.findByBarcodeAsync(barcodes[0]);
        CompletableFuture<InputStream> f2 = query.findByBarcodeAsync(barcodes[1]);
        CompletableFuture<InputStream> f3 = query.findByBarcodeAsync(barcodes[2]);
        try {
            CompletableFuture.allOf(f1, f2, f3).get(5, TimeUnit.SECONDS); // 可能抛出 ExecutionException
        } catch (Throwable t) {
            System.out.println(t); // 捕获异常，继续执行
        }

        try (InputStream is = f1.get()) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = f2.get()) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
        try (InputStream is = f3.get()) {
            String s = inputStreamToString(is);
            System.out.println(s);
        }
    }

    @Test
    public void testSearch() throws IOException {
        InputStream is = query.search(100, 30);
        String s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(0, 50, SortFieldEnum._BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("694")}, 0, 10, SortFieldEnum._BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);

        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(null)}, 10, 15, SortFieldEnum.BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(new long[]{49680933986631205L}), new BrandIdFilter(-1L)}, 0, 10, SortFieldEnum._BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("692"), new RetailPriceFilter(1.1, 2), new LastReceiptPriceFilter(null, 1.2)}, 0, 9, SortFieldEnum._ID);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryIdFilter(49680944612900409L)}, 50, 10, SortFieldEnum.ID);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("伊利")}, 10, 256, SortFieldEnum._RETAIL_PRICE);
        s = inputStreamToString(is);
        System.out.println(s);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testSearchAfter() throws IOException {
        InputStream is = query.search(50, "9588868020855", SortFieldEnum.BARCODE);
        String s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new CategoryIdFilter(49681151224315522L)}, 50);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(49680933986631205L)}, 1, null, SortFieldEnum._BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("6932"), new CategoryIdFilter(49680933986631205L)}, 50);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("692"), new CategoryIdFilter(new long[]{49680944612900409L}), new RetailPriceFilter(2.6, 25.5), new LastReceiptPriceFilter(1.1, 3)}, 5, null, SortFieldEnum._ID);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("伊利"), new KeywordFilter("690")}, 10, "258", SortFieldEnum._RETAIL_PRICE);
        s = inputStreamToString(is);
        System.out.println(s);
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}