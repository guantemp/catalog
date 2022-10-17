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

import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgres.PsqlBrandRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2022-09-21
 */
public class BrandValidatorService {
    private static BrandRepository repository;

    static {
        Config config = ConfigFactory.load("database");
        String provider = config.hasPath("provider") ? config.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
                repository = new PsqlBrandRepository("catalog");
                break;
            case "arangodb":
                repository = new ArangoDBBrandRepository("catalog");
                break;
        }
    }

    public static boolean isBrandExist(String id) {
        if (id.equals(Brand.UNDEFINED.id()))
            return true;
        Brand brand = repository.find(id);
        return brand != null;
    }
}
