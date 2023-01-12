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

import catalog.hoprxi.core.application.CategoryAppService;
import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import catalog.hoprxi.core.infrastructure.query.ArangoDBCategoryQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlCategoryQueryService;
import com.fasterxml.jackson.core.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-04-18
 */
@WebServlet(urlPatterns = {"v1/categories/*"}, name = "categories", asyncSupported = true, initParams = {
        @WebInitParam(name = "provider", value = "arangodb"), @WebInitParam(name = "database", value = "catalog")})
public class CategoryServlet extends HttpServlet {
    private static final Pattern URI_REGEX = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    private static final CategoryAppService APP_SERVICE = new CategoryAppService();
    private CategoryQueryService categoryQueryService;
    private final JsonFactory jasonFactory = JsonFactory.builder().build();

    @Override
    public void init(ServletConfig config) {
        if (config != null) {
            String database = config.getInitParameter("database");
        }
        Config conf = ConfigFactory.load("database");
        String provider = conf.hasPath("provider") ? conf.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
                categoryQueryService = new PsqlCategoryQueryService("catalog");
                break;
            case "arangodb":
                categoryQueryService = new ArangoDBCategoryQueryService("catalog");
                break;
        }
        categoryQueryService.root();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //String[] fields = req.getParameter("fields").split(",");
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8).useDefaultPrettyPrinter();
        generator.writeStartObject();
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String[] parameters = pathInfo.split("/");
            if (parameters.length == 2) {
                if (parameters[1].equals("_search")) {//search
                    System.out.println(12);
                } else {
                    CategoryView view = categoryQueryService.find(parameters[1]);
                    if (view != null) {
                        responseCategoryView(generator, view);
                    } else {//not find
                        responseNotFind(resp, generator, parameters[1]);
                    }
                }
            } else if (parameters.length > 2 && parameters[2] != null) {
                switch (parameters[2]) {
                    case "depth":
                        int depth = categoryQueryService.depth(parameters[1]);
                        generator.writeNumberField("depth", depth);
                        break;
                    case "path":
                        CategoryView[] path = categoryQueryService.path(parameters[1]);
                        for (CategoryView p : path) {
                            responseCategoryView(generator, p);
                        }
                        break;
                    case "children":
                        CategoryView parent = categoryQueryService.find(parameters[1]);
                        if (parent == null)
                            responseNotFind(resp, generator, parameters[1]);
                        else {
                            responseChildren(generator, parent);
                        }
                        break;
                    case "descendants":
                        CategoryView senior = categoryQueryService.find(parameters[1]);
                        if (senior == null)
                            responseNotFind(resp, generator, parameters[1]);
                        else {
                            responseDescendants(generator, senior);
                        }
                        break;
                }
            }
        } else {//all category
            CategoryView[] roots = categoryQueryService.root();
            generator.writeArrayFieldStart("categories");
            for (CategoryView root : roots) {
                generator.writeStartObject();
                responseDescendants(generator, root);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private void responseNotFind(HttpServletResponse resp, JsonGenerator generator, String id) throws IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        generator.writeNumberField("code", 1001);
        generator.writeStringField("message", "Not find category(id=" + id + ")");
    }

    private void responseCategoryView(JsonGenerator generator, CategoryView view) throws IOException {
        generator.writeStringField("id", view.getId());
        generator.writeStringField("parentId", view.getParentId());
        generator.writeBooleanField("isLeaf", view.isLeaf());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", view.getName().name());
        generator.writeStringField("mnemonic", view.getName().mnemonic());
        generator.writeStringField("alias", view.getName().alias());
        generator.writeEndObject();
        if (view.getDescription() != null)
            generator.writeStringField("description", view.getDescription());
        //System.out.println(view.getIcon().toASCIIString());
        if (view.getIcon() != null)
            generator.writeStringField("icon", view.getIcon().toString());
    }

    private void responseChildren(JsonGenerator generator, CategoryView view) throws IOException {
        responseCategoryView(generator, view);
        CategoryView[] children = categoryQueryService.children(view.getId());
        if (children.length >= 1) {
            generator.writeArrayFieldStart("children");
            for (CategoryView child : children) {
                generator.writeStartObject();
                responseCategoryView(generator, child);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
    }

    private void responseDescendants(JsonGenerator generator, CategoryView view) throws IOException {
        responseCategoryView(generator, view);
        CategoryView[] children = categoryQueryService.children(view.getId());
        if (children.length >= 1) {
            generator.writeArrayFieldStart("children");
            for (CategoryView child : children) {
                generator.writeStartObject();
                responseDescendants(generator, child);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = null, alias = null, description = null, icon = null, parentId = null;
        JsonParser parser = jasonFactory.createParser(req.getInputStream());
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "parentId":
                        parentId = parser.getValueAsString();
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
        //valid
        CategoryCreateCommand command = new CategoryCreateCommand(parentId, name, alias, description, URI.create(icon));
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8).useDefaultPrettyPrinter();
        try {
            CategoryView view = APP_SERVICE.create(command);
            generator.writeStartObject();
            if (view != null) {
                responseCategoryView(generator, view);
            } else {
                generator.writeStringField("status", "FAIL");
                generator.writeStringField("code", "10_05_01");
                generator.writeStringField("message", "Do nothing");
            }
            generator.writeEndObject();
        } catch (InvalidCategoryIdException e) {
            generator.writeStartObject();
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_01");
            generator.writeStringField("message", "ParenId is not valid");
            generator.writeEndObject();
        }
        generator.flush();
        generator.close();
    }

    private boolean validParentId(JsonGenerator generator, boolean root, String parentId) throws IOException {
        if (!root && (parentId == null || parentId.isEmpty())) {
            generator.writeStartObject();
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_01");
            generator.writeStringField("message", "ParenId is required");
            generator.writeEndObject();
            return false;
        }
        return true;
    }

    private boolean validName(JsonGenerator generator, String name) throws IOException {
        if (name == null || name.isEmpty()) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_02");
            generator.writeStringField("message", "name is required");
            return false;
        }
        return true;
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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long start = System.currentTimeMillis();
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8).useDefaultPrettyPrinter();
        generator.writeStartObject();
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String[] parameters = pathInfo.split("/");
            if (parameters.length == 2) {
                APP_SERVICE.delete(new CategoryDeleteCommand(parameters[1]));
                generator.writeNumberField("code", 1004);
                generator.writeStringField("message", "Success delete category");
            }
        }
        generator.writeNumberField("execution time", System.currentTimeMillis() - start);
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = null, alias = null, description = null, icon = null, parentId = null, id = null;
        JsonParser parser = jasonFactory.createParser(req.getInputStream());
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "id":
                        id = parser.getValueAsString();
                        break;
                    case "parentId":
                        parentId = parser.getValueAsString();
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
        if (parentId != null)
            commands.add(new CategoryMoveNodeCommand(id, parentId));
        if (name != null || alias != null)
            commands.add(new CategoryRenameCommand(id, name, alias));
        if (description != null)
            commands.add(new CategoryChangeDescriptionCommand(id, description));
        if (icon != null)
            commands.add(new CategoryChangeIconCommand(id, URI.create(icon)));
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8).useDefaultPrettyPrinter();
        try {
            CategoryView view = APP_SERVICE.update(id, commands);
            generator.writeStartObject();
            if (view != null) {
                responseCategoryView(generator, view);
            } else {
                generator.writeStringField("status", "FAIL");
                generator.writeStringField("code", "10_05_03");
                generator.writeStringField("message", "Do nothing");
            }
            generator.writeEndObject();
        } catch (InvalidCategoryIdException e) {
            generator.writeStartObject();
            responseNotFind(resp, generator, id);
            generator.writeEndObject();
        }
        generator.flush();
        generator.close();
    }
}
