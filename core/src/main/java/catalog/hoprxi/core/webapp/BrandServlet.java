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

import catalog.hoprxi.core.application.BrandAppService;
import catalog.hoprxi.core.application.command.CreateBrandCommand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.time.Year;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-09-10
 */
public class BrandServlet extends HttpServlet {
    private static final BrandRepository brandRepository = new ArangoDBBrandRepository("catalog");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        String pathInfo = req.getPathInfo();
        generator.writeStartObject();
        if (pathInfo != null) {
            String id = pathInfo.substring(1, pathInfo.length());
            Brand brand = brandRepository.find(id);
            if (brand != null) {
                generator.writeObjectFieldStart("brand");
                generator.writeStringField("id", brand.id());
                generator.writeObjectFieldStart("name");
                generator.writeStringField("name", brand.name().name());
                generator.writeStringField("mnemonic", brand.name().mnemonic());
                generator.writeStringField("alias", brand.name().alias());
                generator.writeEndObject();
                generator.writeObjectFieldStart("about");
                generator.writeStringField("homepage", brand.about().homepage().toString());
                generator.writeStringField("logo", brand.about().logo().toString());
                generator.writeNumberField("since", brand.about().since().getValue());
                generator.writeStringField("story", brand.about().story());
                generator.writeEndObject();
                generator.writeEndObject();
            }
        } else {
            int offset = NumberHelper.intOf(req.getParameter("offset"), 0);
            int limit = NumberHelper.intOf(req.getParameter("limit"), 15);
            Brand[] brands = brandRepository.findAll(offset, limit);
            for (Brand brand : brands) {

            }
            generator.writeNumberField("offset", offset);
            generator.writeNumberField("limit", limit);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = null, alias = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
        JsonFactory jasonFactory = new JsonFactory();
        JsonParser parser = jasonFactory.createParser(req.getInputStream());
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
        CreateBrandCommand createBrandCommand = new CreateBrandCommand(name, alias, homepage, logo, since, story);
        BrandAppService brandAppService = new BrandAppService();
        Brand brand = brandAppService.createBrand(createBrandCommand);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }
}
