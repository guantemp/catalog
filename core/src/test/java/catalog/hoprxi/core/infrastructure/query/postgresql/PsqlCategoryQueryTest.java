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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.net.MalformedURLException;
import java.net.URI;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-21
 */
public class PsqlCategoryQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final CategoryRepository repository = new PsqlCategoryRepository();

    @BeforeClass
    public void beforeClass() throws MalformedURLException {
        repository.save(Category.UNDEFINED);
        Category root = Category.root(496796322118291457l, new Name("商品分类", "root"));
        repository.save(root);
        //食品
        Category food = new Category(496796322118291457l, 496796322118291460l, new Name("食品", "food"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        repository.save(food);
        Category leisure_food = new Category(496796322118291460l, 496796322118291461l, new Name("休闲食品", "leisure_food"), "人们闲暇、休息时所吃的食品");
        repository.save(leisure_food);
        Category dry_fruits = new Category(496796322118291461l, 496796322118291462l, new Name("干果", "dry_fruits"));
        repository.save(dry_fruits);
        Category puffed_food = new Category(496796322118291461l, 496796322118291463l, new Name("膨化食品", "puffed_food"));
        repository.save(puffed_food);
        //酒水
        Category drinks = new Category(496796322118291457l, 496796322118291470l, new Name("酒", "alcoholic_liquor"), "用粮食、水果等含淀粉或糖的物质发酵制成的含乙醇的饮料。");
        repository.save(drinks);
        Category liquor = new Category(496796322118291470l, 496796322118291471l, new Name("白酒", "liquor"), "以粮谷为主要原料，以大曲、小曲或麸曲及酒母等为糖化发酵剂，经蒸煮、糖化、发酵、蒸馏而制成的蒸馏酒");
        repository.save(liquor);
        Category wine = new Category(496796322118291470l, 496796322118291472l, new Name("葡萄酒", "wine"), "以葡萄为原料酿造的一种果酒。其酒精度高于啤酒而低于白酒。营养丰富，保健作用明显。");
        repository.save(wine);
        Category Yellow_wine = new Category(496796322118291470l, 496796322118291473l, new Name("黄酒", "yellow_wine"), "以稻米、黍米、黑米、玉米、小麦等为原料，经过蒸料，拌以麦曲、米曲或酒药，进行糖化和发酵酿制而成的各类酒");
        repository.save(Yellow_wine);
        //日化
        Category chemicals = new Category(496796322118291457l, 496796322118291480l, new Name("日化", "chemicals"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        repository.save(chemicals);
        Category cosmetics = new Category(496796322118291480l, 496796322118291481l, new Name("化妆品", "cosmetics"),
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        repository.save(cosmetics);
        Category oral_hygiene = new Category(496796322118291480l, 496796322118291485l, new Name("口腔用品", "oral_hygiene"));
        repository.save(oral_hygiene);
        Category clean = new Category(496796322118291480l, 496796322118291486l, new Name("清洁、卫生用品", "clean"));
        repository.save(clean);
        Category hari = new Category(496796322118291480l, 496796322118291487l, "洗、护发用品");
        repository.save(hari);
        Category washing = new Category(496796322118291480l, 496796322118291482l, new Name("洗涤用品", "washing"));
        repository.save(washing);

        Category soap = new Category(496796322118291482l, 496796322118291483l, new Name("肥皂", "soap"), "脂肪酸金属盐的总称");
        repository.save(soap);
        Category washing_liquid = new Category(496796322118291482l, 496796322118291484l, new Name("洗衣液", "washing_liquid"), "多采用非离子型表面活性剂，PH接近中性，对皮肤温和，并且排入自然界后，降解较洗衣粉快");
        repository.save(washing_liquid);
        //one error alias chemicals
        Category beer = new Category(496796322118291482l, 496796322118291488l, new Name("      个人保健用卫生制剂", "beer"));
        repository.save(beer);
        //粮油
        Category grain_oil = new Category(496796322118291457l, 496796322118291490l, new Name("粮油", "grain_oil"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(grain_oil);
        Category rice_flour = new Category(496796322118291490l, 496796322118291491l, new Name("米、面、杂粮", "rice_flour"));
        repository.save(rice_flour);
        Category grain_and_oil_products = new Category(496796322118291490l, 496796322118291493l, new Name("粮油制品", "grain_and_oil_products"));
        repository.save(grain_and_oil_products);

        Category oil = new Category(496796322118291490l, 496796322118291492l, new Name("食用油", "oil"));  //食用油
        repository.save(oil);
        Category rapeseed_oil = new Category(496796322118291492l, 496796322118291494l, new Name("菜籽油", "rapeseed_oil"), "用油菜籽榨出来的一种食用油。是我国主要食用油之一");
        repository.save(rapeseed_oil);
        Category soybean_oil = new Category(496796322118291492l, 496796322118291495l, new Name("大豆油", "soybean_oil"));
        repository.save(soybean_oil);
        Category peanut_oil = new Category(496796322118291492l, 496796322118291496l, new Name("花生油", " peanut_oil"));
        repository.save(peanut_oil);
        Category corn_oil = new Category(496796322118291492l, 496796322118291497l, new Name("玉米油", " corn_oil"), "又叫粟米油、玉米胚芽油，它是从玉米胚芽中提炼出的油");
        repository.save(corn_oil);
        Category olive_oil = new Category(496796322118291492l, 496796322118291498l, new Name("橄榄油", " olive_oil "), "由新鲜的油橄榄果实直接冷榨而成的，不经加热和化学处理，保留了天然营养成分，橄榄油被认为是迄今所发现的油脂中最适合人体营养的油脂");
        repository.save(olive_oil);
        Category sunflower_seed_oil = new Category(496796322118291492l, 496796322118291499l, new Name("葵花籽油", " sunflower_seed_oil"), "是向日葵的果实。它的子仁中含脂肪30%-45%，最多的可达60%。葵花子油颜色金黄，澄清透明，气味清香，是一种重要的食用油。");
        repository.save(sunflower_seed_oil);
        Category blended_oil = new Category(496796322118291492l, 496796322118291700l, new Name("调和油", "blended_oil"), "根据使用需要，将两种以上经精炼的油脂（香味油除外）按比例调配制成的食用油");
        repository.save(blended_oil);

        Category bread_cake = new Category(496796322118291493l, 49581450261846020l, new Name("面包、蛋糕", "bread_cake")); //粮油制品
        repository.save(bread_cake);
        Category flour = new Category(496796322118291493l, 49581450261846021l, new Name("面粉", "flour"));
        repository.save(flour);
        Category instant_noodles = new Category(496796322118291493l, 49581450261846022l, new Name("方便面", "instant_noodles"), "是一种可在短时间之内用热水泡熟食用的面制食品。");
        repository.save(instant_noodles);
        Category fine_dried_noodles = new Category(496796322118291493l, 49581450261846023l, new Name("挂面", "fine_dried_noodles"));
        repository.save(fine_dried_noodles);
        //调味品
        Category condiment = new Category(496796322118291457l, 49581450261846035l, new Name("调味品", "condiment"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(condiment);
        Category sauce = new Category(49581450261846035l, 49581450261846040l, new Name("调味汁", "sauce"));
        repository.save(sauce);
        Category soy_sauce = new Category(49581450261846040l, 49581450261846041l, new Name("酱油", "soy_sauce"), "用大豆或脱脂大豆或黑豆、小麦或麸皮，加入水、食盐酿造而成的液体调味品，色泽呈红褐色，有独特酱香，滋味鲜美，有助于促进食欲。", URI.create("https://inews.gtimg.com/newsapp_bt/0/13781122372/1000").toURL());
        repository.save(soy_sauce);
        Category vinegar = new Category(49581450261846040l, 49581450261846042l, new Name("醋", " vinegar"));
        repository.save(vinegar);
        Category seasoning_oil = new Category(49581450261846040l, 49581450261846043l, new Name("调味油", " seasoning_oil"));
        repository.save(seasoning_oil);
        Category flavoring = new Category(49581450261846035l, 49581450261846057l, new Name("调味料", "flavoring"));
        repository.save(flavoring);
        Category salt = new Category(49581450261846057l, 49581450261846058l, new Name("盐", "salt"));
        repository.save(salt);
        Category chicken_essence_monosodium_glutamate = new Category(49581450261846057l, 49581450261846059l, new Name("鸡精、味精", "chicken_essence_and_monosodium_glutamate"));
        repository.save(chicken_essence_monosodium_glutamate);
    }

    @AfterClass
    public void afterClass() {
        repository.remove(49581450261846059l);
        repository.remove(49581450261846058l);
        repository.remove(49581450261846057l);
        repository.remove(49581450261846043l);
        repository.remove(49581450261846042l);
        repository.remove(49581450261846041l);
        repository.remove(49581450261846040l);
        repository.remove(49581450261846035l);

        repository.remove(49581450261846023l);
        repository.remove(49581450261846022l);
        repository.remove(49581450261846021l);
        repository.remove(49581450261846020l);

        repository.remove(496796322118291700l);
        repository.remove(496796322118291499l);
        repository.remove(496796322118291498l);
        repository.remove(496796322118291497l);
        repository.remove(496796322118291496l);
        repository.remove(496796322118291495l);
        repository.remove(496796322118291494l);
        repository.remove(496796322118291493l);
        repository.remove(496796322118291492l);
        repository.remove(496796322118291491l);
        repository.remove(496796322118291490l);

        repository.remove(496796322118291488l);
        repository.remove(496796322118291487l);
        repository.remove(496796322118291486l);
        repository.remove(496796322118291485l);
        repository.remove(496796322118291484l);
        repository.remove(496796322118291483l);
        repository.remove(496796322118291482l);
        repository.remove(496796322118291481l);
        repository.remove(496796322118291480l);

        repository.remove(496796322118291473l);
        repository.remove(496796322118291472l);
        repository.remove(496796322118291471l);
        repository.remove(496796322118291470l);

        repository.remove(496796322118291463l);
        repository.remove(496796322118291462l);
        repository.remove(496796322118291461l);
        repository.remove(496796322118291460l);

        repository.remove(496796322118291457l);
        repository.remove(Category.UNDEFINED.id());
    }

/*
    @Test(invocationCount = 2, threadPoolSize = 1)
    public void testRoot() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView[] root = query.root();
        Assert.assertEquals(3, root.length);
        for (CategoryView view : root)
            System.out.println(view);
    }

    @Test(invocationCount = 4, threadPoolSize = 1, dependsOnMethods = {"testDescendants"})
    public void testQuery() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView root = query.query(496796322118291457l);
        Assert.assertTrue(root.isRoot());
        root = query.query(Category.UNDEFINED.id());
        Assert.assertTrue(root.isRoot());
        CategoryView grain_oil = query.query(496796322118291490l);//grain_oil
        Assert.assertNotNull(grain_oil);
        CategoryView oil = query.query(496796322118291492l);//oil
        Assert.assertNotNull(oil);
        CategoryView corn_oil = query.query(496796322118291497l);//corn_oil
        Assert.assertNotNull(corn_oil);
        CategoryView sunflower_seed_oil = query.query(496796322118291499l);//sunflower_seed_oil
        Assert.assertNotNull(sunflower_seed_oil);
    }

    @Test(invocationCount = 4, threadPoolSize = 1, dependsOnMethods = {"testDescendants"})
    public void testChildren() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView[] sub = query.children(496796322118291457l);//root
        Assert.assertEquals(5, sub.length);
        sub = query.children(496796322118291460l);//food
        Assert.assertEquals(1, sub.length);
        sub = query.children(496796322118291490l);//grand_oil
        Assert.assertEquals(3, sub.length);
        sub = query.children(496796322118291492l);//oil
        Assert.assertEquals(7, sub.length);
        sub = query.children(496796322118291461l);//leisure_food
        Assert.assertEquals(2, sub.length);
    }

    @Test(invocationCount = 1, priority = 1, dependsOnMethods = {"testRoot"})
    public void testDescendants() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView[] descendants = query.descendants(496796322118291457l);//root
        Assert.assertEquals(40, descendants.length);
        descendants = query.descendants(496796322118291490l);//grand_oil
        Assert.assertEquals(14, descendants.length);
        descendants = query.descendants(496796322118291492l);//oil
        Assert.assertEquals(7, descendants.length);
        //for (CategoryView c : descendants)
        //System.out.println("descendants:" + c);
        descendants = query.descendants(496796322118291480l);//chemicals
        Assert.assertEquals(8, descendants.length);
        descendants = query.descendants(123456);
        Assert.assertEquals(0, descendants.length);
    }

    @Test(dependsOnMethods = {"testDescendants"}, invocationCount = 10)
    public void testQueryByName() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView[] result = query.queryByName("oil");
        Assert.assertEquals(11, result.length);
        result = query.queryByName("oil$");
        Assert.assertEquals(10, result.length);
        result = query.queryByName("hj");
        Assert.assertEquals(1, result.length);
        System.out.println("query:");
        for (CategoryView c : result)
            System.out.println(c);
    }

    @Test(dependsOnMethods = {"testDescendants"})
    public void testSiblings() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView[] siblings = query.siblings(496796322118291490l);//grand_oil
        Assert.assertEquals(4, siblings.length);
        siblings = query.siblings(496796322118291492l);//oil
        Assert.assertEquals(2, siblings.length);
        siblings = query.siblings(496796322118291497l);//corn_oil
        // for (CategoryView c : siblings)
        //     System.out.println("sibling:" + c);
        org.testng.Assert.assertEquals(6, siblings.length);
        siblings = query.siblings(496796322118291457l);//root
        Assert.assertEquals(0, siblings.length);
    }

    @Test(dependsOnMethods = {"testDescendants"})
    public void testPath() {
        CategoryQuery query = new PsqlCategoryQuery();
        CategoryView[] path = query.path(496796322118291494l);//rapeseed_oil
        Assert.assertEquals(4, path.length);
        path = query.path(49581450261846022l);//instant_noodles
        Assert.assertEquals(4, path.length);
        path = query.path(496796322118291471l);//liquor
        Assert.assertEquals(3, path.length);
        path = query.path(496796322118291457l);//root
        Assert.assertEquals(1, path.length);
        path = query.path(496796322118291494l);
        System.out.println("rapeseed_oil path:");
        for (CategoryView c : path)
            System.out.println(c);
    }

    @Test(dependsOnMethods = {"testDescendants"})
    public void testDepth() {
        CategoryQuery query = new PsqlCategoryQuery();
        int depth = query.depth(496796322118291494l);//rapeseed_oil
        Assert.assertEquals(4, depth);
        depth = query.depth(496796322118291490l);//grain_oil
        Assert.assertEquals(2, depth);
        depth = query.depth(496796322118291470l);//drinks
        Assert.assertEquals(2, depth);
        depth = query.depth(496796322118291457l);//root
        Assert.assertEquals(1, depth);
        depth = query.depth(496796322118291484l);//washing_liquid
        Assert.assertEquals(4, depth);
    }

 */
}