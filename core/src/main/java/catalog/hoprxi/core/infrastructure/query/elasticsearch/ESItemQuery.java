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


import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQuerySpec;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.domain.model.barcode.BarcodeValidServices;
import catalog.hoprxi.core.infrastructure.ESUtil;
import catalog.hoprxi.core.infrastructure.query.FluxByteBufOutputStream;
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
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2025/10/24
 */

public class ESItemQuery implements ItemQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final String PREFIX = ESUtil.customized().isBlank() ? "/item" : "/" + ESUtil.customized() + "_item";
    private static final String SEARCH_ENDPOINT = PREFIX + "/_search";
    private static final int AGGS_SIZE = 15;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public InputStream find(long id) {
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());

        return ESItemQuery.byteBufInputStream(String.valueOf(id), request, true);
    }

    @Override
    public Flux<ByteBuf> findAsync(long id) {
        Request request = new Request("GET", PREFIX + "/_doc/" + id);
        request.setOptions(ESUtil.requestOptions());

        return ESItemQuery.byteBufFlux(String.valueOf(id), request, true);
    }

    @Override
    public InputStream findByBarcode(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode ctr");

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildBarcodeFindRequest(barcode));

        return ESItemQuery.byteBufInputStream(barcode, request);
    }

    @Override
    public Flux<ByteBuf> findByBarcodeAsync(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) return Flux.error(new IllegalArgumentException("Not valid barcode"));

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildBarcodeFindRequest(barcode));

        return ESItemQuery.byteBufFlux(barcode, request);
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

    @Override
    public InputStream search(ItemQuerySpec[] specs, int size, String cursor, SortFieldEnum sortField) {
        if (size < 0 || size > 10000)
            throw new IllegalArgumentException("The size rang is 0-10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSearchRequest(specs, size, cursor, sortField));

        return ESItemQuery.byteBufInputStream(ESItemQuery.extractIdentifier(specs), request);
    }

    @Override
    public Flux<ByteBuf> searchAsync(ItemQuerySpec[] specs, int size, String cursor, SortFieldEnum sortField) {
        if (size < 0 || size > 10000)
            throw new IllegalArgumentException("The size rang is 0-10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        System.out.println();
        System.out.println(ESItemQuery.buildSearchRequest(specs, size, cursor, sortField));
        System.out.println();
        request.setJsonEntity(ESItemQuery.buildSearchRequest(specs, size, cursor, sortField));

        return ESItemQuery.byteBufFlux(ESItemQuery.extractIdentifier(specs), request);
    }

    private static String buildSearchRequest(ItemQuerySpec[] filters, int size, String searchAfter, SortFieldEnum sortField) {
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
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private static void buildSearchAfterRequest(JsonGenerator generator, String searchAfter) throws IOException {
        if (searchAfter == null || searchAfter.isBlank()) return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
    }

    @Override
    public Flux<ByteBuf> searchAsync(ItemQuerySpec[] specs, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSearchRequest(specs, offset, size, sortField));

        return ESItemQuery.byteBufFlux(ESItemQuery.extractIdentifier(specs), request);
    }

    @Override
    public InputStream search(ItemQuerySpec[] specs, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSearchRequest(specs, offset, size, sortField));
        return ESItemQuery.byteBufInputStream(ESItemQuery.extractIdentifier(specs), request);
    }

    private static String buildSearchRequest(ItemQuerySpec[] filters, int offset, int size, SortFieldEnum sortField) {
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

    private static void buildMainRequest(JsonGenerator generator, ItemQuerySpec[] filters) throws IOException {
        generator.writeObjectFieldStart("query");
        if (filters == null || filters.length == 0) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("filter");
            for (ItemQuerySpec filter : filters) {
                filter.queryClause(generator);
            }
            generator.writeEndArray();//end must
            generator.writeEndObject();//end bool
        }
        generator.writeEndObject();//end query
    }

    private static void buildSortRequest(JsonGenerator generator, SortFieldEnum sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(MapSortField.mapSortToField(sortField), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }


    private static void buildAggsRequest(JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("aggs");
        ESItemQuery.buildCompositeAggRequest(generator, "brand_aggs", "brand.id", "brand.name");
        ESItemQuery.buildCompositeAggRequest(generator, "category_aggs", "category.id", "category.name");
        generator.writeEndObject();//end eggs
    }

    private static void buildCompositeAggRequest(JsonGenerator gen, String aggName, String... fields)
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

    /**
     * 从过滤器数组中提取用于标识过滤器组的简短标识符。
     * <p>
     * 该方法用于为过滤器组生成一个易于识别的简短标识字符串，可用于日志标记、缓存键分类
     * 或过滤器组的类型区分。
     * <p>
     * 方法按照优先级检查过滤器类型，<b>通过过滤器运行时类的简单名称进行严格类型匹配</b>
     * （而非使用 {@code instanceof} 进行继承体系匹配，仅当过滤器的实际类型严格为指定类时才会被识别）：
     * <ol>
     * <li>优先检查数组中是否存在 {@code KeywordFilter} 类型的过滤器，存在则返回对应标识</li>
     * <li>若不存在关键词过滤器，再检查是否存在 {@code CategoryFilter} 类型的过滤器，存在则返回对应标识</li>
     * <li>若未匹配到上述指定类型的过滤器，则返回包含过滤器总数的标识</li>
     * </ol>
     * 同时处理了输入为 {@code null} 或空数组的边界情况。
     *
     * @param filters 待检查的过滤器数组，元素为 {@link ItemQuerySpec} 的子类实例，允许为 {@code null} 或空数组
     * @return 生成的过滤器组标识符，可能的返回值包括：
     * <ul>
     * <li>{@code "empty-filters"}：当输入为 {@code null} 或空数组时</li>
     * <li>{@code "Keyword filter"}：当数组中存在严格类型为关键词过滤器的元素时</li>
     * <li>{@code "Category filter"}：当数组中存在严格类型为类别过滤器的元素时</li>
     * <li>{@code "filters(N)"}：其他情况，其中 {@code N} 为过滤器的总数量</li>
     * </ul>
     */
    private static String extractIdentifier(ItemQuerySpec[] filters) {
        if (filters == null || filters.length == 0) {
            return "empty-filters";
        }
        for (ItemQuerySpec f : filters) {// 优先找 id
            if ("KeywordFilter".equals(f.getClass().getSimpleName())) {
                return "Keyword filter"; // 假设 getValue() 返回 String 或 Number
            }
        }
        for (ItemQuerySpec f : filters) { // 次选类别
            if ("CategoryFilter".equals(f.getClass().getSimpleName())) {
                return "Category filter";
            }
        }
        // 否则返回数量
        return "filters(" + filters.length + ")";
    }

    private static ByteBufInputStream byteBufInputStream(String tips, Request request, boolean alone) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean success = false;
        try {
            Response response = ESUtil.restClient().performRequest(request);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os);
                 InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is)) {
                if (alone) Extract.extractSourceSkipMeta(parser, generator);
                else Extract.extract(parser, generator, "items");
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                LOGGER.info("Item not found in Elasticsearch: id={}", tips);
                throw new SearchException(String.format("The item(id=%s) not found", tips));
            }
            LOGGER.error("Elasticsearch error for id={}", tips, e);
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

    private static ByteBufInputStream byteBufInputStream(String tips, Request request) {
        return ESItemQuery.byteBufInputStream(tips, request, false);
    }

    private static Flux<ByteBuf> byteBufFlux(String tips, Request request, boolean alone) {
        return Flux.<ByteBuf>create(sink -> {
                    AtomicBoolean isCancelled = new AtomicBoolean(false);
                    sink.onCancel(() -> isCancelled.set(true));
                    // 1. 发起 ES 请求 (运行在 ES I/O 线程)
                    ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            if (isCancelled.get()) {
                                EntityUtils.consumeQuietly(response.getEntity());
                                return;
                            }
                            // 2. 切换到业务线程池处理耗时逻辑
                            // 注意：这里直接在线程池里操作 sink，Reactor 会自动处理线程安全
                            TRANSFORM_POOL.execute(() -> {
                                try (InputStream content = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(content);
                                     OutputStream os = new FluxByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                                    // 执行解析，数据会通过 os 自动 emit 给下游
                                    if (alone) {
                                        Extract.extractSourceSkipMeta(parser, generator);
                                    } else {
                                        Extract.extract(parser, generator, "items");
                                    }
                                    sink.complete();
                                } catch (IOException e) {
                                    sink.error(MapException.mapException(e, tips));
                                } finally {
                                    EntityUtils.consumeQuietly(response.getEntity());
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            sink.error(MapException.mapException(exception, tips));
                        }
                    });
                }, FluxSink.OverflowStrategy.BUFFER) // 策略：如果下游慢，先缓冲（或者用 ERROR 直接报错）
                .doOnTerminate(() -> {
                    String threadName = Thread.currentThread().getName();
                    // 这里的 this 指的是 doOnTerminate 这个操作符内部的上下文，或者直接打印 tips 对应的唯一请求标识
                    LOGGER.debug("Request terminated for id: {}, Thread: {}, FluxIdentity: {}",
                            tips, threadName, System.identityHashCode(tips));
                })
                .doOnDiscard(ByteBuf.class, ByteBuf::release);
    }

    private static Flux<ByteBuf> byteBufFlux(String tips, Request request) {
        return ESItemQuery.byteBufFlux(tips, request, false);
    }
}
