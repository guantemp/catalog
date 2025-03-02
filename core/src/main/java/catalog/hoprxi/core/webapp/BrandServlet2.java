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

package catalog.hoprxi.core.webapp;

import catalog.hoprxi.core.application.BrandAppService;
import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.BrandJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESBrandJsonQuery;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.SortField;
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
import java.util.List;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-11-26
 */

@WebServlet(urlPatterns = {"v2/brands/*"}, name = "brands", asyncSupported = true, initParams = {@WebInitParam(name = "queryImpl", value = "es")})
public class BrandServlet2 extends HttpServlet {
    private static final int OFFSET = 0;
    private static final int LIMIT = 64;
    private final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private final BrandJsonQuery jsonQuery = new ESBrandJsonQuery();
    private final BrandAppService appService = new BrandAppService();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config != null) {
            String queryImpl = config.getInitParameter("queryImpl");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8);
        boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
        if (pretty) generator.useDefaultPrettyPrinter();
        if (pathInfo != null) {
            long id = NumberHelper.longOf(pathInfo.substring(1), 0l);
            try {
                String result = jsonQuery.query(id);
                copyRaw(generator, result);
            } catch (QueryException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                generator.writeStartObject();
                generator.writeStringField("status", "miss");
                generator.writeNumberField("code", 30101);
                generator.writeStringField("message", String.format("No brand with id=%d found", id));
                generator.writeEndObject();
            }
        } else {//query by name
            String name = Optional.ofNullable(req.getParameter("name")).orElse("");
            String searchAfter = Optional.ofNullable(req.getParameter("searchAfter")).orElse("");
            int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
            int limit = NumberHelper.intOf(req.getParameter("limit"), LIMIT);
            String sort = Optional.ofNullable(req.getParameter("sort")).orElse("");
            SortField sortField = SortField.of(sort);
            //System.out.println(sortField);
            if (name.isEmpty()) {
                if (searchAfter.isEmpty()) {
                    copyRaw(generator, jsonQuery.query(offset, limit, sortField));
                } else {
                    copyRaw(generator, jsonQuery.query(limit, searchAfter, sortField));
                }
            } else {
                copyRaw(generator, jsonQuery.query(name, offset, limit, sortField));
            }
        }
        generator.close();
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
        JsonParser parser = JSON_FACTORY.createParser(req.getInputStream());
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
        JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8);
        boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
        if (pretty) generator.useDefaultPrettyPrinter();
        resp.setContentType("application/json; charset=UTF-8");
        responseBrand(generator, brand);
        generator.close();
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
            generator.writeObjectFieldStart("about");
            generator.writeStringField("homepage", brand.about().homepage().toExternalForm());
            generator.writeStringField("logo", brand.about().logo().toExternalForm());
            generator.writeNumberField("since", brand.about().since().getValue());
            generator.writeStringField("story", brand.about().story());
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
            JsonParser parser = JSON_FACTORY.createParser(req.getInputStream());
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
                commands.add(new BrandRenameCommand(id, name, alias));
            if (story != null || homepage != null || logo != null || since != null)
                commands.add(new BrandChangeAboutCommand(id, logo, homepage, since, story));
            appService.handle(commands);
            resp.setContentType("application/json; charset=UTF-8");
            JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
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
            long id = NumberHelper.longOf(parameters[1]);
            BrandDeleteCommand command = new BrandDeleteCommand(id);
            appService.delete(command);
        }
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        generator.writeStringField("status", "success");
        generator.writeNumberField("code", 30103);
        generator.writeStringField("message", "brand remove success");
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }
}
