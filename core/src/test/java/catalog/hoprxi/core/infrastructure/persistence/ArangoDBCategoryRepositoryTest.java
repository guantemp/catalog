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

package catalog.hoprxi.core.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import salt.hoprxi.id.LongId;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-08-09
 */
public class ArangoDBCategoryRepositoryTest {
    private static CategoryRepository repository = new ArangoDBCategoryRepository("catalog");

    @BeforeClass
    public void beforeClass() {
        repository.save(Category.UNDEFINED);
        Category root = Category.createCategoryRoot("root", Name.of("分类"));
        repository.save(root);
        //食品
        Category food = new Category("root", "food", Name.of("食品"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        repository.save(food);
        Category leisure_food = new Category("food", "leisure_food", Name.of("休闲食品"), "人们闲暇、休息时所吃的食品");
        repository.save(leisure_food);
        Category dry_fruits = new Category("leisure_food", "dry_fruits", "干果");
        repository.save(dry_fruits);
        Category puffed_food = new Category("leisure_food", "puffed_food", "膨化食品");
        repository.save(puffed_food);
        //酒水
        Category drinks = new Category("root", "drinks", "酒水", "酒类和水类的统称，指酒、水、饮料等液体可饮用的水");
        repository.save(drinks);
        Category liquor = new Category("drinks", "liquor", Name.of("白酒"), "以粮谷为主要原料，以大曲、小曲或麸曲及酒母等为糖化发酵剂，经蒸煮、糖化、发酵、蒸馏而制成的蒸馏酒");
        repository.save(liquor);
        Category wine = new Category("drinks", "wine", Name.of("葡萄酒"), "以葡萄为原料酿造的一种果酒。其酒精度高于啤酒而低于白酒。营养丰富，保健作用明显。");
        repository.save(wine);
        //日化
        Category chemicals = new Category("root", "chemicals", Name.of("日化"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        repository.save(chemicals);
        Category cosmetics = new Category("chemicals", "cosmetics", "化妆品",
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        repository.save(cosmetics);
        Category skin = new Category("cosmetics", "skin", Name.of("肤用化妆品"));
        repository.save(skin);
        Category medicine = new Category("skin", "medicine", Name.of("药用"));
        repository.save(medicine);
        Category Shampoo = new Category("chemicals", "Shampoo", "洗发水");
        repository.save(Shampoo);
        //will move to drinks sub and rename
        Category beer = new Category("skin", "beer", Name.of("啤酒"));
        repository.save(beer);
        //粮油
        Category grain_oil = new Category("root", "grain_oil", Name.of("粮油"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(grain_oil);
        Category rice_flour = new Category("grain_oil", "rice_flour", Name.of("米/面/杂粮"));
        repository.save(rice_flour);
        Category oil = new Category("grain_oil", "oil", Name.of("食用油"));
        repository.save(oil);
        Category grain_and_oil_products = new Category("grain_oil", "grain_and_oil_products", Name.of("粮油制品"));
        repository.save(grain_and_oil_products);
        //食用油
        Category rapeseed_oil = new Category("oil", "rapeseed_oil", Name.of("菜籽油"));
        repository.save(rapeseed_oil);
        Category soybean_oil = new Category("oil", "soybean_oil", Name.of("大豆油"));
        repository.save(soybean_oil);
        Category peanut_oil = new Category("oil", " peanut_oil", Name.of("花生油"));
        repository.save(peanut_oil);
        Category corn_oil = new Category("oil", " corn_oil", Name.of("玉米油"));
        repository.save(corn_oil);
        Category olive_oil = new Category("oil", " olive_oil", Name.of("橄榄油"));
        repository.save(olive_oil);
        Category sunflower_seed_oil = new Category("oil", " sunflower_seed_oil", Name.of("葵花籽油"));
        repository.save(sunflower_seed_oil);
        Category blended_oil = new Category("oil", " blended_oil", Name.of("调和油"));
        repository.save(blended_oil);
        //制品
        Category bread_cake = new Category("grain_and_oil_products", "bread_cake", Name.of("面包/蛋糕"));
        repository.save(bread_cake);
        Category flour = new Category("grain_oil", "flour", Name.of("面粉"));
        repository.save(flour);
        Category instant_noodles = new Category("grain_and_oil_products", "instant_noodles", Name.of("方便面"));
        repository.save(instant_noodles);
        Category fine_dried_noodles = new Category("grain_oil", "fine_dried_noodles", Name.of("挂面"));
        repository.save(fine_dried_noodles);
        //调味品
        Category condiment = new Category("root", "condiment", Name.of("调味品"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(condiment);
        Category sauce = new Category("condiment", "sauce", Name.of("调味汁"));
        repository.save(sauce);
        Category soy_sauce = new Category("sauce", "soy_sauce", Name.of("酱油"));
        repository.save(soy_sauce);
        Category vinegar = new Category("sauce", " vinegar", Name.of("醋"));
        repository.save(vinegar);
        Category seasoning_oil = new Category("sauce", " seasoning_oil", Name.of("调味油"));
        repository.save(seasoning_oil);
        Category flavoring = new Category("condiment", "flavoring", Name.of("调味料"));
        repository.save(flavoring);
        Category salt = new Category("flavoring", "salt", Name.of("盐"));
        repository.save(salt);
        Category chicken_essence_monosodium_glutamate = new Category("flavoring", "chicken_essence_and_monosodium_glutamate", Name.of("鸡精/味精"));
        repository.save(chicken_essence_monosodium_glutamate);
        //生鲜
        Category fresh = new Category("fresh", "fresh", Name.of("生鲜"), "未经烹调、制作等深加工过程，只做必要保鲜和简单整理上架而出售的初级产品，以及面包、熟食等现场加工品类商品的统称");
        repository.save(fresh);
        Category fruit = new Category("fresh", "fruit", "水果", "可食用的多汁液且有甜味的植物果实的统称");
        repository.save(fruit);
        Category meat = new Category("fresh", "  meat", Name.of("肉类"), "陆上肉食动物及其可食部分的附属品制成的食品的统称");
        repository.save(meat);
        Category vegetables = new Category("fresh", "  vegetables", Name.of("蔬菜"), "可以做菜吃的草本植物");
        repository.save(vegetables);
        Category aquatic = new Category("fresh", "  aquatic", Name.of("水产品"), "水产品是海洋和淡水渔业生产的动植物及其加工产品的统称");
        repository.save(aquatic);
        Category cookedFood = new Category("fresh", "  cookedFood", Name.of("熟食"), "是经过加工或焯水处理后的原料通过配好的卤汁、红油凉拌、熏烤、油炸等制作而成的菜肴");
        repository.save(cookedFood);
        //子类
        Category pork = new Category("meat", "  pork", Name.of("猪肉"), "猪肉及分割品");
        repository.save(pork);
        Category poultry = new Category("meat", "  poultry", Name.of("禽类"));
        repository.save(poultry);
        Category freshwaterFish = new Category("aquatic", "freshwaterFish", Name.of("淡水鱼"));
        repository.save(freshwaterFish);
        Category freshwaterCrabs = new Category("aquatic", "freshwaterCrabs", Name.of("淡水蟹"));
        repository.save(freshwaterCrabs);
        Category freshwaterOther = new Category("aquatic", "freshwaterOther", Name.of("其它淡水类"));
        repository.save(freshwaterOther);
        Category marineShrimp = new Category("aquatic", "marineShrimp", Name.of("海水虾"));
        repository.save(marineShrimp);
        Category driedFish = new Category("aquatic", " driedFish", Name.of("鱼干"));
        repository.save(driedFish);

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

    /*
        @AfterClass
        public void afterClass() {
            repository.remove("medicine");
            repository.remove("skin");
            repository.remove("cosmetics");
            repository.remove("chemicals");

            repository.remove("meat");
            repository.remove("fruit");
            repository.remove("fresh");

            repository.remove("wine");
            repository.remove("liquor");
            repository.remove("drinks");

            repository.remove("puffed_food");
            repository.remove("dry_fruits");
            repository.remove("leisure_food");
            repository.remove("food");

            repository.remove("root");
            repository.remove(Category.UNDEFINED.id());
        }
    */
    @Test(priority = 3)
    public void testBelongTo() {
        Category[] sub = repository.belongTo("root");
        Assert.assertEquals(5, sub.length);
        sub = repository.belongTo(Category.UNDEFINED.id());
        Assert.assertEquals(0, sub.length);
        sub = repository.belongTo("leisure_food");
        Assert.assertEquals(2, sub.length);
        sub = repository.belongTo("fresh");
        Assert.assertEquals(5, sub.length);
    }

    @Test(dependsOnMethods = {"testNextIdentity"})
    public void testFind() {
    }

    @Test
    public void testNextIdentity() {
        String id = String.valueOf(LongId.generate());
        Assert.assertNotNull(id);
    }

    @Test
    public void testRoot() {
        Category[] roots = repository.root();
        Assert.assertEquals(3, roots.length);
    }

    @Test(priority = 1, expectedExceptions = InvalidCategoryIdException.class)
    public void testSave() {
        Category beer = repository.find("beer");
        beer.moveTo("drinks");
        repository.save(beer);

        Category leisure_food = repository.find("leisure_food");
        Assert.assertNotNull(leisure_food);
        Category medicine = repository.find("medicine");
        medicine.moveTo("chemicals");
        repository.save(medicine);
        medicine = repository.find("medicine");
        Assert.assertEquals(medicine.parentId(), "chemicals");
        medicine.rename(Name.of("      个人保健用卫生制剂"));
        medicine.changeDescription("清洁制剂、驱虫");
        repository.save(medicine);
        medicine = repository.find("medicine");
        Assert.assertEquals(medicine.description(), "清洁制剂、驱虫");
        Assert.assertEquals(medicine.name().name(), "个人保健用卫生制剂");

        medicine.moveTo("hy");
    }
}