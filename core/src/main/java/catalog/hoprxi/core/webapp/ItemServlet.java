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

import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBItemRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlItemRepository;
import catalog.hoprxi.core.infrastructure.query.ArangoDBItemQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import com.fasterxml.jackson.core.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.javamoney.moneta.format.CurrencyStyle;
import salt.hoprxi.utils.NumberHelper;

import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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
    private static final int LIMIT = 50;
    private static final String PRE_SUFFIX = ".*?";
    private ItemQueryService queryService;
    private ItemRepository repository;
    private final JsonFactory jasonFactory = JsonFactory.builder().build();

    @Override
    public void init(ServletConfig config) {
        if (config != null) {
            String database = config.getInitParameter("database");
            System.out.println(database);
            String databaseName = config.getInitParameter("databaseName");
        }
        Config conf = ConfigFactory.load("database");
        String provider = conf.hasPath("provider") ? conf.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
                repository = new PsqlItemRepository("catalog");
                queryService = new PsqlItemQueryService("catalog");
                break;
            case "arangodb":
                repository = new ArangoDBItemRepository("catalog");
                queryService = new ArangoDBItemQueryService("catalog");
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
            String name = Optional.ofNullable(req.getParameter("name")).orElse("");
            String categoryId = Optional.ofNullable(req.getParameter("cid")).orElse("");
            String brandId = Optional.ofNullable(req.getParameter("bid")).orElse("");
            generator.writeNumberField("offset", offset);
            generator.writeNumberField("limit", limit);
            if (!brandId.isEmpty() || !categoryId.isEmpty()) {
                if (!brandId.isEmpty()) {
                    ItemView[] itemViews = queryService.belongToBrand(brandId, offset, limit);
                    if (!barcode.isEmpty() || !name.isEmpty()) {
                        Pattern namePattern = Pattern.compile(PRE_SUFFIX + name + PRE_SUFFIX);
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
                if (!categoryId.isEmpty()) {
                    ItemView[] itemViews = queryService.belongToCategory(categoryId, offset, limit);
                    if (!barcode.isEmpty() || !name.isEmpty()) {
                        Pattern namePattern = Pattern.compile(PRE_SUFFIX + name + PRE_SUFFIX);
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
            } else if (!barcode.isEmpty() || !name.isEmpty()) {
                Set<ItemView> itemViewSet = new HashSet<>();
                if (!barcode.isEmpty()) {
                    ItemView[] itemViews = queryService.queryByBarcode(barcode);
                    for (ItemView itemView : itemViews)
                        itemViewSet.add(itemView);
                }
                if (!name.isEmpty()) {
                    ItemView[] itemViews = queryService.serach(name);
                    itemViewSet.addAll(Arrays.asList(itemViews));
                }
                responseItemViews(generator, itemViewSet.toArray(new ItemView[0]));
            } else {
                generator.writeNumberField("total", queryService.size());
                ItemView[] itemViews = queryService.queryAll(offset, limit);
                responseItemViews(generator, itemViews);
            }
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

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
        String name = null, alias = null, barcode = null, brandId = null, categoryId = null, spec = null, grade = null, madeIn = null;
        Number retailPrice = null, memberPrice = null, vipPrice = null;
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
        validate(name, alias);
    }

    private void validate(String name, String alias) {
    }
}
