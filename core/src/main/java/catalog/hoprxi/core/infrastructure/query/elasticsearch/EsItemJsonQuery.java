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

import catalog.hoprxi.core.application.query.ItemJsonQuery;
import catalog.hoprxi.core.application.query.QueryFilter;
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
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-01-11
 */
public class EsItemJsonQuery implements ItemJsonQuery {
    private static final String EMPTY_ITEM = "{}";
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.es");
    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final Pattern BARCODE = Pattern.compile("^\\d{1,13}$");

    @Override
    public String query(String id) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/item/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
                    parser.nextToken();
                    StringWriter writer = new StringWriter(1024);
                    JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.getCurrentName())) break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                    generator.close();
                    //System.out.println(writer.getBuffer().capacity());
                    return writer.toString();
                }
            }
        } catch (IOException e) {
            LOGGER.warn("No item with ID={} found", id, e);
        }
        return EMPTY_ITEM;
    }


    private static void writeSearchAfter(JsonGenerator generator, String searchAfter) throws IOException {
        generator.writeBooleanField("track_scores", false);
        if (searchAfter == null || searchAfter.isEmpty())
            return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
    }

    private String accurateQueryBarcodeJsonEntity(String barcode) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
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
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public String queryByBarcode(String barcode) {
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
        return EMPTY_ITEM;
    }

    @Override
    public String query(String key, QueryFilter[] filters, int size, String searchAfter, SortField sortField) {
        if (size < 0 || size > 10000)
            throw new IllegalArgumentException("size must lager 10000");
        if (searchAfter == null || searchAfter.isEmpty()) {
            LOGGER.info("searchAfter is empty");
        }
        if (sortField == null) {
            sortField = SortField.ID_DESC;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/item/_search");
            request.setOptions(ESUtil.requestOptions());
            System.out.println(queryJsonEntity(key, filters, size, searchAfter, sortField));
            request.setJsonEntity(queryJsonEntity(key, filters, size, searchAfter, sortField));
            Response response = client.performRequest(request);
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            //System.out.println(e);
            LOGGER.warn("No search was found for anything resembling key = {} item ", key, e);
        }
        return EMPTY_ITEM;
    }

    private String queryJsonEntity(String key, QueryFilter[] filters, int size, String searchAfter, SortField sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            writeMain(generator, key, filters);
            writeSortField(generator, sortField);
            writeSearchAfter(generator, searchAfter);
            writeAggs(generator, 20);
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private static void writeAggs(JsonGenerator generator, int size) throws IOException {
        generator.writeObjectFieldStart("aggs");
        generator.writeObjectFieldStart("brand_aggs");
        generator.writeObjectFieldStart("multi_terms");
        generator.writeNumberField("size", size);
        generator.writeArrayFieldStart("terms");
        generator.writeStartObject();
        generator.writeStringField("field", "brand.id");
        generator.writeEndObject();
        generator.writeStartObject();
        generator.writeStringField("field", "brand.name");
        generator.writeEndObject();
        generator.writeEndArray();
        generator.writeEndObject();
        generator.writeEndObject();
        generator.writeObjectFieldStart("category_aggs");
        generator.writeObjectFieldStart("multi_terms");
        generator.writeNumberField("size", size);
        generator.writeArrayFieldStart("terms");
        generator.writeStartObject();
        generator.writeStringField("field", "category.id");
        generator.writeEndObject();
        generator.writeStartObject();
        generator.writeStringField("field", "category.name");
        generator.writeEndObject();
        generator.writeEndArray();
        generator.writeEndObject();
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private static void writeSortField(JsonGenerator generator, SortField sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }

    @Override
    public String query(String key, QueryFilter[] filters, int from, int size, SortField sortField) {
        if (from < 0 || from > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (from + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortField.ID_DESC;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        StringWriter writer = queryJsonEntity(key, filters, from, size, sortField);
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
            if (LOGGER.isDebugEnabled()) LOGGER.debug("No search was found for anything items ", e);
        }
        return EMPTY_ITEM;
    }

    private StringWriter queryJsonEntity(String key, QueryFilter[] filters, int from, int size, SortField sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", from);
            generator.writeNumberField("size", size);
            writeMain(generator, key, filters);
            writeSortField(generator, sortField);
            writeAggs(generator, 20);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();//root
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        return writer;
    }

    private void writeMain(JsonGenerator generator, String key, QueryFilter[] filters) throws IOException {
        generator.writeObjectFieldStart("query");
        if (key == null || key.isEmpty()) {//not key ,query all
            if (filters.length == 0) {
                generator.writeObjectFieldStart("match_all");
                generator.writeEndObject();//match_all
            } else {//add filter
                generator.writeObjectFieldStart("bool");
                generator.writeArrayFieldStart("must");
                for (QueryFilter filter : filters) {
                    filter.filter(generator);
                }
                generator.writeEndArray();//must
                generator.writeEndObject();//bool
            }
        } else {
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");
            if (BARCODE.matcher(key).matches()) {//only barcode query
                generator.writeStartObject();
                generator.writeObjectFieldStart("term");
                generator.writeStringField("barcode", key);
                generator.writeEndObject();
                generator.writeEndObject();
            } else {
                generator.writeStartObject();
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
                generator.writeEndObject();
                generator.writeEndObject();
                generator.writeEndObject();
            }
            for (QueryFilter filter : filters) {
                filter.filter(generator);
            }
            generator.writeEndArray();//must
            generator.writeEndObject();//bool
        }
        generator.writeEndObject();//query
    }

    private String rebuildItems(InputStream is) throws IOException {
        StringWriter writer = new StringWriter(1536);
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        generator.writeStartObject();
        JsonParser parser = JSON_FACTORY.createParser(is);
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                //System.out.println(parser.currentToken() + ":" + parser.getCurrentName());
                String fieldName = parser.getCurrentName();
                if ("hits".equals(fieldName)) {
                    parseHits(parser, generator);
                } else if ("aggregations".equals(fieldName)) {
                    do {
                        generator.copyCurrentEvent(parser);
                        parser.nextToken();
                    } while (!(parser.currentToken() == JsonToken.END_OBJECT && "aggregations".equals(parser.getCurrentName())));
                }
            }
        }
        generator.writeEndObject();
        generator.close();
        //System.out.println(writer.getBuffer().capacity());
        return writer.toString();
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            //System.out.println(parser.currentToken() + ":" + parser.getCurrentName());
            if (parser.currentToken() == JsonToken.END_OBJECT && "hits".equals(parser.getCurrentName())) {
                break;
            }
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
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.getCurrentName())) break;
                    //System.out.println(parser.currentToken()+":"+parser.getCurrentName());
                    generator.copyCurrentEvent(parser);
                }
            }
            if (parser.currentToken() == JsonToken.END_OBJECT && "_source".equals(parser.getCurrentName())) break;
        }
    }

    private void parserSort(JsonParser parser, JsonGenerator generator) throws IOException {
        if (parser.nextToken() == JsonToken.FIELD_NAME && "sort".equals(parser.getCurrentName())) {
            generator.copyCurrentEvent(parser);
            while (parser.nextToken() != null) {
                generator.copyCurrentEvent(parser);
                if (parser.currentToken() == JsonToken.END_ARRAY && "sort".equals(parser.getCurrentName())) break;
            }
        }
    }
}

