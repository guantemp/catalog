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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-21
 */
public class PsqlItemQueryServiceTest {
    private static final String DATABASE_NAME = "catalog";
    private static final ItemQueryService query = new PsqlItemQueryService(DATABASE_NAME);

    static {
        CategoryQueryService categoryQuery = new PsqlCategoryQueryService("catalog");
        categoryQuery.descendants("52495569397272599");
    }

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFind() {
        ItemView itemView = query.find("52496163982907400");
        Assert.assertNotNull(itemView);
        itemView = query.find("52496321492179007");
        Assert.assertNotNull(itemView);
        itemView = query.find("52496163982907400");
        Assert.assertNotNull(itemView);
        itemView = query.find("52496163982907408");
        Assert.assertNull(itemView);
        itemView = query.find("52496321492179007");
        Assert.assertNotNull(itemView);
        //System.out.println(itemView);
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

    @Test(invocationCount = 1)
    public void testBelongToCategoryWith() throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        PsqlItemQueryService itemQueryService = ((PsqlItemQueryService) query);
        ItemView[] skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598");
        //Assert.assertEquals(skuses.length, 8);
        skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598");
        //Assert.assertEquals(skuses.length, 5);
        skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598");
        //Assert.assertEquals(skuses.length, 3);
        skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598");
        //Assert.assertEquals(skuses.length, 2);
        skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598");
        //Assert.assertEquals(skuses.length, 0);
        skuses = itemQueryService.belongToCategoryAndDescendants("52495569397272598");
        //Assert.assertEquals(skuses.length, 1);
    }

    @Test
    public void testFindAll() {
        ItemView[] skuses = query.findAll(0, 25);
        Assert.assertEquals(skuses.length, 14);
        skuses = query.findAll(12, 25);
        Assert.assertEquals(skuses.length, 2);
        skuses = query.findAll(5, 5);
        Assert.assertEquals(skuses.length, 5);
    }

    @Test
    public void testSize() {
        Assert.assertEquals(query.size(), 14);
    }

    @Test
    public void testFromBarcode() {
        ItemView[] items = query.serach("69235552");
        Assert.assertEquals(items.length, 3);
        items = query.serach("690");
        Assert.assertEquals(items.length, 3);
        items = query.serach("123465");
        Assert.assertEquals(items.length, 0);
        items = query.serach("4695");
        Assert.assertEquals(items.length, 1);
        items = query.serach("4547691239136");
        Assert.assertEquals(items.length, 1);
        for (ItemView item : items)
            System.out.println(item);
    }

    @Test
    public void testFromName() {
    }
}