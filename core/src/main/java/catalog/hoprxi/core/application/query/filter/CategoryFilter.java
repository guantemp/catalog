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
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-01-03
 */
public class CategoryFilter implements ItemQueryFilter {
    private long[] categoryIds;

    public CategoryFilter(long[] categoryIds) {
        this.categoryIds = categoryIds == null ? new long[0] : categoryIds;
    }

    public CategoryFilter(long categoryId) {
        Objects.requireNonNull(categoryId, "categoryId is required");
        this.categoryIds = new long[]{categoryId};
    }


    @Override
    public void filter(JsonGenerator generator) throws IOException {
        if (categoryIds.length == 0) {
            return;
        }
        if (categoryIds.length == 1) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("category.id", categoryIds[0]);
            generator.writeEndObject();
            generator.writeEndObject();
        } else {
            generator.writeStartObject();
            generator.writeObjectFieldStart("terms");
            generator.writeArrayFieldStart("category.id");
            for (long categoryId : categoryIds)
                generator.writeNumber(categoryId);
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CategoryFilter.class.getSimpleName() + "[", "]")
                .add("categoryIds=" + Arrays.toString(categoryIds))
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryFilter)) return false;

        CategoryFilter that = (CategoryFilter) o;

        return Arrays.equals(categoryIds, that.categoryIds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(categoryIds);
    }
}
