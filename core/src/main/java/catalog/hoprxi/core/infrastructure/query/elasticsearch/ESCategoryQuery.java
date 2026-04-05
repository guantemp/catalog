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


import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.SearchException;
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
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/18
 */

public class ESCategoryQuery implements CategoryQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final String SINGLE_PREFIX = "/" + ESUtil.customized() + "_category";
    private static final String SEARCH_ENDPOINT = SINGLE_PREFIX + "/_search";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();

    private static final int MAX_SIZE = 9999;
    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public InputStream root() {
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(buildRootJsonRequest());

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        Response response = null;
        try {

            response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                 OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                Extract.extractWithoutAggs(parser, generator, "categories");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling root categories", e);
            throw new SearchException("No search was found for anything resembling root categories", e);
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
    public Flux<ByteBuf> rootAsync() {
        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(buildRootJsonRequest());

        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
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
                        Extract.extractWithoutAggs(parser, generator, "categories");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {
                    if (err != null) {
                        MapException.mapExceptionAndEmit(sink, err, isCancelled, "");
                    } else {
                        sink.tryEmitComplete().orThrow();
                    }
                });
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(exception, ""));
                }
            }
        });
        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated"))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildRootJsonRequest() {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("script");
            generator.writeObjectFieldStart("script");
            generator.writeStringField("lang", "painless");
            generator.writeStringField("source", "doc['id'].value == doc['parent_id'].value");
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        return writer.toString();
    }

    @Override
    public InputStream find(long id) {
        Request request = new Request("GET", "/category/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            Response response = ESUtil.restClient().performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            Extract.extractSourceSkipMeta(parser, generator);
            return new ByteBufInputStream(buffer, true);
        } catch (ResponseException e) {
            if (buffer.refCnt() > 0) buffer.release();
            LOGGER.error("The category(id={}) not found", id, e);
            throw new SearchException(String.format("The category(id=%s) not found", id), e);
        } catch (IOException e) {
            if (buffer.refCnt() > 0) buffer.release();
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        }
    }

    @Override
    public Flux<ByteBuf> findAsync(long id) {
        Request request = new Request("GET", "/category/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
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
    public InputStream children(long id) {
        try {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.writeChildrenJsonEntity(id));
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("There are no related category(id={}) available ", id, e);
            throw new SearchException(String.format("There are no related category(id=%d) available", id), e);
        }
    }

    private String writeChildrenJsonEntity(long id) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            // 写入 must 数组
            generator.writeArrayFieldStart("must");
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("parent_id", id);
            generator.writeEndObject(); // term
            generator.writeEndObject(); // must 中的对象
            generator.writeEndArray(); // must
            // 写入 must_not 数组
            generator.writeArrayFieldStart("must_not");
            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("id", id);
            generator.writeEndObject(); // term
            generator.writeEndObject(); // must_not 中的对象
            generator.writeEndArray(); // must_not
            generator.writeEndObject(); // bool
            generator.writeEndObject(); // query

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "asc");
            generator.writeEndObject();
            generator.writeEndArray();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public InputStream descendants(long id) {
        long rootId = -1;
        int left = 1, right = 1;
        Request request = new Request("GET", "/category/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        try {
            Response response = ESUtil.restClient().performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "root_id" -> rootId = parser.getValueAsLong();
                        case "left" -> left = parser.getValueAsInt();
                        case "right" -> right = parser.getValueAsInt();
                    }
                }
            }
            request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.writeDescendantJsonEntity(rootId, left, right));
            response = ESUtil.restClient().performRequest(request);
            return this.descendantToTree(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("There are no related category(id={}) available", id, e);
            throw new SearchException(String.format("There are no related category(id=%d) available", id), e);
        }
    }

    private String writeDescendantJsonEntity(long rootId, int left, int right) {
        StringWriter writer = new StringWriter(256);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("root_id", rootId);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("left");
            generator.writeNumberField("gte", left);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("right");
            generator.writeNumberField("lte", right);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();//must
            generator.writeEndObject();//bool
            generator.writeEndObject();//query

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("left", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private InputStream descendantToTree(InputStream is) throws IOException {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean transferMark = false;
        Stack<Integer> stack = new Stack<>();
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonParser parser = JSON_FACTORY.createParser(is); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            generator.writeStartObject();//start
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "total".equals(parser.currentName())) {//all number
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                }
                if (parser.currentToken() == JsonToken.START_ARRAY && "hits".equals(parser.currentName())) {
                    generator.writeObjectFieldStart("categories");
                    boolean first = true;
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.currentName()))
                            break;
                        int currentLeft = 1, currentRight = 1;
                        if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {//root source
                            if (!first)//第一次已写categories:{,不需要开始{
                                generator.writeStartObject();
                            while (parser.nextToken() != null) {
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName()))
                                    break;//end _meta
                                generator.copyCurrentEvent(parser);
                                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                    String fileName = parser.currentName();
                                    parser.nextToken();
                                    generator.copyCurrentEvent(parser);
                                    switch (fileName) {
                                        case "left" -> currentLeft = parser.getValueAsInt();
                                        case "right" -> currentRight = parser.getValueAsInt();
                                    }
                                }
                            }
                            first = false;//第一 categories 本体结束了,下面需要写开始{
                        }//end source
                        if (currentRight - currentLeft == 1) {//叶子
                            generator.writeEndObject();
                        }
                        if (currentRight - currentLeft > 1) {
                            generator.writeArrayFieldStart("children");
                            stack.push(currentRight);
                        }
                        while (!stack.isEmpty() && stack.peek() - currentRight == 1) {//end children array
                            generator.writeEndArray();
                            generator.writeEndObject();
                            currentRight = stack.pop();
                        }
                    }//end hits
                    while (!stack.isEmpty()) {
                        generator.writeEndArray();
                        generator.writeEndObject();
                        stack.pop();
                    }
                }
            }
            generator.writeEndObject();//end
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

    @Override
    public InputStream search(String key, int offset, int size) {
        try {
            Request request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(buildKeyRequest(key, offset, size));
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.debug("No search was found for anything resembling key {} category ", key, e);
            throw new SearchException(String.format("No search was found for anything resembling key %s category", key), e);
        }
    }

    @Override
    public Flux<ByteBuf> searchAsync(String key, int offset, int size) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");

        Request request = new Request("GET", "/category/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(buildKeyRequest(key, offset, size));

        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
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
                        Extract.extractWithoutAggs(parser, generator, "categories");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {
                    if (err != null) {
                        MapException.mapExceptionAndEmit(sink, err, isCancelled, key);
                    } else {
                        sink.tryEmitComplete().orThrow();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(e, key));
                }
            }
        });

        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated"))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildKeyRequest(String key, int offset, int limit) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", limit);
            generator.writeObjectFieldStart("query");
            if (key == null || key.isEmpty()) {
                generator.writeObjectFieldStart("match_all");
                generator.writeEndObject();//match_all
            } else {
                generator.writeObjectFieldStart("bool");
                generator.writeObjectFieldStart("filter");
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

                generator.writeEndArray();//end should
                generator.writeEndObject();//end bool
                generator.writeEndObject();
                generator.writeEndObject();//end bool
            }
            generator.writeEndObject();//end query

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Can't assemble name request json", e);
            throw new IllegalStateException("Can't assemble name request json", e);
        }
        return writer.toString();
    }

    @Override
    public InputStream searchSiblings(long id) {
        return null;
    }

    @Override
    public InputStream path(long id) {
        try {
            long rootId = -1;
            int left = 1, right = 1;
            Request request = new Request("GET", "/category/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());
            Response response = ESUtil.restClient().performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (JsonToken.FIELD_NAME == jsonToken) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "root_id" -> rootId = parser.getValueAsInt();
                        case "left" -> left = parser.getValueAsInt();
                        case "right" -> right = parser.getValueAsInt();
                    }
                }
            }
            request = new Request("GET", "/category/_search");
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(this.buildPathRequest(rootId, left, right));
            response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (ResponseException e) {
            LOGGER.warn("The category(id={}) not found", id, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new RuntimeException(e);
        }
    }

    private String buildPathRequest(long rootId, int left, int right) {
        StringWriter writer = new StringWriter(256);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("root_id", rootId);
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("left");
            generator.writeNumberField("lte", left);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("right");
            generator.writeNumberField("gte", right);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("left", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private InputStream reorganization(InputStream is) throws IOException {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean transferMark = false;
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os); JsonParser parser = JSON_FACTORY.createParser(is);) {
            generator.writeStartObject();
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "hits".equals(parser.currentName())) {
                    this.parseHits(parser, generator);
                    break;
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
                    generator.writeArrayFieldStart("categories");
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
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName())) { //filter _meta
                        break;
                    }
                    generator.copyCurrentEvent(parser);
                }
            }
            if (parser.currentToken() == JsonToken.END_OBJECT && "_source".equals(parser.currentName()))
                break;
        }
    }

    private void parserSort(JsonParser parser, JsonGenerator generator) throws IOException {
        if (parser.nextToken() == JsonToken.FIELD_NAME && "sort".equals(parser.currentName())) {
            generator.copyCurrentEvent(parser);
            while (parser.nextToken() != null) {
                generator.copyCurrentEvent(parser);
                if (parser.currentToken() == JsonToken.END_ARRAY && "sort".equals(parser.currentName()))
                    break;
            }
        }
    }
}
