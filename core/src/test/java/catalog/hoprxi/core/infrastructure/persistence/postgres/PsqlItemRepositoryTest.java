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

package catalog.hoprxi.core.infrastructure.persistence.postgres;

import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-14
 */
public class PsqlItemRepositoryTest {
    private static ItemRepository itemRepository = new PsqlItemRepository("catalog");
    private static BrandRepository brandRepository = new PsqlBrandRepository("catalog");
    private static CategoryRepository categoryRepository = new PsqlCategoryRepository("catalog");
    private static CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeTest
    public void setUp() {
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testFind() {
    }

    @Test
    public void testNextIdentity() {
    }

    @Test
    public void testRemove() {
    }

    @Test
    public void testSave() {
    }
}