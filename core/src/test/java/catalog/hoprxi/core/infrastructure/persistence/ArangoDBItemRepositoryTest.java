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

package catalog.hoprxi.core.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.*;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.barcode.EAN_13;
import catalog.hoprxi.core.domain.model.barcode.EAN_8;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import org.javamoney.moneta.Money;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-07-10
 */
public class ArangoDBItemRepositoryTest {
    private static ItemRepository itemRepository = new ArangoDBItemRepository("catalog");
    private static BrandRepository brandRepository = new ArangoDBBrandRepository("catalog");
    private static CategoryRepository categoryRepository = new ArangoDBCategoryRepository("catalog");
    private static CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeTest
    public void setUp() {
        brandRepository.save(Brand.UNDEFINED);
        Brand caihong = new Brand("caihong", new Name("彩虹"));
        brandRepository.save(caihong);
        Brand tianyou = new Brand("tianyou", new Name("天友"));
        brandRepository.save(tianyou);
        Brand changhong = new Brand("changjhong", new Name("长虹"));
        brandRepository.save(changhong);

        categoryRepository.save((Category.UNDEFINED));
        Category root = Category.root("root", new Name("分类", "root"));
        categoryRepository.save(root);
        Category food = new Category("root", "food", new Name("食品", "food"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        categoryRepository.save(food);
        Category dairy = new Category("food", "dairy", Name.of("乳制品"), "使用牛乳或羊乳及其加工制品为主要原料，加入或不加入适量的维生素、矿物质和其他辅料，使用法律法规及标准规定所要求的条件，经加工制成的各种食品");
        categoryRepository.save(dairy);
        Category vegetable_products = new Category("food", " vegetable_products", Name.of("蔬菜制品"), "以蔬菜和食用菌为原料，采用腌制、干燥、油炸等工艺加工而成的各种蔬菜制品，即酱腌菜、蔬菜干制品、食用菌制品、其他蔬菜制品");
        categoryRepository.save(vegetable_products);
        Category chemicals = new Category("root", "chemicals", new Name("日化", "chemicals"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        categoryRepository.save(chemicals);
        Category cosmetics = new Category("chemicals", "cosmetics", "化妆品",
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        categoryRepository.save(cosmetics);
        Category skin = new Category("cosmetics", "skin", Name.of("肤用化妆品"));
        categoryRepository.save(skin);

        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode("6907861191394");
        MadeIn madeIn = new Domestic("四川", "成都");
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(19.59, currency), Unit.HE));
        Item one = new Item("one", barcode, new Name("150ml彩虹柠檬香电热灭蚊香液", "150ml彩虹电热灭蚊香液"), madeIn,
                new Specification("150ml"), Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, skin.id(), caihong.id());
        itemRepository.save(one);

        retailPrice = new RetailPrice(new Price(Money.of(35.50, currency), Unit.HE));
        Item two = new Item("two", new EAN_13("6907861181388"), new Name("彩虹电热灭蚊香液橙子香型2瓶装", "彩虹电热灭蚊香液2瓶装"), madeIn,
                new Specification("2*150ml"), Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, skin.id(), caihong.id());
        itemRepository.save(two);

        retailPrice = new RetailPrice(new Price(Money.of(56.80, currency), Unit.HE));
        barcode = BarcodeGenerateServices.createMatchingBarcode("6907861181395");
        Item three = new Item("three", barcode, new Name("彩虹电热灭蚊香液4瓶装（橙子+芒果香型）", "彩虹电热灭蚊香液4瓶装"), madeIn,
                new Specification("4*120ml"), Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, skin.id(), caihong.id());
        itemRepository.save(three);

        retailPrice = new RetailPrice(new Price(Money.of(4.50, currency), Unit.HE));
        MemberPrice memberPrice = new MemberPrice(new Price(Money.of(3.96, currency), Unit.HE));
        VipPrice vipPrice = new VipPrice("PLUS价", new Price(Money.of(3.00, currency), Unit.HE));
        Item four = new Item("four", new EAN_13("6942070284987"), new Name("天友南美酸奶", "天友南美酸奶"), Domestic.CHONG_QING,
                new Specification("350ml"), Grade.QUALIFIED, retailPrice, memberPrice, vipPrice, dairy.id(), tianyou.id());
        itemRepository.save(four);

        retailPrice = new RetailPrice(new Price(Money.of(10.00, currency), Unit.TI));
        memberPrice = new MemberPrice(new Price(Money.of(9.5, currency), Unit.TI));
        vipPrice = new VipPrice("PLUS", new Price(Money.of(8.8, currency), Unit.TI));
        barcode = BarcodeGenerateServices.createMatchingBarcode("6923555240896");
        Item five = new Item("five", barcode, new Name("天友纯牛奶", "天友纯牛奶"), Domestic.CHONG_QING,
                new Specification("350ml"), Grade.QUALIFIED, retailPrice, memberPrice, vipPrice, dairy.id(), tianyou.id());
        itemRepository.save(five);

        retailPrice = new RetailPrice(new Price(Money.of(17.90, currency), Unit.PCS));
        vipPrice = new VipPrice("PLUS", new Price(Money.of(15.50, currency), Unit.PCS));
        Item six = new Item("six", new EAN_8("20075422"), new Name("天友纯牛奶组合装", "天友组合装"), Domestic.CHONG_QING,
                new Specification("6*250ml"), Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, vipPrice, food.id(), tianyou.id());
        itemRepository.save(six);

        barcode = BarcodeGenerateServices.createMatchingBarcode("6923555240865");
        Item six_1 = new Item("six_1", barcode, new Name("250ml天友纯牛奶(高钙）", "250ml天友高钙纯牛奶"), Domestic.CHONG_QING,
                new Specification("250ml"), Grade.QUALIFIED, RetailPrice.RMB_ZERO, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, dairy.id(), tianyou.id());
        itemRepository.save(six_1);

        retailPrice = new RetailPrice(new Price(Money.of(27.5, currency), Unit.DAI));
        barcode = BarcodeGenerateServices.createMatchingBarcode("6923555240889");
        Item six_2 = new Item("six_2", barcode, new Name("250ml天友纯牛奶", "250ml天友纯牛奶"), Domestic.CHONG_QING,
                new Specification("250ml"), Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, dairy.id(), tianyou.id());
        itemRepository.save(six_2);

        retailPrice = new RetailPrice(new Price(Money.of(2.5, currency), Unit.DAI));
        Item seven = new Item("seven", new EAN_8("21091346"), new Name("麻辣味甘源青豆", "麻辣味甘源青豆"), Domestic.TIAN_JIN, new Specification("25g"),
                Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, food.id(), Brand.UNDEFINED.id());
        itemRepository.save(seven);

        retailPrice = new RetailPrice(new Price(Money.of(4.80, currency), Unit.DAI));
        Item eight = new Item("eight", new EAN_8("21091353"), new Name("甘源青豆牛肉味", "甘源青豆"), Domestic.TIAN_JIN, new Specification("50g"),
                Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, vegetable_products.id(), Brand.UNDEFINED.id());
        itemRepository.save(eight);

        retailPrice = new RetailPrice(new Price(Money.of(6.90, currency), Unit.DAI));
        Item nine = new Item("nine", new EAN_8("21091346"), new Name("鸡肉味甘源青豆", "青豆"), Domestic.TIAN_JIN, new Specification("75g"),
                Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, vegetable_products.id(), Brand.UNDEFINED.id());
        itemRepository.save(nine);

        retailPrice = new RetailPrice(new Price(Money.of(5.00, currency), Unit.DUI));
        Item ten = new Item("ten", new EAN_13("6954695180551"), new Name("长虹5号碱性电池", "长虹电池"), new Domestic("四川", "绵阳"), new Specification("10粒缩卡装"),
                Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, Category.UNDEFINED.id(), changhong.id());
        itemRepository.save(ten);

        retailPrice = new RetailPrice(new Price(Money.of(5, currency), Unit.BEN));
        Item twelve = new Item("twelve", new EAN_13("6925834037159"), new Name("车线本"), new Domestic("浙江", "仓南县"),
                Specification.UNDEFINED, Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
        itemRepository.save(twelve);

        retailPrice = new RetailPrice(new Price(Money.of(32.00, currency), Unit.HE));
        Item thirteen = new Item(itemRepository.nextIdentity(), new EAN_13("4547691239136"), new Name("冈本天然乳胶橡胶避孕套", "冈本避孕套"), new Imported("泰国"),
                new Specification("10片装"), Grade.QUALIFIED, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
        itemRepository.save(thirteen);
    }
/*
    @AfterTest
    public void tearDown() {
        brandRepository.remove(Brand.UNDEFINED.id());
        brandRepository.remove("caihong");
        brandRepository.remove("tianyou");
        brandRepository.remove("changjhong");

        categoryRepository.remove(Category.UNDEFINED.id());
        categoryRepository.remove("skin");
        categoryRepository.remove("cosmetics");
        categoryRepository.remove("chemicals");
        categoryRepository.remove("food");
        categoryRepository.remove("root");

        itemRepository.remove("one");
        itemRepository.remove("two");
        itemRepository.remove("three");
        itemRepository.remove("four");
        itemRepository.remove("five");
        itemRepository.remove("six");
        itemRepository.remove("six_1");
        itemRepository.remove("six_2");
        itemRepository.remove("seven");
        itemRepository.remove("eight");
        itemRepository.remove("nine");
        itemRepository.remove("ten");
        itemRepository.remove("twelve");
        for (Item item : itemRepository.fromBarcode("4547691239136")) {
            itemRepository.remove(item.id());
        }
        itemRepository.remove("twelve");
    }
 */

    @Test(invocationCount = 1, threadPoolSize = 1)
    public void testFind() {
        Item six = itemRepository.find("six_1");
        Assert.assertNotNull(six);
        Item eight = itemRepository.find("eight");
        Assert.assertNotNull(eight);
        Item nine = itemRepository.find("nine1");
        Assert.assertNull(nine);
    }

    @Test
    public void testSave() {
        Item ten = itemRepository.find("ten");
        ten.rename(new Name("长虹5号碳性电池 ", "长虹1号"));
        itemRepository.save(ten);
        ten = itemRepository.find("ten");
        Assert.assertEquals(ten.name(), new Name("长虹5号碳性电池", "长虹1号"));

        Item six = itemRepository.find("six_1");
        Assert.assertNotNull(six);
        six.changeBarcode(new EAN_13("6923555240728"));
        itemRepository.save(six);
    }
}