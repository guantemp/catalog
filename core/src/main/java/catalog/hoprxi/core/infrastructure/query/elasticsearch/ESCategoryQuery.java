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
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
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
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().disable(JsonFactory.Feature.INTERN_FIELD_NAMES).disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES).build();
    private static final int MAX_SIZE = 9999;
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public InputStream root() {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildRootRequest());

        return ESCategoryQuery.byteBufInputStream("root", request);
    }

    @Override
    public Flux<ByteBuf> rootAsync() {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildRootRequest());

        return ESCategoryQuery.byteBufFlux("root", request);
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
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());

        return ESCategoryQuery.byteBufInputStream(String.valueOf(id), request, true);
    }

    @Override
    public Flux<ByteBuf> findAsync(long id) {
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());

        return ESCategoryQuery.byteBufFlux(String.valueOf(id), request, true);
    }

    @Override
    public InputStream children(long id) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildChildrenRequest(id));

        return ESCategoryQuery.byteBufInputStream(String.valueOf(id), request);
    }

    @Override
    public Flux<ByteBuf> childrenAsync(long id) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildChildrenRequest(id));

        return ESCategoryQuery.byteBufFlux(String.valueOf(id), request);
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
            try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content)) {
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
            try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content); OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {

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
        return Mono.defer(() -> {
            Sinks.One<Map<String, Object>> sink = Sinks.one();
            AtomicBoolean isCancelled = new AtomicBoolean(false);
            Request request = new Request("GET", PREFIX + "/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());

            Cancellable cancellable = ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    // 取消标记，和你第二步的逻辑一模一样
                    if (isCancelled.get()) {
                        EntityUtils.consumeQuietly(response.getEntity());
                        sink.tryEmitEmpty();
                        return;
                    }

                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content)) {
                        // 解析完，把结果塞给Mono，和你sink.tryEmitNext 一样
                        Map<String, Object> nodeInfo = new HashMap<>();
                        while (parser.nextToken() != null) {
                            JsonToken jsonToken = parser.currentToken();
                            if (JsonToken.FIELD_NAME == jsonToken) {
                                String fieldName = parser.currentName();
                                parser.nextToken();
                                switch (fieldName) {
                                    case "root_id" -> nodeInfo.put("familyId", parser.getValueAsLong());
                                    case "left" -> nodeInfo.put("left", parser.getValueAsInt());
                                    case "right" -> nodeInfo.put("right", parser.getValueAsInt());
                                }
                            }
                        }
                        sink.tryEmitValue(nodeInfo);
                    } catch (IOException e) {
                        sink.tryEmitError(new UncheckedIOException(e));
                    } finally {
                        // 你原来的finally，静默消费实体，防止连接泄漏，完全一样
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    // 异常处理，和你第二步的逻辑一模一样
                    if (isCancelled.get()) return;
                    LOGGER.error("Failed to query category by id: {}, response exception", id, exception);
                    sink.tryEmitError(MapException.mapException(exception, id));
                }
            });

            return sink.asMono().doOnCancel(() -> {
                isCancelled.set(true);
                cancellable.cancel();
            });
        }).cast(Map.class).flatMapMany(node -> {
            long familyId = (long) node.get("familyId");
            int left = (int) node.get("left");
            int right = (int) node.get("right");
            Request request = new Request("GET", SEARCH_ENDPOINT);
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(ESCategoryQuery.buildDescendantRequest(familyId, left, right));

            return ESCategoryQuery.byteBufFlux(String.valueOf(id), request);
        });
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
     * @param parser    Jackson JsonParser，用于读取 ES 原始 JSON 数据流
     * @param generator Jackson JsonGenerator，用于输出标准树形 JSON 结构
     * @throws IOException           当 JSON 解析、生成、IO 异常时抛出
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
                            parser.skipChildren(); // skip max_score, etc.
                        }
                    }
                } else {//skip not wrap hits
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
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildKeyRequest(key, offset, size));

        return ESCategoryQuery.byteBufInputStream(key, request);
    }

    @Override
    public Flux<ByteBuf> searchAsync(String key, int offset, int size) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(buildKeyRequest(key, offset, size));

        return ESCategoryQuery.byteBufFlux(key, request);
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
            try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content)) {
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
            try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content); OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {

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
        return Mono.defer(() -> {
            Sinks.One<Map<String, Object>> sink = Sinks.one();
            AtomicBoolean isCancelled = new AtomicBoolean(false);
            Request request = new Request("GET", PREFIX + "/_doc/" + id);
            request.setOptions(ESUtil.requestOptions());

            try {
                Cancellable cancellable = ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        // 取消标记，和你第二步的逻辑一模一样
                        if (isCancelled.get()) {
                            EntityUtils.consumeQuietly(response.getEntity());
                            sink.tryEmitEmpty();
                            return;
                        }

                        try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content)) {
                            // 解析完，把结果塞给Mono，和你sink.tryEmitNext 一样
                            Map<String, Object> nodeInfo = new HashMap<>();
                            while (parser.nextToken() != null) {
                                JsonToken jsonToken = parser.currentToken();
                                if (JsonToken.FIELD_NAME == jsonToken) {
                                    String fieldName = parser.currentName();
                                    parser.nextToken();
                                    switch (fieldName) {
                                        case "root_id" -> nodeInfo.put("familyId", parser.getValueAsLong());
                                        case "left" -> nodeInfo.put("left", parser.getValueAsInt());
                                        case "right" -> nodeInfo.put("right", parser.getValueAsInt());
                                    }
                                }
                            }
                            sink.tryEmitValue(nodeInfo);
                        } catch (IOException e) {
                            sink.tryEmitError(new UncheckedIOException(e));
                        } finally {
                            // 你原来的finally，静默消费实体，防止连接泄漏，完全一样
                            EntityUtils.consumeQuietly(response.getEntity());
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // 异常处理，和你第二步的逻辑一模一样
                        if (isCancelled.get()) return;
                        LOGGER.error("Failed to query category by id: {}, response exception", id, exception);
                        sink.tryEmitError(MapException.mapException(exception, id));
                    }
                });
                return sink.asMono().doOnCancel(() -> {
                    isCancelled.set(true);
                    cancellable.cancel();
                });

            } catch (Exception e) {
                return Mono.error(e);
            }
        }).cast(Map.class).flatMapMany(node -> {
            long familyId = (long) node.get("familyId");
            int left = (int) node.get("left");
            int right = (int) node.get("right");
            Request request = new Request("GET", SEARCH_ENDPOINT);
            request.setOptions(ESUtil.requestOptions());
            request.setJsonEntity(ESCategoryQuery.buildPathRequest(familyId, left, right));

            return ESCategoryQuery.byteBufFlux(String.valueOf(id), request);
        });
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

    /**
     * 同步执行 Elasticsearch 请求并返回包含分类数据的输入流
     * <p>
     * 此方法执行同步请求，将响应中的 "categories" 字段提取到 ByteBuf 中，
     * 并返回一个可读取该缓冲区的输入流。方法内部处理资源管理和异常处理。
     * </p>
     *
     * @param tips    用于日志记录和错误消息的关键字标识符
     * @param request 要执行的 Elasticsearch 请求对象
     * @return 包含提取后的分类数据的输入流，调用方负责关闭流
     * @throws SearchException 当请求失败或无法找到匹配的数据时抛出
     */
    private static ByteBufInputStream byteBufInputStream(String tips, Request request, boolean alone) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean success = false;
        Response response = null;
        try {
            response = ESUtil.restClient().performRequest(request);
            try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content); OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                if (alone) Extract.extractSourceSkipMeta(parser, generator);
                else Extract.extractWithoutAggs(parser, generator, "categories");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (IOException e) {
            LOGGER.error("No search was found for anything resembling key {} category ", tips, e);
            throw new SearchException(String.format("No search was found for anything resembling key %s category", tips), e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (!success && buffer.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    private static ByteBufInputStream byteBufInputStream(String tips, Request request) {
        return ESCategoryQuery.byteBufInputStream(tips, request, false);
    }

    /**
     * 异步执行 Elasticsearch 请求并返回包含分类数据的响应式流
     * <p>
     * 此方法执行异步请求，将响应中的 "categories" 字段逐块转换为 ByteBuf 流。
     * 支持取消操作、超时控制和资源自动释放。
     * </p>
     *
     * @param tips    用于日志记录和错误消息的提示标识符
     * @param request 要执行的 Elasticsearch 请求对象
     * @return 包含提取后的分类数据的响应式流，每个元素是一个 ByteBuf
     * 流会在 15 秒后自动超时，超时时抛出 {@link TimeoutException}
     * @implNote 内部使用单播接收器以提高性能，支持背压缓冲
     * 自动处理取消操作和资源释放
     */
    private static Flux<ByteBuf> byteBufFlux(String tips, Request request, boolean alone) {
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
                    try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content); OutputStream os = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                        if (isCancelled.get()) {
                            return; // silent cancel; sink 已由外部处理或无需响应
                        }
                        if (alone) Extract.extractSourceSkipMeta(parser, generator);
                        else Extract.extractWithoutAggs(parser, generator, "categories");
                    } catch (IOException e) {

                        throw new UncheckedIOException(e);
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }, TRANSFORM_POOL).whenComplete((v, err) -> {

                    if (err != null) {
                        MapException.mapExceptionAndEmit(sink, err, isCancelled, tips);
                    } else {
                        sink.tryEmitComplete().orThrow();
                    }
                });
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(exception, tips));
                }
            }
        });

        return sink.asFlux().timeout(Duration.ofSeconds(15), Mono.error(new TimeoutException("Request timed out for : " + tips))).doOnCancel(() -> isCancelled.set(true)).doOnTerminate(() -> LOGGER.debug("Request terminated for {}", tips)).doOnDiscard(ByteBuf.class, ByteBuf::release);
    }

    private static Flux<ByteBuf> byteBufFlux(String tips, Request request) {
        return ESCategoryQuery.byteBufFlux(tips, request, false);
    }
}
