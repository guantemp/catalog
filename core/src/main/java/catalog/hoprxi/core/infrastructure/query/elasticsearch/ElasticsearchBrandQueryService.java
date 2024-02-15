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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;

import catalog.hoprxi.core.application.query.BrandQueryService;
import catalog.hoprxi.core.domain.model.brand.Brand;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-02-14
 */
public class ElasticsearchBrandQueryService implements BrandQueryService {
    /**
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public Brand[] queryAll(int offset, int limit) {
        return new Brand[0];
    }

    /**
     * @param id
     * @return
     */
    @Override
    public Brand query(String id) {
        return BrandQueryService.super.query(id);
    }

    /**
     * @param name is support regular
     * @return
     */
    @Override
    public Brand[] queryByName(String name) {
        return new Brand[0];
    }

    /**
     * @return
     */
    @Override
    public int size() {
        return 0;
    }
}
