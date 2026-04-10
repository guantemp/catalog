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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
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
    private static final String PREFIX = ESUtil.customized().isBlank() ? "/category" : "/" + ESUtil.customized() + "_category";
    private static final String SEARCH_ENDPOINT = PREFIX + "/_search";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();
    private static final int MAX_SIZE = 9999;
    private static final int SINGLE_BUFFER_SIZE = 2048; // 2KB缓冲区/单值
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public InputStream root() {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildRootRequest());

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
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
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildRootRequest());

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
                .doOnTerminate(() -> LOGGER.debug("Request terminated,check"))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildRootRequest() {
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
        Request request = new Request("GET", PREFIX + "/_doc/" + id);//PREFIX+"/_doc/"
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
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
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
                         OutputStream os = new JsonByteBufOutputStream(sink, isCancelled, 2048); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
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
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildChildrenRequest(id));

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
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
            LOGGER.warn("There are no related category(id={}) available ", id, e);
            throw new SearchException(String.format("There are no related category(id=%d) available", id), e);
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
    public Flux<ByteBuf> childrenAsync(long id) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildChildrenRequest(id));

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

    private static String buildChildrenRequest(long id) {
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
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public InputStream descendants(long id) {
        long familyId = -1;
        int left = 1, right = 1;
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent();
                 JsonParser parser = JSON_FACTORY.createParser(content)) {
                while (parser.nextToken() != null) {
                    JsonToken jsonToken = parser.currentToken();
                    if (JsonToken.FIELD_NAME == jsonToken) {
                        String fieldName = parser.currentName();
                        parser.nextToken();
                        switch (fieldName) {
                            case "root_id" -> familyId = parser.getValueAsLong();
                            case "left" -> left = parser.getValueAsInt();
                            case "right" -> right = parser.getValueAsInt();
                        }
                    }
                }
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());// 必须静默消费实体，防止连接泄漏
            }
        } catch (ResponseException e) {
            LOGGER.error("Failed to query category by id: {}, response exception", id, e);
            throw new IllegalStateException(String.format("Failed to query category by id: %d, response exception", id), e);
        } catch (IOException e) {
            LOGGER.error("IO exception when querying category by id: {}", id, e);
            throw new IllegalStateException(String.format("IO exception when querying category by id: %d", id), e);
        }

        request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildDescendantRequest(familyId, left, right));

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean success = false;
        Response response = null;
        try {
            response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent();
                 JsonParser parser = JSON_FACTORY.createParser(content);
                 OutputStream os = new ByteBufOutputStream(buffer);
                 JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {

                ESCategoryQuery.descendantToTree(parser, generator);
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            LOGGER.error("Failed to query category path by id: {}, rootId: {}", id, familyId, e);
            throw new SearchException(String.format("Failed to query category path for id: %d", id), e);
        } catch (IOException e) {
            LOGGER.error("IO exception when querying category path for id: {}", id, e);
            throw new SearchException(String.format("IO exception when querying category path for id: %d", id), e);
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
    public Flux<ByteBuf> descendantsAsync(long id) {
        CompletableFuture<Map<String, Object>> nodeInfoFuture = CompletableFuture.supplyAsync(() -> {
            Request request = new Request("GET", PREFIX + "/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());

            try {
                Response response = ESUtil.restClient().performRequest(request);
                try (InputStream content = response.getEntity().getContent();
                     JsonParser parser = JSON_FACTORY.createParser(content)) {

                    Map<String, Object> result = new HashMap<>();
                    while (parser.nextToken() != null) {
                        if (JsonToken.FIELD_NAME == parser.currentToken()) {
                            String fieldName = parser.currentName();
                            parser.nextToken();
                            switch (fieldName) {
                                case "root_id" -> result.put("familyId", parser.getValueAsLong());
                                case "left" -> result.put("left", parser.getValueAsInt());
                                case "right" -> result.put("right", parser.getValueAsInt());
                            }
                        }
                    }
                    return result;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        nodeInfoFuture.thenCompose(nodeInfo -> {
            long familyId = (Long) nodeInfo.get("familyId");
            int left = (Integer) nodeInfo.get("left");
            int right = (Integer) nodeInfo.get("right");
            return null;
        });
        return null;
    }

    private static String buildDescendantRequest(long rootId, int left, int right) {
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
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    /**
     * 将 ElasticSearch 嵌套集模型（left/right）扁平化结果 转换为 标准树形JSON结构
     * <p>
     * 【功能说明】：
     * 读取 ES 查询返回的 hits 结果，根据 _source 中的 left / right 嵌套集数值，
     * 自动构建层级树结构，输出根节点为对象的标准树形 JSON。
     * <p>
     * 【输出结构】：
     * <pre>
     * {
     *   "total": 43,
     *   "category": {
     *     "id": 1,
     *     "name": { ... },
     *     "left": 1,
     *     "right": 86,
     *     "children": [ ... ]
     *   }
     * }
     * </pre>
     *
     * @param parser   Jackson JsonParser，用于读取 ES 原始 JSON 数据流
     * @param generator Jackson JsonGenerator，用于输出标准树形 JSON 结构
     * @throws IOException 当 JSON 解析、生成、IO 异常时抛出
     * @throws IllegalStateException 当 ES 数据结构不符合预期（非对象/非数组）时抛出
     */
    private static void descendantToTree(JsonParser parser, JsonGenerator generator) throws IOException {
        Deque<Integer> rightValueStack = new ArrayDeque<>();
        generator.writeStartObject();//start
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                if ("hits".equals(name)) {//first hits
                    parser.nextToken(); // should be START_OBJECT
                    if (parser.currentToken() != JsonToken.START_OBJECT) {
                        throw new IllegalStateException("Hits' must be an object");
                    }
                    while (parser.nextToken() != JsonToken.END_OBJECT) {//loop first hits
                        if (parser.currentToken() != JsonToken.FIELD_NAME) continue;
                        String hitsField = parser.currentName();
                        if ("total".equals(hitsField)) {//loop total
                            parser.nextToken();
                            if (parser.currentToken() != JsonToken.START_OBJECT) {
                                throw new IllegalStateException("Total must be an object");
                            }
                            while (parser.nextToken() != JsonToken.END_OBJECT) { // Extract only "value"
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                                    parser.nextToken();
                                    generator.writeNumberField("total", parser.getValueAsLong());
                                } else {
                                    //parser.nextToken();
                                    parser.skipChildren();
                                }
                            }
                        } else if ("hits".equals(hitsField)) {//hits.hits
                            parser.nextToken(); // should be START_ARRAY
                            if (parser.currentToken() != JsonToken.START_ARRAY) {
                                throw new IllegalStateException("'hits.hits' must be an array");
                            }
                            generator.writeObjectFieldStart("category");//不管里面有没有，先写个
                            boolean first = true;
                            while (parser.nextToken() != JsonToken.END_ARRAY) {//loop hits array
                                if (parser.getCurrentToken() != JsonToken.START_OBJECT) {//not start blank start object
                                    parser.skipChildren();
                                    continue;
                                }
                                int left = 1, right = 1;
                                while (parser.nextToken() != JsonToken.END_OBJECT) {//loop { 开始在hits下面的{}中循环,包含_source,_index啥的
                                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.currentName())) {// enter _source
                                        parser.nextToken();
                                        if (parser.currentToken() != JsonToken.START_OBJECT) {
                                            parser.skipChildren();
                                            continue;
                                        }
                                        if (!first)//第一个,上面写了category:{,不写了,后面需要写｛
                                            generator.writeStartObject();
                                        while (parser.nextToken() != JsonToken.END_OBJECT) {//loop source
                                            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                                String srcField = parser.currentName();
                                                if ("_meta".equals(srcField)) {
                                                    //parser.nextToken();
                                                    parser.skipChildren();
                                                } else {
                                                    generator.writeFieldName(srcField);
                                                    parser.nextToken();
                                                    switch (srcField) {//这个位置必须固定在这里
                                                        case "left" -> left = parser.getValueAsInt();
                                                        case "right" -> right = parser.getValueAsInt();
                                                    }
                                                    //System.out.println(parser.currentToken()+":"+parser.currentName()+":"+first);
                                                    //System.out.println(right + ":" + left + ":" + (right - left));
                                                    generator.copyCurrentStructure(parser);
                                                }
                                            }
                                        }//end loop source
                                    } else {
                                        //parser.nextToken();
                                        parser.skipChildren(); // skip _id, _index, sort, _score, etc.
                                    }
                                }//end loop {
                                first = false;//第一 category 本体source结束了,下面直接写开始{
                                if (right - left == 1) {//叶子,闭合
                                    generator.writeEndObject();
                                }
                                if (right - left > 1) {//有儿子
                                    generator.writeArrayFieldStart("children");
                                    rightValueStack.push(right);
                                }
                                while (!rightValueStack.isEmpty() && rightValueStack.peek() - right == 1) {
                                    generator.writeEndArray();//end children array
                                    generator.writeEndObject();//每个end children array后面就结束父对象
                                    right = rightValueStack.pop();
                                }
                            }//end loop hits array
                        } else {//skip warp hits
                            //parser.nextToken();
                            parser.skipChildren(); // skip max_score, etc.
                        }
                    }
                } else {//skip not wrap hits
                    //parser.nextToken();
                    parser.skipChildren(); // skip max_score, etc.
                }
            }
        }
        while (!rightValueStack.isEmpty()) {
            generator.writeEndArray();
            generator.writeEndObject();
            rightValueStack.pop();
        }
        generator.writeEndObject();//end
        generator.flush();
    }

    @Override
    public InputStream search(String key, int offset, int size) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildKeyRequest(key, offset, size));

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
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
            LOGGER.error("No search was found for anything resembling key {} category ", key, e);
            throw new SearchException(String.format("No search was found for anything resembling key %s category", key), e);
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
    public Flux<ByteBuf> searchAsync(String key, int offset, int size) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");

        Request request = new Request("GET", SEARCH_ENDPOINT);
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
        long rootId = -1;
        int left = 1, right = 1;
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent();
                 JsonParser parser = JSON_FACTORY.createParser(content)) {
                while (parser.nextToken() != null) {
                    JsonToken jsonToken = parser.currentToken();
                    if (JsonToken.FIELD_NAME == jsonToken) {
                        String fieldName = parser.currentName();
                        parser.nextToken();
                        switch (fieldName) {
                            case "root_id" -> rootId = parser.getValueAsLong();
                            case "left" -> left = parser.getValueAsInt();
                            case "right" -> right = parser.getValueAsInt();
                        }
                    }
                }
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());// 必须静默消费实体，防止连接泄漏
            }
        } catch (ResponseException e) {
            LOGGER.error("Failed to query category by id: {}, response exception", id, e);
            throw new IllegalStateException(String.format("Failed to query category by id: %d, response exception", id), e);
        } catch (IOException e) {
            LOGGER.error("IO exception when querying category by id: {}", id, e);
            throw new IllegalStateException(String.format("IO exception when querying category by id: %d", id), e);
        }

        request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildPathRequest(rootId, left, right));

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean success = false;
        Response response = null;
        try {
            response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent();
                 JsonParser parser = JSON_FACTORY.createParser(content);
                 OutputStream os = new ByteBufOutputStream(buffer);
                 JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {

                Extract.extractWithoutAggs(parser, generator, "categories");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            LOGGER.error("Failed to query category path by id: {}, rootId: {}", id, rootId, e);
            throw new SearchException(String.format("Failed to query category path for id: %d", id), e);
        } catch (IOException e) {
            LOGGER.error("IO exception when querying category path for id: {}", id, e);
            throw new SearchException(String.format("IO exception when querying category path for id: %d", id), e);
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
    public Flux<ByteBuf> pathAsync(long id) {
        long rootId = -1;
        int left = 1, right = 1;

        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent();
                 JsonParser parser = JSON_FACTORY.createParser(content)) {
                while (parser.nextToken() != null) {
                    JsonToken jsonToken = parser.currentToken();
                    if (JsonToken.FIELD_NAME == jsonToken) {
                        String fieldName = parser.currentName();
                        parser.nextToken();
                        switch (fieldName) {
                            case "root_id" -> rootId = parser.getValueAsLong();
                            case "left" -> left = parser.getValueAsInt();
                            case "right" -> right = parser.getValueAsInt();
                        }
                    }
                }
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());// 必须静默消费实体，防止连接泄漏
            }
        } catch (ResponseException e) {
            LOGGER.error("Failed to query category by id: {}, response exception", id, e);
            throw new IllegalStateException(String.format("Failed to query category by id: %d, response exception", id), e);
        } catch (IOException e) {
            LOGGER.error("IO exception when querying category by id: {}", id, e);
            throw new IllegalStateException(String.format("IO exception when querying category by id: %d", id), e);
        }

        request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildPathRequest(rootId, left, right));
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
                        MapException.mapExceptionAndEmit(sink, err, isCancelled, id);
                    } else {
                        sink.tryEmitComplete().orThrow();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(e, id));
                }
            }
        });

        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated"))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static String buildPathRequest(long rootId, int left, int right) {
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
}
