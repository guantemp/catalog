/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Unit;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.scale.domain.model.Count;
import catalog.hoprxi.scale.domain.model.CountRepository;
import catalog.hoprxi.scale.domain.model.Plu;
import org.javamoney.moneta.Money;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-11-05
 */
public class ArangoDBCountRepositoryTest {
    private static CountRepository countRepository = new ArangoDBCountRepository();
    private static BrandRepository brandRepository = new ArangoDBBrandRepository();
    private static CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public static void setUpBeforeClass() {
        brandRepository.save(Brand.UNDEFINED);
        brandRepository.save(new Brand("tw", "董允坝"));

        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(4.99, currency), Unit.BA));
        Count spinach = new Count(new Plu(3), new Name("菠菜", "又名：秋波"), new Domestic("泸洲", "江阳区"), null, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, "vegetables", Brand.UNDEFINED.id());
        countRepository.save(spinach);
    }

    @AfterClass
    public static void teardown() {
    }

    @Test
    public void nextPlu() {
    }

    @Test
    public void isPluExists() {
    }

    @Test
    public void find() {
    }

    @Test
    public void findAll() {
    }

    @Test
    public void belongingToBrand() {
    }

    @Test
    public void belongingToCategory() {
    }

    @Test
    public void remove() {
    }

    @Test
    public void save() {
    }

    @Test
    public void size() {
    }

    @Test
    public void fromName() {
    }
}