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

import catalog.hoprxi.core.application.query.ItemJsonQuery;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-08-15
 */
public class EsItemJsonQuery implements ItemJsonQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsItemJsonQuery.class);
    private static final int SIZE = 500;

    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/item/_doc/" + id);
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
                        if ("_meta".equals(parser.getCurrentName())) break;
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
            if (LOGGER.isDebugEnabled()) LOGGER.debug("No item with ID={} found", id, e);
        }
        return result;
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESQueryJsonEntity.queryNameJsonEntity(name, 200, new String[0]));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            System.out.println(e);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling name {} item ", name, e);
        }
        return "";
    }

    @Override
    public String queryByBarcode(String barcode, int size, String searchAfter) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(queryByBarcodeJsonEntity(barcode, size, searchAfter));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling barcode {} item ", barcode, e);
        }
        return "";
    }

    private String queryByBarcodeJsonEntity(String barcode, int size, String searchAfter) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeNumberField("size", size);

            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode", barcode);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("barcode.raw", "asc");
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
    public String accurateQueryByBarcode(String barcode) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(this.accurateQueryBarcodeJsonEntity(barcode));
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything resembling barcode {} item ", barcode, e);
        }
        return "";
    }

    private String accurateQueryBarcodeJsonEntity(String barcode) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");

            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode.raw", barcode);
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
    public String queryByBrand(String brandId) {
        return null;
    }

    @Override
    public String queryByCategory(String categoryId) {
        return null;
    }

    @Override
    public String queryByCategoryAndItsDescendants(String categoryId) {
        return null;
    }

    @Override
    public String queryAll(int size, String[] searchAfter) {
        StringWriter writer = queryAllJsonEntity(size, searchAfter);
        RestClientBuilder builder = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(writer.toString());
        try {
            Response response = client.performRequest(request);
            client.close();
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("No search was found for anything items ", e);
        }
        return "";
    }

    private StringWriter queryAllJsonEntity(int size, String[] searchAfter) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "desc");
            generator.writeEndObject();
            generator.writeEndArray();

            if (searchAfter.length > 0) {
                generator.writeArrayFieldStart("search_after");
                for (String s : searchAfter)
                    generator.writeString(s);
                generator.writeEndArray();
            }

            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer;
    }

    private String rebuildItems(InputStream is) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(writer);
        generator.writeStartObject();
        JsonParser parser = jsonFactory.createParser(is);
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "hits".equals(parser.getCurrentName())) {
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
                    generator.writeArrayFieldStart("items");
                    while (parser.nextToken() != null) {
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

