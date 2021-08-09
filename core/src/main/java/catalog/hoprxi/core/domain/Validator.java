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


import catalog.hoprxi.core.domain.model.brand.ValidatorBrandId;
import catalog.hoprxi.core.domain.model.category.ValidatorCategoryId;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-08-27
 */
public class Validator {
    public static boolean isCategoryExist(String id) {
        ValidatorCategoryId validatorCategoryId = new ValidatorCategoryId(new ArangoDBCategoryRepository("catalog"));
        return validatorCategoryId.isIdExist(id);
    }

    public static boolean isBrandExist(String id) {
        ValidatorBrandId validatorBrandId = new ValidatorBrandId(new ArangoDBBrandRepository("catalog"));
        return validatorBrandId.isIdExist(id);
    }
}
