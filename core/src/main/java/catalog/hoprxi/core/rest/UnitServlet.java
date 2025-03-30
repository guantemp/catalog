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

import catalog.hoprxi.core.domain.model.price.Unit;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import salt.hoprxi.utils.NumberHelper;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 20252-03-24
 */
@WebServlet(urlPatterns = {"v1/units"}, name = "units")
public class UnitServlet extends HttpServlet {
    private final JsonFactory jasonFactory = JsonFactory.builder().build();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (JsonGenerator generator = jasonFactory.createGenerator(resp.getOutputStream(), JsonEncoding.UTF8)) {
            boolean pretty = NumberHelper.booleanOf(req.getParameter("pretty"));
            if (pretty) generator.useDefaultPrettyPrinter();
            resp.setContentType("application/json; charset=UTF-8");
            generator.writeStartObject();
            generator.writeNumberField("total", Unit.values().length);
            generator.writeArrayFieldStart("units");
            for (Unit unit : Unit.values()) {
                generator.writeString(unit.toString());
            }
            generator.writeEndArray();
        }
    }
}
