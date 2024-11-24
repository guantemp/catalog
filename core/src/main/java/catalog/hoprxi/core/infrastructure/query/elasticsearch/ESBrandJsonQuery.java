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
import com.fasterxml.jackson.core.*;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-06-15
 */
public class ESBrandJsonQuery implements BrandJsonQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.es.brand");
    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        String result = "{}";
        try {
            Response response = client.performRequest(request);
            JsonParser parser = jsonFactory.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
                    parser.nextToken();
                    StringWriter writer = new StringWriter(1024);
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
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("The brand(id={}) can't retrieve", id, e);
        }
        return result;
    }

    public OutputStream queryT(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        OutputStream os = new ByteArrayOutputStream(1024);
        try {
            Response response = client.performRequest(request);
            JsonParser parser = jsonFactory.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
                    parser.nextToken();
                    JsonGenerator generator = jsonFactory.createGenerator(os, JsonEncoding.UTF8);
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName()))
                            break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                    generator.close();
                    break;
                }
            }
            parser.close();
            client.close();
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("The brand(id={}) can't retrieve", id, e);
        }
        return os;
    }

    @Override
    public String queryByName(String name, int offset, int limit) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESQueryJsonEntity.queryNameJsonEntity(name, offset, limit));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("No search was found for anything resembling name({}) brand", name, e);
        }
        return "{}";
    }

    private String rebuildBrands(InputStream is) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
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
                    generator.writeArrayFieldStart("brands");
                    while (parser.nextToken() != null) {
                        if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                            generator.writeStartObject();
                            parserSource(parser, generator);
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

    @Override
    public String queryAll(int size, String[] searchAfter) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESQueryJsonEntity.paginationQueryJsonEntity(size, searchAfter));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Not query brands from {}:", searchAfter, e);
        }
        return "{}";
    }
}
