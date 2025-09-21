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

import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.SearchException;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.*;

public class ESCategoryQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "129.28.29.105:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final CategoryQuery query = new ESCategoryQuery();

    @Test
    public void testRoot() throws IOException {
        InputStream is = query.root();
        String s = inputStreamToString(is);
        System.out.println("root:\n" + s);
        System.out.println(s.length());
    }

    @Test(expectedExceptions = SearchException.class)
    public void testFind() throws IOException {
        InputStream is = query.find(-1);
        String s = inputStreamToString(is);
        System.out.println("find:\n" + s);
        is = query.find(143);
        s = inputStreamToString(is);
        System.out.println("find:\n" + s);
        is = query.find(121);
        s = inputStreamToString(is);
        System.out.println("find:\n" + s);
        is = query.find(62078013263165440L);
        s = inputStreamToString(is);
        System.out.println("find:\n" + s);
        query.find(19);
    }

    @Test
    public void testChildren() throws IOException {
        InputStream is = query.children(151);
        String s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
        is = query.children(1514);
        s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
        is = query.children(711);
        s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
        is = query.children(1);
        s = inputStreamToString(is);
        System.out.println("Children:\n" + s);
    }

    @Test
    public void testDescendants() throws IOException {
        InputStream is = query.descendants(1);
        String s = inputStreamToString(is);
        System.out.println("Descendants:\n" + s);
        is = query.descendants(14);
        s = inputStreamToString(is);
        System.out.println("Descendants:\n" + s);
    }

    @Test
    public void testSearch() throws IOException {
        InputStream is = query.search("酒");
        String s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
        is = query.search("白萝卜");
        s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
        is = query.search("wine");
        s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
        is = query.search("oil");
        s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
        is = query.search("oil",1,2);
        s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
    }

    @Test
    public void testSearchSiblings() {
    }

    @Test
    public void testPath() throws IOException {
        InputStream is = query.path(1514);
        String s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
        is = query.path(-1);
        s = inputStreamToString(is);
        System.out.println("Search:\n" + s);
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