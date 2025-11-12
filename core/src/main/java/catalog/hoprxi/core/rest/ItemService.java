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
import catalog.hoprxi.core.application.query.SearchException;
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
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESItemQuery;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.filter.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.stream.StreamMessage;
import com.linecorp.armeria.common.stream.StreamWriter;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/10/19
 */
public class ItemService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final int SINGLE_BUFFER_SIZE = 1536; // 1.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Item");
    private static final String MINI_SEPARATION = ",";

    private static final ItemQuery QUERY = new ESItemQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/items/:id")
    @Description("Retrieves the item information by the given ID.")
    public HttpResponse find(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                if (pretty) gen.useDefaultPrettyPrinter();
                InputStream is = QUERY.find(id);
                this.copyRaw(gen, is);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer = null;
                stream.close();
            } catch (IOException | ClosedSessionException | SearchException e) {
                this.handleStreamError(stream, e);
                LOGGER.warn("Error,it's {}", e.getMessage());
            } finally {
                if (buffer != null) buffer.release(); // 只释放未被转移的缓冲区
            }
        });
        return HttpResponse.of(stream);
    }

    private void copyRaw(JsonGenerator generator, InputStream is) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(is);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
        generator.close();
    }

    private void handleStreamError(StreamWriter<HttpObject> stream, Exception e) {
        stream.write(ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
        stream.write(HttpData.ofUtf8("{\"status\":\"error\",\"code\":500,\"message\":\"Error,it's %s\"}", e.getMessage()));
        stream.close();
    }

    @Get("/items")
    public HttpResponse search(ServiceRequestContext ctx, @Param("pretty") @Default("false") boolean pretty) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        String filter = params.get("filter", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        String cursor = params.get("cursor", "");
        SortFieldEnum sortField = SortFieldEnum.of(params.get("sort", "_ID"));
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                if (pretty) gen.useDefaultPrettyPrinter();
                InputStream is;
                if (cursor.isBlank()) {
                    is = QUERY.search(this.parseFilter(search, filter), offset, size, sortField);
                } else {
                    is = QUERY.search(this.parseFilter(search, filter), size, cursor, sortField);
                }
                this.copyRaw(gen, is);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer = null;
                stream.close();
            } catch (IOException | ClosedSessionException | SearchException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release(); // 只释放未被转移的缓冲区
            }
        });
        return HttpResponse.of(stream);
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
            filterList.add(new CategoryIdFilter(categoryIds));
        }
    }

    private void parseBid(List<ItemQueryFilter> filterList, String bids) {
        if (!bids.isBlank()) {
            String[] ss = bids.split(MINI_SEPARATION);
            long[] brandIds = new long[ss.length];
            for (int i = 0; i < ss.length; i++) {
                brandIds[i] = Long.parseLong(ss[i]);
            }
            filterList.add(new BrandIdFilter(brandIds));
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
    public HttpResponse create(ServiceRequestContext ctx, HttpData body, @Param("pretty") @Default("false") boolean pretty) {
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
                System.out.println(e);
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
        LastReceiptPrice lastReceiptPrice = LastReceiptPrice.RMB_ZERO;
        RetailPrice retailPrice = RetailPrice.RMB_ZERO;
        MemberPrice memberPrice = MemberPrice.RMB_ZERO;
        VipPrice vipPrice = VipPrice.RMB_ZERO;
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
                    case "category" -> categoryId = readId(parser);
                    case "brand" -> brandId = readId(parser);
                    case "categoryId" -> categoryId = parser.getValueAsLong();
                    case "brandId" -> brandId = parser.getValueAsLong();
                }
            }
        }

        ItemCreateCommand command = new ItemCreateCommand(barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
        //System.out.println(command);
        Handler<ItemCreateCommand, Item> handler = new ItemCreateHandler();
        return handler.execute(command);
    }

    private Name readName(JsonParser parser) throws IOException {
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
        String madeIn = null, country = null, code = "156";
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
        //System.out.println(new Domestic(code, madeIn));
        return new Domestic(code, madeIn);
    }

    private LastReceiptPrice readLastReceiptPrice(JsonParser parser) throws IOException {
        String name = "";
        Price price = Price.RMB_ZERO;
        while (parser.nextToken() != JsonToken.END_OBJECT ||
               (parser.currentToken() == JsonToken.END_OBJECT && "price".equals(parser.currentName()))) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                if ("price".equals(fieldName)) price = readPrice(parser);
                else if ("name".equals(fieldName)) name = parser.getValueAsString();
            }
        }
        return name.isBlank() ? new LastReceiptPrice(price) : new LastReceiptPrice(name, price);
    }

    private MemberPrice readMemberPrice(JsonParser parser) throws IOException {
        String name = "";
        Price price = Price.RMB_ZERO;
        while (parser.nextToken() != JsonToken.END_OBJECT ||
               (parser.currentToken() == JsonToken.END_OBJECT && "price".equals(parser.currentName()))) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "price" -> price = readPrice(parser);
                    case "name" -> name = parser.getValueAsString();
                }
            }
        }
        return name.isBlank() ? new MemberPrice(price) : new MemberPrice(name, price);
    }

    private VipPrice readVipPrice(JsonParser parser) throws IOException {
        String name = "";
        Price price = Price.RMB_ZERO;
        while (parser.nextToken() != JsonToken.END_OBJECT ||
               (parser.currentToken() == JsonToken.END_OBJECT && "price".equals(parser.currentName()))) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "price" -> price = readPrice(parser);
                    case "name" -> name = parser.getValueAsString();
                }
            }
        }
        return name.isBlank() ? new VipPrice(price) : new VipPrice(name, price);
    }

    private Price readPrice(JsonParser parser) throws IOException {
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
        long id = 0L;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                if ("id".equals(fieldName)) {
                    id = parser.getValueAsLong();
                }
            }
        }
        return id;
    }

    @StatusCode(201)
    @Put("/items/{id}")
    public HttpResponse update(ServiceRequestContext ctx, HttpData body, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            this.update(body, id);
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.ofUtf8("{\"status\":\"success\",\"code\":201,\"message\":\"A category has update,it's %s\"}"));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    private void update(HttpData body, long id) {
    }

    @Delete("/items/:id")
    public HttpResponse delete(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ItemDeleteCommand delete = new ItemDeleteCommand(id);
            Handler<ItemDeleteCommand, Boolean> handler = new ItemDeleteHandler();
            handler.execute(delete);
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.ofUtf8("{\"status\":\"success\",\"code\":200,\"message\":\"The item(id=%s) is move to the recycle bin, you can retrieve it later in the recycle bin!\"}", id));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

}
