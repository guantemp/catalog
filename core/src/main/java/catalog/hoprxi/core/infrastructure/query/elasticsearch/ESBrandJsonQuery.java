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
import catalog.hoprxi.core.infrastructure.ESUtil;
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
        builder.addHeader(HttpHeaders.AUTHORIZATION, ESUtil.encrypt())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        //builder.setHttpAsyncResponseConsumerFactory(
        //new HttpAsyncResponseConsumerFactory
        //.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_doc/" + id);
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
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No brand with ID={} found", id, e);
        }
        return result;
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_search");
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
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            //System.out.println(e);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling name {} brand ", name, e);
        }
        return "";
    }

    private String rebuildBrands(InputStream is) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(writer);
        generator.writeStartObject();
        JsonParser parser = jsonFactory.createParser(is);
        while (!parser.isClosed()) {
            if (parser.nextToken() == JsonToken.FIELD_NAME && "hits".equals(parser.getCurrentName())) {
                parseHits(parser, generator);
                break;
            }
        }
        generator.writeEndObject();
        generator.close();
        return writer.toString();
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
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
                    parserInternalHits(parser, generator);
                }
            }
        }
    }

    private void parserInternalHits(JsonParser parser, JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart("brands");
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            //System.out.println(parser.currentToken() + ":" + parser.getCurrentName());
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
    }

    @Override
    public String queryAll(int offset, int limit) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(COMMON_OPTIONS);
        request.setJsonEntity(ESQueryJsonEntity.paginationQueryJsonEntity(offset, limit));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query brands from {} to {}:", offset, limit, e);
        }
        return "";
    }
}
