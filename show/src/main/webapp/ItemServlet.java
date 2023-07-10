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

import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBItemRepository;
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
 * @version 0.0.1 2020-01-10
 */
@WebServlet(urlPatterns = {"/items/*"}, name = "items", asyncSupported = false)
public class ItemServlet extends HttpServlet {
    private ItemRepository itemRepository = new ArangoDBItemRepository("catalog");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json;charset=UTF-8");
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator j = jasonFactory.createGenerator(response.getOutputStream(), JsonEncoding.UTF8);
        j.setPrettyPrinter(new DefaultPrettyPrinter());
        if (pathInfo != null) {
            Item item = itemRepository.find(pathInfo);
            if (item != null) {

            }
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        j.writeStartObject();
        j.writeStringField("code", "400");
        j.writeStringField("message", "error request path");
        j.writeStringField("description", "400");
        j.writeEndObject();
        j.flush();
        j.close();
    }
}
