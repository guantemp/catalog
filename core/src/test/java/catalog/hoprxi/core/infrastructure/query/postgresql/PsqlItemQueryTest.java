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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.view.ItemView;
import org.testng.Assert;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-21
 */
public class PsqlItemQueryTest {
    private static final String DATABASE_NAME = "catalog";
    private static final ItemQuery query = new PsqlItemQuery(DATABASE_NAME);

    static {
        CategoryQuery categoryQuery = new PsqlCategoryQuery("catalog");
        categoryQuery.descendants("52495569397272599");
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testQuery() {
        ItemView itemView = query.query("52496163982907400");
        Assert.assertNotNull(itemView);
        itemView = query.query("52496321492179007");
        Assert.assertNotNull(itemView);
        itemView = query.query("52496163982907400");
        Assert.assertNotNull(itemView);
        itemView = query.query("52496163982907408");
        Assert.assertNull(itemView);
        itemView = query.query("52496321492179007");
        Assert.assertNotNull(itemView);
    }

    @Test
    public void testBelongToBrand() {
        ItemView[] skuses = query.belongToBrand("52495569395175425", 0, 3);
        Assert.assertEquals(skuses.length, 3);
        skuses = query.belongToBrand("52495569395175425", 1, 3);
        Assert.assertEquals(skuses.length, 2);
        skuses = query.belongToBrand("52495569395175425", 1, 1);
        Assert.assertEquals(skuses.length, 1);
        skuses = query.belongToBrand("52495569395175425", 1, 0);
        Assert.assertEquals(skuses.length, 0);
    }

    @Test(invocationCount = 1)
    public void testBelongToCategory() {
        ItemView[] skuses = query.belongToCategory("52495569397272598", 0, 10);
        Assert.assertEquals(skuses.length, 3);
        skuses = query.belongToCategory("52495569397272598", 2, 5);
        Assert.assertEquals(skuses.length, 1);
        skuses = query.belongToCategory("52495569397272598", 5, 3);
        Assert.assertEquals(skuses.length, 0);
        skuses = query.belongToCategory("52495569397272598", 3, 2);
        Assert.assertEquals(skuses.length, 0);
        skuses = query.belongToCategory("52495569397272598", 0, 0);
        Assert.assertEquals(skuses.length, 0);
        skuses = query.belongToCategory("52495569397272598", 7, 10);
        Assert.assertEquals(skuses.length, 0);
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testBelongToCategoryAndDescendants() {
        PsqlItemQuery itemQueryService = ((PsqlItemQuery) query);
        ItemView[] skuses;
        skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598", 0, 5);
        Assert.assertEquals(skuses.length, 5);
        skuses = itemQueryService.belongToCategoryAndDescendants("-1", 1, 12);
        Assert.assertEquals(skuses.length, 2);
        skuses = itemQueryService.belongToCategoryAndDescendants("14986369380716560", 1, 345);
        Assert.assertEquals(skuses.length, 345);
        skuses = itemQueryService.belongToCategoryAndDescendants("14986369380716560", 512, 1024);
        Assert.assertEquals(skuses.length, 1024);
        //skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598", 0, 512);
        //Assert.assertEquals(skuses.length, 0);
        //kuses = itemQueryService.belongToCategoryAndDescendants("52495569397272597", 0,1024 );
        //Assert.assertEquals(skuses.length, 1);
    }

   /*
    @Test(invocationCount = 1)
    public void testBelongToCategoryTest() {
        PsqlItemQueryService itemQueryService = ((PsqlItemQueryService) query);
        ItemView[] skuses = itemQueryService.belongToCategoryTest("52495569397272598");
        for (ItemView item : skuses)
            System.out.println(item);
        //Assert.assertEquals(skuses.length, 5);
        skuses = itemQueryService.belongToCategoryTest("52495569397272595");
        skuses = itemQueryService.belongToCategoryTest("-1");
        //Assert.assertEquals(skuses.length, 3);
        skuses = itemQueryService.belongToCategoryTest("52495569397272599");
        //Assert.assertEquals(skuses.length, 2);
        skuses = itemQueryService.belongToCategoryTest("52495569397272598");
        //Assert.assertEquals(skuses.length, 0);
        skuses = itemQueryService.belongToCategoryTest("52495569397272597");
        //Assert.assertEquals(skuses.length, 1);
    }

    */

    @Test
    public void testQueryAll() {
        ItemView[] skuses = query.queryAll(0, 25);
        Assert.assertEquals(skuses.length, 14);
        skuses = query.queryAll(12, 25);
        Assert.assertEquals(skuses.length, 2);
        skuses = query.queryAll(5, 5);
        Assert.assertEquals(skuses.length, 5);
    }

    @Test
    public void testSize() {
        Assert.assertEquals(query.size(), 14);
    }

    @Test
    public void testSerach() {
        ItemView[] items = query.queryByRegular("彩虹");
        Assert.assertEquals(items.length, 133);
        items = query.queryByRegular("^彩虹");
        //Assert.assertEquals(items.length, 2);
        items = query.queryByRegular("彩虹|长虹");
        Assert.assertEquals(items.length, 0);
        items = query.queryByRegular("不知道");
        Assert.assertEquals(items.length, 0);
        items = query.queryByRegular("天友|长虹|彩虹");
        Assert.assertEquals(items.length, 0);
        items = query.queryByRegular("^天友|长虹|彩虹");
        Assert.assertEquals(items.length, 0);

        ItemView[] skuses = query.queryByRegular("^ch");
        Assert.assertEquals(skuses.length, 3);
        skuses = query.queryByRegular("qd");
        Assert.assertEquals(skuses.length, 3);
        skuses = query.queryByRegular("ch");
        Assert.assertEquals(skuses.length, 4);
        skuses = query.queryByRegular("chetr");
        Assert.assertEquals(skuses.length, 0);
        skuses = query.queryByRegular("ty");
        Assert.assertEquals(skuses.length, 5);
    }

    @Test
    public void testQueryByBarcode() {
        ItemView[] items = query.queryByBarcode("69235552");
        //Assert.assertEquals(items.length, 3);
        items = query.queryByBarcode("690");
        //Assert.assertEquals(items.length, 3);
        items = query.queryByBarcode("123465");
        Assert.assertEquals(items.length, 0);
        items = query.queryByBarcode("^4695");
        Assert.assertEquals(items.length, 1);
        items = query.queryByBarcode("4547691239136");
        Assert.assertEquals(items.length, 1);
        for (ItemView item : items)
            System.out.println(item);
    }

    @Test
    public void testAccurateQueryByBarcode() {
        ItemView[] items = query.accurateQueryByBarcode("69235552");
        Assert.assertEquals(items.length, 0);
        items = query.accurateQueryByBarcode(" 6923555210066");
        Assert.assertEquals(items.length, 1);

    }

    @Test
    public void testTestSerach() {
        ItemView[] items = query.queryByName("彩虹|690", 130, 1000);
        //for(ItemView view:items)
        //System.out.println(view);
        System.out.println(items.length);
    }
}