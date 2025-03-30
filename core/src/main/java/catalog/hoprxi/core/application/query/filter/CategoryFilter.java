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

import catalog.hoprxi.core.application.query.QueryFilter;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-01-03
 */
public class CategoryFilter implements QueryFilter {
    private String[] categoryIds;

    public CategoryFilter(String[] categoryIds) {
        if (categoryIds == null)
            this.categoryIds = new String[0];
        this.categoryIds = categoryIds;
    }

    public CategoryFilter(String categoryId) {
        Objects.requireNonNull(categoryId, "categoryId is required");
        this.categoryIds = new String[]{categoryId};
    }


    @Override
    public void filter(JsonGenerator generator) {
        try {
            generator.writeStartObject();
            generator.writeObjectFieldStart("terms");
            generator.writeArrayFieldStart("category.id");
            for (String categoryId : categoryIds)
                generator.writeString(categoryId);
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
