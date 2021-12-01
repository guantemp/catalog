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

package catalog.hoprxi.core.domain;


import catalog.hoprxi.core.domain.model.brand.ValidatorBrand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2021-10-23
 */
public class CategoryValidatorService {
    private static final CategoryRepository repository = new ArangoDBCategoryRepository("catalog");

    public static boolean isCategoryExist(String categoryId) {
        if (categoryId.equals(Category.UNDEFINED.id()))
            return true;
        Category category = repository.find(categoryId);
        return category != null;
    }

    public static boolean isBrandExist(String id) {
        ValidatorBrand validatorBrand = new ValidatorBrand(new ArangoDBBrandRepository("catalog"));
        return validatorBrand.isExist(id);
    }
}
