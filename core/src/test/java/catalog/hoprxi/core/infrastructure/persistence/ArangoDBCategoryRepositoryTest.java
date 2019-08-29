/*
 * Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import mi.foxtail.id.LongId;
import org.junit.*;
import org.junit.rules.ExpectedException;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-05-28
 */
public class ArangoDBCategoryRepositoryTest {
    private static CategoryRepository repository = new ArangoDBCategoryRepository();
    private static BrandRepository brandRepository = new ArangoDBBrandRepository();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() {
        repository.save(Category.UNDEFINED);
        Category root = Category.createRootCategory("root", "分类");
        repository.save(root);
        //食品
        Category food = new Category("root", "food", "食品", "可供人类食用或饮用的物质，包括加工食品，半成品和未加工食品，不包括烟草或只作药品用的物质");
        repository.save(food);
        Category leisure_food = new Category("food", "leisure_food", "休闲食品", "人们闲暇、休息时所吃的食品");
        repository.save(leisure_food);
        Category dry_fruits = new Category("leisure_food", "dry_fruits", "干果");
        repository.save(dry_fruits);
        Category puffed_food = new Category("leisure_food", "puffed_food", "膨化食品");
        repository.save(puffed_food);
        //酒水
        Category drinks = new Category("root", "drinks", "酒水", "酒类和水类的统称，可指酒、水、饮料等液体可饮用的水");
        repository.save(drinks);
        Category liquor = new Category("drinks", "liquor", "白酒", "以粮谷为主要原料，以大曲、小曲或麸曲及酒母等为糖化发酵剂，经蒸煮、糖化、发酵、蒸馏而制成的蒸馏酒");
        repository.save(liquor);
        Category wine = new Category("drinks", "wine", "葡萄酒", "以葡萄为原料酿造的一种果酒。其酒精度高于啤酒而低于白酒。营养丰富，保健作用明显。");
        repository.save(wine);
        //fresh
        Category fresh = new Category("root", "fresh", "生鲜", "指未经烹调、制作等深加工过程，只做必要保鲜和简单整理上架而出售的初级产品，以及面包、熟食等现场加工品类的商品的统称");
        repository.save(fresh);
        Category fruit = new Category("fresh", "fruit", "水果", "指可食用的多汁液且有甜味的植物果实的统称");
        repository.save(fruit);
        Category meat = new Category("fresh", "  meat", "肉类", "指陆上肉食动物及其可食部分的附属品制成的食品的统称");
        repository.save(meat);
        //日化
        Category chemicals = new Category("root", "chemicals", "日化", "日用化学品,指人们平日常用的科技化学制品,包括洗发水、沐浴露、护肤、护发、化妆品等等");
        repository.save(chemicals);
        Category cosmetics = new Category("chemicals", "cosmetics", "化妆品",
                "指以涂抹、喷洒或者其他类似方法，散布于人体表面的任何部位，如皮肤、毛发、指趾甲、唇齿等，以达到清洁、保养、美容、修饰和改变外观，或者修正人体气味，保持良好状态为目的的化学工业品或精细化工产品");
        repository.save(cosmetics);
        Category skin = new Category("cosmetics", "skin", "肤用化妆品");
        repository.save(skin);
        //will move to drinks sub and rename
        Category medicine = new Category("skin", "medicine", "肤用化妆品");
        repository.save(medicine);

        //Category Shampoo = new Category("skin_skin", "Shampoo", "洗发水");
        //repository.save(Shampoo);

        /*BrandSpec
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

    @AfterClass
    public static void tearDown() {
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

    @Test
    public void belongTo() {
        Category[] sub = repository.belongTo("root");
        Assert.assertEquals(3, sub.length);
        sub = repository.belongTo(Category.UNDEFINED.id());
        Assert.assertEquals(0, sub.length);
        sub = repository.belongTo("food");
        Assert.assertEquals(2, sub.length);
        sub = repository.belongTo("fresh");
        Assert.assertEquals(2, sub.length);
    }


    @Test
    public void root() {
        Category[] roots = repository.root();
        Assert.assertEquals(2, roots.length);
    }

    @Test
    public void nextIdentity() {
        String id = String.valueOf(LongId.nextId());
        Assert.assertNotNull(id);
    }

    @Test
    public void remove() {
    }


    @Test
    public void save() {
        Category fresh = repository.find("fresh");
        fresh.moveTo("food");
        repository.save(fresh);

        Category leisure_food = repository.find("leisure_food");
        Assert.assertNotNull(leisure_food);
        Category medicine = repository.find("medicine");
        medicine.moveTo("chemicals");
        repository.save(medicine);
        medicine = repository.find("medicine");
        Assert.assertEquals(medicine.parentId(), "chemicals");
        medicine.rename("      医用");
        medicine.changeDescription("消毒药水");
        repository.save(medicine);
        medicine = repository.find("medicine");
        Assert.assertEquals(medicine.description(), "消毒药水");
        Assert.assertEquals(medicine.name(), "医用");

        thrown.expect(InvalidCategoryIdException.class);
        medicine.moveTo("hy");
    }
}