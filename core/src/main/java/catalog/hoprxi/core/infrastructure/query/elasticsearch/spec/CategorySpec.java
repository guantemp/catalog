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

package catalog.hoprxi.core.infrastructure.query.elasticsearch.spec;

import catalog.hoprxi.core.application.query.ItemQuerySpec;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-01-03
 */
public record CategorySpec(long[] ids) implements ItemQuerySpec {
    public CategorySpec {
        if (ids == null)
            ids = new long[0];
    }

    public CategorySpec(long id) {
        this(new long[]{id});
    }

    @Override
    public void queryClause(JsonGenerator generator) throws IOException {
        if (ids.length == 1) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("category.id", ids[0]);
            generator.writeEndObject();
            generator.writeEndObject();
        } else if (ids.length > 1) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("terms");
            generator.writeArrayFieldStart("category.id");
            for (long categoryId : ids)
                generator.writeNumber(categoryId);
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
        }
    }
}
