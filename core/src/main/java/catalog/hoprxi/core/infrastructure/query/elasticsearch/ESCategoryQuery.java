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


import catalog.hoprxi.core.application.query.CategoryQuery;
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
import java.util.Objects;
import java.util.Stack;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/18
 */

public class ESCategoryQuery implements CategoryQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");
    private static final String SINGLE_PREFIX = "/" + ESUtil.customized() + "_category";
    private static final String SEARCH_ENDPOINT = SINGLE_PREFIX + "/_search";

    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    private static final int MAX_SIZE = 9999;
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
    public InputStream children(long id) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.writeChildrenJsonEntity(id));
            Response response = client.performRequest(request);
            return this.rebuild(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("There are no related category(id={}) available ", id, e);
            throw new SearchException(String.format("There are no related category(id=%d) available", id), e);
        }
    }

    private String writeChildrenJsonEntity(long id) {
        StringWriter writer = new StringWriter(128);
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
    public InputStream descendants(long id) {
        try (RestClient client = BUILDER.build()) {
            long rootId = -1;
            int left = 1, right = 1;
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
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
            request.setJsonEntity(this.writeDescendantJsonEntity(rootId, left, right));
            response = client.performRequest(request);
            return this.rebuildDescendantAsTree(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("There are no related category(id={}) available", id, e);
            throw new SearchException(String.format("There are no related category(id=%d) available", id), e);
        }
    }

    private String writeDescendantJsonEntity(long rootId, int left, int right) {
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

    private InputStream rebuildDescendantAsTree(InputStream is) throws IOException {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        OutputStream os = new ByteBufOutputStream(buffer);
        Stack<Integer> stack = new Stack<>();
        try (JsonParser parser = JSON_FACTORY.createParser(is); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            generator.writeStartObject();//start
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "total".equals(parser.currentName())) {//all number
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                }
                if (parser.currentToken() == JsonToken.START_ARRAY && "hits".equals(parser.currentName())) {
                    generator.writeObjectFieldStart("categories");
                    boolean first = true;
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.currentName()))
                            break;
                        int currentLeft = 1, currentRight = 1;
                        if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {//root source
                            if (!first)//第一次已写categories:{,不需要开始{
                                generator.writeStartObject();
                            while (parser.nextToken() != null) {
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName()))
                                    break;//end _meta
                                generator.copyCurrentEvent(parser);
                                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                    String fileName = parser.currentName();
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
                            first = false;//第一 categories 本体结束了,下面需要写开始{
                        }//end source
                        if (currentRight - currentLeft == 1) {//叶子
                            generator.writeEndObject();
                        }
                        if (currentRight - currentLeft > 1) {
                            generator.writeArrayFieldStart("children");
                            stack.push(currentRight);
                        }
                        while (!stack.isEmpty() && stack.peek() - currentRight == 1) {//end children array
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
        return new ByteBufInputStream(buffer, true);
    }

    @Override
    public InputStream search(String key) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.writeKeyJsonEntity(key));
            Response response = client.performRequest(request);
            return this.rebuild(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.debug("No search was found for anything resembling name {} category ", key, e);
            throw new SearchException(String.format("No search was found for anything resembling name %s category", key), e);
        }
    }

    private String writeKeyJsonEntity(String key) {
        Objects.requireNonNull(key, "key is required");
        StringWriter writer = new StringWriter(128);
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
            generator.writeStringField("query", key);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.alias");
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("name.mnemonic", key);
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
    public InputStream searchSiblings(long id) {
        return null;
    }

    @Override
    public InputStream path(long id) {
        try (RestClient client = BUILDER.build()) {
            long rootId = -1;
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
                            rootId = parser.getValueAsInt();
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
            request.setJsonEntity(this.writePathJsonEntity(rootId, left, right));
            response = client.performRequest(request);
            return this.rebuild(response.getEntity().getContent());
        } catch (ResponseException e) {
            LOGGER.warn("The category(id={}) not found", id, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new RuntimeException(e);
        }
    }

    private String writePathJsonEntity(long rootId, int left, int right) {
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
        return writer.toString();
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
