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

import catalog.hoprxi.core.application.query.CategoryJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
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
import java.util.Stack;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2024-12-22
 */
public class ESCategoryJsonQuery implements CategoryJsonQuery {
    private static final String EMPTY_CATEGORY = "";
    private static final int MAX_SIZE = 9999;
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");
    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    private static final String SEARCH_PREFIX = "/" + ESUtil.database() + "_category";

    @Override
    public String query(long id) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    StringWriter writer = new StringWriter(512);
                    JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName())) break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                    generator.close();
                    return writer.toString();
                }
            }
        } catch (IOException e) {
            LOGGER.warn("The category (id = {}) not found.", id, e);
            throw new QueryException(String.format("The category (id = {}) not found.", id), e);
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
            return rebuildCategories(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling root categories", e);
            throw new QueryException("No search was found for anything resembling root categories", e);
        }
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
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryChildren(long id) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryChildrenJsonEntity(id));
            Response response = client.performRequest(request);
            return rebuildCategories(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("There are no related category(id={}) available ", id, e);
            throw new QueryException(String.format("There are no related category(id=%d) available", id), e);
        }
    }


    private String queryChildrenJsonEntity(long id) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            // 写入 must 数组
            generator.writeArrayFieldStart("must");
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("parent_id", id);
            generator.writeEndObject(); // term
            generator.writeEndObject(); // must 中的对象
            generator.writeEndArray(); // must
            // 写入 must_not 数组
            generator.writeArrayFieldStart("must_not");
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("id", id);
            generator.writeEndObject(); // term
            generator.writeEndObject(); // must_not 中的对象
            generator.writeEndArray(); // must_not
            generator.writeEndObject(); // bool
            generator.writeEndObject(); // query

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "asc");
            generator.writeEndObject();
            generator.writeEndArray();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryDescendant(long id) {
        try (RestClient client = BUILDER.build()) {
            long rootId = -1l;
            int left = 1, right = 1;
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (JsonToken.FIELD_NAME == jsonToken) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "root_id":
                            rootId = parser.getValueAsLong();
                            break;
                        case "left":
                            left = parser.getValueAsInt();
                            break;
                        case "right":
                            right = parser.getValueAsInt();
                            break;
                    }
                }
            }
            request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryDescendantJsonEntity(rootId, left, right));
            response = client.performRequest(request);
            return writeDescendantAsTree(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("There are no related category(id={}) available", e);
            throw new QueryException(String.format("There are no related category(id=%d) available", id), e);
        }
    }

    private String queryDescendantJsonEntity(long rootId, int left, int right) {
        StringWriter writer = new StringWriter(256);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("root_id", rootId);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("left");
            generator.writeNumberField("gte", left);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("right");
            generator.writeNumberField("lte", right);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();//must
            generator.writeEndObject();//bool
            generator.writeEndObject();//query

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("left", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        //System.out.println(writer.getBuffer().capacity());
        return writer.toString();
    }

    private String writeDescendantAsTree(InputStream is) throws IOException {
        StringWriter writer = new StringWriter(1024);
        Stack<Integer> stack = new Stack<>();
        try (JsonParser parser = JSON_FACTORY.createParser(is); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();//start
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "total".equals(parser.getCurrentName())) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.getCurrentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                }
                if (parser.currentToken() == JsonToken.START_ARRAY && "hits".equals(parser.getCurrentName())) {
                    generator.writeObjectFieldStart("categories");
                    boolean first = true;
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.getCurrentName()))
                            break;
                        int currentLeft = 1, currentRight = 1;
                        if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {//root source
                            if (!first)//第一次已写categories
                                generator.writeStartObject();
                            while (parser.nextToken() != null) {
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.getCurrentName()))
                                    break;//end _source
                                generator.copyCurrentEvent(parser);
                                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                    String fileName = parser.getCurrentName();
                                    parser.nextToken();
                                    generator.copyCurrentEvent(parser);
                                    switch (fileName) {
                                        case "left":
                                            currentLeft = parser.getValueAsInt();
                                            break;
                                        case "right":
                                            currentRight = parser.getValueAsInt();
                                            break;
                                    }
                                }
                            }
                            first = false;
                        }//end source
                        if (currentRight - currentLeft == 1) {//叶子
                            generator.writeEndObject();
                        }
                        if (currentRight - currentLeft > 1) {
                            generator.writeArrayFieldStart("children");
                            stack.push(currentRight);
                        }
                        while (!stack.isEmpty() && stack.peek() - currentRight == 1) {
                            generator.writeEndArray();
                            generator.writeEndObject();
                            currentRight = stack.pop();
                        }
                    }//end hits
                    while (!stack.isEmpty()) {
                        generator.writeEndArray();
                        generator.writeEndObject();
                        stack.pop();
                    }
                }
            }
            generator.writeEndObject();//end
        }
        return writer.toString();
    }

    @Override
    public String queryByName(String name) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryNameJsonEntity(name));
            Response response = client.performRequest(request);
            return rebuildCategories(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.debug("No search was found for anything resembling name {} category ", name, e);
        }
        return EMPTY_CATEGORY;
    }

    private String queryNameJsonEntity(String name) {
        Objects.requireNonNull(name, "name is required");
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
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
        } catch (IOException e) {
            LOGGER.error("Can't assemble name request json", e);
        }
        return writer.toString();
    }

    @Override
    public String path(long id) {
        try (RestClient client = BUILDER.build()) {
            String rootId = "-1";
            int left = 1, right = 1;
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (JsonToken.FIELD_NAME == jsonToken) {
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
            }
            request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryPathJsonEntity(rootId, left, right));
            response = client.performRequest(request);
            return rebuildCategories(response.getEntity().getContent());
        } catch (ResponseException e) {
            LOGGER.warn("The category(id={}) not found", id, e);
        } catch (JsonParseException e) {
            LOGGER.warn("Incorrect JSON format", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new RuntimeException(e);
        }
        return EMPTY_CATEGORY;
    }

    private String queryPathJsonEntity(String rootId, int left, int right) {
        StringWriter writer = new StringWriter(256);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("root_id", rootId);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("left");
            generator.writeNumberField("lte", left);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("right");
            generator.writeNumberField("gte", right);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("left", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        //System.out.println(writer.getBuffer().capacity());
        return writer.toString();
    }


    private String rebuildCategories(InputStream is) throws IOException {
        StringWriter writer = new StringWriter(1024);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            JsonParser parser = JSON_FACTORY.createParser(is);
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "hits".equals(parser.getCurrentName())) {
                    writeHits(parser, generator);
                }
            }
        }
        return writer.toString();
    }

    private void writeHits(JsonParser parser, JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "total".equals(parser.getCurrentName())) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.getCurrentName())) {
                        parser.nextToken();
                        generator.writeNumberField("total", parser.getValueAsInt());
                        break;
                    }
                }
            }
            if (parser.currentToken() == JsonToken.START_ARRAY && "hits".equals(parser.getCurrentName())) {
                generator.writeArrayFieldStart("categories");
                while (parser.nextToken() != null) {
                    if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                        generator.writeStartObject();
                        writeSource(parser, generator);
                        writeSort(parser, generator);
                        generator.writeEndObject();
                    }
                    if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.getCurrentName())) {
                        break;
                    }
                }
                generator.writeEndArray();
            }
        }
        generator.writeEndObject();
    }

    private void writeSource(JsonParser parser, JsonGenerator generator) throws IOException {
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

    private void writeSort(JsonParser parser, JsonGenerator generator) throws IOException {
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
