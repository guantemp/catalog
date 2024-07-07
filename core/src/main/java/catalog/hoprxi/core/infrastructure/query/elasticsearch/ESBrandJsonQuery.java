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

import catalog.hoprxi.core.application.query.BrandJsonQuery;
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
 * @version 0.0.1 builder 2024-06-15
 */
public class ESBrandJsonQuery implements BrandJsonQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(ESBrandJsonQuery.class);
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
        Request request = new Request("GET", "/brand/_doc/" + id);
        request.setOptions(COMMON_OPTIONS);
        String result = "";
        try {
            Response response = client.performRequest(request);
            //String responseBody = EntityUtils.toString(response.getEntity());
            //System.out.println(responseBody);
            JsonParser parser = jsonFactory.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "_source":
                            result = rebuildSingle(parser);
                            break;
                    }
                }
            }
            parser.close();
            client.close();
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("e");
        }
        return result;
    }

    private String rebuildSingle(JsonParser parser) throws IOException {
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
        return writer.toString();
    }

    @Override
    public String queryAll(int offset, int limit) {
        return null;
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ElasticsearchUtil.host(), ElasticsearchUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(COMMON_OPTIONS);
        request.setJsonEntity(queryNameJsonEntity(name));
        /*
        request.setEntity(new NStringEntity(
                "{\"json\":\"text\"}",
                ContentType.APPLICATION_JSON));
         */
        try {
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
            //String responseBody = EntityUtils.toString(response.getEntity());
            //System.out.println(responseBody);
        } catch (IOException e) {
            //System.out.println(e);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("e");
        }
        return "";
    }

    private String queryNameJsonEntity(String name) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeNumberField("from", 0);
            generator.writeNumberField("size", 400);
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
                LOGGER.debug("e");
        }
        return writer.toString();
    }

    private String rebuildBrands(InputStream is) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(writer);

        JsonParser parser = jsonFactory.createParser(is);
        while (!parser.isClosed()) {
            if (parser.nextToken() == JsonToken.FIELD_NAME && "hits".equals(parser.getCurrentName())) {
                this.parseHits(parser, generator);
                break;
            }
        }
        return writer.toString();
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();
                if (fieldName.equals("total")) {
                    parserTotal(parser);
                } else if (fieldName.equals("hits")) {
                    parserInternalHits(parser);
                }
            }
        }
    }

    private void parserTotal(JsonParser parser) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();
                if (fieldName.equals("value")) {
                    parser.nextToken();
                    System.out.println(parser.getValueAsInt());
                    break;
                }
            }
        }
    }

    private void parserInternalHits(JsonParser parser) throws IOException {
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            System.out.println(parser.currentToken() + ":" + parser.getCurrentName());
            if (parser.currentToken() == JsonToken.START_OBJECT && parser.getCurrentName().equals("_source"))
                System.out.println(rebuildSingle(parser));
        }
    }
}
