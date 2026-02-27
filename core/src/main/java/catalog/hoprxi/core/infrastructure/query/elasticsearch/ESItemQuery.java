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


import catalog.hoprxi.core.application.query.*;
import catalog.hoprxi.core.domain.model.barcode.BarcodeValidServices;
import catalog.hoprxi.core.infrastructure.ESUtil;
import catalog.hoprxi.core.infrastructure.query.JsonByteBufOutputStream;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2025/10/24
 */

public class ESItemQuery implements ItemQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Item");
    private static final String SEARCH_PREFIX_POINT = "/" + ESUtil.customized() + "_item";
    private static final String SEARCH_ENDPOINT = SEARCH_PREFIX_POINT + "/_search";
    private static final int AGGS_SIZE = 12;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final int BATCH_BUFFER_SIZE = 64 * 1024;// 64KB缓冲区
    private static final int SINGLE_BUFFER_SIZE = 1024;//2KB缓冲区
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public InputStream find(long id) {
        Request request = new Request("GET", "/item/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                ESItemQuery.extractSourceSkipMeta(parser, generator);
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                LOGGER.warn("Item not found in Elasticsearch: id={}", id);
                throw new SearchException(String.format("The item(id=%s) not found", id));
            }
            LOGGER.error("Elasticsearch error for id={}", id, e);
            throw new SearchException("Elasticsearch internal error", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (!success && buffer.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    @Override
    public Flux<ByteBuf> findAsync(long id) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        Request request = new Request("GET", "/item/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    sink.tryEmitError(new RuntimeException("Processing cancelled"));
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         OutputStream os = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        ESItemQuery.extractSourceSkipMeta(parser, generator);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {
                    if (err != null && !isCancelled.get()) {
                        Throwable cause = err;
                        if (err instanceof UncheckedIOException) {
                            cause = err.getCause(); // 解包 IO 异常
                        }
                        if (cause instanceof IOException) { // 如果是普通 RuntimeException，cause 就是 err 本身，保持不动
                            LOGGER.warn("Transform failed due to IO error for item id={}", id, cause);
                            sink.tryEmitError(new SearchException("Transform failed: IO error", cause));
                        } else {//【修复空指针】：确保 cause 不为 null 再调用 getMessage()
                            LOGGER.error("Unexpected system error (Bug) while fetching item id={}", id, cause);
                            sink.tryEmitError(new SearchException("Unexpected error: " + cause.getMessage(), cause));
                        }
                    } else if (!isCancelled.get()) {
                        sink.tryEmitComplete();
                    }
                });
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(ESItemQuery.mapException(exception, id));
                }
            }
        });
        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for id: {}", id))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static void extractSourceSkipMeta(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.currentName())) {
                parser.nextToken(); // move to value (START_OBJECT)
                if (parser.currentToken() != JsonToken.START_OBJECT) {
                    throw new IllegalStateException("_source is not an object");
                }
                generator.writeStartObject();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName())) {
                        parser.nextToken();
                        parser.skipChildren(); // skip value of _meta
                    } else {
                        generator.copyCurrentEvent(parser); // copy field name
                        parser.nextToken();
                        generator.copyCurrentStructure(parser); // copy entire value (handles nested)
                    }
                }
                generator.writeEndObject();
            }
        }
        generator.flush();
    }

    @Override
    public InputStream findByBarcode(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode ctr");
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildBarcodeFindRequest(barcode));
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                Extract.extract(parser, generator, "items");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                LOGGER.warn("Item not found in Elasticsearch: id={}", barcode);
                throw new SearchException(String.format("The item(id=%s) not found", barcode));
            }
            LOGGER.error("Elasticsearch error for id={}", barcode, e);
            throw new SearchException("Elasticsearch internal error", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (!success && buffer.refCnt() > 0) {
                buffer.release(); // 仅在未成功返回时释放
            }
        }
    }

    @Override
    public Flux<ByteBuf> findByBarcodeAsync(String barcode) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode");
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildBarcodeFindRequest(barcode));

        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    sink.tryEmitError(new IOException("Processing cancelled"));
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         JsonByteBufOutputStream jbbos = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(jbbos)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        ESItemQuery.extractSourceSkipMeta(parser, generator);
                    } catch (IOException | RuntimeException e) {
                        if (!isCancelled.get()) {
                            sink.tryEmitError(e);
                        }
                    }
                }).whenComplete((v, err) -> {
                    if (err != null && !isCancelled.get()) {
                        sink.tryEmitError(new SearchException("Transform failed", err));
                    } else if (!isCancelled.get()) {
                        sink.tryEmitComplete();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(ESItemQuery.mapException(e, barcode));
                }
            }
        });

        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for barcode: {}", barcode))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildBarcodeFindRequest(String barcode) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");

            generator.writeArrayFieldStart("filter");
            // 开始一个数组元素（一个对象）
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode.raw", barcode);
            generator.writeEndObject();
            generator.writeEndObject(); // 结束数组元素对象
            generator.writeEndArray(); // 结束filter数组

            generator.writeEndObject();//end bool
            generator.writeEndObject();//end query
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Failed to build ES query", e);
        }
        return writer.toString();
    }

    private static Throwable mapException(Exception e, Object identifier) {
        if (e instanceof ResponseException) {
            int status = ((ResponseException) e).getResponse().getStatusLine().getStatusCode();
            if (status == 404) {
                return new NotFoundException("Item not found: " + identifier);
            } else if (status >= 400 && status < 500) {
                return new SearchException("Client error: " + status);
            } else {
                return new SearchException("Server error: " + status);
            }
        } else if (e instanceof IOException) {
            LOGGER.error("I/O error for id={}", identifier, e);
            return new SearchException("Network error", e);
        }
        return new SearchException("Unexpected error", e);
    }

    @Override
    public InputStream search(ItemQueryFilter[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSearchRequest(filters, size, searchAfter, sortField));
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                Extract.extract(parser, generator, "items");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                LOGGER.warn("Item not found in Elasticsearch");
                throw new SearchException("The item(id=%s) not found");
            }
            LOGGER.error("Elasticsearch error", e);
            throw new SearchException("Elasticsearch internal error", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (!success && buffer.refCnt() > 0) {
                buffer.release(); // 仅在未成功返回时释放
            }
        }
    }

    @Override
    public Flux<ByteBuf> searchAsync(ItemQueryFilter[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSearchRequest(filters, size, searchAfter, sortField));

        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    sink.tryEmitError(new IOException("Processing cancelled"));
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         JsonByteBufOutputStream jbbos = new JsonByteBufOutputStream(sink, isCancelled, BATCH_BUFFER_SIZE); JsonGenerator generator = JSON_FACTORY.createGenerator(jbbos)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        Extract.extract(parser, generator, "items");
                    } catch (IOException | RuntimeException e) {
                        if (!isCancelled.get()) {
                            sink.tryEmitError(e);
                        }
                    }
                }).whenComplete((v, err) -> {
                    if (err != null && !isCancelled.get()) {
                        sink.tryEmitError(new SearchException("Transform failed", err));
                    } else if (!isCancelled.get()) {
                        sink.tryEmitComplete();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(ESItemQuery.mapException(e, ESItemQuery.extractIdentifier(filters)));
                }
            }
        });
        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for flier: {}", (Object[]) filters))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildSearchRequest(ItemQueryFilter[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            ESItemQuery.buildMainRequest(generator, filters);
            ESItemQuery.buildSortRequest(generator, sortField);
            ESItemQuery.buildSearchAfterRequest(generator, searchAfter);
            ESItemQuery.buildAggsRequest(generator);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private static void buildSearchAfterRequest(JsonGenerator generator, String searchAfter) throws IOException {
        if (searchAfter == null || searchAfter.isBlank()) return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
    }

    private static Object extractIdentifier(ItemQueryFilter[] filters) {
        if (filters == null || filters.length == 0) {
            return "empty-filters";
        }
        for (ItemQueryFilter f : filters) {// 优先找 id
            if ("KeywordFilter".equals(f.getClass().getSimpleName())) {
                return "Keyword filter"; // 假设 getValue() 返回 String 或 Number
            }
        }
        for (ItemQueryFilter f : filters) { // 次选条码
            if ("CategoryFilter".equals(f.getClass().getSimpleName())) {
                return "Category filter";
            }
        }
        // 否则返回数量
        return "filters(" + filters.length + ")";
    }


    @Override
    public Flux<ByteBuf> searchAsync(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        System.out.println(ESItemQuery.buildSearchRequest(filters, offset, size, sortField));
        request.setJsonEntity(ESItemQuery.buildSearchRequest(filters, offset, size, sortField));

        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    sink.tryEmitError(new IOException("Processing cancelled"));
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         OutputStream os = new JsonByteBufOutputStream(sink, isCancelled, BATCH_BUFFER_SIZE); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        Extract.extract(parser, generator, "items");
                    } catch (IOException | RuntimeException e) {
                        if (!isCancelled.get()) {
                            sink.tryEmitError(e);
                        }
                    }
                }).whenComplete((v, err) -> {
                    if (err != null && !isCancelled.get()) {
                        sink.tryEmitError(new SearchException("Transform failed", err));
                    } else if (!isCancelled.get()) {
                        sink.tryEmitComplete();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(ESItemQuery.mapException(e, filters));
                }
            }
        });
        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for filter: {}", Arrays.stream(filters)
                        .filter(Objects::nonNull)
                        .map(Object::toString)))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    @Override
    public InputStream search(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSearchRequest(filters, offset, size, sortField));
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean success = false;
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                Extract.extract(parser, generator, "items");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                LOGGER.error("Item not found in Elasticsearch");
                throw new SearchException("The item(id=%s) not found");
            }
            LOGGER.error("Elasticsearch error", e);
            throw new SearchException("Elasticsearch internal error", e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        } finally {
            if (!success && buffer.refCnt() > 0) {
                buffer.release(); // 仅在未成功返回时释放
            }
        }
    }

    private static String buildSearchRequest(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", size);
            ESItemQuery.buildMainRequest(generator, filters);
            ESItemQuery.buildSortRequest(generator, sortField);
            ESItemQuery.buildAggsRequest(generator);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();//root
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        //System.out.println(writer);
        return writer.toString();
    }

    private static void buildMainRequest(JsonGenerator generator, ItemQueryFilter[] filters) throws IOException {
        generator.writeObjectFieldStart("query");
        if (filters == null || filters.length == 0) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("filter");
            for (ItemQueryFilter filter : filters) {
                filter.filter(generator);
            }
            generator.writeEndArray();//end must
            generator.writeEndObject();//end bool
        }
        generator.writeEndObject();//end query
    }

    private static void buildSortRequest(JsonGenerator generator, SortFieldEnum sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }

    private static void buildAggsRequest(JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("aggs");
        ESItemQuery.buildCompositeAgg(generator, "brand_aggs", "brand.id", "brand.name");
        ESItemQuery.buildCompositeAgg(generator, "category_aggs", "category.id", "category.name");
        generator.writeEndObject();//end eggs
    }

    private static void buildCompositeAgg(JsonGenerator gen, String aggName, String... fields)
            throws IOException {
        gen.writeObjectFieldStart(aggName);
        gen.writeObjectFieldStart("composite");
        gen.writeNumberField("size", ESItemQuery.AGGS_SIZE);
        gen.writeArrayFieldStart("sources");
        for (String field : fields) {
            gen.writeStartObject();
            gen.writeObjectFieldStart(field.replace('.', '_'));
            gen.writeObjectFieldStart("terms");
            gen.writeStringField("field", field);
            gen.writeEndObject();//end terms
            gen.writeEndObject();//end field
            gen.writeEndObject();
        }
        gen.writeEndArray();//end array sources
        gen.writeEndObject(); // end composite
        gen.writeEndObject(); // end aggName
    }
}
