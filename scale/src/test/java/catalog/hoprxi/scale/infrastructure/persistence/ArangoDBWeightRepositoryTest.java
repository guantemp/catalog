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

package catalog.hoprxi.scale.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.WeightRepository;
import catalog.hoprxi.scale.domain.model.weight_price.*;
import org.javamoney.moneta.Money;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/30
 */
public class ArangoDBWeightRepositoryTest {
    private static WeightRepository weightRepository = new ArangoDBWeightRepository();
    private static BrandRepository brandRepository = new ArangoDBBrandRepository("catalog");
    private static CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public static void setUpBeforeClass() {
        brandRepository.save(Brand.UNDEFINED);
        brandRepository.save(new Brand("tw", "通威"));

        WeightRetailPrice retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(4.99, currency), WeightUnit.KILOGRAM));
        WeightVipPrice vipPrice = WeightVipPrice.zero("vip", Locale.getDefault());
        WeightMemberPrice memberPrice = WeightMemberPrice.zero("会员价", Locale.getDefault());
        Weight apple = new Weight(new Plu(1), new Name("苹果", "apple"), new Domestic("陕西", "榆林"), new Specification("90g"), Grade.ONE_LEVEL, new ShelfLife(7),
                retailPrice, memberPrice, vipPrice, "fruits", Brand.UNDEFINED.id());
        weightRepository.save(apple);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(2.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        vipPrice = new WeightVipPrice("vip", new WeightPrice(Money.of(4.99, currency), WeightUnit.KILOGRAM));
        Weight apple1 = new Weight(new Plu(2), new Name("昭通苹果", "zhaotong apple"), new Domestic("云南省", "昭通市"), Specification.UNDEFINED, Grade.SUPERFINE, new ShelfLife(10),
                retailPrice, memberPrice, vipPrice, "fruits", Brand.UNDEFINED.id());
        weightRepository.save(apple1);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(32.50, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(29.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight marbled = new Weight(new Plu(10), new Name("猪五花肉", "pig marbled meat"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "meat", Brand.UNDEFINED.id());
        weightRepository.save(marbled);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(19.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(16.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight pig_feet = new Weight(new Plu(11), new Name("猪蹄", "pig\'s feet"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "meat", Brand.UNDEFINED.id());
        weightRepository.save(pig_feet);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(27.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(25.5, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight pig_intestine = new Weight(new Plu(12), new Name("猪大肠", "pig intestine"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "meat", Brand.UNDEFINED.id());
        weightRepository.save(pig_intestine);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(35.505050, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(65.99, currency), WeightUnit.KILOGRAM));
        Weight tenderloin = new Weight(new Plu(13), new Name("里脊肉(猪）", "pig tenderloin"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "meat", Brand.UNDEFINED.id());
        weightRepository.save(tenderloin);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(8.50, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(7.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight grass_carp = new Weight(new Plu(20), new Name("草鱼", "grass carp"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "aquatic", Brand.UNDEFINED.id());
        weightRepository.save(grass_carp);

        retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(11, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        memberPrice = new WeightMemberPrice(new WeightPrice(Money.of(9.99, currency), WeightUnit.FIVE_HUNDRED_GRAM));
        Weight crucian_carp = new Weight(new Plu(21), new Name("鲫鱼", "crucian carp"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                retailPrice, memberPrice, vipPrice, "aquatic", Brand.UNDEFINED.id());
        weightRepository.save(crucian_carp);
    }
/*
    @AfterClass
    public static void teardown() {
        weightRepository.remove(new Plu(1));
        weightRepository.remove(new Plu(2));
        weightRepository.remove(new Plu(10));
        weightRepository.remove(new Plu(11));
        weightRepository.remove(new Plu(12));
        weightRepository.remove(new Plu(13));
        weightRepository.remove(new Plu(20));
        weightRepository.remove(new Plu(21));

        brandRepository.remove(Brand.UNDEFINED.id());
        brandRepository.remove("tw");
    }
*/
    @Test
    public void nextPlu() {
    }

    @Test
    public void isPluExists() {
        Assert.assertTrue(weightRepository.isPluExists(10));
        Assert.assertTrue(weightRepository.isPluExists(21));
        Assert.assertFalse(weightRepository.isPluExists(22));
    }

    @Test
    public void find() {
        Weight apple = weightRepository.find(1);
        Assert.assertNotNull(apple);
        Weight tenderloin = weightRepository.find(13);
        System.out.println(tenderloin);
        Weight crucian_carp = weightRepository.find(21);
        Assert.assertNotNull(crucian_carp);
        Weight weight = weightRepository.find(22);
        Assert.assertNull(weight);
        weight = weightRepository.find(25);
        Assert.assertNull(weight);
        weight = weightRepository.find(80);
        Assert.assertNull(weight);
    }

    @Test
    public void belongingToBrand() {
        Weight[] weights = weightRepository.belongingToBrand(Brand.UNDEFINED.id(), 0, 5);
        Assert.assertEquals(5, weights.length);
        weights = weightRepository.belongingToBrand(Brand.UNDEFINED.id(), 7, 5);
        Assert.assertEquals(1, weights.length);
        weights = weightRepository.belongingToBrand(Brand.UNDEFINED.id(), 1, 3);
        Assert.assertEquals(3, weights.length);
    }

    @Test
    public void belongingToCategory() {
        Weight[] weights = weightRepository.belongingToCategory("meat", 0, 5);
        Assert.assertEquals(4, weights.length);
        weights = weightRepository.belongingToCategory("meat", 2, 5);
        Assert.assertEquals(2, weights.length);
        weights = weightRepository.belongingToCategory("meat", 4, 5);
        Assert.assertEquals(0, weights.length);
        weights = weightRepository.belongingToCategory("meat", 2, 2);
        Assert.assertEquals(2, weights.length);
        weights = weightRepository.belongingToCategory("meat", 4, 10);
        Assert.assertEquals(0, weights.length);
    }

    @Test
    public void findAll() {
        Weight[] weights = weightRepository.findAll(0, 10);
        Assert.assertEquals(8, weights.length);
        weights = weightRepository.findAll(0, 8);
        Assert.assertEquals(8, weights.length);
        weights = weightRepository.findAll(1, 8);
        Assert.assertEquals(7, weights.length);
        weights = weightRepository.findAll(1, 2);
        Assert.assertEquals(2, weights.length);
        weights = weightRepository.findAll(7, 4);
        Assert.assertEquals(1, weights.length);
        weights = weightRepository.findAll(7, 0);
        Assert.assertEquals(0, weights.length);
        weights = weightRepository.findAll(4, 3);
        Assert.assertEquals(3, weights.length);
    }


    @Test
    public void save() {
        Weight grass_carp = weightRepository.find(20);
        WeightRetailPrice retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(65, currency), WeightUnit.KILOGRAM));
        grass_carp.changeRetailPrice(retailPrice);
        grass_carp.moveToNewBrand("tw");
        grass_carp.moveToNewCategory("cooked_food");
        weightRepository.save(grass_carp);
        grass_carp = weightRepository.find(20);

        WeightPrice price = grass_carp.retailPrice().weightPrice().convert(WeightUnit.FIVE_HUNDRED_GRAM);
        Assert.assertTrue(price.equals(new WeightPrice(Money.of(32.5, currency), WeightUnit.FIVE_HUNDRED_GRAM)));

        price = grass_carp.retailPrice().weightPrice().convert(WeightUnit.GRAM);
        Assert.assertTrue(price.equals(new WeightPrice(Money.of(0.065, currency), WeightUnit.GRAM)));

        price = grass_carp.retailPrice().weightPrice().convert(WeightUnit.FIVE_HUNDRED_GRAM).convert(WeightUnit.GRAM);
        Assert.assertTrue(price.equals(new WeightPrice(Money.of(0.065, currency), WeightUnit.GRAM)));
    }

    @Test
    public void size() {
        Assert.assertEquals(8, weightRepository.size());
    }

    @Test
    public void fromName() {
        Weight[] weights = weightRepository.fromName("猪");
        Assert.assertEquals(4, weights.length);
        weights = weightRepository.fromName("^猪");
        Assert.assertEquals(3, weights.length);
    }
}