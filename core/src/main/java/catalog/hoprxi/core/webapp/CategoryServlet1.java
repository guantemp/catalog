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
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;
import catalog.hoprxi.core.infrastructure.query.ArangoDBCategoryQueryService;
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
 * @version 0.0.1 builder 2021-09-13
 */
@WebServlet(urlPatterns = {"v1/categories1/*"}, name = "categories1", asyncSupported = false)
public class CategoryServlet1 extends HttpServlet {
    private final CategoryRepository categoryRepository = new ArangoDBCategoryRepository("catalog");
    private final CategoryQueryService categoryQueryService = new ArangoDBCategoryQueryService("catalog");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        //String[] fileds = req.getParameter("fileds").split(",");
        long start = System.currentTimeMillis();
        resp.setContentType("application/json; charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        if (pathInfo != null) {
            String[] parameters = pathInfo.split("/");
            Category category = categoryRepository.find(parameters[1]);
            if (category != null) {
                if (parameters.length > 2 && parameters[2] != null) {
                    switch (parameters[2]) {
                        case "parent":
                            category = categoryRepository.find(category.parentId());
                            responseCategory(generator, category);
                            break;
                        case "children":
                            generator.writeArrayFieldStart("categories");
                            responseChildren(generator, category);
                            generator.writeEndArray();
                            break;
                        case "descendants":
                            generator.writeArrayFieldStart("categories");
                            responseDescendants(generator, category);
                            generator.writeEndArray();
                            break;
                    }
                } else {
                    responseCategory(generator, category);
                }
            } else {
                generator.writeNumberField("code", 204);
                generator.writeStringField("message", "Not find");
            }
        } else {
            Category[] roots = categoryRepository.root();
            generator.writeArrayFieldStart("categories");
            for (Category root : roots) {
                responseChildren(generator, root);
            }
            generator.writeEndArray();
        }
        generator.writeNumberField("execution time", System.currentTimeMillis() - start);
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    private void responseDescendants(JsonGenerator generator, Category category) {
    }

    private void responseChildren(JsonGenerator generator, Category category) throws IOException {
        generator.writeStartObject();
        responseCategory(generator, category);
        Category[] children = categoryRepository.belongTo(category.id());
        if (children.length > 1) {
            generator.writeArrayFieldStart("children");
            for (Category child : children) {
                this.responseChildren(generator, child);
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
    }

    private void responseCategory(JsonGenerator generator, Category category) throws IOException {
        generator.writeStringField("id", category.id());
        generator.writeStringField("parentId", category.parentId());
        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", category.name().name());
        generator.writeStringField("mnemonic", category.name().mnemonic());
        generator.writeStringField("alias", category.name().alias());
        generator.writeEndObject();
        generator.writeStringField("description", category.description());
        if (category.icon() != null)
            generator.writeStringField("icon", category.icon().getFragment());
    }
}
