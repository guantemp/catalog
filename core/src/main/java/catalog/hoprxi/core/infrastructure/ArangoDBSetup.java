/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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
package catalog.hoprxi.core.infrastructure;

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
 * @version 0.0.3 2019-05-04
 */

public class ArangoDBSetup {
    private static final Logger logger = LoggerFactory.getLogger(ArangoDBSetup.class);

    public static void setup(String databaseName) {
        ArangoDB arangoDB = ArangoDBUtil.getResource();
/*
        System.out.println(arangoDB.getUser(databaseName));
        if (arangoDB.getUser(databaseName) != null) {
            arangoDB.deleteUser(databaseName);
        }
        PasswordService ps = new PasswordService();
        String password = ps.generateStrongPassword();
        arangoDB.createUser(databaseName, password);
        arangoDB = ArangoDBUtil.getResource("120.77.47.145", 8529, databaseName, password);
        for(UserEntity userEntity:arangoDB.getUsers())
        System.out.println(userEntity);
        UserCreateOptions options=new UserCreateOptions();
   */
        if (arangoDB.db(databaseName).exists()) {
            arangoDB.db(databaseName).drop();
            logger.info("{} be discarded", databaseName);
        }
        arangoDB.createDatabase(databaseName);


        ArangoDatabase db = arangoDB.db(databaseName);
        //vertex
        for (String s : new String[]{"brand", "category", "item", "prohibit_sell_item", "prohibit_purchase_item", "prohibit_purchase_and_sell_item", "barcode"}) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.keyOptions(true, KeyType.traditional, 1, 1);
            db.createCollection(s, options);
        }
        //index
        Collection<String> index = new ArrayList<>();
        //name.name
        index.add("name.name");
        SkiplistIndexOptions skiplistIndexOptions = new SkiplistIndexOptions().sparse(true);
        db.collection("item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_sell_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_purchase_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_purchase_and_sell_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        //name.mnemonic
        index.clear();
        index.add("name.mnemonic");
        skiplistIndexOptions = new SkiplistIndexOptions().sparse(true);
        db.collection("item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_sell_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_purchase_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_purchase_and_sell_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        //name.alias
        index.clear();
        index.add("name.alias");
        skiplistIndexOptions = new SkiplistIndexOptions().sparse(true);
        db.collection("item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_sell_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_purchase_item").ensureSkiplistIndex(index, skiplistIndexOptions);
        db.collection("prohibit_purchase_and_sell_item").ensureSkiplistIndex(index, skiplistIndexOptions);

        index.clear();
        index.add("barcode");
        skiplistIndexOptions = new SkiplistIndexOptions().sparse(true);
        db.collection("barcode").ensureSkiplistIndex(index, skiplistIndexOptions);
        //edge
        /*
        for (String s : new String[]{"designate", "subordinate", "belong", "has"}) {
            CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.EDGES);
            db.createCollection(s, options);
        }
        */
        //graph
        Collection<EdgeDefinition> edgeList = new ArrayList<>();
        edgeList.add(new EdgeDefinition().collection("designate").from("category").to("brand"));
        edgeList.add(new EdgeDefinition().collection("subordinate").from("category").to("category"));
        edgeList.add(new EdgeDefinition().collection("belong").from("item", "prohibit_sell_item", "prohibit_purchase_item", "prohibit_purchase_and_sell_item").to("category", "brand"));
        edgeList.add(new EdgeDefinition().collection("has").from("item", "prohibit_sell_item", "prohibit_purchase_item", "prohibit_purchase_and_sell_item", "category").to("barcode"));
        db.createGraph("core", edgeList);
        arangoDB.shutdown();
        logger.info("{} be created", databaseName);
    }
}
