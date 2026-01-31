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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.*;
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
    private static final String SINGLE_PREFIX = "/" + ESUtil.customized() + "_item";
    private static final String SEARCH_ENDPOINT = SINGLE_PREFIX + "/_search";
    private static final int AGGS_SIZE = 15;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    //private static final int MAX_SIZE = 9999;
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final int BUFFER_SIZE = 2048;//2KB缓冲区

    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();


    @Override
    public InputStream find(long id) {
        Request request = new Request("GET", "/item/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BUFFER_SIZE);
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer);
                 JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent())) {
                ESItemQuery.copySourceWithoutMeta(parser, generator);
                generator.flush();
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            if (buffer.refCnt() > 0) buffer.release();
            LOGGER.error("The item(id={}) not found", id, e);
            throw new SearchException(String.format("The item(id=%s) not found", id), e);
        } catch (IOException e) {
            if (buffer.refCnt() > 0) buffer.release();
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        }
    }

    /*
        @Override
        public CompletableFuture<InputStream> findAsync(long id) {
            Request request = new Request("GET", "/item/_doc/" + id);//PREFIX+"/_doc/"
            request.setOptions(ESUtil.requestOptions());
            // 创建管道
            PipedInputStream pipedIn = new PipedInputStream(ASYNC_BUFFER_SIZE);//接受端
            final PipedOutputStream pipedOut;//输入端
            try {
                pipedOut = new PipedOutputStream(pipedIn);
            } catch (IOException e) {
                return CompletableFuture.failedFuture(e);
            }

            CompletableFuture<InputStream> future = new CompletableFuture<>();
            ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    TRANSFORM_POOL.execute(() -> {
                        try (JsonGenerator generator = JSON_FACTORY.createGenerator(pipedOut);
                             JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent())) {
                            ESItemQuery.copySource(parser, generator);
                            generator.flush();
                            pipedOut.close();
                            future.complete(pipedIn);
                        } catch (Exception e) {
                            ESItemQuery.closeQuietly(pipedOut, pipedIn);
                            future.completeExceptionally(new SearchException("Error: Elasticsearch timeout or no connection", e));
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    ESItemQuery.closeQuietly(pipedOut, pipedIn);
                    if (e instanceof ResponseException re) {
                        int status = re.getResponse().getStatusLine().getStatusCode();
                        if (status == 404) {
                            future.completeExceptionally(new NotFoundException(String.format("The item(id=%s) not found", id), e));
                            return;
                        }
                        if (status >= 400 && status < 500) {
                            future.completeExceptionally(new SearchException("Client error: " + status));
                            return;
                        }
                        // 5xx 或其他
                        future.completeExceptionally(new SearchException("Server error: " + status));
                    } else {
                        LOGGER.error("Elasticsearch request failed for id={}", id, e);
                        future.completeExceptionally(new SearchException("Network or connection failed", e));
                    }
                }
            });
            return future;
        }
     */
    private static void closeQuietly(PipedOutputStream out, PipedInputStream in) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
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
                    sink.tryEmitError(new IOException("Processing cancelled"));
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                         JsonGenerator generator = JSON_FACTORY.createGenerator(new JsonByteBufOutputStream(sink, isCancelled))) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        ESItemQuery.copySourceWithoutMeta(parser, generator);
                        generator.flush();
                    } catch (IOException | RuntimeException e) {
                        if (!isCancelled.get()) {
                            sink.tryEmitError(e);
                        }
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {
                    if (err != null && !isCancelled.get()) {
                        sink.tryEmitError(new SearchException("Transform failed", err));
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

    private static Throwable mapException(Exception e, long id) {
        if (e instanceof ResponseException) {
            int status = ((ResponseException) e).getResponse().getStatusLine().getStatusCode();
            if (status == 404) {
                return new NotFoundException("Item not found: " + id);
            } else if (status >= 400 && status < 500) {
                return new SearchException("Client error: " + status);
            } else {
                return new SearchException("Server error: " + status);
            }
        } else if (e instanceof IOException) {
            LOGGER.error("I/O error for id={}", id, e);
            return new SearchException("Network error", e);
        }
        return new SearchException("Unexpected error", e);
    }

    private static void copySourceWithoutMeta(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.getCurrentName())) {
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
    }

    @Override
    public InputStream findByBarcode(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode ctr");
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(this.writeFindByBarcodeJson(barcode));
        try {
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling barcode {} item ", barcode, e);
            throw new SearchException(String.format("No search was found for anything resembling barcode %s", barcode), e);
        }
    }

    @Override
    public Flux<ByteBuf> findByBarcodeAsync(String barcode) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode");
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(this.writeFindByBarcodeJson(barcode));

        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    sink.tryEmitError(new IOException("Processing cancelled"));
                    return;
                }

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

    private String writeFindByBarcodeJson(String barcode) {
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
            generator.writeEndObject();//end bool
            generator.writeEndObject();//end query
            //generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private static Throwable mapException(Exception e, String barcode) {
        if (e instanceof ResponseException) {
            int status = ((ResponseException) e).getResponse().getStatusLine().getStatusCode();
            if (status == 404) {
                return new NotFoundException("Item not found: " + barcode);
            } else if (status >= 400 && status < 500) {
                return new SearchException("Client error: " + status);
            } else {
                return new SearchException("Server error: " + status);
            }
        } else if (e instanceof IOException) {
            LOGGER.error("I/O error for id={}", barcode, e);
            return new SearchException("Network error", e);
        }
        return new SearchException("Unexpected error", e);
    }

    private void transformationAsync(Response response, CompletableFuture<InputStream> future, PipedOutputStream pipedOut, PipedInputStream pipedIn) {
        TRANSFORM_POOL.execute(() -> {
            try (JsonGenerator generator = JSON_FACTORY.createGenerator(pipedOut); JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent())) {
                generator.writeStartObject();
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME) {
                        String fieldName = parser.currentName();
                        if ("hits".equals(fieldName)) {
                            this.parseHits(parser, generator);
                        } else if ("aggregations".equals(fieldName)) {
                            generator.writeFieldName("aggregations");
                            int depth = 0;
                            do {
                                JsonToken token = parser.nextToken();
                                generator.copyCurrentEvent(parser);
                                if (token == JsonToken.START_OBJECT) depth++;
                                else if (token == JsonToken.END_OBJECT) depth--;
                            } while (depth > 0);
                        }
                    }
                }
                generator.writeEndObject();
                generator.flush();
                pipedOut.close();
                future.complete(pipedIn);
            } catch (Exception e) {
                closeQuietly(pipedOut, pipedIn);
                LOGGER.error("Failed to transform ES response", e);
                future.completeExceptionally(new SearchException("Failed to transform ES response", e));
            }
        });
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
        request.setJsonEntity(this.writeSearchJson(filters, size, searchAfter, sortField));
        try {
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("No search was found for anything resembling item ", e);
            throw new SearchException(String.format("No search was found for anything items from %s", 2), e);
        }
    }

    private String writeSearchJson(ItemQueryFilter[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            this.writeMain(generator, filters);
            this.writeSortField(generator, sortField);
            this.writeSearchAfter(generator, searchAfter);
            this.writeAggs(generator, AGGS_SIZE);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private void writeSearchAfter(JsonGenerator generator, String searchAfter) throws IOException {
        if (searchAfter == null || searchAfter.isBlank()) return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
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
        request.setJsonEntity(this.writeSearchJson(filters, offset, size, sortField));
        try {
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("No search was found for anything items ", e);
            throw new SearchException(String.format("No search was found for anything items from %s", 2), e);
        }
    }

    private String writeSearchJson(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", size);
            this.writeMain(generator, filters);
            this.writeSortField(generator, sortField);
            this.writeAggs(generator, AGGS_SIZE);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();//root
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        //System.out.println(writer);
        return writer.toString();
    }


    private void writeMain(JsonGenerator generator, ItemQueryFilter[] filters) throws IOException {
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

    private void writeSortField(JsonGenerator generator, SortFieldEnum sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }


    private void writeAggs(JsonGenerator generator, int aggsSize) throws IOException {
        generator.writeObjectFieldStart("aggs");

        generator.writeObjectFieldStart("brand_aggs");
        generator.writeObjectFieldStart("multi_terms");
        generator.writeNumberField("size", aggsSize);
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
        generator.writeNumberField("size", aggsSize);
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

    private InputStream reorganization(InputStream is) throws IOException {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean transferMark = false;
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os); JsonParser parser = JSON_FACTORY.createParser(is);) {
            generator.writeStartObject();
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    if ("hits".equals(fieldName)) {
                        this.parseHits(parser, generator);
                    } else if ("aggregations".equals(fieldName)) {
                        generator.writeFieldName("aggregations");
                        int depth = 0;
                        do {
                            JsonToken token = parser.nextToken();
                            generator.copyCurrentEvent(parser);
                            if (token == JsonToken.START_OBJECT) depth++;
                            else if (token == JsonToken.END_OBJECT) depth--;
                        } while (depth > 0);
                        /*
                        do {
                            generator.copyCurrentEvent(parser);
                            parser.nextToken();
                        } while (!(parser.currentToken() == JsonToken.END_OBJECT && "aggregations".equals(parser.currentName())));
                        generator.writeEndObject();
                         */
                    }
                }
            }

            generator.writeEndObject();
            generator.flush();
            ByteBufInputStream result = new ByteBufInputStream(buffer, true);// 创建输入流并转移所有权
            transferMark = true;
            return result;
        } finally {
            if (!transferMark && buffer != null && buffer.refCnt() > 0) {
                buffer.release(); // 只有在失败时才需要手动释放
            }
        }
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.END_OBJECT && "hits".equals(parser.currentName())) break;
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                if ("total".equals(fieldName)) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                } else if ("hits".equals(fieldName)) {
                    generator.writeArrayFieldStart("items");
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            generator.writeStartObject();
                            this.parserSource(parser, generator);
                            this.parserSort(parser, generator);
                            generator.writeEndObject();
                        }
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.currentName())) {
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
            if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName())) break;
                    generator.copyCurrentEvent(parser);
                }
            }
            if (parser.currentToken() == JsonToken.END_OBJECT && "_source".equals(parser.currentName())) break;
        }
    }

    private void parserSort(JsonParser parser, JsonGenerator generator) throws IOException {
        if (parser.nextToken() == JsonToken.FIELD_NAME && "sort".equals(parser.currentName())) {
            generator.copyCurrentEvent(parser);
            while (parser.nextToken() != null) {
                generator.copyCurrentEvent(parser);
                if (parser.currentToken() == JsonToken.END_ARRAY && "sort".equals(parser.currentName())) break;
            }
        }
    }


}
