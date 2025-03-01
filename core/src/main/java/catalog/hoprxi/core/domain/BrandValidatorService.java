/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.List;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2025-03-01
 */
public class BrandValidatorService {
    private static BrandRepository repository;

    static {
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            String provider = database.getString("provider");
            switch ((provider)) {
                case "postgresql":
                case "psql":
                    repository = new PsqlBrandRepository();
                    break;
            }
        }
    }

    public static boolean isBrandExist(String id) {
        if (id.equals(Brand.UNDEFINED.id()))
            return true;
        Brand brand = repository.find(id);
        return brand != null;
    }
}
