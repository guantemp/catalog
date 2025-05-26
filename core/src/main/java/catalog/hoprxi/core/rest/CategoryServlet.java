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

import catalog.hoprxi.core.application.CategoryAppService;
import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.CategoryJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
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
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-03-03
 */
@WebServlet(urlPatterns = {"v1/categories/*"}, name = "categories", asyncSupported = true,
        initParams = {@WebInitParam(name = "query", value = "es")})
public class CategoryServlet extends HttpServlet {
    private static final Pattern URI_REGEX = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    private final CategoryAppService app = new CategoryAppService();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final CategoryJsonQuery QUERY = new ESCategoryJsonQuery();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config != null) {
            String aQuery = config.getInitParameter("query");
            switch (aQuery) {
                case "es":
                case "ES":
                default:
                    //query = new ESCategoryJsonQuery();
                    break;
            }
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
                if (paths.length == 2) {
                    try {
                        String result = QUERY.query(Long.parseLong(paths[1]));
                        copy(generator, result);
                    } catch (QueryException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "miss");
                        generator.writeNumberField("code", 202001);
                        generator.writeStringField("message", String.format("No category with id %s was found", paths[1]));
                        generator.writeEndObject();
                    } catch (NumberFormatException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "Error id value");
                        generator.writeNumberField("code", 202002);
                        generator.writeStringField("message", String.format("The id(%s) value needs to be a long integer", paths[1]));
                        generator.writeEndObject();
                    }
                } else if (paths.length > 2 && !paths[2].isEmpty()) {
                    long id = Long.parseLong(paths[1]);
                    switch (paths[2]) {
                        case "path":
                            copy(generator, QUERY.path(id));
                            break;
                        case "children":
                        case "ch":
                            copy(generator, QUERY.queryChildren(id));
                            break;
                        case "descendants":
                        case "descs":
                            copy(generator, QUERY.queryDescendant(id));
                            break;
                        default:
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            generator.writeStartObject();
                            generator.writeStringField("status", "Invalid RESTful keyword");
                            generator.writeNumberField("code", 202003);
                            generator.writeStringField("message", String.format("Invalid RESTful keyword %s,Support usage : path,ch(children),descs(descendants)", paths[2]));
                            generator.writeEndObject();
                            break;
                    }
                }
            } else {
                String query_keyword = Optional.ofNullable(req.getParameter("q")).orElse("");
                if (query_keyword.isEmpty()) {
                    copy(generator, QUERY.root());
                } else {
                    copy(generator, QUERY.queryByName(query_keyword));
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            String name = null, alias = null, description = null, icon = "";
            long parentId = 0;
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
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
            /*valid param
            if(!validate(generator,false,parentId,name,alias,description,uri)){
                return;
            }
            */
            CategoryCreateCommand command = new CategoryCreateCommand(parentId, name, alias, description, URI.create(icon));
            resp.setContentType("application/json; charset=UTF-8");
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            try {
                Category category = app.create(command);
                generator.writeStartObject();
                generator.writeStringField("status", "success");
                generator.writeNumberField("code", 2020010);
                generator.writeStringField("message", "category is created");
                responseCategory(generator, category);
                generator.writeEndObject();
            } catch (InvalidCategoryIdException e) {
                generator.writeStartObject();
                generator.writeStringField("status", "FAIL");
                generator.writeNumberField("code", 2020011);
                generator.writeStringField("message", "ParenId is not valid");
                generator.writeEndObject();
            } catch (PersistenceException e) {
                generator.writeStringField("status", "FAIL");
                generator.writeNumberField("code", 2020012);
                generator.writeStringField("message", "Do nothing");
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(req.getInputStream()); JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            String name = null, alias = null, description = null, icon = "";
            long parentId = Long.MIN_VALUE, id = 0;
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
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
            if (parentId != Long.MIN_VALUE)
                commands.add(new CategoryMoveNodeCommand(id, parentId));
            if (name != null || alias != null)
                commands.add(new CategoryRenameCommand(id, name, alias));
            if (description != null)
                commands.add(new CategoryChangeDescriptionCommand(id, description));
            if (icon != null)
                commands.add(new CategoryChangeIconCommand(id, URI.create(icon)));
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            resp.setContentType("application/json; charset=UTF-8");
            try {
                Category category = app.update(id, commands);
                generator.writeStartObject();
                generator.writeStringField("status", "success");
                generator.writeNumberField("code", 2020020);
                generator.writeStringField("message", "Category has been modified");
                responseCategory(generator, category);
                generator.writeEndObject();
            } catch (InvalidCategoryIdException e) {
                generator.writeStartObject();
                generator.writeStringField("status", "FAIL");
                generator.writeNumberField("code", 2020021);
                generator.writeStringField("message", "ParenId is not valid");
                generator.writeEndObject();
            } catch (PersistenceException e) {
                generator.writeStringField("status", "FAIL");
                generator.writeNumberField("code", 2020022);
                generator.writeStringField("message", "Do nothing");
            }
        }
    }

    private boolean validate(JsonGenerator generator, boolean root, String parentId, String name, String alias, String description, String uri) throws IOException {
        boolean result = true;
        if (!root && (parentId == null || parentId.isEmpty())) {
            generator.writeStartObject();
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_01");
            generator.writeStringField("message", "ParenId is required");
            generator.writeEndObject();
            result = false;
        }
        if (name == null || name.isEmpty()) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_02");
            generator.writeStringField("message", "name is required");
        }
        if (alias != null && alias.length() > 256) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_03");
            generator.writeStringField("message", "alias length rang is 1-256");
        }
        if (description != null && description.length() > 512) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_04");
            generator.writeStringField("message", "description length rang is 1-512");
        }
        if (uri != null && !URI_REGEX.matcher(uri).matches()) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_05");
            generator.writeStringField("message", "description length rang is 1-512");
        }
        return result;
    }

    private void responseCategory(JsonGenerator generator, Category category) throws IOException {
        generator.writeObjectFieldStart("category");
        generator.writeNumberField("id", category.id());
        generator.writeNumberField("parentId", category.parentId());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", category.name().name());
        generator.writeStringField("mnemonic", category.name().mnemonic());
        generator.writeStringField("alias", category.name().alias());
        generator.writeEndObject();
        if (category.description() != null)
            generator.writeStringField("description", category.description());
        //System.out.println(view.getIcon().toASCIIString());
        if (category.icon() != null)
            generator.writeStringField("icon", category.icon().toString());
        generator.writeEndObject();
    }


    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            resp.setContentType("application/json; charset=UTF-8");
            String pathInfo = req.getPathInfo();
            if (pathInfo != null) {
                String[] parameters = pathInfo.split("/");
                if (parameters.length == 2) {
                    try {
                        app.delete(new CategoryDeleteCommand(Long.parseLong(parameters[1])));
                        generator.writeStartObject();
                        generator.writeNumberField("code", 2020030);
                        generator.writeStringField("status", "success");
                        generator.writeStringField("message", "category and Descendant removed");
                        generator.writeEndObject();
                    } catch (PersistenceException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "miss");
                        generator.writeNumberField("code", 2020031);
                        generator.writeStringField("message", "Category not found");
                        generator.writeEndObject();
                    } catch (NumberFormatException e) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        generator.writeStartObject();
                        generator.writeStringField("status", "error");
                        generator.writeNumberField("code", 2020032);
                        generator.writeStringField("message", "The ID value is of type long");
                        generator.writeEndObject();
                    }
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                generator.writeStartObject();
                generator.writeStringField("status", "FAIL");
                generator.writeNumberField("code", 2020033);
                generator.writeStringField("message", "Do nothing");
                generator.writeEndObject();
            }
        }
    }

}
