/*
 * Copyright (c) 2021. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.infrastructure.view.ItemView;
import org.testng.Assert;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-10-15
 */
public class ArangoDBItemQueryServiceTest {
    private ItemQueryService itemQueryService = new ArangoDBItemQueryService("catalog");

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testBelongToBrand() {
        ItemView[] skuses = itemQueryService.belongToBrand("caihong", 0, 3);
        Assert.assertEquals(skuses.length, 3);
        skuses = itemQueryService.belongToBrand("caihong", 1, 3);
        Assert.assertEquals(skuses.length, 2);
        skuses = itemQueryService.belongToBrand("caihong", 1, 1);
        Assert.assertEquals(skuses.length, 1);
        skuses = itemQueryService.belongToBrand("caihong", 1, 0);
        Assert.assertEquals(skuses.length, 0);

    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testBelongToCategory() {
        ItemView[] skuses = itemQueryService.belongToCategory("food", 0, 10);
        Assert.assertEquals(skuses.length, 8);
        skuses = itemQueryService.belongToCategory("food", 2, 5);
        Assert.assertEquals(skuses.length, 5);
        skuses = itemQueryService.belongToCategory("food", 5, 3);
        Assert.assertEquals(skuses.length, 3);
        skuses = itemQueryService.belongToCategory("food", 3, 2);
        Assert.assertEquals(skuses.length, 2);
        skuses = itemQueryService.belongToCategory("food", 0, 0);
        Assert.assertEquals(skuses.length, 0);
        skuses = itemQueryService.belongToCategory("food", 7, 10);
        Assert.assertEquals(skuses.length, 1);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFindAll() {
        ItemView[] skuses = itemQueryService.findAll(0, 25);
        Assert.assertEquals(skuses.length, 14);
        skuses = itemQueryService.findAll(12, 25);
        Assert.assertEquals(skuses.length, 2);
        skuses = itemQueryService.findAll(5, 5);
        Assert.assertEquals(skuses.length, 5);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testSize() {
        Assert.assertEquals(itemQueryService.size(), 14);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFromBarcode() {
        ItemView[] items = itemQueryService.fromBarcode("69235552");
        Assert.assertEquals(items.length, 3);
        items = itemQueryService.fromBarcode("690");
        Assert.assertEquals(items.length, 3);
        items = itemQueryService.fromBarcode("123465");
        Assert.assertEquals(items.length, 0);
        items = itemQueryService.fromBarcode("4695");
        Assert.assertEquals(items.length, 1);
        items = itemQueryService.fromBarcode("4547691239136");
        Assert.assertEquals(items.length, 1);
        for (ItemView item : items)
            System.out.println(item);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFromName() {
        ItemView[] items = itemQueryService.fromName("彩虹");
        Assert.assertEquals(items.length, 3);
        items = itemQueryService.fromName("^彩虹");
        Assert.assertEquals(items.length, 2);
        items = itemQueryService.fromName("彩虹|长虹");
        Assert.assertEquals(items.length, 4);
        items = itemQueryService.fromName("不知道");
        Assert.assertEquals(items.length, 0);
        items = itemQueryService.fromName("天友|长虹|彩虹");
        Assert.assertEquals(items.length, 9);
        items = itemQueryService.fromName("^天友|长虹|彩虹");
        Assert.assertEquals(items.length, 7);

        ItemView[] skuses = itemQueryService.fromName("^ch");
        Assert.assertEquals(skuses.length, 3);
        skuses = itemQueryService.fromName("qd");
        Assert.assertEquals(skuses.length, 3);
        skuses = itemQueryService.fromName("ch");
        Assert.assertEquals(skuses.length, 4);
        skuses = itemQueryService.fromName("chetr");
        Assert.assertEquals(skuses.length, 0);
        skuses = itemQueryService.fromName("ty");
        Assert.assertEquals(skuses.length, 5);
    }
}