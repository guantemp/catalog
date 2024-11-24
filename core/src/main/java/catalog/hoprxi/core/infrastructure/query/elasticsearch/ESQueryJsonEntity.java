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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-07-19
 */
public class ESQueryJsonEntity {
    private static final JsonFactory jsonFactory = JsonFactory.builder().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(ESQueryJsonEntity.class);

    public static String queryNameJsonEntity(String name, int size, String[] searchAfter) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("should");

            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", name);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.alias");
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("name.mnemonic", name);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "desc");
            generator.writeEndObject();
            generator.writeEndArray();

            if (searchAfter.length > 0) {
                generator.writeArrayFieldStart("search_after");
                for (String s : searchAfter)
                    generator.writeString(s);
                generator.writeEndArray();
            }

            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            LOGGER.error("Can't assemble name request json", e);
        }
        return writer.toString();

    }

    public static String queryNameJsonEntity(String name, int offset, int limit) {
        Objects.requireNonNull(name, "name id required");
        if (offset < 0)
            offset = 0;
        if (limit < 0)
            limit = 1;
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", limit);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("should");

            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", name);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.alias");
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("name.mnemonic", name);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            LOGGER.error("Can't assemble name request json", e);
        }
        return writer.toString();
    }


    public static String paginationQueryJsonEntity(int size, String[] searchAfter) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lagert zero");
        if (searchAfter == null) searchAfter = new String[0];
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            generator.writeObjectFieldStart("query");
            generator.writeFieldName("match_all");
            generator.writeStartObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            if (searchAfter.length > 0) {
                generator.writeArrayFieldStart("search_after");
                for (String s : searchAfter)
                    generator.writeString(s);
                generator.writeEndArray();
            }

            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            LOGGER.error("Can't assemble pagination query json", e);
        }
        return writer.toString();
    }
}
