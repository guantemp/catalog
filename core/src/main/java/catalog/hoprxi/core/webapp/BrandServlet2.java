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

import catalog.hoprxi.core.application.command.BrandChangeAboutCommand;
import catalog.hoprxi.core.application.command.CategoryRenameCommand;
import catalog.hoprxi.core.application.command.Command;
import catalog.hoprxi.core.application.query.BrandJsonQuery;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESBrandJsonQuery;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.SortField;
import com.fasterxml.jackson.core.*;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.ServletException;
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

@WebServlet(urlPatterns = {"v2/brands/*"}, name = "brands", asyncSupported = true)
public class BrandServlet2 extends HttpServlet {
    private static final int OFFSET = 0;
    private static final int LIMIT = 64;
    private final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private final BrandJsonQuery jsonQuery = new ESBrandJsonQuery();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");
        JsonGenerator generator = JSON_FACTORY.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8);
        boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
        if (pretty) generator.useDefaultPrettyPrinter();
        if (pathInfo != null) {
            long id = NumberHelper.longOf(pathInfo.substring(1), 0l);
            String result = jsonQuery.query(id);
            copyRaw(generator, result);
        } else {//query name/all
            String name = Optional.ofNullable(req.getParameter("name")).orElse("");
            String searchAfter = Optional.ofNullable(req.getParameter("searchAfter")).orElse("");
            int offset = NumberHelper.intOf(req.getParameter("offset"), OFFSET);
            int limit = NumberHelper.intOf(req.getParameter("limit"), LIMIT);
            String sort = Optional.ofNullable(req.getParameter("sort")).orElse("");
            SortField sortField = SortField.of(sort);
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
        generator.flush();
    }

    private void copyRaw(JsonGenerator generator, String result) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(result);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String id = pathInfo.substring(1);
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
                commands.add(new CategoryRenameCommand(id, name, alias));
            if (story != null || homepage != null || logo != null || since != null)
                commands.add(new BrandChangeAboutCommand(id, logo, homepage, since, story));
        }
    }
}
