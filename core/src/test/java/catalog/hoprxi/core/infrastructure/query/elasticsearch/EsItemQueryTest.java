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

import catalog.hoprxi.core.application.query.ItemJsonQuery;
import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.application.query.filter.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-08-24
 */
public class EsItemQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:9200"});
    }

    private static final ItemJsonQuery service = new EsItemJsonQuery();
    private static final ItemQuery query = new EsItemQuery();


    @Test
    public void testFind() throws IOException {
        InputStream is = query.find(51746812605656589L);
        String s = inputStreamToString(is);
        System.out.println(s);
        is = query.find(51748312021100428L);
        s = inputStreamToString(is);
        System.out.println(s);
        is = query.find(51748057162606289L);
        s = inputStreamToString(is);
        System.out.println(s);
    }

    @Test
    public void testSearchPage() throws IOException {
        System.out.println(service.query(100, 30));
        /*


        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(new long[]{62078021456738571L}), new BrandIdFilter(-1L)}, 0, 10, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(new long[]{62078023226734874L}), new RetailPriceFilter(1.1, 2), new LastReceiptPriceFilter(null, 1.2)}, 0, 9, SortField._ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryIdFilter(new long[]{62078023226734874L})}, 50, 10, SortField.ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("伊利")}, 10, 1, SortField._RETAIL_PRICE));
         */
        InputStream is = query.search(100, 30);
        String s = inputStreamToString(is);
        System.out.println(s);

        System.out.println(service.query(0, 50, SortField._BARCODE));
        is = query.search(0, 50, SortField._BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);

        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693")}, 0, 10, SortField._BARCODE));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693")}, 0, 10, SortField._BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);

        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(null)}, 0, 10, SortField.BARCODE));
        is = query.search(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(-1L)}, 0, 10, SortField.BARCODE);
        s = inputStreamToString(is);
        System.out.println(s);
    }

    @Test
    public void testQueryByBarcode() {
        System.out.println(service.queryByBarcode("6900404523737"));
        System.out.println(service.queryByBarcode("6901028025102"));
        System.out.println(service.queryByBarcode("6901586110814"));
        try {
            service.queryByBarcode("690158611081");
            Assert.fail("Expected exception but none thrown!"); // 未抛出异常时失败
        } catch (IllegalArgumentException e) {
            // 验证异常信息
            Assert.assertEquals(e.getMessage(), "Not valid barcode ctr");
        }
        try {
            service.queryByBarcode("dsgf");
            Assert.fail("Expected exception but none thrown!"); // 未抛出异常时失败
        } catch (IllegalArgumentException e) {
            // 验证异常信息
            Assert.assertEquals(e.getMessage(), "Not valid barcode ctr");
        }
        try {
            service.queryByBarcode("");
            Assert.fail("Expected exception but none thrown!"); // 未抛出异常时失败
        } catch (IllegalArgumentException e) {
            // 验证异常信息
            Assert.assertEquals(e.getMessage(), "Not valid barcode ctr");
        }
        try {
            service.queryByBarcode(null);
            Assert.fail("Expected exception but none thrown!"); // 未抛出异常时失败
        } catch (IllegalArgumentException e) {
            // 验证异常信息
            Assert.assertEquals(e.getMessage(), "Not valid barcode ctr");
        }
    }

    @Test
    public void testQueryPageSearchAfter() {
        System.out.println(service.query(50, "9588868020855", SortField.BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new CategoryIdFilter(62080074300112015l)}, 50, null, null));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(new long[]{62078023226734874l})}, 1, null, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryIdFilter(new long[]{62078023226734874l})}, 50, "", SortField.ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryIdFilter(new long[]{62078023226734874l}), new RetailPriceFilter(1.1, 2), new LastReceiptPriceFilter(1.1, 1.2)}, 50, null, SortField._ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("伊利")}, 10, "258", SortField._RETAIL_PRICE));
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