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
import catalog.hoprxi.core.application.query.CategoryJsonQuery;
import catalog.hoprxi.core.application.query.QueryException;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESCategoryJsonQuery;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
            String query1 = config.getInitParameter("query");
            switch (query1) {
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
        JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8);
        boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
        if (pretty) generator.useDefaultPrettyPrinter();
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");
        if (pathInfo != null) {
            String[] paths = pathInfo.split("/");
            if (paths.length == 2) {
                try {
                    long id = Long.parseLong(paths[1]);
                    String result = query.query(id);
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
        generator.close();
    }

    private void copy(JsonGenerator generator, String result) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(result);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

}
