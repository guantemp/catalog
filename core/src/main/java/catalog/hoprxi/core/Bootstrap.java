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

package catalog.hoprxi.core;

import catalog.hoprxi.core.webapp.*;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.crypto.PasswordService;

import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2024-06-14
 */
public class Bootstrap {
    public static final Map<String, SecretKey> SECRET_KEY_MAP = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
    private static final Pattern EXCLUDE = Pattern.compile("^-{1,}.*");

    public static void main(String[] args) throws ServletException {
        String fileName = null, fileProtectedPasswd = "";
        Set<String> entries = new HashSet<>();
        for (int i = 0, j = args.length; i < j; i++) {
            switch (args[i]) {
                case "-f":
                case "--file":
                    if (j > i + 1) {
                        if (EXCLUDE.matcher(args[i + 1]).matches())
                            break;
                        else
                            fileName = args[i + 1];
                    }
                    if (j > i + 2) {
                        if (EXCLUDE.matcher(args[i + 2]).matches())
                            break;
                        else
                            fileProtectedPasswd = args[i + 2];
                    }
                    break;
                case "-e":
                case "--entries":
                    int k = i + 1;
                    while (k < j) {
                        if (EXCLUDE.matcher(args[k]).matches())
                            break;
                        else
                            entries.add(args[k]);
                        k++;
                    }
                    break;
                case "-h":
                case "--help":
                    break;
            }
        }
        Bootstrap.loadSecretKey(fileName, fileProtectedPasswd, entries);
        System.out.println(SECRET_KEY_MAP);

        ServletContainer container = ServletContainer.Factory.newInstance();
        DeploymentInfo deploymentInfo = Servlets.deployment()
                .setClassLoader(Bootstrap.class.getClassLoader())
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
                .addHttpListener(9000, "0.0.0.0")
                .setHandler(path)
                .build();
        server.start();
    }

    private static void loadSecretKey(String fileName, String protectedPasswd, Set<String> entries) {
        try (InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(fis, protectedPasswd.toCharArray());
            Enumeration<String> alias = keyStore.aliases();
            while (alias.hasMoreElements()) {
                entries.add(alias.nextElement());
            }
            for (String entry : entries) {
                String[] ss = entry.split(":");
                if (ss.length == 3)//https://slave.tooo.top:9200
                    SECRET_KEY_MAP.put(ss[0] + ":" + ss[1], (SecretKey) keyStore.getKey(ss[0] + ":" + ss[1], ss[2].toCharArray()));
                if (ss.length == 2) //125.68.186.195:5432
                    SECRET_KEY_MAP.put(ss[0], (SecretKey) keyStore.getKey(ss[0], ss[1].toCharArray()));
                if (ss.length == 1)
                    SECRET_KEY_MAP.put(PasswordService.KEYSTORE_ENTRY, (SecretKey) keyStore.getKey(PasswordService.KEYSTORE_ENTRY, "".toCharArray()));
            }
        } catch (FileNotFoundException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            LOGGER.error("Not find key store file {}", fileName, e);
        } catch (IOException e) {
            LOGGER.error("Keystore protected password was incorrect {}", protectedPasswd, e);
        } catch (UnrecoverableKeyException e) {
            LOGGER.error("Is a bad key is used during decryption", e);
        }
    }
}
