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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;


import catalog.hoprxi.core.application.query.CategoryQuery1;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/18
 */

public class ESCategoryQuery implements CategoryQuery1 {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");
    private static final String SINGLE_PREFIX = "/" + ESUtil.customized() + "_category";
    private static final String SEARCH_ENDPOINT = SINGLE_PREFIX + "/_search";

    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区

    @Override
    public InputStream root() {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.writeRootJsonEntity());
            Response response = client.performRequest(request);
            return this.rebuild(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling root categories", e);
            throw new SearchException("No search was found for anything resembling root categories", e);
        }
    }

    private String writeRootJsonEntity() {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("script");
            generator.writeObjectFieldStart("script");
            generator.writeStringField("lang", "painless");
            generator.writeStringField("source", "doc['id'].value == doc['parent_id'].value");
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public InputStream find(long id) {
        Request request = new Request("GET", "/category/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        try (RestClient client = BUILDER.build(); OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName())) break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                }
            }
        } catch (ResponseException e) {
            LOGGER.error("The category(id={}) not found", id, e);
            throw new SearchException(String.format("The category(id=%s) not found", id), e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        }
        return new ByteBufInputStream(buffer, true);
    }

    @Override
    public InputStream searchChildren(long id) {
        return null;
    }

    @Override
    public InputStream searchDescendants(long id) {
        return null;
    }

    @Override
    public InputStream search(String key) {
        return null;
    }

    @Override
    public InputStream searchSiblings(long id) {
        return null;
    }

    @Override
    public InputStream searchPath(long id) {
        return null;
    }

    private InputStream rebuild(InputStream is) throws IOException {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        OutputStream os = new ByteBufOutputStream(buffer);
        JsonGenerator generator = JSON_FACTORY.createGenerator(os);
        generator.writeStartObject();
        JsonParser parser = JSON_FACTORY.createParser(is);
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "hits".equals(parser.currentName())) {
                this.parseHits(parser, generator);
                break;
            }
        }
        generator.writeEndObject();
        generator.close();
        return new ByteBufInputStream(buffer, true);
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                if ("total".equals(fieldName)) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                } else if ("hits".equals(fieldName)) {
                    generator.writeArrayFieldStart("categories");
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            generator.writeStartObject();
                            this.parserSource(parser, generator);
                            this.parserSort(parser, generator);
                            generator.writeEndObject();
                        }
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.currentName())) {
                            break;
                        }
                    }
                    generator.writeEndArray();
                }
            }
        }
    }

    private void parserSource(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName())) { //filter _meta
                        break;
                    }
                    generator.copyCurrentEvent(parser);
                }
            }
            if (parser.currentToken() == JsonToken.END_OBJECT && "_source".equals(parser.currentName()))
                break;
        }
    }

    private void parserSort(JsonParser parser, JsonGenerator generator) throws IOException {
        if (parser.nextToken() == JsonToken.FIELD_NAME && "sort".equals(parser.currentName())) {
            generator.copyCurrentEvent(parser);
            while (parser.nextToken() != null) {
                generator.copyCurrentEvent(parser);
                if (parser.currentToken() == JsonToken.END_ARRAY && "sort".equals(parser.currentName()))
                    break;
            }
        }
    }
}
