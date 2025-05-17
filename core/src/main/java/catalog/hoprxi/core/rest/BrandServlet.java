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

import catalog.hoprxi.core.application.BrandAppService;
import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.BrandJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESBrandJsonQuery;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-05-10
 */

@WebServlet(urlPatterns = {"v1/brands/*"}, name = "brands", asyncSupported = true, initParams = {@WebInitParam(name = "query", value = "es")})
public class BrandServlet extends HttpServlet {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final EnumSet<SortField> SUPPORT_SORT_FIELD = EnumSet.of(SortField._ID, SortField.ID, SortField.NAME, SortField._NAME);
    private static final BrandJsonQuery QUERY = new ESBrandJsonQuery();
    private static final BrandAppService APP = new BrandAppService();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config != null) {
            String query = config.getInitParameter("queryImpl");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            if (pathInfo != null) {
                String path = pathInfo.substring(1);
                try {
                    long id = Long.parseLong(path);
                    String result = QUERY.query(id);
                    copyRaw(generator, result);
                } catch (QueryException e) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    generator.writeStartObject();
                    generator.writeStringField("status", "miss");
                    generator.writeNumberField("code", 201002);
                    generator.writeStringField("message", String.format("No brand with id %s was found", path));
                    generator.writeEndObject();
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    generator.writeStartObject();
                    generator.writeStringField("status", "Error RESTful path");
                    generator.writeNumberField("code", 201001);
                    generator.writeStringField("message", String.format("The id(%s) value needs to be a long integer", path));
                    generator.writeEndObject();
                }
            } else {//query by name
                String query = Optional.ofNullable(req.getParameter("q")).orElse("");
                String cursor = Optional.ofNullable(req.getParameter("cursor")).orElse("");
                int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
                int size = NumberHelper.intOf(req.getParameter("size"), SIZE);
                String sort = Optional.ofNullable(req.getParameter("sort")).orElse("");
                SortField sortField = SortField.of(sort);
                if (!SUPPORT_SORT_FIELD.contains(sortField)) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    generator.writeStartObject();
                    generator.writeStringField("status", "Error sort filed");
                    generator.writeNumberField("code", 201003);
                    generator.writeStringField("message", String.format("The filed(%s) not support", sortField.name()));
                    generator.writeEndObject();
                } else {
                    resp.addHeader("Link", "https://www.hoprxi.com/core/v1/brands?cursor=;rel=prev");
                    resp.addHeader("Link", "https://www.hoprxi.com/core/v1/brands;rel=next;");
                    if (cursor.isEmpty()) {
                        copyRaw(generator, this.QUERY.query(query, offset, size, sortField));
                    } else {
                        copyRaw(generator, this.QUERY.query(query, size, cursor, sortField));
                    }
                }
            }
        }
    }

    private void copyRaw(JsonGenerator generator, String result) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(result);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = null, alias = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
        try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
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
                            homepage = new URL(parser.getValueAsString());
                            break;
                        case "logo":
                            logo = new URL(parser.getValueAsString());
                            break;
                        case "since":
                            since = Year.of(parser.getIntValue());
                            break;
                    }
                }
            }
            BrandCreateCommand brandCreateCommand = new BrandCreateCommand(name, alias, homepage, logo, since, story);
            Brand brand = APP.createBrand(brandCreateCommand);
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            resp.setContentType("application/json; charset=UTF-8");
            generator.writeStartObject();
            generator.writeStringField("status", "success");
            generator.writeNumberField("code", 2010010);
            generator.writeStringField("message", "brand is created");
            responseBrand(generator, brand);
            generator.writeEndObject();
        }
    }

    private void responseBrand(JsonGenerator generator, Brand brand) throws IOException {
        generator.writeObjectFieldStart("brand");
        generator.writeNumberField("id", brand.id());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", brand.name().name());
        generator.writeStringField("mnemonic", brand.name().mnemonic());
        generator.writeStringField("alias", brand.name().alias());
        generator.writeEndObject();
        if (brand.about() != null) {
            AboutBrand about = brand.about();
            generator.writeObjectFieldStart("about");
            generator.writeStringField("homepage", about.homepage() == null ? "" : about.homepage().toExternalForm());
            generator.writeStringField("logo", about.logo() == null ? "" : about.logo().toExternalForm());
            generator.writeNumberField("since", about.since() == null ? -1 : about.since().getValue());
            generator.writeStringField("story", about.story() == null ? "" : about.story());
            generator.writeEndObject();
        }
        generator.writeEndObject();
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            long id = NumberHelper.longOf(pathInfo.substring(1));
            String name = null, alias = null, story = null;
            URL logo = null, homepage = null;
            Year since = null;
            try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME) {
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
                                homepage = new URL(parser.getValueAsString());
                                break;
                            case "logo":
                                logo = new URL(parser.getValueAsString());
                                break;
                            case "since":
                                since = Year.of(parser.getIntValue());
                                break;
                        }
                    }
                }
                List<Command> commands = new ArrayList<>();
                if (name != null || alias != null)
                    commands.add(new BrandRenameCommand(id, name, alias));
                if (story != null || homepage != null || logo != null || since != null)
                    commands.add(new BrandChangeAboutCommand(id, logo, homepage, since, story));
                APP.handle(commands);
                boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
                if (pretty) generator.useDefaultPrettyPrinter();
                resp.setContentType("application/json; charset=UTF-8");
                generator.writeStartObject();
                generator.writeStringField("status", "success");
                generator.writeNumberField("code", 2010020);
                generator.writeStringField("message", "brand update success");
                generator.writeFieldName("brand");
                copyRaw(generator, QUERY.query(id));
                generator.writeEndObject();
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            long id = NumberHelper.longOf(pathInfo.substring(1));
            BrandDeleteCommand command = new BrandDeleteCommand(id);
            APP.delete(command);
        }
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        generator.writeStringField("status", "success");
        generator.writeNumberField("code", 2010030);
        generator.writeStringField("message", "brand remove success");
        generator.writeEndObject();
        generator.close();
    }
}
