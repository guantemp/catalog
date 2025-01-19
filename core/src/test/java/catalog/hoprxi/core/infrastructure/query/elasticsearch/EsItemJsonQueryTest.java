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
import catalog.hoprxi.core.application.query.QueryFilter;
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
        System.out.println(service.query("62078192003431444"));
        Assert.assertNotNull(service.query("62078526044092825"));
        System.out.println(service.query("3635768734650054656"));
    }

    @Test
    public void testQueryFilter() {
        System.out.println(service.query("693", new QueryFilter[]{new CategoryFilter(new String[]{"-1", "62078023226734874"})}, 1, new String[0], SortField.BARCODE_DESC));
        System.out.println(service.query("6931", new QueryFilter[]{new CategoryFilter(new String[]{"-1", "62078023226734874"})}, 100, new String[0], SortField.ID_ASC));
    }

    @Test
    public void testAccurateQueryByBarcode() {
        System.out.println(service.accurateQueryByBarcode("6902779313692"));
        System.out.println(service.accurateQueryByBarcode("6920208924028"));
    }

    @Test
    public void testQueryAll() {
        System.out.println(service.queryAll(500, new String[0]));
    }
}