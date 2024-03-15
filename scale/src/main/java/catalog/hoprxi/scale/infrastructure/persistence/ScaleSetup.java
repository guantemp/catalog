/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.KeyType;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.SkiplistIndexOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2021-09-19
 */

public class ScaleSetup {
    private static final Logger logger = LoggerFactory.getLogger(ScaleSetup.class);

    public static void setup(String databaseName) {
        ArangoDB arangoDB = ArangoDBUtil.getResource();
        /*
        if (arangoDB.db(databaseName).exists()) {
            arangoDB.db(databaseName).drop();
            logger.info("{} be discarded", databaseName);
        }
        CoreSetup.setup(databaseName);
        */
        ArangoDatabase db = arangoDB.db(databaseName);
        //vertex
        for (String s : new String[]{"weight", "count", "plu"}) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.keyOptions(true, KeyType.traditional, 1, 1);
            db.createCollection(s, options);
        }
        //db.createCollection("plu_seq", new CollectionCreateOptions().keyOptions(false, KeyType.autoincrement, 1, 0));
        //index
        Collection<String> index = new ArrayList<>();
        //name.name
        index.add("name.name");
        SkiplistIndexOptions skiplistIndexOptions = new SkiplistIndexOptions().sparse(true);
        db.collection("weight").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("count").ensureSkiplistIndex(index, skiplistIndexOptions);
        //name.mnemonic
        index.clear();
        index.add("name.mnemonic");
        skiplistIndexOptions = new SkiplistIndexOptions().sparse(true);
        db.collection("weight").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("count").ensureSkiplistIndex(index, skiplistIndexOptions);
        //plu
        /*
        index.clear();
        index.add("plu");
        HashIndexOptions hashIndexOptions = new HashIndexOptions().unique(true);
        db.collection("plu").ensureHashIndex(index, hashIndexOptions);
        */
        //graph
        Collection<EdgeDefinition> edgeList = new ArrayList<>();
        edgeList.add(new EdgeDefinition().collection("scale").from("plu").to("weight", "count"));
        edgeList.add(new EdgeDefinition().collection("subordinate").from("category").to("category"));
        edgeList.add(new EdgeDefinition().collection("belong_scale").from("weight", "count").to("category", "brand"));
        db.createGraph("scale", edgeList);
        arangoDB.shutdown();
        logger.info("{} be created", databaseName);
    }

    public static void createCategory() {
        CategoryRepository repository = new ArangoDBCategoryRepository("catalog");
        //生鲜
        Category fresh = new Category("fresh", "fresh", new Name("生鲜", "fresh"), "未经烹调、制作等深加工过程，只做必要保鲜和简单整理上架而出售的初级产品，以及面包、熟食等现场加工品类商品的统称");
        repository.save(fresh);
        Category fruit = new Category("fresh", "fruit", new Name("水果", "fruit"), "可食用的多汁液且有甜味的植物果实的统称");
        repository.save(fruit);
        Category meat = new Category("fresh", "  meat", new Name("肉类", "meat"), "陆上肉食动物及其可食部分的附属品制成的食品的统称");
        repository.save(meat);
        Category vegetables = new Category("fresh", "  vegetables", new Name("蔬菜", "vegetables"), "可以做菜吃的草本植物");
        repository.save(vegetables);
        Category aquatic = new Category("fresh", "  aquatic", new Name("水产品", "aquatic"), "水产品是海洋和淡水渔业生产的动植物及其加工产品的统称");
        repository.save(aquatic);
        Category cookedFood = new Category("fresh", "  cookedFood", new Name("熟食", "cookedFood"), "是经过加工或焯水处理后的原料通过配好的卤汁、红油凉拌、熏烤、油炸等制作而成的菜肴");
        repository.save(cookedFood);
        //子类
        Category pork = new Category("meat", "  pork", new Name("猪肉", "pork"), "猪肉及分割品");
        repository.save(pork);
        Category poultry = new Category("meat", "  poultry", new Name("禽类", "poultry"));
        repository.save(poultry);
        Category freshwaterFish = new Category("aquatic", "freshwaterFish", new Name("淡水鱼", "freshwaterFish"));
        repository.save(freshwaterFish);
        Category freshwaterCrabs = new Category("aquatic", "freshwaterCrabs", new Name("淡水蟹", "freshwaterCrabs"));
        repository.save(freshwaterCrabs);
        Category freshwaterOther = new Category("aquatic", "freshwaterOther", new Name("其它淡水类", "freshwaterOther"));
        repository.save(freshwaterOther);
        Category marineShrimp = new Category("aquatic", "marineShrimp", new Name("海水虾", "marineShrimp"));
        repository.save(marineShrimp);
        Category driedFish = new Category("aquatic", " driedFish", new Name("鱼干", "driedFish"));
        repository.save(driedFish);
    }
}
