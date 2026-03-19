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
 * @version 0.0.1 builder 2025-04-29
 */
public record RetailPriceSpec(Number min, Number max) implements ItemQuerySpec {

    public RetailPriceSpec {
        if (min == null && max == null)
            throw new IllegalArgumentException("min.max cannot all be NULL");
        if (min != null && Double.compare(min.doubleValue(), max.doubleValue()) >= 0)
            throw new IllegalArgumentException("min must less than max");
    }

    @Override
    public void queryClause(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeObjectFieldStart("range");
        generator.writeObjectFieldStart("retail_price.number");
        if (min != null)
            generator.writeNumberField("gte", min.doubleValue());
        if (max != null)
            generator.writeNumberField("lte", max.doubleValue());
        generator.writeEndObject();//end retail_price.number
        generator.writeEndObject();
        generator.writeEndObject();
    }
}
