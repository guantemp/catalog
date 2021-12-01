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

package catalog.hoprxi.core.infrastructure.query;

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.domain.model.category.Category;
import salt.hoprxi.tree.Tree;

import java.util.HashMap;
import java.util.Map;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-12-01
 */
public class ArangoDBCategoryQueryService implements CategoryQueryService {
    private static final Map<String, Tree<Category>> cache = new HashMap<>();

    @Override
    public Category[] root() {
        return new Category[0];
    }

    @Override
    public Category root(String rootId) {
        return null;
    }

    @Override
    public Category[] children(String parentId) {
        return new Category[0];
    }

    @Override
    public Category[] descendants(String parentId) {
        return new Category[0];
    }

    @Override
    public Category[] silblings(String id) {
        return new Category[0];
    }

    @Override
    public Category[] path(String id) {
        return new Category[0];
    }
}
