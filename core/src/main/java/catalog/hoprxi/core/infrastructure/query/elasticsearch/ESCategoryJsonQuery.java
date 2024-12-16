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
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-07-17
 */
public class ESCategoryJsonQuery implements CategoryJsonQuery {
    private static final String EMPTY_CATEGORY = "{}";
    private static final Logger LOGGER = LoggerFactory.getLogger(ESCategoryJsonQuery.class);
    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    StringWriter writer = new StringWriter(512);
                    JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName()))
                            break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                    generator.close();
                    //System.out.println(writer.getBuffer().capacity());
                    return writer.toString();
                }
            }
        } catch (ResponseException e) {
            LOGGER.warn("The item(id={}) not found", id, e);
        } catch (JsonParseException e) {
            LOGGER.warn("Incorrect JSON format,value is={} ", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
        }
        return EMPTY_CATEGORY;
    }

    @Override
    public String root() {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(rootJsonEntity());
            Response response = client.performRequest(request);
            //EntityUtils.toString(response.getEntity())
            return rebuildCategories(response.getEntity().getContent(), false);
        } catch (ResponseException e) {
            LOGGER.warn("No search was found for anything resembling root categories", e);
        } catch (JsonParseException e) {
            LOGGER.warn("Incorrect JSON format", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
        }
        return EMPTY_CATEGORY;
    }

    private String rootJsonEntity() {
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
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryChildren(String id) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryChildrenJsonEntity(id));
            Response response = client.performRequest(request);
            return rebuildCategories(response.getEntity().getContent(), true);
        } catch (IOException e) {
            //System.out.println(e);
            LOGGER.warn("There are no related category(id={}) available ", id, e);
        }
        return EMPTY_CATEGORY;
    }

    private String queryChildrenJsonEntity(String id) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("term");
            generator.writeStringField("parent_id", id);
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
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryDescendant(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        try (RestClient client = BUILDER.build()) {
            String rootId = "-1";
            int left = 1, right = 1;
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                parser.nextToken();
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "root_id":
                        rootId = parser.getValueAsString();
                        break;
                    case "left":
                        left = parser.getValueAsInt();
                        break;
                    case "right":
                        right = parser.getValueAsInt();
                        break;
                }
            }
            request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryDescendantJsonEntity(rootId, left, right));
            response = client.performRequest(request);
            parser = JSON_FACTORY.createParser(response.getEntity().getContent());


        } catch (ResponseException e) {
            LOGGER.warn("The item(id={}) not found", id, e);
        } catch (JsonParseException e) {
            LOGGER.warn("Incorrect JSON format,value is={} ", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
        }
        return null;
    }

    private String queryDescendantJsonEntity(String rootId, int left, int right) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", 9999);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("root_id", rootId);
            generator.writeEndObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("left");
            generator.writeNumberField("gte", left);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("right");
            generator.writeNumberField("lte", right);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESQueryJsonEntity.queryNameJsonEntity(name, 200, 1));
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

    private String rebuildCategories(InputStream is, boolean tree) throws IOException {
        StringWriter writer = new StringWriter(1024);
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer).useDefaultPrettyPrinter();
        generator.writeStartObject();
        JsonParser parser = JSON_FACTORY.createParser(is);
        while (!parser.isClosed()) {
            if (parser.nextToken() == JsonToken.FIELD_NAME && "hits".equals(parser.getCurrentName())) {
                parseHits(parser, generator, tree);
                break;
            }
        }
        generator.writeEndObject();
        generator.close();
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
                    generator.writeArrayFieldStart("categories");
                    while (parser.nextToken() != null) {
                        System.out.println(parser.currentToken() + ":" + parser.getCurrentName());
                        if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                            generator.writeStartObject();
                            this.parserSource(parser, generator);
                            this.parserSort(parser, generator);
                            generator.writeEndObject();
                        }
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.getCurrentName())) {
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
            if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.getCurrentName())) { //filter _meta
                        break;
                    }
                    generator.copyCurrentEvent(parser);
                }
            }
            if (parser.currentToken() == JsonToken.END_OBJECT && "_source".equals(parser.getCurrentName()))

                break;
        }
    }

    private void parserSort(JsonParser parser, JsonGenerator generator) throws IOException {
        if (parser.nextToken() == JsonToken.FIELD_NAME && "sort".equals(parser.getCurrentName())) {
            generator.copyCurrentEvent(parser);
            while (parser.nextToken() != null) {
                generator.copyCurrentEvent(parser);
                if (parser.currentToken() == JsonToken.END_ARRAY && "sort".equals(parser.getCurrentName()))
                    break;
            }
        }
    }
}
