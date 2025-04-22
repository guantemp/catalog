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
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.application.query.filter.BrandFilterItem;
import catalog.hoprxi.core.application.query.filter.CategoryFilterItem;
import catalog.hoprxi.core.application.query.filter.KeywordFilter;
import catalog.hoprxi.core.application.query.filter.PriceRangFilter;
import org.testng.Assert;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-08-24
 */
public class EsItemJsonQueryTest {

    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final ItemJsonQuery service = new EsItemJsonQuery();


    @Test
    public void testQueryId() {
        System.out.println(service.query(62078192003431444l));
        Assert.assertNotNull(service.query(62078526044092825l));
        System.out.println(service.query(3635768734650054656l));
    }

    @Test
    public void testQueryPage() {
        System.out.println(service.query(100, 30));
        System.out.println(service.query(0, 20, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693")}, 0, 10, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(null)}, 0, 10, SortField.BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(new long[]{62078023226734874l}), new BrandFilterItem(62083691847465266l)}, 0, 10, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(new long[]{62078023226734874l}), new PriceRangFilter(PriceRangFilter.PriceType.RETAIL, 1.1, 2), new PriceRangFilter(PriceRangFilter.PriceType.LAST_RECEIPT, 1.1, 1.2)}, 15, 5, SortField.ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryFilterItem(new long[]{62078023226734874l})}, 50, 10, SortField.ID));
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
        System.out.println(service.query(new ItemQueryFilter[]{new CategoryFilterItem(62080074300112015l)}, 50, null, null));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(new long[]{62078023226734874l})}, 1, null, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryFilterItem(new long[]{62078023226734874l})}, 50, "", SortField.ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(new long[]{62078023226734874l}), new PriceRangFilter(PriceRangFilter.PriceType.RETAIL, 1.1, 2), new PriceRangFilter(PriceRangFilter.PriceType.LAST_RECEIPT, 1.1, 1.2)}, 50, "", SortField.ID));

    }

}