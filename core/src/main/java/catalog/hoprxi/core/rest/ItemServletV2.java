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

import catalog.hoprxi.core.application.ItemAppService;
import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.application.query.ItemJsonQuery;
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.domain.model.Grade;
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
import catalog.hoprxi.core.infrastructure.query.elasticsearch.EsItemJsonQuery;
import com.fasterxml.jackson.core.*;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;
import salt.hoprxi.utils.NumberHelper;

import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-03-24
 */
public class ItemServletV2 extends HttpServlet {
    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.CHINA)
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private final ItemJsonQuery query = new EsItemJsonQuery();
    private final ItemAppService app = new ItemAppService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            String pathInfo = req.getPathInfo();
            resp.setContentType("application/json; charset=UTF-8");
            if (pathInfo != null) {
                String[] paths = pathInfo.split("/");
                if (paths.length == 0) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    generator.writeStartObject();
                    generator.writeStringField("status", "Unfounded id");
                    generator.writeNumberField("code", 30405);
                    generator.writeStringField("message", "An id value is required, such as /123");
                    generator.writeEndObject();
                } else if (paths.length == 2) {
                    try {
                        String result = query.query(Long.parseLong(paths[1]));
                        copy(generator, result);
                    } catch (QueryException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "miss");
                        generator.writeNumberField("code", 30202);
                        generator.writeStringField("message", String.format("No category with id %s was found", paths[1]));
                        generator.writeEndObject();
                    } catch (NumberFormatException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "Error id value");
                        generator.writeNumberField("code", 30404);
                        generator.writeStringField("message", String.format("The id(%s) value needs to be a long", paths[1]));
                        generator.writeEndObject();
                    }
                }
            } else {
                String cursor = Optional.ofNullable(req.getParameter("cursor")).orElse("");
                String query = Optional.ofNullable(req.getParameter("q")).orElse("");
                int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
                int size = NumberHelper.intOf(req.getParameter("size"), SIZE);
                String sort = Optional.ofNullable(req.getParameter("sort")).orElse("");
                SortField sortField = SortField.of(sort);
                if (cursor.isEmpty()) {
                    copy(generator, this.query.query(parseFilter(query), offset, size, sortField));
                } else {
                    copy(generator, this.query.query(parseFilter(query), size, cursor, sortField));
                }
            }
        }
    }

    private ItemQueryFilter[] parseFilter(String filter) {
        return new ItemQueryFilter[0];
    }

    private void copy(JsonGenerator generator, String result) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(result);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            ItemCreateCommand itemCreateCommand = read(parser);
            app.createItem(itemCreateCommand);
            resp.setContentType("application/json; charset=UTF-8");
            generator.writeStartObject();
            generator.writeEndObject();
        }
    }

    private ItemCreateCommand read(JsonParser parser) throws IOException {
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
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "barcode":
                        barcode = BarcodeGenerateServices.createBarcode(parser.getValueAsString());
                        break;
                    case "name":
                        name = readName(parser);
                        break;
                    case "spec":
                        spec = Specification.valueOf(parser.getValueAsString());
                        break;
                    case "grade":
                        grade = Grade.of(parser.getValueAsString());
                        break;
                    case "madeIn":
                        madeIn = readMadeIn(parser);
                        break;
                    case "latestReceiptPrice":
                        break;
                    case "retailPrice":
                        Price price = readPrice(parser);
                        retailPrice = new RetailPrice(price);
                        break;
                    case "memberPrice":
                        memberPrice = new MemberPrice(readPrice(parser));
                        break;
                    case "vipPrice":
                        vipPrice = new VipPrice(readPrice(parser));
                        break;
                    case "category":
                        categoryId = readId(parser);
                        break;
                    case "brand":
                        brandId = readId(parser);
                        break;
                }
            }
        }
        return new ItemCreateCommand(barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
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
        String currency = null, unit = null;
        Number number = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "currency":
                        currency = parser.getValueAsString();
                        break;
                    case "unit":
                        unit = parser.getValueAsString();
                        break;
                    case "number":
                        number = parser.getNumberValue();
                        break;
                }
            }
        }
        return new Price(Money.of(number, currency), Unit.of(unit));
    }

    //read category or brand id
    private long readId(JsonParser parser) throws IOException {
        long id = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "id":
                        id = parser.getValueAsLong();
                        break;
                }
            }
        }
        return id;
    }
}
