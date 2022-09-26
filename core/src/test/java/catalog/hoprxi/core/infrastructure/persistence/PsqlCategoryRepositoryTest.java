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

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-09-26
 */
public class PsqlCategoryRepositoryTest {
    private static final CategoryRepository repository = new PsqlCategoryRepository("catalog");

    @BeforeClass
    public void beforeClass() {
        repository.save(Category.UNDEFINED);
        Category root = Category.root("496796322118291457", new Name("分类", "root"));
        repository.save(root);
        //食品
        Category food = new Category("496796322118291457", "496796322118291460", new Name("食品", "food"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        repository.save(food);
        Category leisure_food = new Category("496796322118291460", "496796322118291461", new Name("休闲食品", "leisure_food"), "人们闲暇、休息时所吃的食品");
        repository.save(leisure_food);
        Category dry_fruits = new Category("496796322118291461", "496796322118291462", new Name("干果", "dry_fruits"));
        repository.save(dry_fruits);
        Category puffed_food = new Category("496796322118291461", "496796322118291463", new Name("膨化食品", "puffed_food"));
        repository.save(puffed_food);
        //酒水
        Category drinks = new Category("496796322118291457", "496796322118291470", new Name("酒水", "drinks"), "酒类和水类的统称，指酒、水、饮料等液体可饮用的水");
        repository.save(drinks);
        Category liquor = new Category("496796322118291470", "496796322118291471", new Name("白酒", "liquor"), "以粮谷为主要原料，以大曲、小曲或麸曲及酒母等为糖化发酵剂，经蒸煮、糖化、发酵、蒸馏而制成的蒸馏酒");
        repository.save(liquor);
        Category wine = new Category("496796322118291470", "496796322118291472", new Name("葡萄酒", "wine"), "以葡萄为原料酿造的一种果酒。其酒精度高于啤酒而低于白酒。营养丰富，保健作用明显。");
        repository.save(wine);
        Category Yellow_wine = new Category("496796322118291470", "496796322118291473", new Name("黄酒", "yellow_wine"), "以稻米、黍米、黑米、玉米、小麦等为原料，经过蒸料，拌以麦曲、米曲或酒药，进行糖化和发酵酿制而成的各类酒");
        repository.save(Yellow_wine);
        //日化
        Category chemicals = new Category("496796322118291457", "496796322118291480", new Name("日化", "chemicals"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        repository.save(chemicals);
        Category cosmetics = new Category("496796322118291480", "496796322118291481", new Name("化妆品", "cosmetics"),
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        repository.save(cosmetics);
        Category washing = new Category("496796322118291480", "496796322118291482", new Name("洗涤用品", "washing"));
        repository.save(washing);
        Category soap = new Category("496796322118291482", "496796322118291483", new Name("肥皂", "soap"));
        repository.save(soap);
        Category washing_liquid = new Category("496796322118291482", "496796322118291484", new Name("洗衣液", "washing_liquid"));
        repository.save(washing_liquid);
        Category oral_hygiene = new Category("496796322118291480", "496796322118291485", new Name("口腔用品", "oral_hygiene"));
        repository.save(oral_hygiene);
        Category clean = new Category("496796322118291480", "496796322118291486", new Name("清洁/卫生用品", "clean"));
        repository.save(clean);
        Category hari = new Category("496796322118291480", "496796322118291487", "洗/护发用品");
        repository.save(hari);
        //will move to drinks sub and rename
        Category beer = new Category("496796322118291482", "496796322118291488", new Name("      个人保健用卫生制剂", "beer"));
        repository.save(beer);
        //粮油
        Category grain_oil = new Category("496796322118291457", "496796322118291490", new Name("粮油", "grain_oil"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(grain_oil);
        Category rice_flour = new Category("496796322118291490", "496796322118291491", new Name("米/面/杂粮", "rice_flour"));
        repository.save(rice_flour);
        Category oil = new Category("496796322118291490", "496796322118291492", new Name("食用油", "oil"));
        repository.save(oil);
        Category grain_and_oil_products = new Category("496796322118291490", "496796322118291493", new Name("粮油制品", "grain_and_oil_products"));
        repository.save(grain_and_oil_products);
        //食用油
        Category rapeseed_oil = new Category("496796322118291492", "496796322118291494", new Name("菜籽油", "rapeseed_oil"), "用油菜籽榨出来的一种食用油。是我国主要食用油之一");
        repository.save(rapeseed_oil);
        Category soybean_oil = new Category("496796322118291492", "496796322118291495", new Name("大豆油", "soybean_oil"));
        repository.save(soybean_oil);
        Category peanut_oil = new Category("496796322118291492", " 496796322118291496", new Name("花生油", " peanut_oil"));
        repository.save(peanut_oil);
        Category corn_oil = new Category("496796322118291492", "496796322118291497", new Name("玉米油", " corn_oil"), "又叫粟米油、玉米胚芽油，它是从玉米胚芽中提炼出的油");
        repository.save(corn_oil);
        Category olive_oil = new Category("496796322118291492", "496796322118291498", new Name("橄榄油", " olive_oil "));
        repository.save(olive_oil);
        Category sunflower_seed_oil = new Category("496796322118291492", "496796322118291499", new Name("葵花籽油", " sunflower_seed_oil"), "是向日葵的果实。它的子仁中含脂肪30%-45%，最多的可达60%。葵花子油颜色金黄，澄清透明，气味清香，是一种重要的食用油。");
        repository.save(sunflower_seed_oil);
        Category blended_oil = new Category("496796322118291492", "496796322118291500", new Name("调和油", "blended_oil"));
        repository.save(blended_oil);
        //制品
        Category bread_cake = new Category("grain_and_oil_products", "bread_cake", Name.of("面包/蛋糕"));
        repository.save(bread_cake);
        Category flour = new Category("grain_oil", "flour", Name.of("面粉"));
        repository.save(flour);
        Category instant_noodles = new Category("grain_and_oil_products", "instant_noodles", Name.of("方便面"), "是一种可在短时间之内用热水泡熟食用的面制食品。");
        repository.save(instant_noodles);
        Category fine_dried_noodles = new Category("grain_oil", "fine_dried_noodles", Name.of("挂面"));
        repository.save(fine_dried_noodles);
        //调味品
        Category condiment = new Category("root", "condiment", Name.of("调味品"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(condiment);
        Category sauce = new Category("condiment", "sauce", Name.of("调味汁"));
        repository.save(sauce);
        Category soy_sauce = new Category("sauce", "soy_sauce", new Name("酱油", "soy_sauce"), "用大豆或脱脂大豆或黑豆、小麦或麸皮，加入水、食盐酿造而成的液体调味品，色泽呈红褐色，有独特酱香，滋味鲜美，有助于促进食欲。");
        repository.save(soy_sauce);
        Category vinegar = new Category("sauce", " vinegar", Name.of("醋"));
        repository.save(vinegar);
        Category seasoning_oil = new Category("sauce", " seasoning_oil", new Name("调味油", " seasoning_oil"));
        repository.save(seasoning_oil);
        Category flavoring = new Category("condiment", "flavoring", Name.of("调味料"));
        repository.save(flavoring);
        Category salt = new Category("flavoring", "salt", Name.of("盐"));
        repository.save(salt);
        Category chicken_essence_monosodium_glutamate = new Category("flavoring", "chicken_essence_and_monosodium_glutamate", Name.of("鸡精/味精"));
        repository.save(chicken_essence_monosodium_glutamate);
    /*
        Brand lancome = new Brand("lancome", "兰蔻");
        Brand shiseido = new Brand("shiseido", "资生堂");
        brandRepository.save(lancome);
        brandRepository.save(shiseido);
        SpecificationFamily families = new SpecificationFamily.Builder("品牌")
                .with(new BrandSpecification(shiseido.toBrandDescriptor()))
                .with(new BrandSpecification(lancome.toBrandDescriptor()))
                .build();
         */
    }

    @Test
    public void testFind() {
    }

    @Test
    public void testRemove() {
    }

    @Test
    public void testRoot() {
    }

    @Test
    public void testSave() {
    }
}