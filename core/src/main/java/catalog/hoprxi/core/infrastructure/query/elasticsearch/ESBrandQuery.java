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


import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.infrastructure.ESUtil;
import catalog.hoprxi.core.infrastructure.query.JsonByteBufOutputStream;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/8/29
 */

public final class ESBrandQuery implements BrandQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final String SINGLE_PREFIX = "/" + ESUtil.database() + "_brand";
    private static final String SEARCH_ENDPOINT = SINGLE_PREFIX + "/_search";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();
    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public InputStream find(long id) throws SearchException {
        Request request = new Request("GET", "/brand/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        Response response = null;
        try {
            response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                Extract.extractSourceSkipMeta(parser, generator);
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            LOGGER.error("The brand(id={}) not found", id, e);
            throw new SearchException(String.format("The brand(id=%s) not found", id), e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (!success && buffer.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    @Override
    public Flux<ByteBuf> findAsync(long id) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        Request request = new Request("GET", "/brand/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    EntityUtils.consumeQuietly(response.getEntity()); // 必须加
                    sink.tryEmitComplete().orThrow();
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         OutputStream os = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        Extract.extractSourceSkipMeta(parser, generator);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {
                    if (err != null) {
                        MapException.mapExceptionAndEmit(sink, err, isCancelled, id);
                    } else {
                        sink.tryEmitComplete().orThrow();
                    }
                });
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(exception, id));
                }
            }
        });
        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for id: {}", id))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
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
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchJsonRequest(name, offset, size, sortField));
        boolean success = false;
        Response response = null;
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        try {
            response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                Extract.extract(parser, generator, "brands");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            LOGGER.error("No search was found for anything resembling name({}) brand", name, e);
            throw new SearchException(String.format("No search was found for anything resembling name(%s) brand", name), e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (!success && buffer.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
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
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchJsonRequest(name, offset, size, sortField));
        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    EntityUtils.consumeQuietly(response.getEntity()); // 必须加
                    sink.tryEmitComplete().orThrow();
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         JsonByteBufOutputStream jbbos = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(jbbos)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        Extract.extract(parser, generator, "brands");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {
                    if (err != null) {
                        MapException.mapExceptionAndEmit(sink, err, isCancelled, name);
                    } else {
                        sink.tryEmitComplete().orThrow();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(e, name));
                }
            }
        });

        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for barcode: {}", name))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildSearchJsonRequest(String name, int offset, int limit, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter(384);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", limit);
            ESBrandQuery.buildCommonJsonRequest(name, sortField, generator);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        return writer.toString();
    }

    @Override
    public InputStream search(String name, int size, String searchAfter, SortFieldEnum sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("The size value range is 0-10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESBrandQuery.buildSearchAfterJsonRequest(name, size, searchAfter, sortField));
        boolean success = false;
        Response response = null;
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        try {
            response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                Extract.extract(parser, generator, "brands");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            LOGGER.error("No search was found for anything resembling name({}) brand", name, e);
            throw new SearchException(String.format("No search was found for anything resembling name(%s) brand", name), e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (!success && buffer.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    private static String buildSearchAfterJsonRequest(String name, int size, String searchAfter, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            ESBrandQuery.buildCommonJsonRequest(name, sortField, generator);
            if (searchAfter != null && !searchAfter.isEmpty()) {
                generator.writeArrayFieldStart("search_after");
                generator.writeString(searchAfter);
                generator.writeEndArray();
            }
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private static void buildCommonJsonRequest(String name, SortFieldEnum sortField, JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("query");
        if (name == null || name.isEmpty()) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
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

            generator.writeEndArray();//end should
            generator.writeEndObject();//end bool
            generator.writeEndObject();
            generator.writeEndObject();//end bool
        }
        generator.writeEndObject();//end query

        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(MapSortField.mapSortToField(sortField), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }
}
