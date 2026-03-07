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
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.domain.model.Count;
import catalog.hoprxi.scale.domain.model.CountRepository;
import catalog.hoprxi.scale.domain.model.Plu;
import org.javamoney.moneta.Money;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

public class PsqlCountRepositoryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465", "slave.tooo.top:6543:P$Qwe123465Pg");
    }

    private static final CountRepository repository = new PsqlCountRepository();
    private static final CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public void beforeClass() {
        LastReceiptPrice lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(0.87, currency), UnitEnum.BA));
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(1.99, currency), UnitEnum.BA));
        VipPrice vipPrice = new VipPrice("plus会员价", new Price(Money.of(1.29, currency), UnitEnum.BA));
        Count spinach = new Count(new Plu(45), new Name("菠菜", "秋波"), new Domestic("510502", "江阳区"), new Specification("275±5克"), GradeEnum.QUALIFIED, ShelfLife.SAME_DAY,
                lastReceiptPrice, retailPrice, MemberPrice.ZERO_RMB_PCS, vipPrice, 1421, 55308232186130227L);
        System.out.println(spinach);
        repository.save(spinach);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(0.68, currency), UnitEnum.GENG));
        retailPrice = new RetailPrice(new Price(Money.of(0.99, currency), UnitEnum.GENG));
        MemberPrice memberPrice = new MemberPrice("会员", new Price(Money.of(0.88, currency), UnitEnum.GENG));
        Count cucumber = new Count(new Plu(46), new Name("青黄瓜"), new Domestic("510522", "合江县"), Specification.of("鲜切"), GradeEnum.QUALIFIED, ShelfLife.SAME_DAY,
                lastReceiptPrice, retailPrice, memberPrice, VipPrice.ZERO_RMB_PCS, 1421, Brand.UNDEFINED.id());
        repository.save(cucumber);

        retailPrice = new RetailPrice(new Price(Money.of(11.99, currency), UnitEnum.GE));
        memberPrice = new MemberPrice("白银会员", new Price(Money.of(10.59, currency), UnitEnum.GE));
        vipPrice = new VipPrice("plus会员价", new Price(Money.of(9.29, currency), UnitEnum.GE));
        Count pumpkin = new Count(new Plu(55), new Name("南瓜", "美国南瓜"), new Domestic("510504", "龙马潭区"), Specification.of("鲜切"), GradeEnum.QUALIFIED, ShelfLife.SAME_DAY,
                LastReceiptPrice.ZERO_RMB_PCS, retailPrice, memberPrice, vipPrice, 1351, 55308279734854175L);
        repository.save(pumpkin);

        retailPrice = new RetailPrice(new Price(Money.of(2.99, currency), UnitEnum.TIAO));
        memberPrice = new MemberPrice(new Price(Money.of(1.59, currency), UnitEnum.TIAO));
        vipPrice = new VipPrice("白金价", new Price(Money.of(1.29, currency), UnitEnum.TIAO));
        Count lettuce = new Count(new Plu(75), new Name("莴笋"), new Domestic("510502", "江阳区"), Specification.of("鲜切"), GradeEnum.QUALIFIED, ShelfLife.SAME_DAY,
                lastReceiptPrice, retailPrice, memberPrice, vipPrice, 496796322118291493L, 55308631288834430L);
        repository.save(lettuce);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(0.246, currency), UnitEnum.KUN));
        retailPrice = new RetailPrice(new Price(Money.of(0.69, currency), UnitEnum.KUN));
        vipPrice = new VipPrice("plus会员价", new Price(Money.of(0.49, currency), UnitEnum.KUN));
        Count radish = new Count(new Plu(76), new Name("长白萝卜","长条不是圆的"), new Domestic("510502", "江阳区"), Specification.of("1.5Kg±25g"), GradeEnum.QUALIFIED, ShelfLife.SAME_DAY,
                lastReceiptPrice, retailPrice, MemberPrice.ZERO_RMB_PCS, vipPrice, 49581450261846042L, 55309503571944143L);
        repository.save(radish);
    }

    @AfterMethod
    public void tearDown() {
        /*
        repository.delete(new Plu(45));
        repository.delete(new Plu(46));
        repository.delete(new Plu(55));
        repository.delete(new Plu(75));
        repository.delete(new Plu(76));

         */
    }

    @Test
    public void testFind() {
        Count lettuce = repository.find(new Plu(75));
        System.out.println(lettuce);
    }

    @Test
    public void testNextPlu() {
    }

    @Test
    public void testDelete() {
    }

    @Test
    public void testSave() {
    }

    @Test
    public void testIsPluExists() {
    }
}