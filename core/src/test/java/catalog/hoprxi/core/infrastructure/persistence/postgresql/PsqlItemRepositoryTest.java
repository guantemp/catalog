/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.infrastructure.persistence.postgresql;

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
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import org.javamoney.moneta.Money;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025-11-08
 */
public class PsqlItemRepositoryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465", "slave.tooo.top:6543:P$Qwe123465Pg");
    }

    private static final ItemRepository itemRepository = new PsqlItemRepository();
    private static final BrandRepository brandRepository = new PsqlBrandRepository();
    private static final CategoryRepository categoryRepository = new PsqlCategoryRepository();
    private static final CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeTest
    public void setUp() {
        brandRepository.save(Brand.UNDEFINED);
        Brand caihong = new Brand(52495569395175425L, new Name("彩虹"));
        brandRepository.save(caihong);
        Brand tianyou = new Brand(52495569395175426L, new Name("天友"));
        brandRepository.save(tianyou);
        Brand changhong = new Brand(52495569395175427L, new Name("长虹"));
        brandRepository.save(changhong);

        categoryRepository.save((Category.UNDEFINED));
        Category root = Category.root(52495569397272599L, new Name("item测试分类", "root"));
        categoryRepository.save(root);
        Category food = new Category(52495569397272599L, 52495569397272598L, new Name("食品", "food"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        categoryRepository.save(food);
        Category dairy = new Category(52495569397272598L, 52495569397272597L, new Name("乳制品", "dairy"), "使用牛乳或羊乳及其加工制品为主要原料，加入或不加入适量的维生素、矿物质和其他辅料，使用法律法规及标准规定所要求的条件，经加工制成的各种食品");
        categoryRepository.save(dairy);
        Category vegetable_products = new Category(52495569397272598L, 52495569397272596L, new Name("蔬菜制品", " vegetable_products"), "以蔬菜和食用菌为原料，采用腌制、干燥、油炸等工艺加工而成的各种蔬菜制品，即酱腌菜、蔬菜干制品、食用菌制品、其他蔬菜制品");
        categoryRepository.save(vegetable_products);
        Category chemicals = new Category(52495569397272599L, 52495569397272595L, new Name("日化", "chemicals"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        categoryRepository.save(chemicals);
        Category cosmetics = new Category(52495569397272595L, 52495569397272594L, new Name("化妆品", "cosmetics"), "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        categoryRepository.save(cosmetics);
        Category skin = new Category(52495569397272594L, 52495569397272593L, new Name("肤用化妆品", "skin"));
        categoryRepository.save(skin);


        Barcode barcode = BarcodeGenerateServices.createBarcode("6907861191394");
        MadeIn madeIn = new Domestic("510100", "成都");
        LastReceiptPrice lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(12.11, currency), UnitEnum.HE));
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(19.59, currency), UnitEnum.HE));
        Item one = new Item(52496163982907400L, barcode, new Name("150ml彩虹柠檬香电热灭蚊香液", "150ml彩虹电热灭蚊香液"), madeIn, new Specification("150ml"), GradeEnum.QUALIFIED, new ShelfLife(180), lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, skin.id(), caihong.id());
        itemRepository.save(one);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(21.98, currency), UnitEnum.HE));
        retailPrice = new RetailPrice(new Price(Money.of(35.50, currency), UnitEnum.HE));
        Item two = new Item(52496163982907401L, new EAN_13("6907861181388"), new Name("彩虹电热灭蚊香液橙子香型2瓶装", "彩虹电热灭蚊香液2瓶装"), madeIn, new Specification("2*150ml"), GradeEnum.QUALIFIED, new ShelfLife(180), lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, skin.id(), caihong.id());
        itemRepository.save(two);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(32, currency), UnitEnum.HE));
        retailPrice = new RetailPrice(new Price(Money.of(56.80, currency), UnitEnum.HE));
        barcode = BarcodeGenerateServices.createBarcode("6907861181395");
        Item three = new Item(52496163982907402L, barcode, new Name("彩虹电热灭蚊香液4瓶装（橙子+芒果香型）", "彩虹电热灭蚊香液4瓶装"), madeIn, new Specification("4*120ml"), GradeEnum.QUALIFIED, new ShelfLife(180), lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, skin.id(), caihong.id());
        itemRepository.save(three);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(2.9, currency), UnitEnum.HE));
        retailPrice = new RetailPrice(new Price(Money.of(4.50, currency), UnitEnum.HE));
        MemberPrice memberPrice = new MemberPrice(new Price(Money.of(3.96, currency), UnitEnum.HE));
        VipPrice vipPrice = new VipPrice("PLUS价", new Price(Money.of(3.00, currency), UnitEnum.HE));
        Item four = new Item(52496163982907403L, new EAN_13("6942070284987"), new Name("天友南美酸奶", "天友南美酸奶"), Domestic.CHONG_QING, new Specification("350ml"), GradeEnum.QUALIFIED, new ShelfLife(30), lastReceiptPrice, retailPrice, memberPrice, vipPrice, dairy.id(), tianyou.id());
        itemRepository.save(four);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(8.58, currency), UnitEnum.TI));
        retailPrice = new RetailPrice(new Price(Money.of(10.00, currency), UnitEnum.TI));
        memberPrice = new MemberPrice(new Price(Money.of(9.5, currency), UnitEnum.TI));
        vipPrice = new VipPrice("PLUS", new Price(Money.of(8.8, currency), UnitEnum.TI));
        barcode = BarcodeGenerateServices.createBarcode("6923555240896");
        Item five = new Item(52496163982907404L, barcode, new Name("天友纯牛奶", "天友纯牛奶"), Domestic.CHONG_QING, new Specification("350ml"), GradeEnum.QUALIFIED, new ShelfLife(90), lastReceiptPrice, retailPrice, memberPrice, vipPrice, dairy.id(), tianyou.id());
        itemRepository.save(five);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(12.1, currency), UnitEnum.ZHU));
        retailPrice = new RetailPrice(new Price(Money.of(17.90, currency), UnitEnum.ZHU));
        memberPrice = new MemberPrice(new Price(Money.of(16.99, currency), UnitEnum.ZHU));
        vipPrice = new VipPrice("PLUS", new Price(Money.of(15.50, currency), UnitEnum.ZHU));
        Item six = new Item(52496163982907405L, new EAN_8("20075422"), new Name("天友纯牛奶组合装", "天友组合装"), Domestic.CHONG_QING, new Specification("6*250ml"), GradeEnum.QUALIFIED, new ShelfLife(90), lastReceiptPrice, retailPrice, memberPrice, vipPrice, food.id(), tianyou.id());
        itemRepository.save(six);

        barcode = BarcodeGenerateServices.createBarcode("6923555240865");
        Item six_1 = new Item(52496321492179000L, barcode, new Name("250ml天友纯牛奶(高钙）", "250ml天友高钙纯牛奶"), Domestic.CHONG_QING, new Specification("250ml"), GradeEnum.QUALIFIED, new ShelfLife(90), LastReceiptPrice.RMB_PCS_ZERO, RetailPrice.RMB_PCS_ZERO, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, dairy.id(), tianyou.id());
        itemRepository.save(six_1);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(25.82, currency), UnitEnum.DAI));
        retailPrice = new RetailPrice(new Price(Money.of(27.5, currency), UnitEnum.DAI));
        barcode = BarcodeGenerateServices.createBarcode("6923555240889");
        Item six_2 = new Item(52496321492179001L, barcode, new Name("250ml天友纯牛奶", "250ml天友纯牛奶"), Domestic.CHONG_QING, new Specification("250ml"), GradeEnum.QUALIFIED, new ShelfLife(90), lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, dairy.id(), tianyou.id());
        itemRepository.save(six_2);

        retailPrice = new RetailPrice(new Price(Money.of(2.5, currency), UnitEnum.DAI));
        Item seven = new Item(52496321492179002L, new EAN_8("21091346"), new Name("麻辣味甘源青豆", "麻辣味甘源青豆"), Domestic.TIAN_JIN, new Specification("25g"), GradeEnum.QUALIFIED, new ShelfLife(180), LastReceiptPrice.RMB_PCS_ZERO, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, food.id(), Brand.UNDEFINED.id());
        itemRepository.save(seven);

        retailPrice = new RetailPrice(new Price(Money.of(4.80, currency), UnitEnum.DAI));
        Item eight = new Item(52496321492179003L, new EAN_8("21091353"), new Name("甘源青豆牛肉味", "甘源青豆"), Domestic.TIAN_JIN, new Specification("50g"), GradeEnum.QUALIFIED, new ShelfLife(180), LastReceiptPrice.RMB_PCS_ZERO, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, vegetable_products.id(), Brand.UNDEFINED.id());
        itemRepository.save(eight);

        retailPrice = new RetailPrice(new Price(Money.of(6.90, currency), UnitEnum.DAI));
        Item nine = new Item(52496321492179004L, new EAN_8("21091346"), new Name("鸡肉味甘源青豆", "青豆"), Domestic.TIAN_JIN, new Specification("75g"), GradeEnum.QUALIFIED, new ShelfLife(180), LastReceiptPrice.RMB_PCS_ZERO, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, vegetable_products.id(), Brand.UNDEFINED.id());
        itemRepository.save(nine);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(3.5, currency), UnitEnum.DUI));
        retailPrice = new RetailPrice(new Price(Money.of(5.00, currency), UnitEnum.DUI));
        //memberPrice=new MemberPrice(new Price(Money.of(4.5, currency), UnitEnum.DUI));
        Item ten = new Item(52496321492179005L, new EAN_13("6954695180551"), new Name("长虹5号碱性电池", "长虹电池"), new Domestic("510700", "绵阳市"), new Specification("10粒缩卡装"), GradeEnum.QUALIFIED, new ShelfLife(360 * 3), lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, Category.UNDEFINED.id(), changhong.id());
        itemRepository.save(ten);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(2.5, currency), UnitEnum.BEN));
        retailPrice = new RetailPrice(new Price(Money.of(5, currency), UnitEnum.BEN));
        Item twelve = new Item(52496321492179006L, new EAN_13("6925834037159"), new Name("车线本"), new Domestic("330500", "湖州市"), Specification.UNDEFINED, GradeEnum.QUALIFIED, lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
        itemRepository.save(twelve);

        lastReceiptPrice = new LastReceiptPrice(new Price(Money.of(8.9, currency), UnitEnum.HE));
        retailPrice = new RetailPrice(new Price(Money.of(32.00, currency), UnitEnum.HE));
        Item thirteen = new Item(52496321492179007L, new EAN_13("4547691239136"), new Name("冈本天然乳胶橡胶避孕套", "冈本避孕套"), new Imported("764", "泰国"), new Specification("10片装"), GradeEnum.QUALIFIED, new ShelfLife(360 * 3), lastReceiptPrice, retailPrice, MemberPrice.RMB_PCS_ZERO, VipPrice.RMB_PCS_ZERO, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
        itemRepository.save(thirteen);
    }

    @AfterTest
    public void tearDown() {
        itemRepository.delete(52496163982907400L);
        itemRepository.delete(52496163982907401L);
        itemRepository.delete(52496163982907402L);
        itemRepository.delete(52496163982907403L);
        itemRepository.delete(52496163982907404L);
        itemRepository.delete(52496163982907405L);
        itemRepository.delete(52496321492179000L);
        itemRepository.delete(52496321492179001L);
        itemRepository.delete(52496321492179002L);
        itemRepository.delete(52496321492179003L);
        itemRepository.delete(52496321492179004L);
        itemRepository.delete(52496321492179005L);
        itemRepository.delete(52496321492179006L);
        itemRepository.delete(52496321492179007L);

        //brandRepository.remove(Brand.UNDEFINED.id());
        brandRepository.remove(52495569395175425L);
        brandRepository.remove(52495569395175426L);
        brandRepository.remove(52495569395175427L);

        //categoryRepository.remove(Category.UNDEFINED.id());
        categoryRepository.remove(52495569397272593L);
        categoryRepository.remove(52495569397272594L);
        categoryRepository.remove(52495569397272595L);
        categoryRepository.remove(52495569397272596L);
        categoryRepository.remove(52495569397272597L);
        categoryRepository.remove(52495569397272598L);
        categoryRepository.remove(52495569397272599L);
    }


    @Test(invocationCount = 1, threadPoolSize = 1, priority = 2)
    public void testSave() {
        Item ten = itemRepository.find(52496321492179005L);
        System.out.println("52496321492179005L:\n" + ten);
        //old Name("长虹5号碱性电池", "长虹电池")
        ten.rename(new Name("长虹5号碳性电池 ", "长虹1号"));
        itemRepository.save(ten);
        ten = itemRepository.find(52496321492179005L);
        Assert.assertEquals(ten.name(), new Name("长虹5号碳性电池", "长虹1号"));
        // old Name("250ml天友纯牛奶(高钙）", "250ml天友高钙纯牛奶")
        Item six = itemRepository.find(52496163982907405L);
        Assert.assertNotNull(six);
        six.changeBarcode(new EAN_13("6923555240728"));
        six.adjustRetailPrice(new RetailPrice(new Price(Money.of(29.9, currency), UnitEnum.ZHU)));
        six.changeGrade(GradeEnum.PREMIUM);
        six.adjustVipPrice(new VipPrice(new Price(Money.of(14.9, currency), UnitEnum.ZHU)));
        six.moveToNewCategory(52495569397272598L);
        itemRepository.save(six);
        six = itemRepository.find(52496163982907405L);
        Assert.assertEquals(six.barcode(), BarcodeGenerateServices.createBarcode("6923555240728"));
        Assert.assertEquals(six.categoryId(), 52495569397272598L);
        System.out.println("52496163982907405L  has changed:\n" + six);

        Item ce = itemRepository.find(52496321492179006L);
        Assert.assertNotNull(ce);
        System.out.println("52496321492179006L:\n" + ce);
    }
}