/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi;

import catalog.hoprxi.core.webapp.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        for (int i = 0, j = args.length; i < j; i++) {
            if ("-iv".equals(args[i])) {
                System.out.println(args[i + 1]);
            }
        }
        JsonFactory jsonFactory = JsonFactory.builder().build();
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeNumberField("from", 0);
            generator.writeNumberField("size", 400);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("bool");

            generator.writeArrayFieldStart("should");

            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", "海天");
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.alias");
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("name.mnemonic", "海天");
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(writer);
        //Config test = ConfigFactory.load("databases");
    }

    private void runServlet() throws ServletException {
        ServletContainer container = ServletContainer.Factory.newInstance();
        DeploymentInfo deploymentInfo = Servlets.deployment()
                .setClassLoader(App.class.getClassLoader())
                .setContextPath("catalog/core")
                .setDeploymentName("catalog.war")
                .addServlets(
                        Servlets.servlet("unitServlet", UnitServlet.class)
                                .addMapping("/v1/units"),
                        Servlets.servlet("brandServlet", BrandServlet.class)
                                .addMapping("/v1/brands/*"),
                        Servlets.servlet("categoryServlet", CategoryServlet.class)
                                .addMapping("/v1/categories/*"),
                        Servlets.servlet("itemServlet", ItemServlet.class)
                                .addInitParam("database", "arangodb")
                                .addInitParam("databaseName", "catalog")
                                .addMapping("/v1/items/*"),
                        Servlets.servlet("uploadServlet", UploadServlet.class)
                                //.addInitParam("UPLOAD_DIRECTORY", "temp")
                                //.addInitParam("databaseName", "catalog")
                                .addMapping("/v1/upload"));
        DeploymentManager manager = container.addDeployment(deploymentInfo);
        manager.deploy();
        PathHandler path = Handlers.path(Handlers.redirect("/core"))
                .addPrefixPath(deploymentInfo.getContextPath(), manager.start());

        Undertow server = Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setHandler(path)
                .build();
        server.start();
    }
}
