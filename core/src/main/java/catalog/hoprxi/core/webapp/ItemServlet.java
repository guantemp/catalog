/*
 * Copyright (c) 2021. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.infrastructure.query.ArangoDBItemQueryService;
import catalog.hoprxi.core.infrastructure.view.ItemView;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
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
import java.util.Locale;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-09-19
 */
@WebServlet(urlPatterns = {"v1/items/*"}, name = "items", asyncSupported = false, initParams = {
        @WebInitParam(name = "database", value = "arangodb3"), @WebInitParam(name = "databaseName", value = "catalog")})
public class ItemServlet extends HttpServlet {
    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.CHINA)
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());
    private static final int OFFSET = 0;
    private static final int LIMIT = 20;
    private ItemQueryService itemQueryService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (config != null) {
            String database = config.getInitParameter("database");
            String databaseName = config.getInitParameter("databaseName");
            itemQueryService = new ArangoDBItemQueryService(databaseName);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        long start = System.currentTimeMillis();
        resp.setContentType("application/json; charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            ItemView itemView = itemQueryService.find(id);
            if (itemView != null) {
                responseItemView(generator, itemView);
            } else {
                //resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                generator.writeNumberField("code", 204);
                generator.writeStringField("message", "Not find");
            }
        } else {
            int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
            int limit = NumberHelper.intOf(req.getParameter("limit"), LIMIT);
            String barcode = Optional.ofNullable(req.getParameter("barcode")).orElse("");
            String name = Optional.ofNullable(req.getParameter("name")).orElse("");
            String categoryId = Optional.ofNullable(req.getParameter("cid")).orElse("");
            String brandId = Optional.ofNullable(req.getParameter("bid")).orElse("");
            System.out.println(name);
            generator.writeNumberField("total", itemQueryService.size());
            generator.writeNumberField("offset", offset);
            generator.writeNumberField("limit", limit);
            generator.writeArrayFieldStart("items");
            if (!barcode.isEmpty() && !name.isEmpty()) {
                ItemView[] items = itemQueryService.fromBarcode(barcode);
                responseItems(generator, items);
            } else if (!barcode.isEmpty() || !name.isEmpty()) {
                if (!barcode.isEmpty()) {
                    ItemView[] items = itemQueryService.fromBarcode(barcode);
                    responseItems(generator, items);
                }
                if (!name.isEmpty()) {
                    ItemView[] items = itemQueryService.fromName(name);
                    responseItems(generator, items);
                }
            } else {
                ItemView[] items = itemQueryService.findAll(offset, limit);
                responseItems(generator, items);
            }
            generator.writeEndArray();
            generator.writeNumberField("execution time", System.currentTimeMillis() - start);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private void responseItemView(JsonGenerator generator, ItemView itemView) throws IOException {
        generator.writeStringField("id", itemView.id());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", itemView.name().name());
        generator.writeStringField("mnemonic", itemView.name().mnemonic());
        generator.writeStringField("alias", itemView.name().alias());
        generator.writeEndObject();
        generator.writeStringField("barcode", (String) itemView.barcode().barcode());
        generator.writeObjectField("spec", itemView.spec().value());
        generator.writeObjectFieldStart("madeIn");
        generator.writeNumberField("code", itemView.madeIn().code());
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
        generator.writeObjectFieldStart("retailPrice");
        Price price = itemView.retailPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        generator.writeObjectFieldStart("memberPrice");
        price = itemView.memberPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        generator.writeObjectFieldStart("vipPrice");
        price = itemView.vipPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
    }

    private void responseItems(JsonGenerator generator, ItemView[] itemViews) throws IOException {
        for (ItemView itemView : itemViews) {
            generator.writeStartObject();
            responseItemView(generator, itemView);
            generator.writeEndObject();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = null, alias = null, barcode = null, brandId = null, categoryId = null, spec = null, grade = null, madeIn = null;
        Number retailPrice = null, memberPrice = null, vipPrice = null;
        JsonFactory jasonFactory = new JsonFactory();
        JsonParser parser = jasonFactory.createParser(req.getInputStream());
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
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
    }
}
