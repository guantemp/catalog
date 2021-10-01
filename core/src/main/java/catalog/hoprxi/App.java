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

package catalog.hoprxi;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.webapp.BrandServlet;
import catalog.hoprxi.core.webapp.CategoryServlet;
import catalog.hoprxi.core.webapp.ItemServlet;
import catalog.hoprxi.core.webapp.UnitServlet;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Locale;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException, ServletException {
        CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤ #,##0.00###")//"#,##0.00### ¤"
                .build());
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(19.55419, currency), Unit.DAI));
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(System.out, JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        generator.writeNumberField("offset", 0);
        generator.writeNumberField("limit", 15);
        generator.writeNumberField("total", Unit.values().length);
        generator.writeArrayFieldStart("units");
        for (Unit unit : Unit.values()) {
            generator.writeString(unit.toString());
        }
        generator.writeEndArray();
        generator.writeObjectField("grade", Grade.QUALIFIED.toString());
        generator.writeObjectFieldStart("retailPrice");
        generator.writeStringField("value", format.format(retailPrice.price().amount()));
        generator.writeStringField("unit", retailPrice.price().unit().toString());
        generator.writeEndObject();
        generator.writeStringField("retailPrice", format.format(retailPrice.price().amount()) + "/" + retailPrice.price().unit().toString());
        generator.writeEndObject();
        generator.flush();
        generator.close();

        ServletContainer container = ServletContainer.Factory.newInstance();
        DeploymentInfo deploymentInfo = Servlets.deployment()
                .setClassLoader(App.class.getClassLoader())
                .setContextPath("/core")
                .setDeploymentName("catalog.war")
                .addServlets(
                        Servlets.servlet("unitServlet", UnitServlet.class)
                                .addMapping("/v1/units"),
                        Servlets.servlet("brandServlet", BrandServlet.class)
                                .addMapping("/v1/brands/*"),
                        Servlets.servlet("categoryServlet", CategoryServlet.class)
                                .addMapping("/v1/categories/*"),
                        Servlets.servlet("itemServlet", ItemServlet.class)
                                .addInitParam("database", "arangodb3")
                                .addInitParam("databaseName", "catalog")
                                .addMapping("/v1/items/*"));
        DeploymentManager manager = container.addDeployment(deploymentInfo);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/core"))
                .addPrefixPath(deploymentInfo.getContextPath(), manager.start());

        Undertow server = Undertow.builder()
                .addHttpListener(80, "0.0.0.0")
                .setHandler(path)
                .build();
        server.start();
    }
}
