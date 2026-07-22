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
import java.util.Objects;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-04-04
 */
public record KeywordSpec(String keyword) implements ItemQuerySpec {
    private static final Pattern BARCODE = Pattern.compile("^\\d{5,14}$");
    private static final Pattern NUMBER = Pattern.compile("^\\d{2,4}$");

    public KeywordSpec {
        Objects.requireNonNull(keyword, "keyword required");
    }

    @Override
    public void queryClause(JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart("should");
        if (BARCODE.matcher(keyword).matches()) {//only barcode query
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeObjectFieldStart("barcode.raw");
            generator.writeStringField("value", keyword);
            generator.writeNumberField("boost", 5);
            generator.writeEndObject();//barcode.raw
            generator.writeEndObject();//end term
            generator.writeEndObject();//end

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeObjectFieldStart("barcode");
            generator.writeStringField("value", keyword);
            generator.writeNumberField("boost", 2);
            generator.writeEndObject();//barcode.raw
            generator.writeEndObject();//end term
            generator.writeEndObject();//end
        } else {
            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", keyword);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name^3");
            generator.writeString("name.shortName^2");
            generator.writeString("spec^1.5");
            generator.writeString("madeIn.madeIn^1");
            generator.writeEndArray();//end fields
            generator.writeEndObject();//end multi_match
            generator.writeEndObject();//end

            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", keyword);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name.pinyin^3");
            generator.writeString("shortName.pinyin^2");
            generator.writeString("spec.pinyin^1.5");
            generator.writeString("madeIn.pinyin^1");
            generator.writeEndArray();//end fields
            generator.writeEndObject();//end multi_match
            generator.writeEndObject();//end

            if (keyword.length() >= 2 && keyword.length() <= 4) {
                generator.writeStartObject();
                generator.writeObjectFieldStart("match");
                generator.writeObjectFieldStart("suggest");
                generator.writeStringField("query", keyword);
                generator.writeNumberField("boost", 0.5);
                generator.writeEndObject();//end suggest
                generator.writeEndObject();//end match
                generator.writeEndObject();//end
            }
            if (NUMBER.matcher(keyword).matches()) {
                generator.writeStartObject();
                generator.writeObjectFieldStart("term");
                generator.writeObjectFieldStart("barcode");
                generator.writeStringField("value", keyword);
                generator.writeNumberField("boost", 4);
                generator.writeEndObject();//end suggest
                generator.writeEndObject();//end term
                generator.writeEndObject();//end
            }
        }
        generator.writeEndArray();//end should
        generator.writeNumberField("minimum_should_match", 1);
    }
}
