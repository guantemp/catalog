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

package catalog.hoprxi.core.application.query.filter;

import catalog.hoprxi.core.application.query.ItemQueryFilter;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-01-06
 */
public class BrandFilterItem implements ItemQueryFilter {
    private long[] ids;

    public BrandFilterItem(long[] brandIds) {
        ids = brandIds == null ? new long[0] : brandIds;
    }

    public BrandFilterItem(long brandIds) {
        ids = new long[]{brandIds};
    }

    @Override
    public void filter(JsonGenerator generator) throws IOException {
        if (ids.length == 1) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("brand.id", ids[0]);
            generator.writeEndObject();
            generator.writeEndObject();
        } else {
            generator.writeStartObject();
            generator.writeObjectFieldStart("terms");
            generator.writeArrayFieldStart("brand.id");
            for (long id : ids)
                generator.writeNumber(id);
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
        }
    }
}
