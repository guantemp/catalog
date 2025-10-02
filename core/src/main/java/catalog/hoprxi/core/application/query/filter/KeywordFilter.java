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
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-04-04
 */
public class KeywordFilter implements ItemQueryFilter {
    private static final Pattern BARCODE = Pattern.compile("^\\d{2,13}$");
    private final String keyword;

    public KeywordFilter(String keyword) {
        this.keyword = Objects.requireNonNull(keyword, "keyword required");
    }

    @Override
    public void filter(JsonGenerator generator) throws IOException {
        if (BARCODE.matcher(keyword).matches()) {//only barcode query
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode", keyword);
            generator.writeEndObject();
            generator.writeEndObject();
        } else {
            generator.writeStartObject();
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("should");

            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", keyword);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.alias");
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("name.mnemonic", keyword);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();//end should
            generator.writeEndObject();//end bool
            generator.writeEndObject();//end
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeywordFilter)) return false;

        KeywordFilter that = (KeywordFilter) o;

        return Objects.equals(keyword, that.keyword);
    }

    @Override
    public int hashCode() {
        return keyword.hashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KeywordFilter.class.getSimpleName() + "[", "]")
                .add("keyword='" + keyword + "'")
                .toString();
    }
}
