/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.BrandAppService;
import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlBrandQuery;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2022-07-10
 */
@WebServlet(urlPatterns = {"v1/brands/*"}, name = "brands", asyncSupported = true)
public class BrandServlet extends HttpServlet {
    private static final int OFFSET = 0;
    private static final int LIMIT = 64;
    private final JsonFactory jasonFactory = JsonFactory.builder().build();
    private BrandAppService appService = new BrandAppService();
    private BrandQuery query;

    public BrandServlet() {
        Config conf = ConfigFactory.load("database");
        String provider = conf.hasPath("provider") ? conf.getString("provider") : "postgresql";
        String databaseName = conf.hasPath("databaseName") ? conf.getString("databaseName") : "catalog";
        switch ((provider)) {
            case "postgresql":
            case "psql":
                query = new PsqlBrandQuery();
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            Brand brand = query.query(id);
            if (brand != null) {
                generator.writeObjectFieldStart("brand");
                responseBrand(generator, brand);
                generator.writeEndObject();
            } else {
                generator.writeStringField("status", "miss");
                generator.writeNumberField("code", 30101);
                generator.writeStringField("message", MessageFormat.format("No brand with id={0} found", id));
            }
        } else {//query all
            int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
            int limit = NumberHelper.intOf(req.getParameter("limit"), LIMIT);
            String keyword = Optional.ofNullable(req.getParameter("keyword")).orElse("");
            if (!keyword.isEmpty()) {
                Brand[] brands = query.queryByName(keyword);
                responseBrands(generator, brands);
            } else {
                generator.writeNumberField("total", query.size());
                generator.writeNumberField("offset", offset);
                generator.writeNumberField("limit", limit);
                Brand[] brands = query.queryAll(offset, limit);
                responseBrands(generator, brands);
            }
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = null, alias = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
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
                    case "story":
                        story = parser.getValueAsString();
                        break;
                    case "homepage":
                        homepage = parser.readValueAs(URL.class);
                        break;
                    case "logo":
                        logo = parser.readValueAs(URL.class);
                        break;
                    case "since":
                        since = parser.readValueAs(Year.class);
                        break;
                }
            }
        }
        BrandCreateCommand brandCreateCommand = new BrandCreateCommand(name, alias, homepage, logo, since, story);
        Brand brand = appService.createBrand(brandCreateCommand);
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        resp.setContentType("application/json; charset=UTF-8");
        generator.writeObjectFieldStart("brand");
        responseBrand(generator, brand);
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
            String name = null, alias = null, story = null;
            URL logo = null, homepage = null;
            Year since = null;
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
                        case "story":
                            story = parser.getValueAsString();
                            break;
                        case "homepage":
                            homepage = parser.readValueAs(URL.class);
                            break;
                        case "logo":
                            logo = parser.readValueAs(URL.class);
                            break;
                        case "since":
                            since = parser.readValueAs(Year.class);
                            break;
                    }
                }
            }
            List<Command> commands = new ArrayList<>();
            if (name != null || alias != null)
                commands.add(new CategoryRenameCommand(id, name, alias));
            if (story != null || homepage != null || logo != null || since != null)
                commands.add(new BrandChangeAboutCommand(id, logo, homepage, since, story));
            appService.handle(commands);
            resp.setContentType("application/json; charset=UTF-8");
            JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                    .setPrettyPrinter(new DefaultPrettyPrinter());
            generator.writeStartObject();
            generator.writeStringField("status", "success");
            generator.writeNumberField("code", 30102);
            generator.writeStringField("message", "brand handle success");
            generator.writeEndObject();
            generator.flush();
            generator.close();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String[] parameters = pathInfo.split("/");
            BrandDeleteCommand command = new BrandDeleteCommand(parameters[1]);
            appService.delete(command);
        }
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        generator.writeStringField("status", "success");
        generator.writeNumberField("code", 30103);
        generator.writeStringField("message", "brand delete success");
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private void responseBrands(JsonGenerator generator, Brand[] brands) throws IOException {
        generator.writeArrayFieldStart("brands");
        for (Brand brand : brands) {
            generator.writeStartObject();
            responseBrand(generator, brand);
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    private void responseBrand(JsonGenerator generator, Brand brand) throws IOException {
        generator.writeStringField("id", brand.id());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", brand.name().name());
        generator.writeStringField("mnemonic", brand.name().mnemonic());
        generator.writeStringField("alias", brand.name().alias());
        generator.writeEndObject();
        if (brand.about() != null) {
            generator.writeObjectFieldStart("about");
            generator.writeStringField("homepage", brand.about().homepage().toExternalForm());
            generator.writeStringField("logo", brand.about().logo().toExternalForm());
            generator.writeNumberField("since", brand.about().since().getValue());
            generator.writeStringField("story", brand.about().story());
            generator.writeEndObject();
        }
    }
}
