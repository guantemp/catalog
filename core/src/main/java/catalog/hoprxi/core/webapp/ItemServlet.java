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

import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBItemRepository;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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
    private ItemRepository repository;

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (config != null) {
            String database = config.getInitParameter("database");
            String databaseName = config.getInitParameter("databaseName");
            repository = new ArangoDBItemRepository(databaseName);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            Item item = repository.find(id);
            if (item != null) {
                responseItem(generator, item);
            } else {
                //resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                generator.writeNumberField("code", 204);
                generator.writeStringField("message", "Not find");
            }
        } else {
            int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
            int limit = NumberHelper.intOf(req.getParameter("limit"), LIMIT);
            generator.writeNumberField("total", repository.size());
            generator.writeNumberField("offset", offset);
            generator.writeNumberField("limit", limit);
            Item[] items = repository.findAll(offset, limit);
            responseItems(generator, items);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private void responseItem(JsonGenerator generator, Item item) throws IOException {
        generator.writeStringField("id", item.id());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", item.name().name());
        generator.writeStringField("mnemonic", item.name().mnemonic());
        generator.writeStringField("alias", item.name().alias());
        generator.writeEndObject();
        generator.writeStringField("barcode", (String) item.barcode().barcode());
        generator.writeObjectField("grade", item.grade().toString());
        generator.writeObjectField("spec", item.spec().value());
        generator.writeObjectFieldStart("madeIn");
        generator.writeNumberField("code", item.madeIn().code());
        generator.writeStringField("madeIn", item.madeIn().madeIn());
        generator.writeEndObject();
        generator.writeObjectFieldStart("retailPrice");
        Price price = item.retailPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        generator.writeObjectFieldStart("memberPrice");
        price = item.memberPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
        generator.writeObjectFieldStart("vipPrice");
        price = item.vipPrice().price();
        generator.writeStringField("amount", MONETARY_AMOUNT_FORMAT.format(price.amount()));
        generator.writeStringField("unit", price.unit().toString());
        generator.writeEndObject();
    }

    private void responseItems(JsonGenerator generator, Item[] items) throws IOException {
        generator.writeArrayFieldStart("items");
        for (Item item : items) {
            generator.writeStartObject();
            responseItem(generator, item);
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }
}
