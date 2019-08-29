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

package catalog.hoprxi.core.domain.model.brand;

import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-08-28
 */
public class ValidatorBrandId {
    private BrandRepository repository;

    public ValidatorBrandId(BrandRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository required");
    }

    public boolean isIdExist(String id) {
        //Brand brand = repository.find(parentId);
        // return brand == null ? false : true;
        return true;
    }
}
