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

import catalog.hoprxi.core.application.query.BrandJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.*;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2025-03-01
 */
public class ESBrandJsonQuery implements BrandJsonQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Brand");
    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final String EMPTY_BRAND = "";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    private static final String SEARCH_PREFIX = "/" + ESUtil.database() + "_brand";

    @Override
    public String query(long id) {
        //System.out.println(SEARCH_PREFIX + "/_doc/" + id);
        //Request request = new Request("GET", "/"+ESUtil.databasePrefx()+"/_doc/" + id);
        Request request = new Request("GET", "/brand/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        try (RestClient client = BUILDER.build()) {
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    StringWriter writer = new StringWriter(256);
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
        } catch (ResponseException e) {
            LOGGER.info("The brand(id={}) not found", id, e);
            throw new QueryException(String.format("The brand(id=%s) not found", id), e);
        } catch (JsonParseException e) {
            LOGGER.error("Incorrect JSON format", e);
            throw new QueryException(String.format("The brand(id=%s) not found", id), e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new QueryException(String.format("The brand(id=%s) not found", id), e);
        }
        return EMPTY_BRAND;
    }

    @Deprecated
    public OutputStream queryForTest(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/brand/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            OutputStream os = new ByteArrayOutputStream(1024);
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
                    parser.nextToken();
                    JsonGenerator generator = JSON_FACTORY.createGenerator(os, JsonEncoding.UTF8).useDefaultPrettyPrinter();
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
            return os;
        } catch (IOException e) {
            LOGGER.error("The brand(id={}) can't retrieve", id, e);
        }
        return null;
    }

    @Override
    public String query(String name, int offset, int limit, SortField sortField) {
        Objects.requireNonNull(name, "name is required");
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("The offset value range is 0-10000");
        if (limit < 0 || limit > 10000) throw new IllegalArgumentException("The size value range is 0-10000");
        if (limit + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortField.ID_DESC;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        try (RestClient client = BUILDER.build()) {
            request.setJsonEntity(queryNameJsonEntity(name, offset, limit, sortField));
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("No search was found for anything resembling name({}) brand", name, e);
            throw new QueryException(String.format("No search was found for anything resembling name(%s) brand", name), e);
        }
    }

    private String queryNameJsonEntity(String name, int offset, int limit, SortField sortField) throws IOException {
        StringWriter writer = new StringWriter(384);
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        generator.writeStartObject();
        generator.writeNumberField("from", offset);
        generator.writeNumberField("size", limit);
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
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();

        generator.writeEndObject();
        generator.close();
        //System.out.println(writer.getBuffer().capacity());
        return writer.toString();
    }

    @Override
    public String query(int size, String searchAfter, SortField sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("The size value range is 0-10000");
        if (sortField == null) {
            sortField = SortField.ID_DESC;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        try (RestClient client = BUILDER.build()) {
            request.setJsonEntity(this.paginationQueryJsonEntity(size, searchAfter, sortField));
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("Not brand found from {}:", searchAfter, e);
            throw new QueryException(String.format("Not brand found from %s", searchAfter), e);
        }
    }

    private String paginationQueryJsonEntity(int size, String searchAfter, SortField sortField) throws IOException {
        StringWriter writer = new StringWriter(128);
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        generator.writeStartObject();
        generator.writeNumberField("size", size);
        generator.writeObjectFieldStart("query");
        generator.writeFieldName("match_all");
        generator.writeStartObject();
        generator.writeEndObject();
        generator.writeEndObject();

        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();

        if (searchAfter != null && !searchAfter.isEmpty()) {
            generator.writeArrayFieldStart("search_after");
            generator.writeString(searchAfter);
            generator.writeEndArray();
        }
        generator.writeEndObject();
        generator.close();
        return writer.toString();
    }

    @Override
    public String query(int offset, int limit, SortField sortField) {
        if (offset < 0) throw new IllegalArgumentException("offset can't be negative");
        if (limit < 0) throw new IllegalArgumentException("limit can't be negative");
        if (offset + limit > 10000)
            throw new IllegalArgumentException("offset + limit must be less than or equal to: [10000].");
        if (sortField == null) {
            sortField = SortField.ID_DESC;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        try (RestClient client = BUILDER.build()) {
            request.setJsonEntity(this.paginationQueryJsonEntity(offset, limit, sortField));
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("Not brand found from {} to {}:", offset, offset + limit, e);
            throw new QueryException(String.format("Not brand found from %d to %d", offset, offset + limit), e);
        }
    }

    private String paginationQueryJsonEntity(int offset, int limit, SortField sortField) throws IOException {
        StringWriter writer = new StringWriter(96);
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        generator.writeStartObject();
        generator.writeNumberField("from", offset);
        generator.writeNumberField("size", limit);
        generator.writeObjectFieldStart("query");
        generator.writeFieldName("match_all");
        generator.writeStartObject();
        generator.writeEndObject();
        generator.writeEndObject();//query

        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();//sort

        generator.writeEndObject();//root
        generator.close();
        //System.out.println(writer.getBuffer().capacity());
        //System.out.println(writer);
        return writer.toString();
    }

    private String rebuildBrands(InputStream is) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        generator.writeStartObject();
        JsonParser parser = JSON_FACTORY.createParser(is);
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
