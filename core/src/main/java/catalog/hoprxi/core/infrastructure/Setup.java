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

package catalog.hoprxi.core.infrastructure;

import catalog.hoprxi.core.Bootstrap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import salt.hoprxi.crypto.util.AESUtil;
import salt.hoprxi.utils.Selector;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2024-06-14
 */
public class Setup {

    private static final Pattern ENCRYPTED = Pattern.compile("^ENC:.*");
    private static final Selector WRITES_SELECTOR = new Selector();

    public static void setup() throws SQLException {
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            String type = database.getString("type");
            switch (type) {
                case "r":
                case "read":

                    break;
                case "w":
                case "write":
                    Properties props = new Properties();
                    props.setProperty("dataSourceClassName", database.getString("hikari.dataSourceClassName"));
                    props.setProperty("dataSource.serverName", database.getString("host"));
                    props.setProperty("dataSource.portNumber", database.getString("port"));
                    String entry = database.getString("host") + ":" + database.getString("port");
                    props.setProperty("dataSource.user", decrypt(entry, database.getString("user")));
                    props.setProperty("dataSource.password", decrypt(entry, database.getString("password")));
                    props.setProperty("dataSource.databaseName", database.getString("databaseName"));
                    props.put("maximumPoolSize", config.hasPath("hikari.maximumPoolSize") ? config.getInt("hikari.maximumPoolSize") : Runtime.getRuntime().availableProcessors() * 2 + 1);
                    props.put("dataSource.logWriter", new PrintWriter(System.out));
                    HikariConfig hikariConfig = new HikariConfig(props);
                    HikariDataSource dataSource = new HikariDataSource(hikariConfig);
                    WRITES_SELECTOR.add(new Selector.Divisor<>(database.hasPath("weight") ? database.getInt("weight") : 1, dataSource));
                    break;
            }
            System.out.println(type);
            String provider = database.hasPath("provider") ? database.getString("provider") : "postgresql";
            System.out.println(provider);
        }
    }

    private static String decrypt(String entry, String securedPlainText) {

        if (ENCRYPTED.matcher(securedPlainText).matches()) {
            securedPlainText = securedPlainText.split(":")[1];
            byte[] aesData = Base64.getDecoder().decode(securedPlainText);
            Bootstrap.SECRET_KEY_MAP.get(entry);
            byte[] decryptData = AESUtil.decryptSpec(aesData, Bootstrap.SECRET_KEY_MAP.get(entry));
            return new String(decryptData, StandardCharsets.UTF_8);
        }
        return securedPlainText;
    }
}
