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


import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQuerySpec;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.domain.model.barcode.BarcodeValidServices;
import catalog.hoprxi.core.infrastructure.ESUtil;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.spec.KeywordSpec;
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
import java.io.UncheckedIOException;
import java.util.Arrays;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.3 builder 2026/04/22
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
    public InputStream findByBarcode(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode ctr");

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildBarcodeFindRequest(barcode));

        return ReactiveStream.toSingleByteBufInputStream(request, barcode);
    }

    @Override
    public Mono<ByteBuf> findByBarcodeAsync(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) return Mono.error(new IllegalArgumentException("Not valid barcode"));

        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildBarcodeFindRequest(barcode));

        return ReactiveStream.toMonoByteBuf(request, barcode);
    }

    private static String buildBarcodeFindRequest(String barcode) {
        try (StringWriter writer = new StringWriter(); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode.raw", barcode);
            generator.writeEndObject();
            generator.writeEndObject();//end query
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Failed to build ES query", e);
        }
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

        return ReactiveStream.toByteBufInputStream(request, "items", ESItemQuery.extractIdentifier(specs));
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
        request.setJsonEntity(ESItemQuery.buildSearchRequest(specs, size, cursor, sortField));

        return ReactiveStream.toFluxByteBuf(request, "items", ESItemQuery.extractIdentifier(specs));
    }

    private static String buildSearchRequest(ItemQuerySpec[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        try (StringWriter writer = new StringWriter(); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            ESItemQuery.buildMainRequest(generator, filters);
            ESItemQuery.buildSortRequest(generator, sortField);
            ESItemQuery.buildSearchAfterRequest(generator, searchAfter);
            ESItemQuery.buildAggsRequest(generator);
            generator.writeEndObject();
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON", e);
        }
    }

    private static void buildSearchAfterRequest(JsonGenerator generator, String searchAfter) throws IOException {
        if (searchAfter == null || searchAfter.isBlank()) return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
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
        return ReactiveStream.toByteBufInputStream(request, "items", ESItemQuery.extractIdentifier(specs));
    }

    @Override
    public Flux<ByteBuf> searchAsync(ItemQuerySpec[] specs, int offset, int size, SortFieldEnum sortField) {
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

        return ReactiveStream.toFluxByteBuf(request, "items", ESItemQuery.extractIdentifier(specs));
    }

    private static String buildSearchRequest(ItemQuerySpec[] specs, int offset, int size, SortFieldEnum sortField) {
        try (StringWriter writer = new StringWriter(); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", size);
            ESItemQuery.buildMainRequest(generator, specs);
            ESItemQuery.buildSortRequest(generator, sortField);
            ESItemQuery.buildAggsRequest(generator);
            generator.writeEndObject();//root
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
    }

    private static void buildMainRequest(JsonGenerator generator, ItemQuerySpec[] specs) throws IOException {
        generator.writeObjectFieldStart("query");
        if (specs == null || specs.length == 0) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
            generator.writeObjectFieldStart("bool");
            ItemQuerySpec[] filterSpecs = Arrays.stream(specs)
                    .filter(spec -> {
                        if (spec instanceof KeywordSpec) {
                            try {
                                spec.queryClause(generator);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            return false;
                        }
                        return true;
                    })
                    .toArray(ItemQuerySpec[]::new);
            // 然后直接遍历数组
            if (filterSpecs.length > 0) {
                generator.writeArrayFieldStart("filter");
                for (ItemQuerySpec filter : filterSpecs) {
                    filter.queryClause(generator);
                }
                generator.writeEndArray();//end filter
            }
            generator.writeEndObject();//end bool
        }
        generator.writeEndObject();//end query
    }

    private static void buildSortRequest(JsonGenerator generator, SortFieldEnum sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        //generator.writeStartObject();
        //generator.writeStringField("_score", sortField.sort());
        //generator.writeEndObject();
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
        gen.writeObjectFieldStart("multi_terms");
        gen.writeNumberField("size", ESItemQuery.AGGS_SIZE);
        gen.writeArrayFieldStart("terms");
        for (String field : fields) {
            gen.writeStartObject();
            gen.writeStringField("field", field);
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.writeEndObject();//end multi_terms
        gen.writeEndObject();//end aggname
/*
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

 */
    }

    @Override
    public Mono<ByteBuf> suggest(String keyword) {
        Request request = new Request("GET", SEARCH_ENDPOINT);
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(ESItemQuery.buildSuggestRequest(keyword, 10, 1));

        return ReactiveStream.toSuggestMonoByteBuf(request);
    }

    private static String buildSuggestRequest(String keyword, int size, int score) {
        try (StringWriter writer = new StringWriter(); JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            // 开始最外层对象 {
            generator.writeStartObject();
            generator.writeNumberField("size", 0);
            // 开始 "query" 对象
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            // 开始 "should" 数组
            generator.writeArrayFieldStart("should");
            // 第一个 should 对象
            generator.writeStartObject();
            generator.writeObjectFieldStart("match");
            generator.writeStringField("suggest", keyword);
            generator.writeEndObject();
            generator.writeEndObject();
            // 第二个 should 对象
            generator.writeStartObject();
            generator.writeObjectFieldStart("match");
            generator.writeStringField("suggest.pinyin", keyword);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndArray(); // 结束 "should" 数组
            // 开始 "filter" 数组
            generator.writeArrayFieldStart("filter");
            generator.writeStartObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("suggest_hot_score");
            generator.writeNumberField("gte", score);
            generator.writeEndObject(); // 结束 "suggest_hot_score"
            generator.writeEndObject(); // 结束 "range"
            generator.writeEndObject(); // 结束 filter 里的对象
            generator.writeEndArray(); // 结束 "filter" 数组

            generator.writeNumberField("minimum_should_match", 1);
            generator.writeEndObject(); // 结束 "bool"
            generator.writeEndObject(); // 结束 "query"

            // 开始 "aggs" 对象
            generator.writeObjectFieldStart("aggs");
            generator.writeObjectFieldStart("sampler");
            generator.writeObjectFieldStart("sampler");
            generator.writeNumberField("shard_size", 2000);
            generator.writeEndObject(); // 结束内层 "sampler"

            generator.writeObjectFieldStart("aggs");
            generator.writeObjectFieldStart("suggests");
            generator.writeObjectFieldStart("terms");
            generator.writeStringField("field", "suggest.raw");
            generator.writeNumberField("size", size);
            generator.writeObjectFieldStart("order");
            generator.writeStringField("hot", "desc");
            generator.writeEndObject(); // 结束 "order"
            generator.writeEndObject(); // 结束 "terms"

            generator.writeObjectFieldStart("aggs");
            generator.writeObjectFieldStart("hot");
            generator.writeObjectFieldStart("max");
            generator.writeStringField("field", "suggest_hot_score");
            generator.writeEndObject(); // 结束 "max"
            generator.writeEndObject(); // 结束 "hot"

            generator.writeObjectFieldStart("hit");
            generator.writeObjectFieldStart("top_hits");
            generator.writeNumberField("size", 1);
            generator.writeArrayFieldStart("_source");
            generator.writeString("id");
            generator.writeString("barcode");
            generator.writeString("name.name");
            generator.writeEndArray(); // 结束 "_source"
            generator.writeEndObject(); // 结束 "top_hits"
            generator.writeEndObject(); // 结束 "hit"
            generator.writeEndObject(); // 结束内层 "aggs" (hit的父级)
            generator.writeEndObject(); // 结束 "suggests"
            generator.writeEndObject(); // 结束外层 "aggs" (suggests的父级)
            generator.writeEndObject(); // 结束 "sampler" (aggs的父级)
            generator.writeEndObject(); // 结束最外层 "aggs"

            generator.writeEndObject(); // 结束最外层对象 }
            generator.close(); // 确保所有数据都写入 StringWriter

            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
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
     * @param specs 待检查的过滤器数组，元素为 {@link ItemQuerySpec} 的子类实例，允许为 {@code null} 或空数组
     * @return 生成的过滤器组标识符，可能的返回值包括：
     * <ul>
     * <li>{@code "empty-filters"}：当输入为 {@code null} 或空数组时</li>
     * <li>{@code "Keyword filter"}：当数组中存在严格类型为关键词过滤器的元素时</li>
     * <li>{@code "Category filter"}：当数组中存在严格类型为类别过滤器的元素时</li>
     * <li>{@code "filters(N)"}：其他情况，其中 {@code N} 为过滤器的总数量</li>
     * </ul>
     */
    private static String extractIdentifier(ItemQuerySpec[] specs) {
        if (specs == null || specs.length == 0) {
            return "empty-filters";
        }
        for (ItemQuerySpec f : specs) {// 优先找 id
            if ("KeywordFilter".equals(f.getClass().getSimpleName())) {
                return "Keyword filter"; // 假设 getValue() 返回 String 或 Number
            }
        }
        for (ItemQuerySpec f : specs) { // 次选类别
            if ("CategoryFilter".equals(f.getClass().getSimpleName())) {
                return "Category filter";
            }
        }
        // 否则返回数量
        return "filters(" + specs.length + ")";
    }
}
