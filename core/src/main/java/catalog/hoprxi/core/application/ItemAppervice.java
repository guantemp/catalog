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

package catalog.hoprxi.core.application;

import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBItemRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlItemRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2022-11-29
 */
public class ItemAppervice {
    private static ItemRepository itemRepository;
    private static CategoryRepository categoryRepository;
    private static BrandRepository brandRepository;


    static {
        Config config = ConfigFactory.load("database");
        String provider = config.hasPath("provider") ? config.getString("provider").toLowerCase() : "postgresql";
        String database = config.hasPath("database") ? config.getString("database").toLowerCase() : "catalog";
        switch ((provider)) {
            case "psql":
            case "postgres":
            case "postgresql":
                itemRepository = new PsqlItemRepository(database);
                categoryRepository = new PsqlCategoryRepository(database);
                brandRepository = new PsqlBrandRepository(database);
                break;
            case "arangodb":
                itemRepository = new ArangoDBItemRepository(database);
                categoryRepository = new ArangoDBCategoryRepository(database);
                brandRepository = new ArangoDBBrandRepository(database);
                break;
        }
    }

    public Item findItem(String id) {
        return itemRepository.find(id);
    }

    public void batchImport() {

    }
}
