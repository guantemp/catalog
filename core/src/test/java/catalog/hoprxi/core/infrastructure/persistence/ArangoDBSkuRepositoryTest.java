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

package catalog.hoprxi.core.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.*;
import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcode;
import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.barcode.EAN_13;
import catalog.hoprxi.core.domain.model.barcode.EAN_8;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import org.javamoney.moneta.Money;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-06-05
 */
public class ArangoDBSkuRepositoryTest {
    private static SkuRepository skuRepository = new ArangoDBSkuRepository();
    private static BrandRepository brandRepository = new ArangoDBBrandRepository();
    private static CategoryRepository categoryRepository = new ArangoDBCategoryRepository();
    private static CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public static void setUpBeforeClass() {
        brandRepository.save(Brand.UNDEFINED);
        Brand caihong = new Brand("caihong", new Name("彩虹"));
        brandRepository.save(caihong);
        Brand tianyou = new Brand("tianyou", new Name("天友"));
        brandRepository.save(tianyou);
        Brand changhong = new Brand("changjhong", new Name("长虹"));
        brandRepository.save(changhong);

        categoryRepository.save((Category.UNDEFINED));
        Category root = Category.createRootCategory("root", "分类");
        categoryRepository.save(root);
        Category food = new Category("root", "food", "食品", "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        categoryRepository.save(food);
        Category chemicals = new Category("root", "chemicals", "日化", "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        categoryRepository.save(chemicals);
        Category cosmetics = new Category("chemicals", "cosmetics", "化妆品",
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        categoryRepository.save(cosmetics);
        Category skin = new Category("cosmetics", "skin", "肤用化妆品");
        categoryRepository.save(skin);

        EANUPCBarcode barcode = EANUPCBarcodeGenerateServices.createMatchingBarcode("6907861191394");
        MadeIn madeIn = new Domestic("四川", "成都");
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(19.59, currency), Unit.HE));
        Sku one = new Sku("one", barcode, new Name("150ml彩虹柠檬香电热灭蚊香液", "彩虹电热灭蚊香液"), madeIn,
                new Specification("150ml"), Grade.QUALIFIED, retailPrice, MemberPrice.ZERO, VipPrice.ZERO, caihong.id(), skin.id());
        skuRepository.save(one);

        retailPrice = new RetailPrice(new Price(Money.of(35.50, currency), Unit.HE));
        Sku two = new Sku("two", new EAN_13("6907861181388"), new Name("彩虹电热灭蚊香液橙子香型2瓶装", "彩虹电热灭蚊香液2瓶装"), madeIn,
                new Specification("2*150ml"), Grade.QUALIFIED, retailPrice, MemberPrice.ZERO, VipPrice.ZERO, caihong.id(), skin.id());
        skuRepository.save(two);

        retailPrice = new RetailPrice(new Price(Money.of(56.80, currency), Unit.HE));
        barcode = EANUPCBarcodeGenerateServices.createMatchingBarcode("6907861181395");
        Sku three = new Sku("three", barcode, new Name("彩虹电热灭蚊香液4瓶装（橙子+芒果香型）", "彩虹电热灭蚊香液4瓶装"), madeIn,
                new Specification("4*120ml"), Grade.QUALIFIED, retailPrice, MemberPrice.ZERO, VipPrice.ZERO, caihong.id(), skin.id());
        skuRepository.save(three);

        madeIn = new Domestic("重庆市");
        retailPrice = new RetailPrice(new Price(Money.of(4.50, currency), Unit.HE));
        MemberPrice memberPrice = new MemberPrice(new Price(Money.of(3.96, currency), Unit.HE));
        VipPrice vipPrice = new VipPrice("PLUS价", new Price(Money.of(3.00, currency), Unit.HE));
        Sku four = new Sku("four", new EAN_13("6942070284987"), new Name("天友南美酸奶", "天友南美酸奶"), madeIn,
                new Specification("350ml"), Grade.QUALIFIED, retailPrice, memberPrice, vipPrice, tianyou.id(), food.id());
        skuRepository.save(four);

        retailPrice = new RetailPrice(new Price(Money.of(55.00, currency), Unit.TI));
        memberPrice = new MemberPrice(new Price(Money.of(50.00, currency), Unit.TI));
        vipPrice = new VipPrice("PLUS价", new Price(Money.of(48.00, currency), Unit.TI));
        barcode = EANUPCBarcodeGenerateServices.createMatchingBarcode("6923555240896");
        Sku five = new Sku("five", barcode, new Name("天友纯牛奶", "天友纯牛奶"), madeIn,
                new Specification("350ml"), Grade.QUALIFIED, retailPrice, memberPrice, vipPrice, tianyou.id(), food.id());
        skuRepository.save(five);

        retailPrice = new RetailPrice(new Price(Money.of(17.90, currency), Unit.PCS));
        vipPrice = new VipPrice("PLUS价", new Price(Money.of(15.50, currency), Unit.PCS));
        Sku six = new Sku("six", new EAN_8("20075422"), new Name("天友纯牛奶组合装", "天友组合装"), madeIn,
                new Specification("6*250ml"), Grade.QUALIFIED, retailPrice, MemberPrice.ZERO, vipPrice, tianyou.id(), food.id());
        skuRepository.save(six);

        barcode = EANUPCBarcodeGenerateServices.createMatchingBarcode("6923555240865");
        Sku six_1 = new Sku("six_1", barcode, new Name("250ml天友纯牛奶(高钙）", "天友高钙纯牛奶"), madeIn,
                new Specification("250ml"), Grade.QUALIFIED, RetailPrice.ZERO, MemberPrice.ZERO, VipPrice.ZERO, tianyou.id(), food.id());
        skuRepository.save(six_1);

        retailPrice = new RetailPrice(new Price(Money.of(2.5, currency), Unit.DAI));
        barcode = EANUPCBarcodeGenerateServices.createMatchingBarcode("6923555240889");
        Sku six_2 = new Sku("six_2", barcode, new Name("250ml天友纯牛奶", "天友纯牛奶"), madeIn,
                new Specification("250ml"), Grade.QUALIFIED, retailPrice, MemberPrice.ZERO, VipPrice.ZERO, tianyou.id(), food.id());
        skuRepository.save(six_2);

        madeIn = new Domestic("天津市");
        retailPrice = new RetailPrice(new Price(Money.of(2.5, currency), Unit.DAI));
        Sku seven = new Sku("seven", new EAN_8("21091346"), new Name("麻辣味甘源青豆", "麻辣味甘源青豆"), madeIn, new Specification("25g"),
                Grade.QUALIFIED, retailPrice, Brand.UNDEFINED.id(), food.id());
        skuRepository.save(seven);

        retailPrice = new RetailPrice(new Price(Money.of(4.80, currency), Unit.DAI));
        Sku eight = new Sku("eight", new EAN_8("21091353"), new Name("甘源青豆牛肉味", "甘源青豆"), madeIn, new Specification("50g"),
                Grade.QUALIFIED, retailPrice, Brand.UNDEFINED.id(), food.id());
        skuRepository.save(eight);

        retailPrice = new RetailPrice(new Price(Money.of(6.90, currency), Unit.DAI));
        Sku nine = new Sku("nine", new EAN_8("21091346"), new Name("鸡肉味甘源青豆", "青豆"), madeIn, new Specification("75g"),
                Grade.QUALIFIED, retailPrice, Brand.UNDEFINED.id(), food.id());
        skuRepository.save(nine);

        retailPrice = new RetailPrice(new Price(Money.of(5.00, currency), Unit.DUI));
        Sku ten = new Sku("ten", new EAN_13("6954695180551"), new Name("长虹5号碱性电池", "长虹电池"), new Domestic("四川", "绵阳"), new Specification("10粒缩卡装"),
                Grade.QUALIFIED, retailPrice, changhong.id(), Category.UNDEFINED.id());
        skuRepository.save(ten);

        retailPrice = new RetailPrice(new Price(Money.of(3.50, currency), Unit.BEN));
        Sku twelve = new Sku("twelve", new EAN_13("6925834037159"), new Name("车线本"), new Domestic("浙江", "仓南县"),
                Specification.UNDEFINED, Grade.QUALIFIED, retailPrice, Brand.UNDEFINED.id(), Category.UNDEFINED.id());
        skuRepository.save(twelve);

        retailPrice = new RetailPrice(new Price(Money.of(25.00, currency), Unit.HE));
        Sku thirteen = new Sku(skuRepository.nextIdentity(), new EAN_13("4547691239136"), new Name("冈本天然乳胶橡胶避孕套", "冈本避孕套"), new Imported("泰国"),
                new Specification("10片装"), Grade.QUALIFIED, retailPrice, Brand.UNDEFINED.id(), Category.UNDEFINED.id());
        skuRepository.save(thirteen);
    }

    @AfterClass
    public static void teardown() {
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

        skuRepository.remove("one");
        skuRepository.remove("two");
        skuRepository.remove("three");
        skuRepository.remove("four");
        skuRepository.remove("five");
        skuRepository.remove("six");
        skuRepository.remove("six_1");
        skuRepository.remove("six_2");
        skuRepository.remove("seven");
        skuRepository.remove("eight");
        skuRepository.remove("nine");
        skuRepository.remove("ten");
        skuRepository.remove("twelve");

        for (Sku sku : skuRepository.fromBarcode("4547691239136"))
            skuRepository.remove(sku.id());

        skuRepository.remove("twelve");
    }

    @Test
    public void belongToBrand() {
        Sku[] skus = skuRepository.belongToBrand("caihong", 0, 3);
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.belongToBrand("caihong", 1, 3);
        Assert.assertEquals(skus.length, 2);
        skus = skuRepository.belongToBrand("caihong", 1, 1);
        Assert.assertEquals(skus.length, 1);
        skus = skuRepository.belongToBrand("caihong", 1, 0);
        Assert.assertEquals(skus.length, 0);
    }

    @Test
    public void belongToCategory() {
        Sku[] skus = skuRepository.belongToCategory("food", 0, 10);
        Assert.assertEquals(skus.length, 8);
        skus = skuRepository.belongToCategory("food", 2, 5);
        Assert.assertEquals(skus.length, 5);
        skus = skuRepository.belongToCategory("food", 5, 3);
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.belongToCategory("food", 3, 2);
        Assert.assertEquals(skus.length, 2);
        skus = skuRepository.belongToCategory("food", 0, 0);
        Assert.assertEquals(skus.length, 0);
        skus = skuRepository.belongToCategory("food", 7, 10);
        Assert.assertEquals(skus.length, 1);
    }

    @Test
    public void find() {
        Sku six = skuRepository.find("six_1");
        Assert.assertNotNull(six);
        Sku eight = skuRepository.find("eight");
        Assert.assertNotNull(eight);
        Sku nine = skuRepository.find("nine1");
        Assert.assertNull(nine);
    }

    @Test
    public void findAll() {
        Sku[] skus = skuRepository.findAll(0, 25);
        Assert.assertEquals(skus.length, 14);
        skus = skuRepository.findAll(12, 25);
        Assert.assertEquals(skus.length, 2);
        skus = skuRepository.findAll(5, 5);
        Assert.assertEquals(skus.length, 5);
    }

    @Test
    public void save() {
        Sku ten = skuRepository.find("ten");
        ten.rename(new Name("长虹5号碳性电池 ", "长虹1号"));
        skuRepository.save(ten);
        ten = skuRepository.find("ten");
        Assert.assertEquals(ten.name(), new Name("长虹5号碳性电池", "长虹1号"));

        Sku six = skuRepository.find("six_1");
        Assert.assertNotNull(six);
        six.changeBarcode(new EAN_13("6923555240728"));
        skuRepository.save(six);
    }

    @Test
    public void size() {
        Assert.assertEquals(skuRepository.size(), 14);
    }

    @Test
    public void fromBarcode() {
        Sku[] skus = skuRepository.fromBarcode("69235552");
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.fromBarcode("690");
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.fromBarcode("123465");
        Assert.assertEquals(skus.length, 0);
        skus = skuRepository.fromBarcode("4695");
        Assert.assertEquals(skus.length, 1);
    }

    @Test
    public void fromMnemonic() {
        Sku[] skus = skuRepository.fromMnemonic("^ch");
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.fromMnemonic("qd");
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.fromMnemonic("ch");
        Assert.assertEquals(skus.length, 4);
        skus = skuRepository.fromMnemonic("chetr");
        Assert.assertEquals(skus.length, 0);
        skus = skuRepository.fromMnemonic("ty");
        Assert.assertEquals(skus.length, 5);
    }

    @Test
    public void fromName() {
        Sku[] skus = skuRepository.fromName("彩虹");
        Assert.assertEquals(skus.length, 3);
        skus = skuRepository.fromName("^彩虹");
        Assert.assertEquals(skus.length, 2);
        skus = skuRepository.fromName("彩虹|长虹");
        Assert.assertEquals(skus.length, 4);
        skus = skuRepository.fromName("不知道");
        Assert.assertEquals(skus.length, 0);
        skus = skuRepository.fromName("天友|长虹|彩虹");
        Assert.assertEquals(skus.length, 9);
        skus = skuRepository.fromName("^天友|长虹|彩虹");
        Assert.assertEquals(skus.length, 7);
    }
}