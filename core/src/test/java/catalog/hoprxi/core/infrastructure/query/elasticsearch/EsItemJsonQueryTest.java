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
    public void testQuery() {
        System.out.println(service.query(62078192003431444l));
        Assert.assertNotNull(service.query(62078526044092825l));
        System.out.println(service.query(3635768734650054656l));
    }

    @Test
    public void testQueryKey() {
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(new String[]{"62078023226734874"})}, 1, null, SortField._BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("6931"), new CategoryFilterItem(new String[]{"62078023226734874"})}, 50, "", SortField.ID));
        System.out.println(service.query(new ItemQueryFilter[]{new KeywordFilter("693"), new CategoryFilterItem(new String[]{"62078023226734874"}), new PriceRangFilter(PriceRangFilter.PriceType.RETAIL, 1.1, 2), new PriceRangFilter(PriceRangFilter.PriceType.LAST_RECEIPT, 1.1, 1.2)}, 50, "", SortField.ID));
    }

    @Test
    public void testAccurateQueryByBarcode() {
        System.out.println(service.queryByBarcode("6902779313692"));
        System.out.println(service.queryByBarcode("6920208924028"));
    }

    @Test
    public void testQuerySearchAfter() {
        System.out.println(service.query(50, "9588868020855", SortField.BARCODE));
        System.out.println(service.query(new ItemQueryFilter[]{new CategoryFilterItem(new String[]{"62080074300112015"})}, 50, null, null));
    }

    @Test
    public void testQueryFrom() {
        System.out.println(service.query(2, 48, null));
        System.out.println(service.query(0, 50, SortField._BARCODE));
    }
}