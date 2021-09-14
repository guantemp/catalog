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

import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;
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
@WebServlet(urlPatterns = {"v1/categories/*"}, name = "categories", asyncSupported = false)
public class CategoryServlet extends HttpServlet {
    private final CategoryRepository categoryRepository = new ArangoDBCategoryRepository("catalog");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        //String[] fileds = req.getParameter("fileds").split(",");
        resp.setContentType("application/json; charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        if (pathInfo != null) {
            String[] parameters = pathInfo.split("/");
            Category category = categoryRepository.find(parameters[1]);
            if (category != null) {
                responseCategory(generator, category);
            } else {

            }
        } else {

        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
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

    private void responseCategories(JsonGenerator generator, Category[] categories) throws IOException {
        for (Category category : categories) {
            generator.writeStartObject();
            responseCategory(generator, category);
            generator.writeEndObject();
        }
    }
}
