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

package catalog.hoprxi.core.infrastructure;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-25
 */
public class PsqlUtil {
    private static final Config config;

    static {
        Config cache = ConfigFactory.load("core");
        Config units = ConfigFactory.load("database");
        config = cache.withFallback(units);
    }

    private static HikariDataSource hikariDataSource;

    public static Connection getConnection(String databaseName) throws SQLException {
        if (hikariDataSource == null) {
            Properties props = new Properties();
            props.setProperty("dataSourceClassName", config.getString("postgresql_write.dataSourceClassName"));
            props.setProperty("dataSource.serverName", config.getString("postgresql_write.host"));
            props.setProperty("dataSource.portNumber", config.getString("postgresql_write.port"));
            props.setProperty("dataSource.user", config.getString("postgresql_write.user"));
            props.setProperty("dataSource.password", config.getString("postgresql_write.password"));
            props.setProperty("dataSource.databaseName", databaseName);
            props.put("dataSource.logWriter", new PrintWriter(System.out));
            HikariConfig hikariConfig = new HikariConfig(props);
            hikariDataSource = new HikariDataSource(hikariConfig);
        }
        return hikariDataSource.getConnection();
    }

    public static Connection getConnection() throws SQLException {
        if (hikariDataSource == null) {
            Properties props = new Properties();
            props.setProperty("dataSourceClassName", config.getString("postgresql_write.dataSourceClassName"));
            props.setProperty("dataSource.serverName", config.getString("postgresql_write.host"));
            props.setProperty("dataSource.portNumber", config.getString("postgresql_write.port"));
            props.setProperty("dataSource.user", config.getString("postgresql_write.user"));
            props.setProperty("dataSource.password", config.getString("postgresql_write.password"));
            props.setProperty("catalog", config.hasPath("databaseName") ? config.getString("databaseName") : "catalog");
            props.put("dataSource.logWriter", new PrintWriter(System.out));
            HikariConfig hikariConfig = new HikariConfig(props);
            hikariDataSource = new HikariDataSource(hikariConfig);
        }
        return hikariDataSource.getConnection();
    }

    public static void release(Connection connection) {
        if (hikariDataSource == null)
            return;
        hikariDataSource.evictConnection(connection);
    }
}
