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

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.net.MalformedURLException;
import java.net.URI;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025-09-24
 */
public class PsqlCategoryRepositoryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final CategoryRepository repository = new PsqlCategoryRepository();

    @BeforeClass
    public void beforeClass() throws MalformedURLException {
        repository.save(Category.UNDEFINED);
        Category root = Category.root(1, new Name("商品类别", "root"), "我的测试分类");
        repository.save(root);
        //食品
        Category food = new Category(1, 11, new Name("可食用", "food"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        repository.save(food);
        Category leisure_food = new Category(11, 111, new Name("休闲食品", "leisure_food"), "人们闲暇、休息时所吃的食品");
        repository.save(leisure_food);
        Category dry_fruits = new Category(111, 1111, new Name("干果", "dry_fruits"));
        repository.save(dry_fruits);
        Category puffed_food = new Category(111, 1112, new Name("膨化食品", "puffed_food"), "膨化食品是以谷物、薯类或豆类等为主要原料，采用膨化工艺如焙烤、油炸、微波或挤压等制成体积明显增大的，具有一定膨化度的一种组织酥脆、香味扑鼻、风格各异的休闲食品。");
        repository.save(puffed_food);
        //酒水
        Category drinks = new Category(1, 12, new Name("酒水", "drinks"), "酒类和水类的统称，指酒、水、饮料等液体可饮用的水");
        repository.save(drinks);
        Category liquor = new Category(12, 121, new Name("白酒", "liquor"), "以粮谷为主要原料，以大曲、小曲或麸曲及酒母等为糖化发酵剂，经蒸煮、糖化、发酵、蒸馏而制成的蒸馏酒");
        repository.save(liquor);
        Category wine = new Category(12, 122, new Name("葡萄酒", "wine"), "以葡萄为原料酿造的一种果酒。其酒精度高于啤酒而低于白酒。营养丰富，保健作用明显。");
        repository.save(wine);
        Category Yellow_wine = new Category(12, 123, new Name("黄酒", "yellow_wine"), "以稻米、黍米、黑米、玉米、小麦等为原料，经过蒸料，拌以麦曲、米曲或酒药，进行糖化和发酵酿制而成的各类酒");
        repository.save(Yellow_wine);
        //日化
        Category chemicals = new Category(1, 13, new Name("日用化学品", "chemicals"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        repository.save(chemicals);
        Category cosmetics = new Category(13, 131, new Name("化妆品", "cosmetics"),
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        repository.save(cosmetics);
        Category washing = new Category(13, 132, new Name("洗涤用品", "washing"));
        repository.save(washing);
        Category soap = new Category(132, 1321, new Name("肥皂", "soap"), "脂肪酸金属盐的总称");
        repository.save(soap);
        Category oral_hygiene = new Category(13, 133, new Name("口腔用品", "oral_hygiene"), "用于口腔卫生保健的日常生活用品");
        repository.save(oral_hygiene);
        Category clean = new Category(13, 135, new Name("清洁/卫生用品", "clean"));
        repository.save(clean);
        Category washing_liquid = new Category(132, 1322, new Name("洗衣液", "washing_liquid"), "多采用非离子型表面活性剂，PH接近中性，对皮肤温和，并且排入自然界后，降解较洗衣粉快");
        repository.save(washing_liquid);
        Category hari = new Category(13, 136, "洗/护发用品");
        repository.save(hari);
        Category shampoo = new Category(135, 1352, new Name("洗发水", "shampoo"));
        repository.save(shampoo);
        Category HairCare = new Category(135, 1351, new Name("护发", "HairCare"));
        repository.save(HairCare);
        //will move to drinks sub and rename
        Category beer = new Category(132, 1323, new Name("      个人保健用卫生制剂", "beer"));
        repository.save(beer);
        //粮油
        Category grain_oil = new Category(1, 14, new Name("粮油", "grain_oil"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(grain_oil);
        Category oil = new Category(14, 142, new Name("食用油", "oil"), "指在制作食品过程中使用的，动物或者植物油脂。常温下为液态。", URI.create("https://baike.baidu.com/pic/%E9%A3%9F%E7%94%A8%E6%B2%B9/10955297/1/241f95cad1c8a786c9175e1ce75ede3d70cf3bc7fad0?fromModule=lemma_top-image&ct=single#aid=1&pic=241f95cad1c8a786c9175e1ce75ede3d70cf3bc7fad0").toURL());
        repository.save(oil);
        Category rice_flour = new Category(14, 141, new Name("米/面/杂粮", "rice_flour"));
        repository.save(rice_flour);
        Category grain_and_oil_products = new Category(14, 143, new Name("粮油/制品", "grain_and_oil_products"));
        repository.save(grain_and_oil_products);
        //食用油
        Category rapeseed_oil = new Category(142, 1421, new Name("菜籽油", "rapeseed_oil"), "用油菜籽榨出来的一种食用油。是我国主要食用油之一");
        repository.save(rapeseed_oil);
        Category peanut_oil = new Category(142, 1423, new Name("花生油", " peanut_oil"));
        repository.save(peanut_oil);
        Category soybean_oil = new Category(142, 1422, new Name("大豆油", "soybean_oil"));
        repository.save(soybean_oil);
        Category olive_oil = new Category(142, 1426, new Name("橄榄油", " olive_oil "), "由新鲜的油橄榄果实直接冷榨而成的，不经加热和化学处理，保留了天然营养成分，橄榄油被认为是迄今所发现的油脂中最适合人体营养的油脂");
        repository.save(olive_oil);
        Category corn_oil = new Category(142, 1425, new Name("玉米油", " corn_oil"), "又叫粟米油、玉米胚芽油，它是从玉米胚芽中提炼出的油");
        repository.save(corn_oil);
        Category sunflower_seed_oil = new Category(142, 1427, new Name("葵花籽油", " sunflower_seed_oil"), "是向日葵的果实。它的子仁中含脂肪30%-45%，最多的可达60%。葵花子油颜色金黄，澄清透明，气味清香，是一种重要的食用油。");
        repository.save(sunflower_seed_oil);
        Category blended_oil = new Category(142, 1428, new Name("调和油", "blended_oil"), "根据使用需要，将两种以上经精炼的油脂（香味油除外）按比例调配制成的食用油");
        repository.save(blended_oil);
        //制品
        Category bread_cake = new Category(143, 1431, new Name("面包/蛋糕", "bread_cake"));
        repository.save(bread_cake);
        Category instant_noodles = new Category(143, 1433, new Name("方便面", "instant_noodles"), "是一种可在短时间之内用热水泡熟食用的面制食品。");
        repository.save(instant_noodles);
        Category fine_dried_noodles = new Category(143, 1434, new Name("挂面", "fine_dried_noodles"));
        repository.save(fine_dried_noodles);
        Category flour = new Category(143, 1432, new Name("面粉", "flour"), "是一种由小麦磨成的粉状物。按面粉中蛋白质含量的多少，可以分为高筋面粉、中筋面粉、低筋面粉及无筋面粉。");
        repository.save(flour);
        //调味品
        Category condiment = new Category(1, 15, new Name("调味品", "condiment"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(condiment);
        Category sauce = new Category(15, 151, new Name("调味汁", "sauce"));
        repository.save(sauce);
        Category vinegar = new Category(151, 1512, new Name("醋", " vinegar"), "醋是一种发酵的酸味液态调味品，多由糯米、高粱、大米、玉米、小麦以及糖类和酒类发酵制成。", URI.create("https://baike.baidu.com/pic/%E9%86%8B/319503/0/09fa513d269759ee41fefa71bafb43166c22dfda?fr=lemma&fromModule=lemma_content-image#aid=0&pic=09fa513d269759ee41fefa71bafb43166c22dfda").toURL());
        repository.save(vinegar);
        Category soy_sauce = new Category(151, 1511, new Name("酱油", "soy_sauce"), "用大豆或脱脂大豆或黑豆、小麦或麸皮，加入水、食盐酿造而成的液体调味品，色泽呈红褐色，有独特酱香，滋味鲜美，有助于促进食欲。", URI.create("https://inews.gtimg.com/newsapp_bt/0/13781122372/1000").toURL());
        repository.save(soy_sauce);
        Category flavoring = new Category(15, 1514, new Name("调味料", "flavoring"));//ceshi move
        repository.save(flavoring);
        Category seasoning_oil = new Category(151, 1513, new Name("调味油", " seasoning_oil"));
        repository.save(seasoning_oil);
        Category salt = new Category(14, 15141, new Name("盐", "salt"));
        repository.save(salt);
        Category chicken_essence_monosodium_glutamate = new Category(1514, 15142, new Name("鸡精/味精", "chicken_essence_and_monosodium_glutamate"));
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
        root = Category.root(496796322118291457L, new Name("商品分类", "root"));
        repository.save(root);
        //食品
        food = new Category(496796322118291457L, 496796322118291460L, new Name("食品", "food"), "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        repository.save(food);
        leisure_food = new Category(496796322118291460L, 496796322118291461L, new Name("休闲食品", "leisure_food"), "人们闲暇、休息时所吃的食品");
        repository.save(leisure_food);
        dry_fruits = new Category(496796322118291461L, 496796322118291462L, new Name("干果", "dry_fruits"));
        repository.save(dry_fruits);
        puffed_food = new Category(496796322118291461L, 496796322118291463L, new Name("膨化食品", "puffed_food"));
        repository.save(puffed_food);
        //酒水
        drinks = new Category(496796322118291457L, 496796322118291470L, new Name("酒", "alcoholic_liquor"), "用粮食、水果等含淀粉或糖的物质发酵制成的含乙醇的饮料。");
        repository.save(drinks);
        liquor = new Category(496796322118291470L, 496796322118291471L, new Name("白酒", "liquor"), "以粮谷为主要原料，以大曲、小曲或麸曲及酒母等为糖化发酵剂，经蒸煮、糖化、发酵、蒸馏而制成的蒸馏酒");
        repository.save(liquor);
        wine = new Category(496796322118291470L, 496796322118291472L, new Name("葡萄酒", "wine"), "以葡萄为原料酿造的一种果酒。其酒精度高于啤酒而低于白酒。营养丰富，保健作用明显。");
        repository.save(wine);
        Yellow_wine = new Category(496796322118291470L, 496796322118291473L, new Name("黄酒", "yellow_wine"), "以稻米、黍米、黑米、玉米、小麦等为原料，经过蒸料，拌以麦曲、米曲或酒药，进行糖化和发酵酿制而成的各类酒");
        repository.save(Yellow_wine);
        //日化
        chemicals = new Category(496796322118291457L, 496796322118291480L, new Name("日化", "chemicals"), "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        repository.save(chemicals);
        cosmetics = new Category(496796322118291480L, 496796322118291481L, new Name("化妆品", "cosmetics"),
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        repository.save(cosmetics);
        oral_hygiene = new Category(496796322118291480L, 496796322118291485L, new Name("口腔用品", "oral_hygiene"));
        repository.save(oral_hygiene);
        clean = new Category(496796322118291480L, 496796322118291486L, new Name("清洁、卫生用品", "clean"));
        repository.save(clean);
        hari = new Category(496796322118291480L, 496796322118291487L, "洗、护发用品");
        repository.save(hari);
        washing = new Category(496796322118291480L, 496796322118291482L, new Name("洗涤用品", "washing"));
        repository.save(washing);

        soap = new Category(496796322118291482L, 496796322118291483L, new Name("肥皂", "soap"), "脂肪酸金属盐的总称");
        repository.save(soap);
        washing_liquid = new Category(496796322118291482L, 496796322118291484L, new Name("洗衣液", "washing_liquid"), "多采用非离子型表面活性剂，PH接近中性，对皮肤温和，并且排入自然界后，降解较洗衣粉快");
        repository.save(washing_liquid);
        //one error alias chemicals
        beer = new Category(496796322118291482L, 496796322118291488L, new Name("      个人保健用卫生制剂", "beer"));
        repository.save(beer);
        //粮油
        grain_oil = new Category(496796322118291457L, 496796322118291490L, new Name("粮油", "grain_oil"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(grain_oil);
        rice_flour = new Category(496796322118291490L, 496796322118291491L, new Name("米、面、杂粮", "rice_flour"));
        repository.save(rice_flour);
        grain_and_oil_products = new Category(496796322118291490L, 496796322118291493L, new Name("粮油制品", "grain_and_oil_products"));
        repository.save(grain_and_oil_products);

        oil = new Category(496796322118291490L, 496796322118291492L, new Name("食用油", "oil"));  //食用油
        repository.save(oil);
        rapeseed_oil = new Category(496796322118291492L, 496796322118291494L, new Name("菜籽油", "rapeseed_oil"), "用油菜籽榨出来的一种食用油。是我国主要食用油之一");
        repository.save(rapeseed_oil);
        soybean_oil = new Category(496796322118291492L, 496796322118291495L, new Name("大豆油", "soybean_oil"));
        repository.save(soybean_oil);
        peanut_oil = new Category(496796322118291492L, 496796322118291496L, new Name("花生油", " peanut_oil"));
        repository.save(peanut_oil);
        corn_oil = new Category(496796322118291492L, 496796322118291497L, new Name("玉米油", " corn_oil"), "又叫粟米油、玉米胚芽油，它是从玉米胚芽中提炼出的油");
        repository.save(corn_oil);
        olive_oil = new Category(496796322118291492L, 496796322118291498L, new Name("橄榄油", " olive_oil "), "由新鲜的油橄榄果实直接冷榨而成的，不经加热和化学处理，保留了天然营养成分，橄榄油被认为是迄今所发现的油脂中最适合人体营养的油脂");
        repository.save(olive_oil);
        sunflower_seed_oil = new Category(496796322118291492L, 496796322118291499L, new Name("葵花籽油", " sunflower_seed_oil"), "是向日葵的果实。它的子仁中含脂肪30%-45%，最多的可达60%。葵花子油颜色金黄，澄清透明，气味清香，是一种重要的食用油。");
        repository.save(sunflower_seed_oil);
        blended_oil = new Category(496796322118291492L, 496796322118291700L, new Name("调和油", "blended_oil"), "根据使用需要，将两种以上经精炼的油脂（香味油除外）按比例调配制成的食用油");
        repository.save(blended_oil);

        bread_cake = new Category(496796322118291493L, 49581450261846020L, new Name("面包、蛋糕", "bread_cake")); //粮油制品
        repository.save(bread_cake);
        flour = new Category(496796322118291493L, 49581450261846021L, new Name("面粉", "flour"));
        repository.save(flour);
        instant_noodles = new Category(496796322118291493L, 49581450261846022L, new Name("方便面", "instant_noodles"), "是一种可在短时间之内用热水泡熟食用的面制食品。");
        repository.save(instant_noodles);
        fine_dried_noodles = new Category(496796322118291493L, 49581450261846023L, new Name("挂面", "fine_dried_noodles"));
        repository.save(fine_dried_noodles);
        //调味品
        condiment = new Category(496796322118291457L, 49581450261846035L, new Name("调味品", "condiment"), "对谷类、豆类等粮食和油料及其加工成品和半成品的统称");
        repository.save(condiment);
        sauce = new Category(49581450261846035L, 49581450261846040L, new Name("调味汁", "sauce"));
        repository.save(sauce);
        soy_sauce = new Category(49581450261846040L, 49581450261846041L, new Name("酱油", "soy_sauce"), "用大豆或脱脂大豆或黑豆、小麦或麸皮，加入水、食盐酿造而成的液体调味品，色泽呈红褐色，有独特酱香，滋味鲜美，有助于促进食欲。", URI.create("https://inews.gtimg.com/newsapp_bt/0/13781122372/1000").toURL());
        repository.save(soy_sauce);
        vinegar = new Category(49581450261846040L, 49581450261846042L, new Name("醋", " vinegar"));
        repository.save(vinegar);
        seasoning_oil = new Category(49581450261846040L, 49581450261846043L, new Name("调味油", " seasoning_oil"));
        repository.save(seasoning_oil);
        flavoring = new Category(49581450261846035L, 49581450261846057L, new Name("调味料", "flavoring"));
        repository.save(flavoring);
        salt = new Category(49581450261846057L, 49581450261846058L, new Name("盐", "salt"));
        repository.save(salt);
        chicken_essence_monosodium_glutamate = new Category(49581450261846057L, 49581450261846059L, new Name("鸡精、味精", "chicken_essence_and_monosodium_glutamate"));
        repository.save(chicken_essence_monosodium_glutamate);
    }
/*
    @AfterClass
    public void afterClass() {
        repository.remove("15142");
        repository.remove("15141");
        repository.remove("1514");
        repository.remove("1513");
        repository.remove("1512");
        repository.remove("1511");
        repository.remove("151");
        repository.remove(" 15");

        repository.remove("1434");
        repository.remove("1433");
        repository.remove("1432");
        repository.remove("1431");

        repository.remove("1428");
        repository.remove("1427");
        repository.remove("1426");
        repository.remove("1425");
        repository.remove("1423");
        repository.remove("1422");
        repository.remove("1421");
        repository.remove("143");
        repository.remove("142");
        repository.remove("141");
        repository.remove("14");

        repository.remove("1323");
        repository.remove("136");
        repository.remove("135");
        repository.remove("133");
        repository.remove("1322");
        repository.remove("1321");
        repository.remove("132");
        repository.remove("131");
        repository.remove("13");

        repository.remove("123");
        repository.remove("122");
        repository.remove("121");
        repository.remove("12");

        repository.remove("1112");
        repository.remove("1111");
        repository.remove("111");
        repository.remove("11");

        repository.remove(" 1");

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

 */

    @Test
    public void testFind() {
        Category instant_noodles = repository.find(1433);
        Assert.assertNotNull(instant_noodles);
        Assert.assertNotNull(repository.find(1427));//葵花籽油
        Assert.assertNotNull(repository.find(121));//白酒
        Assert.assertNull(repository.find(1211));
        System.out.println(repository.find(1511));
    }

    @Test
    public void testNextIdentity() {
        long id = repository.nextIdentity();
        System.out.println(id);
    }

    /*
        @Test(priority = 3)
        public void testRemove() {
            repository.remove("143");
            Assert.assertNull(repository.query("1431"));
            Assert.assertNull(repository.query("1433"));
        }
    */
    @Test
    public void testRoot() {
        Category[] roots = repository.root();
        Assert.assertEquals(roots.length, 3);
    }

    @Test(priority = 1, expectedExceptions = InvalidCategoryIdException.class)
    public void testSave() throws MalformedURLException {
        Category beer = repository.find(1323);
        System.out.println(beer);

        Assert.assertEquals(beer.parentId(), 132);//washing
        beer.moveTo(12);//drinks
        beer.rename("      啤酒    ", "beer");
        beer.changeDescription("是一种以小麦芽和大麦芽为主要原料，并加啤酒花，经过液态糊化和糖化，再经过液态发酵酿制而成的酒精饮料");
        repository.save(beer);
        beer = repository.find(1323);
        System.out.println("updated:" + beer);
        Assert.assertEquals(beer.parentId(), 12);//drinks
        Assert.assertEquals(beer.name().name(), "啤酒");
        Assert.assertEquals(beer.description(), "是一种以小麦芽和大麦芽为主要原料，并加啤酒花，经过液态糊化和糖化，再经过液态发酵酿制而成的酒精饮料");
        Assert.assertEquals(beer.name().alias(), "beer");

        Category leisure_food = repository.find(111);
        Assert.assertNotNull(leisure_food);
        leisure_food.changeDescription(null);
        leisure_food.changeIcon(URI.create("https://gitee.com/static/images/logo-black.svg?t=158106664").toURL());
        //System.out.println(leisure_food);
        repository.save(leisure_food);
        leisure_food = repository.find(111);
        Assert.assertNull(leisure_food.description());

        Category flavoring = repository.find(1514);//调味料
        flavoring.moveTo(151);//调味汁
        repository.save(flavoring);

        Category salt = repository.find(15141);//调味料
        Assert.assertEquals(salt.parentId(), 14);
        salt.moveTo(1514);//调味汁
        repository.save(salt);
        salt = repository.find(15141);
        Assert.assertEquals(salt.parentId(), 1514);

        //异常必须要最后，后面语句不会被执行
        beer.moveTo(12458763225L);
    }
}