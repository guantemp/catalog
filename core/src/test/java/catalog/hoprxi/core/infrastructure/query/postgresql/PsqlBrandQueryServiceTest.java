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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.BrandQueryService;
import catalog.hoprxi.core.domain.model.brand.Brand;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-18
 */
public class PsqlBrandQueryServiceTest {
    private static BrandQueryService query = new PsqlBrandQueryService("catalog");

    @Test
    public void testFindAll() {
    }

    @Test
    public void testFindByName() {
        Brand[] brands = query.findByName("康威");
        assertEquals(brands.length, 1);
        brands = query.findByName("康威|@hua");
        assertEquals(brands.length, 2);
        brands = query.findByName("康威|dh|dsp|kw");
        assertEquals(brands.length, 3);
    }

    @Test
    public void testSize() {
    }
}