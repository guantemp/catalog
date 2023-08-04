/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.webapp;

import catalog.hoprxi.core.application.ItemAppService;
import catalog.hoprxi.core.application.command.Command;
import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.application.command.ItemDeleteCommand;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
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
import catalog.hoprxi.core.infrastructure.query.ArangoDBItemQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import com.fasterxml.jackson.core.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-09-19
 */
@WebServlet(urlPatterns = {"v1/items/*"}, name = "items", asyncSupported = true, initParams = {
        @WebInitParam(name = "database", value = "arangodb"), @WebInitParam(name = "databaseName", value = "catalog")})
public class ItemServlet extends HttpServlet {
    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.CHINA)
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());
    private static final int OFFSET = 0;
    private static final int LIMIT = 64;
    private static final String PRE_SUFFIX = ".*?";
    private final JsonFactory jasonFactory = JsonFactory.builder().build();
    private ItemQueryService queryService;
    private ItemAppService app = new ItemAppService();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config != null) {
            String database = config.getInitParameter("database");
            //System.out.println(database);
            //String databaseName = config.getInitParameter("databaseName");
        }
        Config conf = ConfigFactory.load("database");
        String provider = conf.hasPath("provider") ? conf.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
                //repository = new PsqlItemRepository("catalog");
                queryService = new PsqlItemQueryService("catalog");
                break;
            case "arangodb":
                //repository = new ArangoDBItemRepository("catalog");
                queryService = new ArangoDBItemQueryService("catalog");
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long start = System.currentTimeMillis();
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8).useDefaultPrettyPrinter();
        generator.writeStartObject();
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            ItemView itemView = queryService.query(id);
            if (itemView != null) {
                responseItemView(generator, itemView);
            } else {
                //resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                generator.writeNumberField("code", 204);
                generator.writeStringField("message", "Not query");
            }
        } else {
            int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
            int limit = NumberHelper.intOf(req.getParameter("limit"), LIMIT);
            String barcode = Optional.ofNullable(req.getParameter("barcode")).orElse("");
            String key = Optional.ofNullable(req.getParameter("key")).orElse("");
            String categoryId = Optional.ofNullable(req.getParameter("cid")).orElse("");
            String brandId = Optional.ofNullable(req.getParameter("bid")).orElse("");
            String sort = Optional.ofNullable(req.getParameter("sort")).orElse("");
            generator.writeNumberField("offset", offset);
            generator.writeNumberField("limit", limit);
            if (!barcode.isEmpty()) {
                ItemView[] itemViews = queryService.queryByBarcode(barcode);
                responseItemViews(generator, itemViews);
            } else if (!key.isEmpty()) {
                ItemView[] itemViews = queryService.serach(key, offset, limit);
                itemViews = Arrays.stream(itemViews).filter(i -> categoryId.isEmpty() ? true : categoryId.equals(i.categoryView().id()))
                        //.skip(offset).limit(limit)
                        .toArray(ItemView[]::new);
                System.out.println(itemViews.length);
                responseItemViews(generator, itemViews);
            } else {
                generator.writeNumberField("total", queryService.size());
                ItemView[] itemViews = queryService.queryAll(offset, limit);
                responseItemViews(generator, itemViews);
            }
        }
        generator.writeNumberField("Execute time", System.currentTimeMillis() - start);
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }
    /*
          if (!brandId.isEmpty() || !categoryId.isEmpty()) {
                if (!brandId.isEmpty()) {
                    ItemView[] itemViews = queryService.belongToBrand(brandId, offset, limit);
                    if (!barcode.isEmpty() || !key.isEmpty()) {
                        Pattern namePattern = Pattern.compile(PRE_SUFFIX + key + PRE_SUFFIX);
                        itemViews = Arrays.stream(itemViews).parallel()
                                .filter(b -> Pattern.compile(PRE_SUFFIX + barcode + PRE_SUFFIX).matcher(b.barcode().barcode()).matches())
                                .filter(n -> namePattern.matcher(n.name().name()).matches())
                                .filter(n -> namePattern.matcher(n.name().mnemonic()).matches())
                                .filter(n -> namePattern.matcher(n.name().alias()).matches())
                                .toArray(ItemView[]::new);
                    }
                    generator.writeNumberField("total", itemViews.length);
                    responseItemViews(generator, itemViews);
                }
                if (!categoryId.isEmpty()) {
                    ItemView[] itemViews = queryService.belongToCategory(categoryId, offset, limit);
                    if (!barcode.isEmpty() || !key.isEmpty()) {
                        Pattern namePattern = Pattern.compile(PRE_SUFFIX + key + PRE_SUFFIX);
                        itemViews = Arrays.stream(itemViews)
                                .filter(b -> Pattern.compile(PRE_SUFFIX + barcode + PRE_SUFFIX).matcher(b.barcode().barcode()).matches())
                                .filter(n -> namePattern.matcher(n.name().name()).matches())
                                .filter(n -> namePattern.matcher(n.name().mnemonic()).matches())
                                .filter(n -> namePattern.matcher(n.name().alias()).matches())
                                .toArray(ItemView[]::new);
                    }
                    generator.writeNumberField("total", itemViews.length);
                    responseItemViews(generator, itemViews);
                }
            } else if (!barcode.isEmpty() || !key.isEmpty()) {
                Set<ItemView> itemViewSet = new HashSet<>();
                if (!barcode.isEmpty()) {
                    ItemView[] itemViews = queryService.queryByBarcode(barcode);
                    for (ItemView itemView : itemViews)
                        itemViewSet.add(itemView);
                }
                if (!key.isEmpty()) {
                    ItemView[] itemViews = queryService.serach(key, offset, limit);
                    itemViewSet.addAll(Arrays.asList(itemViews));
                }
                responseItemViews(generator, itemViewSet.toArray(new ItemView[0]));
            }
     */

    private void responseItemView(JsonGenerator generator, ItemView itemView) throws IOException {
        generator.writeStringField("id", itemView.id());
        generator.writeStringField("barcode", (String) itemView.barcode().barcode());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", itemView.name().name());
        generator.writeStringField("mnemonic", itemView.name().mnemonic());
        generator.writeStringField("alias", itemView.name().alias());
        generator.writeEndObject();
        generator.writeObjectField("spec", itemView.spec().value());
        generator.writeObjectFieldStart("madeIn");
        generator.writeStringField("code", itemView.madeIn().code());
        generator.writeStringField("madeIn", itemView.madeIn().madeIn());
        generator.writeEndObject();
        generator.writeObjectField("grade", itemView.grade().toString());
        generator.writeObjectFieldStart("category");
        generator.writeStringField("id", itemView.categoryView().id());
        generator.writeStringField("name", itemView.categoryView().name());
        generator.writeEndObject();
        generator.writeObjectFieldStart("brand");
        generator.writeStringField("id", itemView.brandView().id());
        generator.writeStringField("name", itemView.brandView().name());
        generator.writeEndObject();

        generator.writeObjectFieldStart("lastReceiptPrice");
        generator.writeStringField("name", itemView.lastReceiptPrice().name());
        Price price = itemView.lastReceiptPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();

        generator.writeObjectFieldStart("retailPrice");
        price = itemView.retailPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        generator.writeObjectFieldStart("memberPrice");
        generator.writeStringField("name", itemView.memberPrice().name());
        price = itemView.memberPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        generator.writeObjectFieldStart("vipPrice");
        generator.writeStringField("name", itemView.vipPrice().name());
        price = itemView.vipPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        if (itemView.images().length >= 1) {
            generator.writeArrayFieldStart("images");
            for (URI uri : itemView.images()) {
                //System.out.println(uri.toASCIIString());
                generator.writeString(uri.toASCIIString());
            }
            generator.writeEndArray();
        }
    }

    private void responseItemViews(JsonGenerator generator, ItemView[] itemViews) throws IOException {
        generator.writeArrayFieldStart("items");
        for (ItemView itemView : itemViews) {
            generator.writeStartObject();
            responseItemView(generator, itemView);
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonParser parser = jasonFactory.createParser(req.getInputStream());
        ItemCreateCommand itemCreateCommand = read(parser);
        app.createItem(itemCreateCommand);
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8).useDefaultPrettyPrinter();
        generator.writeStartObject();
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private ItemCreateCommand read(JsonParser parser) throws IOException {
        Name name = Name.EMPTY;
        MadeIn madeIn = MadeIn.UNKNOWN;
        Grade grade = Grade.QUALIFIED;
        Specification spec = Specification.UNDEFINED;
        String brandId = Brand.UNDEFINED.id(), categoryId = Category.UNDEFINED.id();
        Barcode barcode = null;
        LastReceiptPrice lastReceiptPrice = LastReceiptPrice.RMB_ZERO;
        RetailPrice retailPrice = RetailPrice.RMB_ZERO;
        MemberPrice memberPrice = MemberPrice.RMB_ZERO;
        VipPrice vipPrice = VipPrice.RMB_ZERO;
        while (!parser.isClosed()) {
            if (parser.nextToken() == JsonToken.FIELD_NAME) {
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
    private String readId(JsonParser parser) throws IOException {
        String id = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "id":
                        id = parser.getValueAsString();
                        break;
                }
            }
        }
        return id;
    }

    private void validate(String name, String alias) {
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            Command itemDeleteCommand = new ItemDeleteCommand(id);
            app.deleteItem(itemDeleteCommand);
        }
    }
}
