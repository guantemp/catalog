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
import catalog.hoprxi.core.infrastructure.ElasticsearchUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.http.HttpHeaders;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ESCategoryJsonQuery.class);
    private static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader(HttpHeaders.AUTHORIZATION, ElasticsearchUtil.encrypted())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        //builder.setHttpAsyncResponseConsumerFactory(
        //new HttpAsyncResponseConsumerFactory
        //.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ElasticsearchUtil.host(), ElasticsearchUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_doc/" + id);
        request.setOptions(COMMON_OPTIONS);
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
        } catch (IOException e) {
            LOGGER.error("No category with ID={} found", id, e);
        }
        return result;
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ElasticsearchUtil.host(), ElasticsearchUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/category/_search");
        request.setOptions(COMMON_OPTIONS);
        request.setJsonEntity(ESQueryJsonEntity.queryNameJsonEntity(name));
        /*
        request.setEntity(new NStringEntity(
                "{\"json\":\"text\"}",
                ContentType.APPLICATION_JSON));
         */
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildCategories(response.getEntity().getContent());
        } catch (IOException e) {
            //System.out.println(e);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling name {} brand ", name, e);
        }
        return "";
    }

    private String rebuildCategories(InputStream content) {
        return "";
    }

    private String queryNameJsonEntity(String name) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            //generator.writeNumberField("from", 0);
            //generator.writeNumberField("size", 400);
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
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryChildren(String id) {
        return null;
    }

    @Override
    public String queryDescendant(String id) {
        return null;
    }

    @Override
    public String queryAll(int offset, int limit) {
        return null;
    }
}
