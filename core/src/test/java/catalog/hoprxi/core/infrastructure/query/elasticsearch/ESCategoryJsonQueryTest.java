/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-07-17
 */
public class ESCategoryJsonQueryTest {
    private static final CategoryJsonQuery service = new ESCategoryJsonQuery();

    @Test
    public void testQuery() {
        System.out.println(service.query("8178520749625496"));
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
        System.out.println(service.queryChildren("496796322118291470"));
    }

    @Test
    public void testQueryDescendant() {
    }

    @Test
    public void testQueryAll() {
        System.out.println(service.queryAll(0, 2));
        System.out.println(service.queryAll(0, 50));
    }

    @Test
    public void testRoot() {
        System.out.println(service.root());
    }
}