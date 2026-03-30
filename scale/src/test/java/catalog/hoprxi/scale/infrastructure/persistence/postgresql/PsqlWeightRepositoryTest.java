/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.persistence.postgresql;

import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.WeightRepository;
import catalog.hoprxi.scale.domain.model.price.*;
import org.javamoney.moneta.Money;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.NumberValue;
import java.util.Locale;

public class PsqlWeightRepositoryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465", "slave.tooo.top:6543:P$Qwe123465Pg");
    }

    private static final WeightRepository repository = new PsqlWeightRepository();
    private static final CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public void beforeClass() {
        WeightRetailPrice retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(9.99, currency), WeightUnit.KILOGRAM));
        WeightVipPrice vipPrice = new WeightVipPrice(new WeightPrice(Money.of(7.99, currency), WeightUnit.KILOGRAM));
        WeightLastReceiptPrice lastReceiptPrice = new WeightLastReceiptPrice(new WeightPrice(Money.of(4.87, currency), WeightUnit.KILOGRAM));
        Weight apple = new Weight(new Plu(1), new Name("苹果", "apple"), new Domestic("610528", "富平县"), new Specification("90#"), GradeEnum.ONE_LEVEL, new ShelfLife(15),
                lastReceiptPrice, retailPrice, WeightMemberPrice.ZERO_KILOGRAM_RMB, vipPrice, 55308263825858876L, Brand.UNDEFINED.id());
        repository.save(apple);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(2.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        vipPrice = new WeightVipPrice(new WeightPrice(Money.of(4.99, currency), WeightUnit.KILOGRAM));
        lastReceiptPrice = new WeightLastReceiptPrice(new WeightPrice(Money.of(1.98, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight apple1 = new Weight(new Plu(2), new Name("昭通苹果", "zhaotong apple"), new Domestic("530600", "昭通"), new Specification("85#"), GradeEnum.ONE_LEVEL, new ShelfLife(7),
                lastReceiptPrice, retailPrice, WeightMemberPrice.ZERO_KILOGRAM_RMB, vipPrice, 55308263825858876L, Brand.UNDEFINED.id());
        repository.save(apple1);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(15.8, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        WeightMemberPrice memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(27.89, currency), WeightUnit.KILOGRAM));
        Weight marbled = new Weight(new Plu(100), new Name("猪五花肉", "pig marbled meat"), new Domestic("510500", "泸州"), new Specification("散装"), GradeEnum.ONE_LEVEL, ShelfLife.THREE_DAY,
                WeightLastReceiptPrice.ZERO_KILOGRAM_RMB, retailPrice, memberPrice, WeightVipPrice.ZERO_KILOGRAM_RMB, 55308263825858876L, Brand.UNDEFINED.id());
        repository.save(marbled);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(17.8, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(30.68, currency), WeightUnit.KILOGRAM));
        Weight pig_intestine = new Weight(new Plu(101), new Name("猪蹄", "pig's feet"), new Domestic("510522", "合江县"), new Specification("先切"), GradeEnum.ONE_LEVEL, ShelfLife.THREE_DAY,
                WeightLastReceiptPrice.ZERO_KILOGRAM_RMB, retailPrice, memberPrice, WeightVipPrice.ZERO_KILOGRAM_RMB, 55307862716180229L, Brand.UNDEFINED.id());
        repository.save(pig_intestine);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(21.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(39.98, currency), WeightUnit.KILOGRAM));
        Weight tenderloin = new Weight(new Plu(102), new Name("猪大肠", "pig intestine"), MadeIn.UNKNOWN, new Specification("先切"), GradeEnum.ONE_LEVEL, ShelfLife.THREE_DAY,
                WeightLastReceiptPrice.ZERO_KILOGRAM_RMB, retailPrice, memberPrice, WeightVipPrice.ZERO_KILOGRAM_RMB, 55307862716180229L, Brand.UNDEFINED.id());
        repository.save(tenderloin);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(14.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(12.58, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        lastReceiptPrice = new WeightLastReceiptPrice(new WeightPrice(Money.of(7.76, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight crucian_carp = new Weight(new Plu(410), new Name("鲫鱼", "crucian carp"), new Domestic("510504", "龙马潭"), new Specification("鲜活"), GradeEnum.ONE_LEVEL, ShelfLife.SAME_DAY,
                lastReceiptPrice, retailPrice, memberPrice, WeightVipPrice.ZERO_KILOGRAM_RMB, 55308663786302342L, Brand.UNDEFINED.id());
        repository.save(crucian_carp);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(24, currency), WeightUnit.KILOGRAM));
        lastReceiptPrice = new WeightLastReceiptPrice(new WeightPrice(Money.of(7.89, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight grass_carp = new Weight(new Plu(411), new Name("草鱼", "grass carp"), MadeIn.UNKNOWN, new Specification("鲜活"), GradeEnum.QUALIFIED, ShelfLife.SAME_DAY,
                lastReceiptPrice, retailPrice, WeightMemberPrice.ZERO_KILOGRAM_RMB, WeightVipPrice.ZERO_KILOGRAM_RMB, 55308663786302342L, Brand.UNDEFINED.id());
        repository.save(grass_carp);
    }

    @AfterClass
    public void afterClass() {
        //repository.delete(new Plu(1));
        /*
        repository.delete(new Plu(2));
        repository.delete(new Plu(410));
        repository.delete(new Plu(411));
        repository.delete(new Plu(100));
        repository.delete(new Plu(101));
        repository.delete(new Plu(102));

         */
    }

    @Test
    public void testFind() {
        Weight appale = repository.find(1);
        System.out.println(appale);
        Weight grass_carp = repository.find(new Plu(411));
        System.out.println(grass_carp);
        Weight weight = repository.find(new Plu(4111));
        Assert.assertNull(weight);
    }

    @Test
    public void testNextPlu() {
    }

    @Test
    public void testSave() {
        Weight appale = repository.find(1);
        appale.adjustRetailPrice(new WeightRetailPrice(new WeightPrice(Money.of(7.99, currency), WeightUnit.FIVE_HUNDRED_GRAM)));
        repository.save(appale);
        appale=repository.find(1);
        Assert.assertEquals(appale.retailPrice().price().amount().getNumber().doubleValue(), 7.99);
        //System.out.println(v);
    }

    @Test
    public void testIsPluExists() {
    }
}