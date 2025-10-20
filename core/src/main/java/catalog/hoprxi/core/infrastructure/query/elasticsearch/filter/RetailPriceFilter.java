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

package catalog.hoprxi.core.infrastructure.query.elasticsearch.filter;

import catalog.hoprxi.core.application.query.ItemQueryFilter;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-04-29
 */
public class RetailPriceFilter implements ItemQueryFilter {
    private final Number mix;
    private final Number max;

    public RetailPriceFilter(Number mix, Number max) {
        if (mix == null && max == null)
            throw new IllegalArgumentException("mmin.max cannot all be NULL");
        this.mix = mix;
        this.max = max;
    }


    @Override
    public void filter(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeObjectFieldStart("range");
        generator.writeObjectFieldStart("retail_price.number");
        if (mix != null)
            generator.writeNumberField("gte", mix.doubleValue());
        if (max != null)
            generator.writeNumberField("lte", max.doubleValue());
        generator.writeEndObject();//end retail_price.number
        generator.writeEndObject();
        generator.writeEndObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RetailPriceFilter)) return false;

        RetailPriceFilter that = (RetailPriceFilter) o;

        if (!Objects.equals(mix, that.mix)) return false;
        return Objects.equals(max, that.max);
    }

    @Override
    public int hashCode() {
        int result = mix != null ? mix.hashCode() : 0;
        result = 31 * result + (max != null ? max.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetailPriceFilter.class.getSimpleName() + "[", "]")
                .add("mix=" + mix)
                .add("max=" + max)
                .toString();
    }
}
