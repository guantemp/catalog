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

package catalog.hoprxi.core.domain;


import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlCategoryQuery;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.3 2022-09-21
 */
public class CategoryValidatorService {
    private static CategoryRepository repository;
    private static CategoryQuery query;

    static {

        Config config = ConfigFactory.load("database");
        String provider = config.hasPath("provider") ? config.getString("provider").toLowerCase() : "postgresql";
        String databaseName = config.hasPath("databaseName") ? config.getString("databaseName").toLowerCase() : "catalog";
        switch ((provider)) {
            case "psql":
            case "postgresql":
                repository = new PsqlCategoryRepository();
                query = new PsqlCategoryQuery();
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
