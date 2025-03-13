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

import catalog.hoprxi.core.application.query.CategoryJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import org.testng.Assert;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-07-17
 */
public class ESCategoryJsonQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final CategoryJsonQuery service = new ESCategoryJsonQuery();

    @Test(expectedExceptions = QueryException.class)
    public void testQuery() {
        System.out.println(service.query(-1));
        Assert.assertNotNull(service.query(-1));
        Assert.assertNotNull(service.query(143));
        System.out.println(service.query(121));
        Assert.assertEquals("", service.query(19));
    }

    @Test
    public void testQueryByName() {
        System.out.println(service.queryByName("酒"));
        System.out.println(service.queryByName("白萝卜"));
        System.out.println(service.queryByName("wine"));
        System.out.println(service.queryByName("oil"));
    }

    @Test
    public void testQueryChildren() {
        System.out.println(service.queryChildren(151));
        System.out.println("\n");
        System.out.println(service.queryChildren(1514));
        System.out.println("\n");
        System.out.println(service.queryChildren(711));
        System.out.println("\n");
        System.out.println(service.queryChildren(1));
    }

    @Test(priority = 2)
    public void testQueryDescendant() {
        System.out.println(service.queryDescendant(1));
        Assert.assertNotNull(service.queryDescendant(-1));
        System.out.println(service.queryDescendant(14));
    }

    @Test
    public void testRoot() {
        System.out.println(service.root());
    }

    @Test
    public void testPath() {
        System.out.println(service.path(1514));
        System.out.println(service.path(-1));
    }
}