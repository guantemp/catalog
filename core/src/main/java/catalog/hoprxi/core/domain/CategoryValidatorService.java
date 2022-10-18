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

package catalog.hoprxi.core.domain;


import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.3 2022-09-21
 */
public class CategoryValidatorService {
    private static CategoryRepository repository;

    static {
        Config config = ConfigFactory.load("database");
        String provider = config.hasPath("provider") ? config.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
                repository = new PsqlCategoryRepository("catalog");
                break;
            case "arangodb":
                repository = new ArangoDBCategoryRepository("catalog");
                break;
        }
    }

    public static boolean isCategoryExist(String categoryId) {
        if (categoryId.equals(Category.UNDEFINED.id())) return true;
        Category category = repository.find(categoryId);
        return category != null;
    }

    public static boolean isCurrentCategoryDescendant(String currentId, String descendantId) {
        return false;
    }
}
