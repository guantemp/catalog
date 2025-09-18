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
import catalog.hoprxi.core.application.query.SortField;
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
 * @version 0.0.1 builder 2024-06-15
 */
public class ESBrandJsonQueryTest {
    static {
        //String[] entyies = new String[]{"125.68.186.195:9200:P$Qwe123465El", "125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "https://slave.tooo.top:9200"};
        //Bootstrap.loadSecretKey("keystore.jks", "Qwe123465", new HashSet<>(Arrays.asList(entyies)));
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "129.28.29.105:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final ESBrandJsonQuery jsonQuery = new ESBrandJsonQuery();
    private static final BrandQuery query = new ESBrandQuery();


    @Test(priority = 2, invocationCount = 1, threadPoolSize = 1, expectedExceptions = SearchException.class)
    public void testJsonQuery() {
        String w = jsonQuery.query(495651176959596552l);
        System.out.println(w);
        System.out.println(w.length());
        w = jsonQuery.query(495651176959596602l);
        System.out.println(w);
        System.out.println(w.length());
        System.out.println(jsonQuery.query(62078079843547680l));
        System.out.println(jsonQuery.query(-1l));
        System.out.println(jsonQuery.query(817884324788650l));
    }

    @Test(priority = 1, invocationCount = 1, threadPoolSize = 1, expectedExceptions = SearchException.class)
    public void testFind() throws IOException {
        InputStream is = query.find(495651176959596552l);
        System.out.println("id查询（过滤）：");
        String s = inputStreamToString(is);
        System.out.println(s);
        System.out.println(s.length());

        is = query.find(495651176959596602l);
        System.out.println("id查询（过滤）：");
        System.out.println(is.readAllBytes().length);
        is = query.find(-1l);
        System.out.println("id查询（过滤）：");
        System.out.println(is.readAllBytes().length);

        is = query.find(817884324788650l);
        System.out.println("id查询（过滤）：");
        System.out.println(is.readAllBytes().length);
    }

    @Test(priority = 2)
    public void testSearchAll1() throws IOException {
        InputStream is = query.search(100, 5);
        String s = inputStreamToString(is);
        System.out.println(s);
        System.out.println(s.length());
        query.search(100, 5, SortField._NAME);
        is =query.search(0, 256, SortField._ID);
        s = inputStreamToString(is);
        System.out.println(s);
        System.out.println(s.length());
        query.search(64, null, SortField.NAME);
        is = query.search(128, "62078470412941622", SortField.ID);
        s = inputStreamToString(is);
        System.out.println(s);
        System.out.println(s.length());
        query.search("", 8, "62078807563681609", SortField.ID);
    }

    @Test(priority = 1)
    public void testJsonQueryAll() {
        System.out.println(jsonQuery.query(100, 5, null));
        System.out.println(jsonQuery.query(100, 5, SortField._NAME));
        System.out.println(jsonQuery.query(0, 256, SortField._ID));
        System.out.println(jsonQuery.query(64, "", SortField.NAME));
        System.out.println(jsonQuery.query(128, "62078470412941622", SortField.ID));
        System.out.println(jsonQuery.query("", 8, "62078807563681609", SortField.ID));
    }

    @Test(priority =3)
    public void testSearchName() throws IOException {
        InputStream is = query.search("天", 0, 20);
        String s = inputStreamToString(is);
        System.out.println(s);
        System.out.println(s.length());

        query.search("白萝卜", 10, 5);
        is = query.search("天", 0, 20, SortField.NAME);
        s = inputStreamToString(is);
        System.out.println(s);
        System.out.println(s.length());
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

    @Test(priority = 3)
    public void testJsonQueryName() {
        System.out.println(jsonQuery.query("天", 0, 20, null));
        System.out.println(jsonQuery.query("白萝卜", 10, 5, null));
        System.out.println(jsonQuery.query("天", 0, 20, SortField.NAME));
    }
}