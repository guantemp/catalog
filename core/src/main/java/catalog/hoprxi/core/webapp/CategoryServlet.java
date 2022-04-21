/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.infrastructure.query.ArangoDBCategoryQueryService;
import catalog.hoprxi.core.infrastructure.view.CategoryView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-04-18
 */
@WebServlet(urlPatterns = {"v1/categories/*"}, name = "categories", asyncSupported = false)
public class CategoryServlet extends HttpServlet {
    private final CategoryQueryService categoryQueryService = new ArangoDBCategoryQueryService("catalog");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //String[] fileds = req.getParameter("fileds").split(",");
        long start = System.currentTimeMillis();
        resp.setContentType("application/json; charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String[] parameters = pathInfo.split("/");
            if (parameters.length == 2) {
                CategoryView view = categoryQueryService.find(parameters[1]);
                if (view != null) {
                    responseCategoryView(generator, view);
                } else {//not find
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    generator.writeNumberField("code", 01204);
                    generator.writeStringField("message", "Not find category(id=" + parameters[1] + ")");
                }
            } else if (parameters.length > 2 && parameters[2] != null) {
                switch (parameters[2]) {
                    case "depth":
                        int depth = categoryQueryService.depth(parameters[1]);
                        generator.writeNumberField("depth", depth);
                        break;
                    case "path":
                        CategoryView[] path = categoryQueryService.path(parameters[1]);
                        responsePath(generator, path);
                    case "children":
                    case "descendants":
                }
            }
        } else {//all category
            CategoryView[] views = categoryQueryService.root();
            generator.writeArrayFieldStart("categories");
            for (CategoryView root : views) {
                //responseChildren(generator, root);
            }
            generator.writeEndArray();

        }
        generator.writeNumberField("execution time", System.currentTimeMillis() - start);
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private void responsePath(JsonGenerator generator, CategoryView[] views) throws IOException {
        for (CategoryView view : views) {
            responseCategoryView(generator, view);
        }
    }

    private void responseCategoryView(JsonGenerator generator, CategoryView view) throws IOException {
        generator.writeStringField("id", view.getId());
        generator.writeStringField("parentId", view.getParentId());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", view.getName().name());
        generator.writeStringField("mnemonic", view.getName().mnemonic());
        generator.writeStringField("alias", view.getName().alias());
        generator.writeEndObject();
        generator.writeStringField("description", view.getDescription());
        if (view.getIcon() != null)
            generator.writeStringField("icon", view.getIcon().getFragment());
    }

    private void responseChildren(JsonGenerator generator, CategoryView[] view) {

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }
}
