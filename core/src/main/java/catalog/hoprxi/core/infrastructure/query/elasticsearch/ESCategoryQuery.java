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


import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.*;
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
    private static final int MAX_SIZE = 999;

    private static String buildRootRequest() {
        try (StringWriter writer = new StringWriter(); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("runtime_mappings");
            generator.writeObjectFieldStart("is_self_parent");
            generator.writeStringField("type", "boolean");
            generator.writeStringField("script", "emit(doc['id'].value == doc['parent_id'].value)");
            generator.writeEndObject();//is_self_parent
            generator.writeEndObject();//end runtime

            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("term");
            generator.writeBooleanField("is_self_parent", true);
            generator.writeEndObject();//term
            generator.writeEndObject();//end query

            generator.writeObjectFieldStart("sort");
            generator.writeStringField("id", "asc");
            generator.writeEndObject();

            generator.writeEndObject();//end root
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
    }

    private static String buildChildrenRequest(long id) {
        try (StringWriter writer = new StringWriter(128); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            // 写入 must 数组
            generator.writeArrayFieldStart("filter");
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
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
    }

    /**
     * 从 Elasticsearch 响应流中提取树形结构数据，并将结果以 JSON 格式输出。
     * <p>
     * 此方法假定输入 JSON 流包含 Elasticsearch 的标准搜索响应（带有 {@code hits.hits} 数组），
     * 其中每个命中的 {@code _source} 必须包含 {@code left} 和 {@code right} 整数字段，用于表示嵌套集（Nested Set）模型。
     * 方法会遍历所有命中，根据 {@code left}/{@code right} 值动态重组树形层次结构，生成嵌套的 JSON 对象，
     * 并在根节点下以指定的 {@code title} 作为字段名输出该树。
     * </p>
     * <p>
     * 输出 JSON 结构示例（假设 title = "category"）：
     * <pre>
     * {
     *   "category": {
     *     "name": "root",
     *     "left": 1,
     *     "right": 10,
     *     "children": [
     *       { "name": "child1", "left": 2, "right": 3 },
     *       { "name": "child2", "left": 4, "right": 9, "children": [...] }
     *     ]
     *   }
     * }
     * </pre>
     * </p>
     *
     * @param parser Jackson JSON 解析器，已指向 Elasticsearch 响应的起始位置
     * @param gen    Jackson JSON 生成器，用于输出重组后的树形 JSON
     * @param title  输出树形结构的根字段名称，该字段的值即为整个树对象；不可为 {@code null} 或空白
     * @throws IOException           如果解析或生成过程中发生 I/O 错误
     * @throws IllegalStateException 如果输入 JSON 结构不符合预期（例如缺少 {@code hits.hits} 数组，
     *                               或 {@code _source} 中缺少 {@code left}/{@code right} 字段）
     */
    private static void extractAsTree(JsonParser parser, JsonGenerator gen, String title) throws IOException {
        Deque<Integer> rightValueStack = new ArrayDeque<>();
        gen.writeStartObject();//start
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
                                    gen.writeNumberField("total", parser.getValueAsLong());
                                } else {
                                    parser.skipChildren();
                                }
                            }
                        } else if ("hits".equals(hitsField)) {//hits.hits
                            parser.nextToken(); // should be START_ARRAY
                            if (parser.currentToken() != JsonToken.START_ARRAY) {
                                throw new IllegalStateException("'hits.hits' must be an array");
                            }
                            gen.writeObjectFieldStart(title);//不管里面有没有，先写个
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
                                        if (!first)//第一个,上面写了title:{,不写了,后面需要写｛
                                            gen.writeStartObject();
                                        while (parser.nextToken() != JsonToken.END_OBJECT) {//loop source
                                            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                                String srcField = parser.currentName();
                                                if ("_meta".equals(srcField)) {
                                                    //parser.nextToken();
                                                    parser.skipChildren();
                                                } else {
                                                    gen.writeFieldName(srcField);
                                                    parser.nextToken();
                                                    switch (srcField) {//这个位置必须固定在这里
                                                        case "left" -> left = parser.getValueAsInt();
                                                        case "right" -> right = parser.getValueAsInt();
                                                    }
                                                    //System.out.println(parser.currentToken()+":"+parser.currentName()+":"+first);
                                                    //System.out.println(right + ":" + left + ":" + (right - left));
                                                    gen.copyCurrentStructure(parser);
                                                }
                                            }
                                        }//end loop source
                                    } else {
                                        parser.skipChildren(); // skip _id, _index, sort, _score, etc.
                                    }
                                }//end loop {
                                first = false;//第一 category 本体source结束了,下面直接写开始{
                                if (right - left == 1) {//叶子,闭合
                                    gen.writeEndObject();
                                }
                                if (right - left > 1) {//有儿子
                                    gen.writeArrayFieldStart("children");
                                    rightValueStack.push(right);
                                }
                                while (!rightValueStack.isEmpty() && rightValueStack.peek() - right == 1) {
                                    gen.writeEndArray();//end children array
                                    gen.writeEndObject();//每个end children array后面就结束父对象
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
            gen.writeEndArray();
            gen.writeEndObject();
            rightValueStack.pop();
        }
        gen.writeEndObject();//end
        gen.close();
    }

    private static String buildDescendantRequest(long familyId, int left, int right) {
        try (StringWriter writer = new StringWriter(256); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("filter");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("family_id", familyId);
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
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
    }

    private static String buildKeyRequest(String key, int offset, int limit) {
        try (StringWriter writer = new StringWriter(128); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", limit);
            generator.writeObjectFieldStart("query");
            if (key == null || key.isEmpty()) {
                generator.writeObjectFieldStart("match_all");
                generator.writeEndObject();//match_all
            } else {
                generator.writeObjectFieldStart("constant_score");
                generator.writeObjectFieldStart("filter");
                generator.writeObjectFieldStart("multi_match");
                generator.writeStringField("query", key);
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
            generator.writeStartObject();
            generator.writeStringField("id", "asc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Can't assemble name request json", e);
            throw new IllegalStateException("Can't assemble name request json", e);
        }
    }

    private static String buildPathRequest(long familyId, int left, int right) {
        try (StringWriter writer = new StringWriter(256); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", MAX_SIZE);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("filter");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeNumberField("family_id", familyId);
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
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
    }

    @Override
    public InputStream root() {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildRootRequest());

        return ReactiveStream.toByteBufInputStream(request, "categories", "root");
    }

    @Override
    public Flux<ByteBuf> rootAsync() {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildRootRequest());

        return ReactiveStream.toFluxByteBuf(request, "categories", "root");
    }

    @Override
    public InputStream find(long id) {
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
    public InputStream children(long id) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildChildrenRequest(id));

        return ReactiveStream.toSingleByteBufInputStream(request, String.valueOf(id));
    }

    @Override
    public Flux<ByteBuf> childrenAsync(long id) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildChildrenRequest(id));

        return ReactiveStream.toFluxByteBuf(request, "categories", String.valueOf(id));
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
                            case "family_id" -> familyId = parser.getValueAsLong();
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

        return ReactiveStream.toByteBufInputStream(request, String.valueOf(id),
                (parser, generator) -> ESCategoryQuery.extractAsTree(parser, generator, "category"));
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
                                    case "family_id" -> nodeInfo.put("familyId", parser.getValueAsLong());
                                    case "left" -> nodeInfo.put("left", parser.getValueAsInt());
                                    case "right" -> nodeInfo.put("right", parser.getValueAsInt());
                                }
                            }
                        }
                        sink.tryEmitValue(nodeInfo);
                    } catch (IOException e) {
                        sink.tryEmitError(new UncheckedIOException(e));
                    } finally {
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

            return ReactiveStream.toFluxByteBuf(request, String.valueOf(id),
                    (parser, generator) -> ESCategoryQuery.extractAsTree(parser, generator, "category"));
        });
    }

    @Override
    public InputStream search(String key, int offset, int size) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildKeyRequest(key, offset, size));

        return ReactiveStream.toByteBufInputStream(request, "categories", key);
    }

    @Override
    public Flux<ByteBuf> searchAsync(String key, int offset, int size) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Offset value range is 0-10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("Size value range is 0-10000");
        if (size + offset > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESCategoryQuery.buildKeyRequest(key, offset, size));

        return ReactiveStream.toFluxByteBuf(request, "categories", key);
    }

    @Override
    public InputStream siblings(long id) {
        return null;
    }

    @Override
    public InputStream path(long id) {
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
                            case "family_id" -> familyId = parser.getValueAsLong();
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
        request.setJsonEntity(ESCategoryQuery.buildPathRequest(familyId, left, right));

        return ReactiveStream.toByteBufInputStream(request, "categories", String.valueOf(id));
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
                                                case "family_id" -> nodeInfo.put("familyId", parser.getValueAsLong());
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

                                // 检查是否为 404 导致的 ResponseException
                                if (exception instanceof ResponseException) {
                                    Response response = ((ResponseException) exception).getResponse();
                                    if (response.getStatusLine().getStatusCode() == 404) {
                                        // 资源不存在，正常完成，不发送错误
                                        sink.tryEmitEmpty();   // 对应 Mono.empty()
                                        return;
                                    }
                                }
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
                }).cast(Map.class)
                .defaultIfEmpty(Collections.emptyMap())
                .flatMapMany(node -> {
                    if (node.isEmpty()) return Flux.empty();
                    long familyId = (long) node.get("familyId");
                    int left = (int) node.get("left");
                    int right = (int) node.get("right");
                    Request request = new Request("GET", SEARCH_ENDPOINT);
                    request.setOptions(ESUtil.requestOptions());
                    request.setJsonEntity(ESCategoryQuery.buildPathRequest(familyId, left, right));

                    return ReactiveStream.toFluxByteBuf(request, "categories", String.valueOf(id));
                });
    }
}
