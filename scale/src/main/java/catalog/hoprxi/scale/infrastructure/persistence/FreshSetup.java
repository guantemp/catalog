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
package catalog.hoprxi.scale.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBUtil;
import catalog.hoprxi.core.infrastructure.persistence.CoreSetup;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.KeyType;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2019-05-04
 */

public class FreshSetup {
    private static final Logger logger = LoggerFactory.getLogger(FreshSetup.class);

    public static void setup(String databaseName) {
        ArangoDB arangoDB = ArangoDBUtil.getResource();
        if (arangoDB.db(databaseName).exists()) {
            arangoDB.db(databaseName).drop();
            logger.info("{} be discarded", databaseName);
        }
        CoreSetup.setup(databaseName);
        ArangoDatabase db = arangoDB.db(databaseName);
        //vertex
        for (String s : new String[]{"weight", "count", "plu"}) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.keyOptions(true, KeyType.traditional, 1, 1);
            db.createCollection(s, options);
        }
        //db.createCollection("plu", new CollectionCreateOptions().keyOptions(false, KeyType.autoincrement, 1, 1));
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
        index.clear();
        index.add("plu");
        HashIndexOptions hashIndexOptions = new HashIndexOptions().unique(true);
        db.collection("plu").ensureHashIndex(index, hashIndexOptions);
        //graph
        Collection<EdgeDefinition> edgeList = new ArrayList<>();
        edgeList.add(new EdgeDefinition().collection("belong_fresh").from("weight", "count").to("category", "brand"));
        edgeList.add(new EdgeDefinition().collection("scale").from("weight", "count").to("plu"));
        db.createGraph("fresh", edgeList);
        arangoDB.shutdown();
        logger.info("{} be created", databaseName);
    }

    public static void createCategory() {
        CategoryRepository categoryRepository = new ArangoDBCategoryRepository();
        Category root = Category.createRootCategory("fresh", "生鲜", "指未经烹调、制作等深加工过程，只做必要保鲜和简单整理上架而出售的初级产品", null);
        categoryRepository.save(root);
        Category fruits = new Category("fresh", "fruits", "水果", "指多汁且主要味觉为甜味和酸味，可食用的植物果实");
        categoryRepository.save(fruits);
        Category vegetables = new Category("fresh", "vegetables", "蔬菜", "指可以做菜、烹饪成为食品的一类植物或菌类");
        categoryRepository.save(vegetables);
        Category freshMeat = new Category("fresh", "meat", "肉类", "包含冷鲜肉和热鲜肉");
        categoryRepository.save(freshMeat);
        Category cooked_food = new Category("fresh", "cooked_food", "熟食", "包含冷鲜肉和热鲜肉");
        categoryRepository.save(cooked_food);
        Category aquatic = new Category("fresh", "aquatic", "水产", "指江、河、湖、海里出产的经济动、植物的统称。可分为活鲜水产、冰鲜水产、冷冻水产、水发水、水产干货");
        categoryRepository.save(aquatic);
        Category dried_foods = new Category("fresh", "dried_foods", "干货", "指江、河、湖、海里出产的经济动、植物的统称。可分为活鲜水产、冰鲜水产、冷冻水产、水发水、水产干货");
        categoryRepository.save(dried_foods);

    }
}
