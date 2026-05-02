/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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


import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.netty.buffer.ByteBuf;
import org.elasticsearch.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2026/04/22
 */

public final class ESBrandQuery implements BrandQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final String PREFIX = ESUtil.customized().isBlank() ? "/brand" : "/" + ESUtil.customized() + "_brand";
    private static final String SEARCH_ENDPOINT = PREFIX + "/_search";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();

    @Override
    public InputStream find(long id) throws SearchException {
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        return ReactiveStream.toSingleByteBufInputStream(request, String.valueOf(id));
    }

    @Override
    public Mono<ByteBuf> findAsync(long id) {
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        return ReactiveStream.toMonoByteBuf(request, String.valueOf(id));
    }

    @Override
    public InputStream search(String name, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchJsonRequest(name, offset, size, sortField));
        return ReactiveStream.toByteBufInputStream(request, "brands", name);
    }

    @Override
    public Flux<ByteBuf> searchAsync(String name, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size value range is 0-10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchJsonRequest(name, offset, size, sortField));
        return ReactiveStream.toFluxByteBuf(request, "brands", name);
    }

    private static String buildSearchJsonRequest(String name, int offset, int limit, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter(384);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", limit);
            ESBrandQuery.buildCommonJsonRequest(name, sortField, generator);
            generator.writeEndObject();
            generator.flush();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
    }

    @Override
    public InputStream search(String name, int size, String searchAfter, SortFieldEnum sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("The size value range is 0-10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchAfterJsonRequest(name, size, searchAfter, sortField));
        return ReactiveStream.toByteBufInputStream(request, "brands", name);
    }

    @Override
    public Flux<ByteBuf> searchAsync(String name, int size, String searchAfter, SortFieldEnum sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("The size value range is 0-10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchAfterJsonRequest(name, size, searchAfter, sortField));
        return ReactiveStream.toFluxByteBuf(request, "brands", name);
    }

    private static String buildSearchAfterJsonRequest(String name, int size, String searchAfter, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            ESBrandQuery.buildCommonJsonRequest(name, sortField, generator);
            if (searchAfter != null && !searchAfter.isBlank()) {
                generator.writeArrayFieldStart("search_after");
                generator.writeString(searchAfter);
                generator.writeEndArray();
            }
            generator.writeEndObject();
            generator.flush();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
    }

    private static void buildCommonJsonRequest(String name, SortFieldEnum sortField, JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("query");
        if (name == null || name.isBlank()) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
            generator.writeObjectFieldStart("constant_score");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", name);
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.shortName");
            generator.writeString("name.name.pinyin");
            generator.writeString("name.shortName.pinyin");
            generator.writeEndArray();//end array fields
            generator.writeEndObject();//end multi_match
            generator.writeEndObject();//end filter
            generator.writeEndObject();//end constant_score
        }
        generator.writeEndObject();//end query

        generator.writeArrayFieldStart("sort");
        /*
        generator.writeStartObject();
        generator.writeStringField("_score", sortField.sort());
        generator.writeEndObject();
         */
        generator.writeStartObject();
        generator.writeStringField(MapSortField.mapSortToField(sortField), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }
}
