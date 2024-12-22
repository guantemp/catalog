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
import org.elasticsearch.client.*;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.infrastructure.query.elasticsearch.Brand");
    private static final RestClientBuilder BUILDER = RestClient.builder(new HttpHost(ESUtil.host(), ESUtil.port(), "https"));
    private static final String EMPTY_BRAND = "{}";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/brand/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = client.performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.START_OBJECT && "_source".equals(parser.getCurrentName())) {
                    StringWriter writer = new StringWriter(256);
                    JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
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
            }
        } catch (ResponseException e) {
            LOGGER.warn("The brand(id={}) not found", id, e);
        } catch (JsonParseException e) {
            LOGGER.warn("Incorrect JSON format", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
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
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("The brand(id={}) can't retrieve", id, e);
        }
        return null;
    }

    @Override
    public String queryByName(String name, int offset, int limit, SortField sortField) {
        Objects.requireNonNull(name, "name is required");
        if (sortField == null)
            sortField = SortField.ID_ASC;
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/brand/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(queryNameJsonEntity(name, offset, limit, sortField));
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling name({}) brand", name, e);
        }
        return "{}";
    }

    private String queryNameJsonEntity(String name, int offset, int limit, SortField sortField) {
        StringWriter writer = new StringWriter(384);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
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
        } catch (IOException e) {
            LOGGER.error("Can't assemble name request json", e);
        }
        //System.out.println(writer.getBuffer().capacity());
        return writer.toString();
    }

    @Override
    public String queryAll(int size, String[] searchAfter, SortField sortField) {
        if (size < 0 || size > 10000)
            throw new IllegalArgumentException("size must be can't be negative and less than or equal to: [10000].");
        if (searchAfter == null)
            throw new IllegalArgumentException("searchAfter required");
        if (sortField == null)
            sortField = SortField.ID_ASC;
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/brand/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.paginationQueryJsonEntity(size, searchAfter, sortField));
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("Not brand found from {}:", searchAfter, e);
        }
        return "{}";
    }

    private String paginationQueryJsonEntity(int size, String[] searchAfter, SortField sortField) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
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

            if (searchAfter.length > 0) {
                generator.writeArrayFieldStart("search_after");
                for (String s : searchAfter)
                    generator.writeString(s);
                generator.writeEndArray();
            }
            generator.writeEndObject();
        } catch (IOException e) {
            System.out.println(e);
            LOGGER.error("Can't assemble pagination query json", e);
        }
        //System.out.println(writer.getBuffer().capacity());
        return writer.toString();
    }

    @Override
    public String queryAll(int offset, int limit, SortField sortField) {
        if (offset < 0) throw new IllegalArgumentException("offset can't be negative");
        if (limit < 0) throw new IllegalArgumentException("limit can't be negative");
        if (offset + limit > 10000)
            throw new IllegalArgumentException("from + size must be less than or equal to: [10000].");
        if (sortField == null)
            sortField = SortField.ID_ASC;
        try (RestClient client = BUILDER.build()) {
            Request request = new Request("GET", "/brand/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.paginationQueryJsonEntity(offset, limit, sortField));
            Response response = client.performRequest(request);
            return rebuildBrands(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("Not brand found from {} to {}:", offset, offset + limit, e);
        }
        return "{}";
    }

    private String paginationQueryJsonEntity(int offset, int limit, SortField sortField) {
        StringWriter writer = new StringWriter(96);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", limit);
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

            generator.writeEndObject();
        } catch (IOException e) {
            //System.out.println(e);
            LOGGER.error("Can't assemble pagination query json", e);
        }
        //System.out.println(writer.getBuffer().capacity());
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
