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
import catalog.hoprxi.core.application.command.Command;
import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.application.command.ItemDeleteCommand;
import catalog.hoprxi.core.application.query.ItemJsonQuery;
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.domain.model.GradeEnum;
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
import catalog.hoprxi.core.infrastructure.query.elasticsearch.filter.*;
import com.fasterxml.jackson.core.*;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;
import salt.hoprxi.utils.NumberHelper;

import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-03-24
 */

@WebServlet(urlPatterns = {"v2/items/*"}, name = "items", asyncSupported = true,
        initParams = {@WebInitParam(name = "query", value = "es"), @WebInitParam(name = "project_separation", value = ";")})
public class ItemServletV2 extends HttpServlet {
    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.CHINA)
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final String MINI_SEPARATION = ",";
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final ItemJsonQuery QUERY = new EsItemJsonQuery();
    private final ItemAppService app = new ItemAppService();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config != null) {
            String query = config.getInitParameter("query");
            System.out.println(query);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            String pathInfo = req.getPathInfo();
            resp.setContentType("application/json; charset=UTF-8");
            if (pathInfo != null) {
                String[] paths = pathInfo.split("/");
                if (paths.length == 0) {//No id
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    generator.writeStartObject();
                    generator.writeStringField("status", "Unfounded id");
                    generator.writeNumberField("code", 30405);
                    generator.writeStringField("message", "An id value is required, such as /123");
                    generator.writeEndObject();
                } else if (paths.length == 2) {//id query
                    try {
                        String result = QUERY.query(Long.parseLong(paths[1]));
                        copy(generator, result);
                    } catch (SearchException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "miss");
                        generator.writeNumberField("code", 30202);
                        generator.writeStringField("message", String.format("No category with id(%s) was found", paths[1]));
                        generator.writeEndObject();
                    } catch (NumberFormatException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "Error id value");
                        generator.writeNumberField("code", 30404);
                        generator.writeStringField("message", String.format("The id(%s) value needs to be a integer", paths[1]));
                        generator.writeEndObject();
                    }
                }
            } else {
                String cursor = Optional.ofNullable(req.getParameter("cursor")).orElse("");
                String query = Optional.ofNullable(req.getParameter("q")).orElse("");
                String filter = Optional.ofNullable(req.getParameter("filter")).orElse("");
                int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
                int size = NumberHelper.intOf(req.getParameter("size"), SIZE);
                String sort = Optional.ofNullable(req.getParameter("sort")).orElse("");
                SortFieldEnum sortField = SortFieldEnum.of(sort);
                if (cursor.isEmpty()) {
                    copy(generator, this.QUERY.query(parseFilter(query, filter), offset, size, sortField));
                } else {
                    copy(generator, this.QUERY.query(parseFilter(query, filter), size, cursor, sortField));
                }
            }
        }
    }

    private ItemQueryFilter[] parseFilter(String query, String filter) {
        List<ItemQueryFilter> filterList = new ArrayList<>();
        if (!query.isEmpty())
            filterList.add(new KeywordFilter(query));
        String[] filters = filter.split(";");//Project separation
        for (String s : filters) {
            String[] con = s.split(":");//Project name : Project value
            if (con.length == 2) {
                switch (con[0]) {
                    case "cid":
                    case "categoryId":
                        parseCid(filterList, con[1]);
                        break;
                    case "bid":
                    case "brandId":
                        parseBid(filterList, con[1]);
                        break;
                    case "retail_price":
                    case "r_price":
                        parseRetailPrice(filterList, con[1]);
                        break;
                    case "last_receipt_price":
                    case "lst_rcpt_price":
                        parseLastReceiptPrice(filterList, con[1]);
                        break;
                    case "member_price":
                    case "m_price":
                        parseMemberPrice(filterList, con[1]);
                        break;
                    case "vip_price":
                    case "v_price":
                        parseVipPrice(filterList, con[1]);
                        break;
                }
            }
        }
        //for (ItemQueryFilter f : filterList)
        //System.out.println(f);
        return filterList.toArray(new ItemQueryFilter[0]);
    }

    private void parseRetailPrice(List<ItemQueryFilter> filterList, String retail_price) {
        if (!retail_price.isEmpty()) {
            String[] ss = retail_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new RetailPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseMemberPrice(List<ItemQueryFilter> filterList, String member_price) {
        if (!member_price.isEmpty()) {
            String[] ss = member_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new MemberPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseVipPrice(List<ItemQueryFilter> filterList, String vip_price) {
        if (!vip_price.isEmpty()) {
            String[] ss = vip_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new VipPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseLastReceiptPrice(List<ItemQueryFilter> filterList, String last_receipt_price) {
        if (!last_receipt_price.isEmpty()) {
            String[] ss = last_receipt_price.split(MINI_SEPARATION);
            if (ss.length == 2) {
                filterList.add(new LastReceiptPriceFilter(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
            }
        }
    }

    private void parseBid(List<ItemQueryFilter> filterList, String bids) {
        if (!bids.isEmpty()) {
            String[] ss = bids.split(MINI_SEPARATION);
            long[] brandIds = new long[ss.length];
            for (int i = 0; i < ss.length; i++) {
                brandIds[i] = Long.parseLong(ss[i]);
            }
            filterList.add(new BrandIdFilter(brandIds));
        }
    }

    private void parseCid(List<ItemQueryFilter> filterList, String cids) {
        if (!cids.isEmpty()) {
            String[] ss = cids.split(MINI_SEPARATION);
            long[] categoryIds = new long[ss.length];
            for (int i = 0; i < ss.length; i++) {
                categoryIds[i] = Long.parseLong(ss[i]);
            }
            filterList.add(new CategoryIdFilter(categoryIds));
        }
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
                        grade = GradeEnum.of(parser.getValueAsString());
                        break;
                    case "madeIn":
                        madeIn = readMadeIn(parser);
                        break;
                    case "latestReceiptPrice":
                        lastReceiptPrice = new LastReceiptPrice(readPrice(parser));
                        break;
                    case "retailPrice":
                        retailPrice = new RetailPrice(readPrice(parser));
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
        String city = null;
        String country = null;
        String code = "156";
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "code" -> code = parser.getValueAsString();
                    case "city" -> city = parser.getValueAsString();
                    case "country" -> country = parser.getValueAsString();
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
        Number number = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "currency" -> currency = parser.getValueAsString();
                    case "unit" -> unit = parser.getValueAsString();
                    case "number" -> number = parser.getNumberValue();
                }
            }
        }
        return new Price(Money.of(number, currency), UnitEnum.of(unit));
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            ItemCreateCommand itemCreateCommand = read(parser);

            resp.setContentType("application/json; charset=UTF-8");
            generator.writeStartObject();
            generator.writeEndObject();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            Command itemDeleteCommand = new ItemDeleteCommand(1L);
            app.deleteItem(itemDeleteCommand);
        }
    }
}
