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

package catalog.hoprxi.core.rest;


import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.application.command.ItemDeleteCommand;
import catalog.hoprxi.core.application.handler.Handler;
import catalog.hoprxi.core.application.handler.ItemCreateHandler;
import catalog.hoprxi.core.application.handler.ItemDeleteHandler;
import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQuerySpec;
import catalog.hoprxi.core.application.query.NotFoundException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESItemQuery;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.spec.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/10/19
 */
public final class ItemService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final String MINI_SEPARATION = ",";
    private static final ItemQuery QUERY = new ESItemQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Get("/items/{id}")
    @Description("Retrieves the item information by the given ID.")
    public Mono<HttpResponse> find(@Param("id") long id) {
        return QUERY.findAsync(id)
                .map(byteBuf -> HttpResponse.of(
                        ResponseHeaders.builder(HttpStatus.OK)
                                .contentType(MediaType.JSON_UTF_8)
                                .build(),
                        HttpData.wrap(byteBuf)
                ))
                .onErrorResume(NotFoundException.class, error ->
                        Mono.just(HttpResponse.of(
                                ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build(),
                                HttpData.ofUtf8(String.format("{\"Error\":\"Item not found: %d\"}", id))
                        ))
                )
                .onErrorResume(error ->
                        Mono.just(HttpResponse.of(
                                ResponseHeaders.builder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build(),
                                HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                        ))
                );
    }

    @Get("/items")
    public HttpResponse search(QueryParams params) {
        //ServiceRequestContext.current().setRequestTimeoutMillis(60_000);
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        String cursor = params.get("cursor", "");
        SortFieldEnum sortField = SortFieldEnum.of(params.get("sort", "_ID"));
        // 解析查询 specs（key,cid,bid,filters 价格等）
        ItemQuerySpec[] querySpecs = ItemService.parseQuerySpecs(params);
        Flux<ByteBuf> dataFlux = cursor.isBlank()
                ? QUERY.searchAsync(querySpecs, offset, size, sortField)
                : QUERY.searchAsync(querySpecs, size, cursor, sortField);
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .transform(flux -> {
                    AtomicBoolean headersSent = new AtomicBoolean(false);
                    return flux.concatMap(data -> {
                        if (!headersSent.getAndSet(true)) {
                            // 第一个数据：先响应头 200，再发数据
                            return Flux.concat(
                                    Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build()),
                                    Flux.just(data)
                            );
                        }
                        return Flux.just(data);
                    });
                })
                .switchIfEmpty(Flux.just(
                        ResponseHeaders.of(HttpStatus.NOT_FOUND),
                        HttpData.ofUtf8(String.format("{\"Warn\":\"No items found for search keyword: %s\"}", "query"))
                ))
                .onErrorResume(throwable -> {
                    if (throwable instanceof IllegalArgumentException) {
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.BAD_REQUEST),
                                HttpData.ofUtf8(String.format("{\"error\":\"Invalid parameter: %s\"}", throwable.getMessage()))
                        );
                    }
                    LOGGER.error("Data stream error", throwable);
                    return Flux.just(
                            ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                            HttpData.ofUtf8("{\"error\":\"Internal server error\"}")
                    );
                });
        return HttpResponse.of(responseStream);
    }

    private static ItemQuerySpec[] parseQuerySpecs(QueryParams params) {
        List<ItemQuerySpec> filterList = new ArrayList<>();
        Optional.ofNullable(params.get("q"))
                .filter(s -> !s.isBlank())
                .ifPresent(query -> filterList.add(new KeywordSpec(query)));
        ItemService.parseIdArray(params.get("cid"))
                .ifPresent(cids -> filterList.add(new CategorySpec(cids)));
        parseIdArray(params.get("bid"))
                .ifPresent(bids -> filterList.add(new BrandSpec(bids)));
        // filters 中的条件
        String filters = params.get("filters", "");
        if (!filters.isBlank()) {
            for (String condition : filters.split(";")) {
                String[] keyValue = condition.split(MINI_SEPARATION);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    switch (key) {
                        case "retail_price", "r_price" -> ItemService.parsePriceSpec(filterList, value, RetailPriceSpec::new);
                        case "last_receipt_price", "l_r_price" -> ItemService.parsePriceSpec(filterList, value, LastReceiptPriceSpec::new);
                        case "member_price", "m_price" -> ItemService.parsePriceSpec(filterList, value, MemberPriceSpec::new);
                        case "vip_price", "v_price" -> ItemService.parsePriceSpec(filterList, value, VipPriceSpec::new);
                        default -> { /* 未知条件忽略，可记录日志 */ }
                    }
                }
            }
        }
        return filterList.toArray(new ItemQuerySpec[0]);
    }

    private static Optional<long[]> parseIdArray(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        long[] ids = Arrays.stream(value.split(MINI_SEPARATION))
                //.limit(10)  // 只取前 10 个
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s); // 保留 -1，不特殊处理
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid ID format: '{}', ignoring", s);
                        return null; // 用 null 标记解析失败
                    }
                })
                .filter(Objects::nonNull) // 丢弃解析失败的
                .mapToLong(Long::longValue)
                .toArray();
        return ids.length > 0 ? Optional.of(ids) : Optional.empty();
    }


    @FunctionalInterface
    private interface PriceSpecFactory {
        ItemQuerySpec create(Double min, Double max); // 允许 null
    }

    // ---------- 解析价格区间（支持部分有效） ----------
    private static void parsePriceSpec(List<ItemQuerySpec> filterList, String priceStr, PriceSpecFactory factory) {
        if (priceStr == null || priceStr.isBlank()) {
            return;
        }
        String[] parts = priceStr.split(MINI_SEPARATION, -1); // 保留空字符串
        if (parts.length != 2) {
            LOGGER.warn("Invalid price format (expected min,max): '{}'", priceStr);
            return;
        }
        Double min = null;
        Double max = null;
        boolean hasValid = false;
        // 解析 min
        String minStr = parts[0].trim();
        if (!minStr.isEmpty()) {
            try {
                min = Double.parseDouble(minStr);
                hasValid = true;
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid min price number: '{}', ignoring", minStr);
            }
        }
        // 解析 max
        String maxStr = parts[1].trim();
        if (!maxStr.isEmpty()) {
            try {
                max = Double.parseDouble(maxStr);
                hasValid = true;
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid max price number: '{}', ignoring", maxStr);
            }
        }
        // 至少有一个有效值才继续
        if (!hasValid) {
            LOGGER.warn("Both min and max are invalid for price condition: '{}', ignoring", priceStr);
            return;
        }

        try {
            filterList.add(factory.create(min, max)); // 工厂创建，可能会校验 min < max
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid price range (min={}, max={}), ignoring: {}", min, max, e.getMessage());
        }
    }

    private static  String mapQuerySpec(ItemQuerySpec[] specs){
        return "query";
    }

    @Get("/items/suggest")
    public Mono<HttpResponse> suggest(@Param("word") String word) {
        return QUERY.suggest(word)
                .map(byteBuf -> HttpResponse.of(
                        ResponseHeaders.builder(HttpStatus.OK)
                                .contentType(MediaType.JSON_UTF_8)
                                .build(),
                        HttpData.wrap(byteBuf)
                ))
                .onErrorResume(NotFoundException.class, e ->
                        Mono.just(HttpResponse.of(
                                ResponseHeaders.builder(HttpStatus.NOT_FOUND).contentType(MediaType.JSON_UTF_8).build(),
                                HttpData.ofUtf8("{\"Error\":\"not found\"}")
                        ))
                )
                .onErrorResume(e ->
                        Mono.just(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                                "{\"status\":fail,\"code\":500,\"message\":\"Internal server error,cause by %s\"}", e))
                );
    }

    @Post("/items")
    public HttpResponse create(ServiceRequestContext ctx, HttpData body) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) ||
              MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ctx.blockingTaskExecutor().execute(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Item item = ItemService.createItem(parser);
                future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A item created,it's %s\"}", item));
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
                future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":fail,\"code\":500,\"message\":\"Can't create a item,cause by %s\"}", e));
            }
        });
        return HttpResponse.of(future);
    }

    private static Item createItem(JsonParser parser) throws IOException {
        Name name = Name.EMPTY;
        MadeIn madeIn = MadeIn.UNORIGINATED;
        GradeEnum grade = GradeEnum.QUALIFIED;
        Specification spec = Specification.UNDEFINED;
        long brandId = Brand.UNBRANDED.id();
        long categoryId = Category.UNCATEGORIZED.id();
        Barcode barcode = null;
        LastReceiptPrice lastReceiptPrice = LastReceiptPrice.ZERO_RMB_PCS;
        RetailPrice retailPrice = RetailPrice.ZERO_RMB_PCS;
        MemberPrice memberPrice = MemberPrice.ZERO_RMB_PCS;
        VipPrice vipPrice = VipPrice.ZERO_RMB_PCS;
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "barcode" -> barcode = BarcodeGenerateServices.createBarcode(parser.getValueAsString());
                    case "name" -> name = readName(parser);
                    case "spec" -> spec = Specification.of(parser.getValueAsString());
                    case "grade" -> grade = GradeEnum.of(parser.getValueAsString());
                    case "madeIn" -> madeIn = readMadeIn(parser);
                    case "latestReceiptPrice", "last_receipt_price" -> lastReceiptPrice = readLastReceiptPrice(parser);
                    case "retailPrice", "retail_price" -> retailPrice = new RetailPrice(readPrice(parser));
                    case "memberPrice", "member_price" -> memberPrice = readMemberPrice(parser);
                    case "vipPrice", "vip_price" -> vipPrice = readVipPrice(parser);
                    case "category", "categoryId" -> categoryId = readId(parser);
                    case "brand", "brandId" -> brandId = readId(parser);
                }
            }
        }
        ItemCreateCommand command = new ItemCreateCommand(barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
        Handler<ItemCreateCommand, Item> handler = new ItemCreateHandler();
        return handler.execute(command);
    }

    private static Name readName(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object for 'name'");
        }
        String name = null, shortName = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name" -> name = parser.getValueAsString();
                    case "shortName" -> shortName = parser.getValueAsString();
                }
            }
        }
        return new Name(name, shortName);
    }

    private static MadeIn readMadeIn(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object for 'madeIn'");
        }
        String madeIn = null;
        String country = null;
        String code = "156";
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "code" -> code = parser.getValueAsString();
                    case "madeIn" -> madeIn = parser.getValueAsString();
                    case "country" -> country = parser.getValueAsString();
                }
            }
        }
        if (country != null && !country.isEmpty())
            return new Imported(code, country);
        //System.out.println(new Domestic(code, madeIn));
        return new Domestic(code, madeIn);
    }

    private static LastReceiptPrice readLastReceiptPrice(JsonParser parser) throws IOException {
        return readNamedPrice(parser, LastReceiptPrice::new);
    }

    private static MemberPrice readMemberPrice(JsonParser parser) throws IOException {
        return readNamedPrice(parser, MemberPrice::new);
    }

    private static VipPrice readVipPrice(JsonParser parser) throws IOException {
        return readNamedPrice(parser, VipPrice::new);
    }

    private static <T> T readNamedPrice(JsonParser parser, BiFunction<String, Price, T> constructor) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object for named price field");
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken();
            switch (fieldName) {
                case "price" -> price = readPrice(parser);
                case "name" -> name = parser.getValueAsString();
            }
        }
        return constructor.apply(name, price);
    }

    private static Price readPrice(JsonParser parser) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object for 'price'");
        }
        CurrencyUnit currencyUnit = Monetary.getCurrency(Locale.getDefault());
        UnitEnum unit = UnitEnum.PCS;
        Number number = 0.0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "currency", "currencyCode" -> currencyUnit = Monetary.getCurrency(parser.getValueAsString());
                    case "unit" -> unit = UnitEnum.of(parser.getValueAsString());
                    case "number" -> number = parser.getNumberValue();
                }
            }
        }
        return new Price(Money.of(number, currencyUnit), unit);
    }

    //read category or brand id
    private static long readId(JsonParser parser) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getValueAsLong();
        } else if (token == JsonToken.START_OBJECT) {
            long id = -1L;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "id".equals(parser.currentName())) {
                    parser.nextToken();
                    id = parser.getValueAsLong();
                }
            }
            return id;
        } else if (token == JsonToken.VALUE_NULL) {
            return -1L;
        } else {
            throw new IllegalArgumentException("Expected number or object with 'id' field");
        }
    }

    @Put("/items/{id}")
    public HttpResponse update(ServiceRequestContext ctx, HttpData body, @Param("id") long id) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Item item = this.update(parser, id);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":200,\"message\":\"Item update\"}")));
            } catch (Exception e) {
                //System.out.println(e);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":\"fail\",\"code\":500,\"message\":\"Can't update a item\"}")));
            }
        });
        return HttpResponse.of(future);
    }

    private Item update(JsonParser parser, long id) throws IOException {
        return null;

    }

    @StatusCode(204)
    @Delete("/items/{id}")
    public HttpResponse delete(@Param("id") long id) {
        return HttpResponse.of(CompletableFuture.supplyAsync(() -> {
            try {
                ItemDeleteCommand command = new ItemDeleteCommand(id);
                Handler<ItemDeleteCommand, Boolean> handler = new ItemDeleteHandler();
                handler.execute(command);
                // 1. 删除成功，推荐使用 204 No Content
                return HttpResponse.of(HttpStatus.NO_CONTENT);
            } catch (NotFoundException e) {
                // 2. 精细化异常处理：资源不存在返回 404
                return HttpResponse.of(HttpStatus.NOT_FOUND, MediaType.JSON_UTF_8,
                        String.format("{\"status\":\"error\",\"code\":404,\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                // 3. 其他未知异常返回 500
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":\"error\",\"code\":500,\"message\":\"Delete failed due to internal error\"}");
            }
        }, VIRTUAL_EXECUTOR));
    }
}
