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
import catalog.hoprxi.core.application.view.CategoryView;
import org.testng.Assert;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-12-03
 */
public class ArangoDBCategoryQueryServiceTest {
    private final CategoryQueryService query = new ArangoDBCategoryQueryService("catalog");

    @Test(invocationCount = 1)
    public void testRoot() {
        Assert.assertEquals(2, query.root().length);
    }

    @Test(priority = 3, invocationCount = 3, threadPoolSize = 1)
    public void testChildren() {
        CategoryView[] sub = query.children("root");
        Assert.assertEquals(5, sub.length);
        sub = query.children("food");
        Assert.assertEquals(3, sub.length);
        sub = query.children("grain_oil");
        Assert.assertEquals(5, sub.length);
        sub = query.children("oil");
        Assert.assertEquals(7, sub.length);
        sub = query.children("leisure_food");
        Assert.assertEquals(2, sub.length);
        //测试新对象持有同一个缓存tree
        CategoryQueryService multiple = new ArangoDBCategoryQueryService("catalog");
        sub = multiple.children("oil");
        Assert.assertEquals(7, sub.length);
        sub = multiple.children("flour");
        Assert.assertEquals(0, sub.length);
    }

    @Test(priority = 3, invocationCount = 2)
    public void testFind() {
        CategoryView root = query.find("root");
        Assert.assertTrue(root.isRoot());
        root = query.find(" undefined");
        Assert.assertTrue(root.isRoot());
        CategoryView grain_oil = query.find("grain_oil");
        Assert.assertNotNull(grain_oil);
        grain_oil = query.find("grain_oil");
        Assert.assertNotNull(grain_oil);
        CategoryView oil = query.find("oil");
        Assert.assertNotNull(oil);
        CategoryView corn_oil = query.find("corn_oil");
        Assert.assertNotNull(corn_oil);
        CategoryView sunflower_seed_oil = query.find("sunflower_seed_oil");
        Assert.assertNotNull(sunflower_seed_oil);
        sunflower_seed_oil = query.find("sunflower_seed_oil");
        Assert.assertNotNull(sunflower_seed_oil);
    }

    @Test(invocationCount = 2, dependsOnMethods = {"testRoot"})
    public void testDescendants() {
        CategoryView[] descendants = query.descendants("root");
        Assert.assertEquals(43, descendants.length);
        descendants = query.descendants("oil");
        Assert.assertEquals(7, descendants.length);
        descendants = query.descendants("grain_oil");
        Assert.assertEquals(14, descendants.length);
        for (CategoryView c : descendants)
            System.out.println("descendants:" + c);
        descendants = query.descendants(" chemicals");
        Assert.assertEquals(8, descendants.length);
        descendants = query.descendants(" dairy");
        Assert.assertEquals(0, descendants.length);
    }

    @Test(invocationCount = 2, dependsOnMethods = {"testDescendants"})
    public void testSilblings() {
        CategoryView[] siblings = query.siblings("grain_oil");
        Assert.assertEquals(4, siblings.length);
        siblings = query.siblings("oil");
        Assert.assertEquals(4, siblings.length);
        siblings = query.siblings("corn_oil");
        // for (CategoryView c : siblings)
        //     System.out.println("sibling:" + c);
        Assert.assertEquals(6, siblings.length);
        siblings = query.siblings("root");
        Assert.assertEquals(0, siblings.length);
    }

    @Test(invocationCount = 1, dependsOnMethods = {"testRoot"})
    public void testPath() {
        CategoryView[] sub = query.path("rapeseed_oil");
        for (CategoryView c : sub)
            System.out.println("rapeseed_oil path:" + c);
    }

    @Test(invocationCount = 1, dependsOnMethods = {"testRoot"})
    public void testSearchName() {
        CategoryView[] sub = query.searchName("食品");
        for (CategoryView c : sub)
            System.out.println("search:" + c);
    }
}