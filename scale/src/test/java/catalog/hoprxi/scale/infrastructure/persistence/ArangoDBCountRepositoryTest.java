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
import org.junit.Assert;
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
        brandRepository.save(new Brand("dyb", "董允坝"));

        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(4.99, currency), Unit.BA));
        MemberPrice memberPrice = MemberPrice.RMB_ZERO;
        VipPrice vipPrice = new VipPrice("plus会员价", new Price(Money.of(1.99, currency), Unit.BA));
        Count spinach = new Count(new Plu(3), new Name("菠菜", "又名：秋波"), new Domestic("泸洲", "江阳区"), null, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "vegetables", "dyb");
        countRepository.save(spinach);

        retailPrice = new RetailPrice(new Price(Money.of(0.99, currency), Unit.GENG));
        memberPrice = new MemberPrice(new Price(Money.of(0.88, currency), Unit.GENG));
        Count cucumber = new Count(new Plu(4), new Name("黄瓜"), new Domestic("泸洲", "合江县"), null, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, VipPrice.RMB_ZERO, "vegetables", Brand.UNDEFINED.id());
        countRepository.save(cucumber);

        retailPrice = new RetailPrice(new Price(Money.of(1.99, currency), Unit.GE));
        memberPrice = new MemberPrice(new Price(Money.of(1.59, currency), Unit.GE));
        Count pumpkin = new Count(new Plu(5), new Name("南瓜", "美国南瓜"), new Domestic("泸洲", "龙马潭区"), null, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, VipPrice.RMB_ZERO, "vegetables", Brand.UNDEFINED.id());
        countRepository.save(pumpkin);

        retailPrice = new RetailPrice(new Price(Money.of(2.99, currency), Unit.TIAO));
        memberPrice = new MemberPrice(new Price(Money.of(1.59, currency), Unit.TIAO));
        Count lettuce = new Count(new Plu(6), new Name("莴笋"), new Domestic("泸洲", "江阳区"), null, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, VipPrice.RMB_ZERO, "vegetables", "dyb");
        countRepository.save(lettuce);

        retailPrice = new RetailPrice(new Price(Money.of(0.99, currency), Unit.KUN));
        vipPrice = new VipPrice("plus会员价", new Price(Money.of(0.49, currency), Unit.KUN));
        Count radish = new Count(new Plu(7), new Name("长白萝卜"), new Domestic("泸洲", "江阳区"), null, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, MemberPrice.RMB_ZERO, vipPrice, "vegetables", "dyb");
        countRepository.save(radish);

    }

    @AfterClass
    public static void teardown() {
    }

    @Test
    public void nextPlu() {
    }

    @Test
    public void isPluExists() {
        Assert.assertTrue(countRepository.isPluExists(10));
        Assert.assertTrue(countRepository.isPluExists(5));
        Assert.assertFalse(countRepository.isPluExists(22));
        Assert.assertFalse(countRepository.isPluExists(222));
    }

    @Test
    public void find() {
        Count radish = countRepository.find(7);
        Assert.assertNotNull(radish);
        Count cucumber = countRepository.find(4);
        Assert.assertNotNull(cucumber);
        Count count = countRepository.find(1);
        Assert.assertNull(count);
        count = countRepository.find(21);
        Assert.assertNull(count);
    }

    @Test
    public void findAll() {
        Count[] counts = countRepository.findAll();
        Assert.assertEquals(5, counts.length);
    }

    @Test
    public void belongingToBrand() {
        Count[] counts = countRepository.belongingToBrand(Brand.UNDEFINED.id());
        Assert.assertEquals(2, counts.length);
        counts = countRepository.belongingToBrand("dyb");
        Assert.assertEquals(3, counts.length);
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