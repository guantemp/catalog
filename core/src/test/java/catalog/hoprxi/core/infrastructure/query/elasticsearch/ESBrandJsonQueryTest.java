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

import catalog.hoprxi.core.application.query.BrandJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.application.query.SortField;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.IOException;
import java.io.OutputStream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-06-15
 */
public class ESBrandJsonQueryTest {
    static {
        //String[] entyies = new String[]{"125.68.186.195:9200:P$Qwe123465El", "125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "https://slave.tooo.top:9200"};
        //Bootstrap.loadSecretKey("keystore.jks", "Qwe123465", new HashSet<>(Arrays.asList(entyies)));
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }
    private static final BrandJsonQuery service = new ESBrandJsonQuery();

    @Test(priority = 1, invocationCount = 1, threadPoolSize = 1, expectedExceptions = QueryException.class)
    public void testQuery() {
        System.out.println(service.query(495651176959596552l));
        System.out.println(service.query(495651176959596602l));
        System.out.println(service.query(62078079843547680l));
        System.out.println(service.query(-1l));
        System.out.println(service.query(817884324788650l));
    }

    @Test(priority = 2, invocationCount = 1, threadPoolSize = 1)
    public void testQueryForTest() throws IOException {
        OutputStream os = ((ESBrandJsonQuery) service).queryForTest("495651176959596552");
        System.out.println(os);
        os.close();
        os = ((ESBrandJsonQuery) service).queryForTest(" 495651176959596602");
        System.out.println(os);
        os.close();
        os = ((ESBrandJsonQuery) service).queryForTest("-1");
        System.out.println(os);
        os.close();
        os = ((ESBrandJsonQuery) service).queryForTest("817884324788650");
        System.out.println(os);
        if (os != null)
            os.close();
    }

    @Test(priority = 3)
    public void testQueryAll() {
        System.out.println(service.query(100, 5, null));
        System.out.println(service.query(100, 5, SortField._NAME));
        System.out.println(service.query(0, 128, SortField._ID));
        System.out.println(service.query(16, "", SortField.NAME));
        System.out.println(service.query(16, "xcx", SortField._NAME));
    }

    @Test
    public void testQueryByName() {
        System.out.println(service.query("天", 0, 20, null));
        System.out.println(service.query("白萝卜", 10, 5, null));
        System.out.println(service.query("天", 0, 20, SortField.NAME));
    }
}