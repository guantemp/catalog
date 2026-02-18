/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/2/17
 * @since JDK 21
 */

public final class Extract {

    public static void extract(JsonParser parser, JsonGenerator gen, String objectName) throws IOException {
        Objects.requireNonNull(objectName, "objectName is required");
        boolean hitsFound = false;
        gen.writeStartObject();
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                if ("hits".equals(name)) {
                    parser.nextToken(); // should be START_OBJECT
                    if (parser.currentToken() != JsonToken.START_OBJECT) {
                        throw new IllegalStateException("Hits' must be an object");
                    }
                    hitsFound = true;
                    boolean hasHitsArray = false;
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        if (parser.currentToken() != JsonToken.FIELD_NAME) continue;
                        String hitsField = parser.currentName();
                        if ("total".equals(hitsField)) {
                            parser.nextToken(); // enter total object
                            if (parser.currentToken() != JsonToken.START_OBJECT) {
                                throw new IllegalStateException("Total must be an object");
                            }
                            while (parser.nextToken() != JsonToken.END_OBJECT) { // Extract only "value"
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.getCurrentName())) {
                                    parser.nextToken();
                                    gen.writeNumberField("total", parser.getValueAsLong());
                                } else {
                                    parser.nextToken();
                                    parser.skipChildren();
                                }
                            }
                        } else if ("hits".equals(hitsField)) {
                            parser.nextToken(); // should be START_ARRAY
                            if (parser.currentToken() != JsonToken.START_ARRAY) {
                                throw new IllegalStateException("ES7 'hits.hits' must be an array");
                            }

                            gen.writeArrayFieldStart(objectName);
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
                                    parser.skipChildren();
                                    continue;
                                }
                                gen.writeStartObject();
                                while (parser.nextToken() != JsonToken.END_OBJECT) { // Process one hit
                                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.currentName())) {
                                        parser.nextToken(); // enter _source
                                        if (parser.currentToken() != JsonToken.START_OBJECT) {
                                            parser.skipChildren();
                                            continue;
                                        }
                                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                                            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                                String srcField = parser.currentName();
                                                if ("_meta".equals(srcField)) {
                                                    parser.nextToken();
                                                    parser.skipChildren();
                                                } else {
                                                    gen.writeFieldName(srcField);
                                                    parser.nextToken();
                                                    gen.copyCurrentStructure(parser);
                                                }
                                            }
                                        }
                                    } else if (parser.currentToken() == JsonToken.FIELD_NAME && "sort".equals(parser.currentName())) {
                                        gen.writeFieldName("sort");
                                        parser.nextToken();
                                        gen.copyCurrentStructure(parser);
                                    } else {
                                        parser.nextToken();
                                        parser.skipChildren(); // skip _id, _index, sort, _score, etc.
                                    }
                                }
                                gen.writeEndObject();//end source + sort
                            }
                            gen.writeEndArray();//end objectName array
                            hasHitsArray = true;//have object
                        } else {
                            parser.nextToken();
                            parser.skipChildren(); // skip max_score, etc.
                        }
                    }

                    if (!hasHitsArray) {//no objectName array,write empty array
                        gen.writeArrayFieldStart(objectName);
                        gen.writeEndArray();
                    }
                } else if ("aggregations".equals(name)) {
                    parser.nextToken();
                    gen.writeFieldName("aggregations");
                    gen.copyCurrentStructure(parser);
                } else {
                    parser.nextToken();
                    parser.skipChildren(); // skip took, timed_out, _shards, etc.
                }
            }
        }
        if (!hitsFound) {
            gen.writeNumberField("total", 0);
            gen.writeArrayFieldStart(objectName);
            gen.writeEndArray();
        }
        gen.writeEndObject();
        gen.flush();
    }
}
