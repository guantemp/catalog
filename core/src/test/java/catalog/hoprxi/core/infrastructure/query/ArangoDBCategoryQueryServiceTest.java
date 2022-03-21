/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.infrastructure.query;

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.infrastructure.view.CategoryView;
import org.testng.Assert;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-12-03
 */
public class ArangoDBCategoryQueryServiceTest {
    private CategoryQueryService query = new ArangoDBCategoryQueryService("catalog");

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testRoot() {
        Assert.assertEquals(2, query.root().length);
        for (CategoryView c : query.root()) {
            System.out.println(c);
        }
    }

    @Test(priority = 2)
    public void testChildren() {
        CategoryView[] sub = query.children("root");
        //Assert.assertEquals(5, sub.length);
    }

    @Test(priority = 2)
    public void testFind() {
        CategoryView root = query.find("root");
        Assert.assertTrue(root.isRoot());
    }

    @Test
    public void testSilblings() {
        // Category[] sub = query.descendants("root");
        //Assert.assertEquals(43, sub.length);
    }

    @Test
    public void testPath() {
    }
}