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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-25
 */
public final class PsqlUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlUtil.class);
    private static HikariDataSource hikariDataSource;

    static {
        String[] providers = new String[]{"postgresql", "mysql", "Microsoft SQL Server", "Oracle", "Db2"};
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
                    props.setProperty("dataSource.user", KeyStoreLoad.decrypt(entry, database.getString("user")));
                    props.setProperty("dataSource.password", KeyStoreLoad.decrypt(entry, database.getString("password")));
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
                    //System.out.println(props);
                    HikariConfig hikariConfig = new HikariConfig(props);
                    hikariDataSource = new HikariDataSource(hikariConfig);
                    break;
                default:
                    break;
            }
        }
    }

    public static Connection getConnection(String databaseName) throws SQLException {
        return hikariDataSource.getConnection();
    }

    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
        //return readsSelector.<HikariDataSource>select().getConnection();
    }

    public static void release(Connection connection) {
        if (hikariDataSource == null)
            return;
        hikariDataSource.evictConnection(connection);
    }
}
