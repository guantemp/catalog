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

import catalog.hoprxi.core.application.CategoryAppService;
import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.CategoryJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESCategoryJsonQuery;
import com.fasterxml.jackson.core.*;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-03-03
 */
@WebServlet(urlPatterns = {"v2/categories/*"}, name = "categories", asyncSupported = true,
        initParams = {@WebInitParam(name = "query", value = "es")})
public class CategoryServlet2 extends HttpServlet {
    private static final CategoryAppService APP = new CategoryAppService();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private CategoryJsonQuery query;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config != null) {
            String aQuery = config.getInitParameter("query");
            switch (aQuery) {
                case "es":
                case "ES":
                default:
                    query = new ESCategoryJsonQuery();
                    break;
            }
        }
        //query.root();
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
                if (paths.length == 2) {
                    try {
                        long id = Long.parseLong(paths[1]);
                        System.out.println(id);
                        String result = query.query(id);
                        System.out.println(result);
                        copy(generator, result);
                    } catch (QueryException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "miss");
                        generator.writeNumberField("code", 30202);
                        generator.writeStringField("message", String.format("No category with id(%s) found", paths[1]));
                        generator.writeEndObject();
                    } catch (NumberFormatException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "Error RESTful path");
                        generator.writeNumberField("code", 30201);
                        generator.writeStringField("message", String.format("RESTful path(%s), not long type", paths[1]));
                        generator.writeEndObject();
                    }
                } else if (paths.length > 2 && !paths[2].isEmpty()) {
                    long id = Long.parseLong(paths[1]);
                    switch (paths[2]) {
                        case "path":
                            copy(generator, query.path(id));
                            break;
                        case "children":
                            copy(generator, query.queryChildren(id));
                            break;
                        case "descendants":
                        case "DSC":
                            copy(generator, query.queryDescendant(id));
                            break;
                        default:
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            generator.writeStartObject();
                            generator.writeStringField("status", "Invalid RESTful request path");
                            generator.writeNumberField("code", 30203);
                            generator.writeStringField("message", String.format("Invalid RESTful request path(%s)", pathInfo));
                            generator.writeEndObject();
                            break;
                    }
                }
            } else {
                String query_keyword = Optional.ofNullable(req.getParameter("query")).orElse("");
                //System.out.println(sortField);
                if (query_keyword.isEmpty()) {
                    copy(generator, query.root());
                } else {
                    copy(generator, query.queryByName(query_keyword));
                }
            }
        }
    }

    private void copy(JsonGenerator generator, String result) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(result);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            long start = System.currentTimeMillis();
            resp.setContentType("application/json; charset=UTF-8");
            generator.writeStartObject();
            String pathInfo = req.getPathInfo();
            if (pathInfo != null) {
                String[] parameters = pathInfo.split("/");
                if (parameters.length == 2) {
                    APP.delete(new CategoryDeleteCommand(Long.parseLong(parameters[1])));
                    generator.writeNumberField("code", 1004);
                    generator.writeStringField("message", "Success remove category");
                }
            }
            generator.writeNumberField("execution time", System.currentTimeMillis() - start);
            generator.writeEndObject();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            String name = null, alias = null, description = null, icon = null;
            long parentId = 0, id = 0;
            while (parser.nextToken() != null) {
                JsonToken jsonToken = parser.currentToken();
                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "id":
                            id = parser.getValueAsLong();
                            break;
                        case "parentId":
                            parentId = parser.getValueAsLong();
                            break;
                        case "name":
                            name = parser.getValueAsString();
                            break;
                        case "alias":
                            alias = parser.getValueAsString();
                            break;
                        case "description":
                            description = parser.getValueAsString();
                            break;
                        case "icon":
                            icon = parser.getValueAsString();
                            break;
                    }
                }
            }
            List<Command> commands = new ArrayList<>();
            commands.add(new CategoryMoveNodeCommand(id, parentId));
            if (name != null || alias != null)
                commands.add(new CategoryRenameCommand(id, name, alias));
            if (description != null)
                commands.add(new CategoryChangeDescriptionCommand(id, description));
            if (icon != null)
                commands.add(new CategoryChangeIconCommand(id, URI.create(icon)));

            resp.setContentType("application/json; charset=UTF-8");

            CategoryView view = APP.update(id, commands);
            generator.writeStartObject();
            if (view != null) {
                //responseCategoryView(generator, view);
            } else {
                generator.writeStringField("status", "FAIL");
                generator.writeStringField("code", "10_05_03");
                generator.writeStringField("message", "Do nothing");
            }
            generator.writeEndObject();
        } catch (InvalidCategoryIdException e) {
            //generator.writeStartObject();
            //responseNotFind(resp, generator, id);
            //generator.writeEndObject();
        }
    }
}
