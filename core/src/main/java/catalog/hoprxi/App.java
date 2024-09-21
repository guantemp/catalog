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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import salt.hoprxi.crypto.util.AESUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Hello world!
 */
public class App {
    public static final Map<String, SecretKey> SECRET_KEY_PARAMETER = new HashMap<>();
    private static final Pattern ENCRYPTED = Pattern.compile("^ENC:.*");

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        for (int i = 0, j = args.length; i < j; i++) {
            if ("-iv".equals(args[i])) {
                System.out.println(args[i + 1]);
            }
        }
        String[] ss = "https://slave.tooo.top:9200".split(":");
        for (String s : ss)
            System.out.println(ss.length);
        JsonFactory jsonFactory = JsonFactory.builder().build();
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");

            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode.raw", "773301701166");
            generator.writeEndObject();

            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println(writer);
        decrypt();
        loadConfig();
    }

    private static void loadConfig() {
        String[] providers = new String[]{"mysql", "postgresql", "Oracle", "Microsoft SQL Server", "Db2"};
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            Properties props = new Properties();
            String provider = database.getString("provider");
            for (String p : providers) {
                if (p.equalsIgnoreCase(provider)) {
                    props.setProperty("dataSourceClassName", database.getString("hikari.dataSourceClassName"));
                    props.setProperty("dataSource.serverName", database.getString("host"));
                    props.setProperty("dataSource.portNumber", database.getString("port"));
                    String entry = database.getString("host") + ":" + database.getString("port");
                    props.setProperty("dataSource.user", decrypt(entry, database.getString("user")));
                    props.setProperty("dataSource.password", decrypt(entry, database.getString("password")));
                    props.setProperty("dataSource.databaseName", database.getString("databaseName"));
                    props.put("maximumPoolSize", database.hasPath("hikari.maximumPoolSize") ? database.getInt("hikari.maximumPoolSize") : Runtime.getRuntime().availableProcessors() * 2 + 1);
                    props.put("dataSource.logWriter", new PrintWriter(System.out));
                    break;
                }
            }
            switch (database.getString("type")) {
                case "read":
                case "R":

                    break;
                case "write":
                case "W":
                case "read/write":
                case "R/W":
                    System.out.println(props);
                    //HikariConfig hikariConfig = new HikariConfig(props);
                    //HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
                    break;
                default:
                    break;
            }
        }
    }

    private static String decrypt(String entry, String securedPlainText) {
        if (ENCRYPTED.matcher(securedPlainText).matches()) {
            securedPlainText = securedPlainText.split(":")[1];
            byte[] aesData = Base64.getDecoder().decode(securedPlainText);
            //Bootstrap.SECRET_KEY_MAP.get(entry);
            try {
                byte[] decryptData = AESUtil.decryptSpec(aesData, SECRET_KEY_PARAMETER.get(entry));
                return new String(decryptData, StandardCharsets.UTF_8);
            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                     NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                //LOGGER.error("decrypt entry={},securedPlainText={} exception", entry, securedPlainText, e);
                throw new RuntimeException("Decrypt data exception", e);
            }
        }
        return securedPlainText;
    }

    private static void decrypt() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:9200:Qwe123465El", "125.68.186.195:5432:Qwe123465Pg", "120.77.47.145:5432:Qwe123465Pg"});
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            if ("write".equals(database.getString("type"))) {
                if (database.getString("provider").equals("postgresql") || database.getString("provider").equals("psql") || database.getString("provider").equals("mysql")) {
                    String entry = database.getString("host") + ":" + database.getString("port");
                    String securedPlainText = database.getString("user");
                    if (ENCRYPTED.matcher(securedPlainText).matches()) {
                        securedPlainText = securedPlainText.split(":")[1];
                        byte[] aesData = Base64.getDecoder().decode(securedPlainText);
                        byte[] decryptData = AESUtil.decryptSpec(aesData, SECRET_KEY_PARAMETER.get(entry));
                        System.out.println("user:" + new String(decryptData, StandardCharsets.UTF_8));
                    }
                    securedPlainText = database.getString("password");
                    if (ENCRYPTED.matcher(securedPlainText).matches()) {
                        securedPlainText = securedPlainText.split(":")[1];
                        byte[] aesData = Base64.getDecoder().decode(securedPlainText);
                        byte[] decryptData = AESUtil.decryptSpec(aesData, SECRET_KEY_PARAMETER.get(entry));
                        System.out.println("password:" + new String(decryptData, StandardCharsets.UTF_8));
                    }
                }
            }
        }
    }

    private static void loadSecretKey(String fileName, String protectedPasswd, String[] entries) {
        try (InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(fis, protectedPasswd.toCharArray());
            for (String entry : entries) {
                String[] ss = entry.split(":");
                if (ss.length == 3)//https://slave.tooo.top:9200
                    SECRET_KEY_PARAMETER.put(ss[0] + ":" + ss[1], (SecretKey) keyStore.getKey(ss[0] + ":" + ss[1], ss[2].toCharArray()));
                if (ss.length == 2) {//125.68.186.195:5432
                    //System.out.println(ss[0] + ":" + ss[1]);
                    SECRET_KEY_PARAMETER.put(ss[0], (SecretKey) keyStore.getKey(ss[0], ss[1].toCharArray()));
                }
                if (ss.length == 1)
                    //System.out.println(ss[0] + ":" + ss[1]);
                    SECRET_KEY_PARAMETER.put("security.keystore.aes.password", (SecretKey) keyStore.getKey("security.keystore.aes.password", "".toCharArray()));
            }
        } catch (FileNotFoundException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            System.out.println("Not find key store file：" + fileName);
        } catch (IOException e) {
            System.out.println("Keystore password was incorrect: " + protectedPasswd);
        } catch (UnrecoverableKeyException e) {
            System.out.println("Is a bad key is used during decryption: " + "");
        }
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
