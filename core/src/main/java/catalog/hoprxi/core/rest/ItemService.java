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
import catalog.hoprxi.core.application.query.ItemQueryFilter;
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
import catalog.hoprxi.core.infrastructure.query.elasticsearch.filter.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.stream.StreamMessage;
import com.linecorp.armeria.common.stream.StreamWriter;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import org.javamoney.moneta.Money;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/10/19
 */
public class ItemService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Item");
    private static final String MINI_SEPARATION = ",";

    private static final ItemQuery QUERY = new ESItemQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private final Handler<ItemDeleteCommand, Boolean> deleteHandler = new ItemDeleteHandler();

    @Get("/items/:id")
    @Description("Retrieves the item information by the given ID.")
    public HttpResponse find(ServiceRequestContext ctx, @Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.findAsync(id); // 假设返回 Flux<ByteBuf>
        Flux<HttpObject> bodyStream = dataFlux.map(HttpData::wrap);// 先不发 headers！等第一个数据到来再发
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = bodyStream
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        return handleErrorResponse(ctx, id, signal.getThrowable());
                    } else if (signal.isOnComplete()) {
                        // 空流？返回 404 或 204
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.NOT_FOUND),
                                HttpData.ofUtf8("{\"error\":\"Item not found\",\"id\":" + id + "}")
                        );
                    } else {
                        return Flux.concat(// 有数据：前置 200 headers
                                Flux.just(ResponseHeaders.of(HttpStatus.OK)),
                                flux // 原始流（包含第一个元素）
                        );
                    }
                })
                .onErrorResume(e -> handleErrorResponse(ctx, id, e));

        return HttpResponse.of(responseStream);
 
        /*
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            try (InputStream is = QUERY.find(id)) {
                byte[] buf = new byte[SINGLE_BUFFER_SIZE];
                int n;
                while ((n = is.read(buf)) != -1) {
                    ByteBuf chunk = ctx.alloc().buffer(n);
                    chunk.writeBytes(buf, 0, n);
                    stream.write(HttpData.wrap(chunk));
                }
                stream.close();
            } catch (IOException e) {
                this.handleStreamError(stream, e);
                LOGGER.warn("Error,it's {}", e.getMessage());
            }
        });
        return HttpResponse.of(stream);
         */
    }

    private Publisher<? extends HttpObject> handleErrorResponse(ServiceRequestContext ctx, long id, Throwable cause) {
        LOGGER.warn("Error for id={}", id, cause);
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeCharSequence("{\"error\":\"Item not found\",\"id\":" + id + "}", StandardCharsets.UTF_8);
        return Flux.just(
                ResponseHeaders.of(HttpStatus.NOT_FOUND),
                HttpData.wrap(buf)
        );
    }

    @Get("/items")
    public HttpResponse search(ServiceRequestContext ctx) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        String filter = params.get("filter", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        String cursor = params.get("cursor", "");
        SortFieldEnum sortField = SortFieldEnum.of(params.get("sort", "_ID"));

        Flux<ByteBuf> dataFlux;
        if (cursor.isBlank()) {
            dataFlux = QUERY.searchAsync(this.parseFilter(search, filter), offset, size, sortField);
        } else {
            dataFlux = QUERY.searchAsync(this.parseFilter(search, filter), size, cursor, sortField);
        }
        Flux<HttpObject> bodyStream = dataFlux.map(HttpData::wrap);// 先不发 headers！等第一个数据到来再发
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = bodyStream
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) {// 信号->错误
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                HttpData.ofUtf8("{\"error\":\"Item not found\",\"id\":" + "}")
                        );
                    } else if (signal.isOnComplete()) {   // 空流？返回 404 或 204
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.NOT_FOUND),
                                HttpData.ofUtf8("{\"error\":\"Item not found\",\"id\":" + "}")
                        );
                    } else {
                        return Flux.concat(// 有数据：前置 200 headers
                                Flux.just(ResponseHeaders.of(HttpStatus.OK)),
                                flux // 原始流（包含第一个元素）
                        );
                    }
                })
                .onErrorResume(e -> Flux.just(
                        ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                        HttpData.ofUtf8("{\"error\":\"Item not found\",\"id\":" + e + "}")
                ));

        return HttpResponse.of(responseStream);
    }

    private ItemQueryFilter[] parseFilter(String query, String filter) {
        List<ItemQueryFilter> filterList = new ArrayList<>();
        if (!query.isBlank())
            filterList.add(new KeywordFilter(query));
        String[] filters = filter.split(";");//Project separation
        for (String s : filters) {
            String[] con = s.split(":");//Project name : Project value
            if (con.length == 2) {
                switch (con[0]) {
                    case "cid", "categoryId" -> parseCid(filterList, con[1]);
                    case "bid", "brandId" -> parseBid(filterList, con[1]);
                    case "retail_price", "r_price" -> parseRetailPrice(filterList, con[1]);
                    case "last_receipt_price", "l_r_price" -> parseLastReceiptPrice(filterList, con[1]);
                    case "member_price", "m_price" -> parseMemberPrice(filterList, con[1]);
                    case "vip_price", "v_price" -> parseVipPrice(filterList, con[1]);
                }
            }
        }
        //for (ItemQueryFilter f : filterList)
        //System.out.println(f);
        return filterList.toArray(new ItemQueryFilter[0]);
    }

    private void parseCid(List<ItemQueryFilter> filterList, String cids) {
        if (!cids.isBlank()) {
            String[] ss = cids.split(MINI_SEPARATION);
            long[] categoryIds = new long[ss.length];
            for (int i = 0; i < ss.length; i++) {
                categoryIds[i] = Long.parseLong(ss[i]);
            }
            filterList.add(new CategoryFilter(categoryIds));
        }
    }

    private void parseBid(List<ItemQueryFilter> filterList, String bids) {
        if (!bids.isBlank()) {
            String[] ss = bids.split(MINI_SEPARATION);
            long[] brandIds = new long[ss.length];
            for (int i = 0; i < ss.length; i++) {
                brandIds[i] = Long.parseLong(ss[i]);
            }
            filterList.add(new BrandFilter(brandIds));
        }
    }

    private void parseRetailPrice(List<ItemQueryFilter> filterList, String retail_price) {
        if (!retail_price.isBlank()) {
            String[] ss = retail_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new RetailPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseMemberPrice(List<ItemQueryFilter> filterList, String member_price) {
        if (!member_price.isBlank()) {
            String[] ss = member_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new MemberPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseVipPrice(List<ItemQueryFilter> filterList, String vip_price) {
        if (!vip_price.isBlank()) {
            String[] ss = vip_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new VipPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseLastReceiptPrice(List<ItemQueryFilter> filterList, String last_receipt_price) {
        if (!last_receipt_price.isBlank()) {
            String[] ss = last_receipt_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new LastReceiptPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
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
                Item item = this.createItem(parser);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A item created,it's %s\"}", item)));
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
                //System.out.println(e);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":fail,\"code\":500,\"message\":\"Can't create a item,cause by %s\"}", e)));
            }
        });
        return HttpResponse.of(future);
    }

    private Item createItem(JsonParser parser) throws IOException {
        Name name = Name.EMPTY;
        MadeIn madeIn = MadeIn.UNKNOWN;
        GradeEnum grade = GradeEnum.QUALIFIED;
        Specification spec = Specification.UNDEFINED;
        long brandId = Brand.UNDEFINED.id();
        long categoryId = Category.UNDEFINED.id();
        Barcode barcode = null;
        LastReceiptPrice lastReceiptPrice = LastReceiptPrice.RMB_PCS_ZERO;
        RetailPrice retailPrice = RetailPrice.RMB_PCS_ZERO;
        MemberPrice memberPrice = MemberPrice.RMB_PCS_ZERO;
        VipPrice vipPrice = VipPrice.RMB_PCS_ZERO;
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "barcode" -> barcode = BarcodeGenerateServices.createBarcode(parser.getValueAsString());
                    case "name" -> name = this.readName(parser);
                    case "spec" -> spec = Specification.valueOf(parser.getValueAsString());
                    case "grade" -> grade = GradeEnum.of(parser.getValueAsString());
                    case "madeIn" -> madeIn = this.readMadeIn(parser);
                    case "latestReceiptPrice", "last_receipt_price" ->
                            lastReceiptPrice = this.readLastReceiptPrice(parser);
                    case "retailPrice", "retail_price" -> retailPrice = new RetailPrice(this.readPrice(parser));
                    case "memberPrice", "member_price" -> memberPrice = this.readMemberPrice(parser);
                    case "vipPrice", "vip_price" -> vipPrice = this.readVipPrice(parser);
                    case "category", "categoryId" -> categoryId = readId(parser);
                    case "brand", "brandId" -> brandId = readId(parser);
                }
            }
        }
        ItemCreateCommand command = new ItemCreateCommand(barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
        Handler<ItemCreateCommand, Item> handler = new ItemCreateHandler();
        return handler.execute(command);
    }

    private Name readName(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object for 'name'");
        }
        String name = null, alias = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name" -> name = parser.getValueAsString();
                    case "alias" -> alias = parser.getValueAsString();
                }
            }
        }
        return new Name(name, alias);
    }

    private MadeIn readMadeIn(JsonParser parser) throws IOException {
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

    private LastReceiptPrice readLastReceiptPrice(JsonParser parser) throws IOException {
        return readNamedPrice(parser, LastReceiptPrice::new);
    }

    private MemberPrice readMemberPrice(JsonParser parser) throws IOException {
        return readNamedPrice(parser, MemberPrice::new);
    }

    private VipPrice readVipPrice(JsonParser parser) throws IOException {
        return readNamedPrice(parser, VipPrice::new);
    }

    private <T> T readNamedPrice(JsonParser parser, BiFunction<String, Price, T> constructor) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object for named price field");
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken();
            switch (fieldName) {
                case "price" -> price = readPrice(parser);
                case "name" -> name = parser.getValueAsString();
            }
        }
        return constructor.apply(name, price);
    }

    private Price readPrice(JsonParser parser) throws IOException {
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
    private long readId(JsonParser parser) throws IOException {
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

    @StatusCode(200)
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

    @StatusCode(200)
    @Delete("/items/:id")
    public HttpResponse delete(ServiceRequestContext ctx, @Param("id") long id) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);

        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ItemDeleteCommand delete = new ItemDeleteCommand(id);
            try {
                deleteHandler.execute(delete);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.ofUtf8("{\"status\":\"success\",\"code\":200,\"message\":\"The item(id=%d) is moved to the recycle bin, you can retrieve it later in the recycle bin!\"}", id));
            } catch (Exception e) {
                LOGGER.warn("Failed to delete item {}", id, e);
                stream.write(ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR));
                stream.write(HttpData.ofUtf8("{\"error\":\"Server error\"}"));
            } finally {
                stream.close();
            }
        });
        return HttpResponse.of(stream);
    }

}
