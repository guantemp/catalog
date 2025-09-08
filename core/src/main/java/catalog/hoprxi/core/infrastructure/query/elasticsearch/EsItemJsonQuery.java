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
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.domain.model.barcode.BarcodeValidServices;
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
 * @version 0.0.1 builder 2025-01-11
 */
public class EsItemJsonQuery implements ItemJsonQuery {
    private static final String EMPTY_ITEM = "";
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.item");
    private static final int AGGS_SIZE = 15;
    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Override
    public String query(long id) {
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/item/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
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
                    return writer.toString();
                }
            }
        } catch (IOException e) {
            LOGGER.warn("No item with ID={} found", id, e);
            throw new SearchException(String.format("The item (id = %s) not found.", id), e);
        }
        return EMPTY_ITEM;
    }

    private static void writeSortField(JsonGenerator generator, SortField sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
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
        generator.writeEndObject();//end brand_aggs

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
        generator.writeEndObject();//end category_aggs
        generator.writeEndObject();//end eggs
    }

    private static void writeSearchAfter(JsonGenerator generator, String searchAfter) throws IOException {
        if (searchAfter == null || searchAfter.isEmpty()) return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
    }

    @Override
    public String queryByBarcode(String barcode) {
        if (!BarcodeValidServices.valid(barcode))
            throw new IllegalArgumentException("Not valid barcode ctr");
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/item/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(writeQueryBarcodeJson(barcode));
            Response response = client.performRequest(request);
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling barcode {} item ", barcode, e);
        }
        return EMPTY_ITEM;
    }

    private String writeQueryBarcodeJson(String barcode) {
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
    public String query(ItemQueryFilter[] filters, int from, int size, SortField sortField) {
        if (from < 0 || from > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (from + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortField._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        StringWriter writer = writeQueryJson(filters, from, size, sortField);
        //System.out.println("\n" + writer);
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/item/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(writer.toString());
            Response response = client.performRequest(request);
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("No search was found for anything items ", e);
        }
        return EMPTY_ITEM;
    }

    private StringWriter writeQueryJson(ItemQueryFilter[] filters, int from, int size, SortField sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", from);
            generator.writeNumberField("size", size);
            writeMain(generator, filters);
            writeSortField(generator, sortField);
            writeAggs(generator, AGGS_SIZE);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();//root
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        return writer;
    }

    private void writeMain(JsonGenerator generator, ItemQueryFilter[] filters) throws IOException {
        generator.writeObjectFieldStart("query");
        if (filters == null || filters.length == 0) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");
            for (ItemQueryFilter filter : filters) {
                filter.filter(generator);
            }
            generator.writeEndArray();//must
            generator.writeEndObject();//bool
        }
        generator.writeEndObject();//query
    }

    @Override
    public String query(ItemQueryFilter[] filters, int size, String searchAfter, SortField sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (sortField == null) {
            sortField = SortField._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/item/_search");
            request.setOptions(ESUtil.requestOptions());
            //System.out.println(writeQueryJson(key, filters, size, searchAfter, sortField));
            request.setJsonEntity(writeQueryJson(filters, size, searchAfter, sortField));
            Response response = client.performRequest(request);
            return rebuildItems(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling key = {} item ", e);
        }
        return EMPTY_ITEM;
    }

    private String writeQueryJson(ItemQueryFilter[] filters, int size, String searchAfter, SortField sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            writeMain(generator, filters);
            writeSortField(generator, sortField);
            writeSearchAfter(generator, searchAfter);
            writeAggs(generator, AGGS_SIZE);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private String rebuildItems(InputStream is) throws IOException {
        StringWriter writer = new StringWriter(1536);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
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
                        generator.writeEndObject();
                    }
                }
            }
            generator.writeEndObject();
            generator.flush();
            //System.out.println(writer.getBuffer().capacity());
            return writer.toString();
        }
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            //System.out.println(parser.currentToken() + ":" + parser.getCurrentName());
            if (parser.currentToken() == JsonToken.END_OBJECT && "hits".equals(parser.getCurrentName())) break;
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
                            parserSource(parser, generator);
                            parserSort(parser, generator);
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

