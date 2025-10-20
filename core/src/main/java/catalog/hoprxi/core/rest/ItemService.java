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
import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.domain.model.Grade;
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
import io.netty.buffer.PooledByteBufAllocator;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/10/19
 */
@PathPrefix("/catalog/core/v1")
public class ItemService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;

    private static final int SINGLE_BUFFER_SIZE = 1536; // 1.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Item");

    private static final ItemQuery QUERY = new ESItemQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/items/:id")
    @Description("Retrieves the item information by the given ID.")
    public HttpResponse query(ServiceRequestContext ctx, @Param("id") @Default("-1") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.find(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
            } catch (IOException e) {
                handleStreamError(stream, e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    private void copyRaw(JsonGenerator generator, InputStream source) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(source);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    private void handleStreamError(StreamWriter<HttpObject> stream, IOException e) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
            gen.writeStartObject();
            gen.writeStringField("status", "fail");
            gen.writeNumberField("code", 500);
            gen.writeStringField("message", e.getMessage());
            gen.writeEndObject();
        } catch (IOException ex) {
            LOGGER.error("Json write error：{0}", e);
        }
        stream.write(ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
        stream.write(HttpData.wrap(buffer));
        stream.close();
    }

    @Get("/brands")
    public HttpResponse query(ServiceRequestContext ctx, @Param("pretty") @Default("false") boolean pretty) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        String cursor = params.get("cursor", "");
        SortField sortField = SortField.of(params.get("sort", ""));
        return null;
    }

    @Post("/items")
    public HttpResponse create(ServiceRequestContext ctx, HttpData body, @Param("pretty") @Default("false") boolean pretty) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ctx.blockingTaskExecutor().execute(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Item item = this.toItemJson(parser);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A item created,it's %s\"}", item)));
            } catch (Exception e) {
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"code\":500,\"message\":\"Can't create a item,cause by {}\"}", e)));
            }
        });
        return HttpResponse.of(future);
    }

    private Item toItemJson(JsonParser parser) throws IOException {
        Name name = Name.EMPTY;
        MadeIn madeIn = MadeIn.UNKNOWN;
        Grade grade = Grade.QUALIFIED;
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
                    case "grade" -> grade = Grade.of(parser.getValueAsString());
                    case "madeIn" -> madeIn = this.readMadeIn(parser);
                    case "latestReceiptPrice" -> lastReceiptPrice = new LastReceiptPrice(this.readPrice(parser));
                    case "retailPrice" -> retailPrice = new RetailPrice(this.readPrice(parser));
                    case "memberPrice" -> memberPrice = new MemberPrice(readPrice(parser));
                    case "vipPrice" -> vipPrice = new VipPrice(readPrice(parser));
                    case "category" -> categoryId = readId(parser);
                    case "brand" -> brandId = readId(parser);
                }
            }
        }
        new ItemCreateCommand(barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
        return null;
    }

    private Name readName(JsonParser parser) throws IOException {
        String name = null, alias = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name":
                        name = parser.getValueAsString();
                        break;
                    case "alias":
                        alias = parser.getValueAsString();
                        break;
                }
            }
        }
        return new Name(name, alias);
    }

    private MadeIn readMadeIn(JsonParser parser) throws IOException {
        MadeIn result = MadeIn.UNKNOWN;
        String city = null, country = null, code = "156";
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "code":
                        code = parser.getValueAsString();
                        break;
                    case "city":
                        city = parser.getValueAsString();
                        break;
                    case "country":
                        country = parser.getValueAsString();
                        break;
                }
            }
        }
        if (city != null) {
            result = new Imported(code, country);
        } else if (country != null) {
            result = new Domestic(code, city);
        }
        return result;
    }

    private Price readPrice(JsonParser parser) throws IOException {
        String currency = null;
        Unit unit = Unit.PCS;
        Number number = Integer.MIN_VALUE;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "currency" -> currency = parser.getValueAsString();
                    case "unit" -> unit = Unit.of(parser.getValueAsString());
                    case "number" -> number = parser.getNumberValue();
                }
            }
        }
        return new Price(Money.of(number, currency), unit);
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
}
