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

import catalog.hoprxi.core.application.query.CategoryJsonQuery;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.*;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-07-17
 */
public class ESCategoryJsonQuery implements CategoryJsonQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.es");
    private static final int SIZE = 200;

    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        String result = "";
        try {
            Response response = client.performRequest(request);
            JsonParser parser = jsonFactory.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
                    parser.nextToken();
                    StringWriter writer = new StringWriter();
                    JsonGenerator generator = jsonFactory.createGenerator(writer);
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName()))
                            break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                    generator.close();
                    result = writer.toString();
                    break;
                }
            }
            parser.close();
            client.close();
        } catch (ResponseException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("The item(id={}) not found", id, e);
        } catch (JsonParseException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Incorrect JSON format,value is={} ", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
        }
        return result;
    }

    @Override
    public String root() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(rootJsonEntity());
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildCategories(response.getEntity().getContent(), false);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling root categories ", e);
        }
        return "";
    }

    private String rootJsonEntity() {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("script");
            generator.writeObjectFieldStart("script");
            generator.writeStringField("lang", "painless");
            generator.writeStringField("source", "doc['id'].value == doc['parent_id'].value");
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESQueryJsonEntity.queryNameJsonEntity(name, SIZE, 1));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildCategories(response.getEntity().getContent(), false);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling name {} brand ", name, e);
        }
        return "";
    }

    private String rebuildCategories(InputStream is, boolean tree) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(writer);
        generator.writeStartObject();
        JsonParser parser = jsonFactory.createParser(is);
        while (!parser.isClosed()) {
            if (parser.nextToken() == JsonToken.FIELD_NAME && "hits".equals(parser.getCurrentName())) {
                parseHits(parser, generator, tree);
                break;
            }
        }
        generator.writeEndObject();
        generator.close();
        is.close();
        return writer.toString();
    }

    private void parseHits(JsonParser parser, JsonGenerator generator, boolean tree) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();
                if ("total".equals(fieldName)) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.getCurrentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                } else if ("hits".equals(fieldName)) {
                    parserInternalHits(parser, generator, tree);
                }
            }
        }
    }

    private void parserInternalHits(JsonParser parser, JsonGenerator generator, boolean tree) throws IOException {
        generator.writeArrayFieldStart("categories");
        if (tree) {//假定parent是排在第一位的
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName()))
                            break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeArrayFieldStart("children");//剩余都是儿子
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                            generator.writeStartObject();
                            while (parser.nextToken() != null) {
                                if ("_meta".equals(parser.getCurrentName()))
                                    break;
                                generator.copyCurrentEvent(parser);
                            }
                            generator.writeEndObject();
                        }
                    }
                    generator.writeEndArray();
                    generator.writeEndObject();
                }
            }
        } else {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName()))
                            break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                }
            }
        }
        generator.writeEndArray();
    }


    @Override
    public String queryChildren(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(queryChildrenJsonEntity(id));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildCategories(response.getEntity().getContent(), true);
        } catch (IOException e) {
            System.out.println(e);
            LOGGER.warn("No search was found for anything resembling name {} category ", id, e);
        }
        return "";
    }

    private String queryChildrenJsonEntity(String id) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("bool");

            generator.writeArrayFieldStart("should");
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("id", id);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("parent_id", id);
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
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryDescendant(String id) {
        return null;
    }

    @Override
    public String queryAll(int offset, int limit) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESQueryJsonEntity.paginationQueryJsonEntity(offset, new String[0]));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildCategories(response.getEntity().getContent(), false);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query brands from {} to {}:", offset, limit, e);
        }
        return "";
    }
}
