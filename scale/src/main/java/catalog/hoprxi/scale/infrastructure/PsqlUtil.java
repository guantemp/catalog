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

package catalog.hoprxi.scale.infrastructure;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import salt.hoprxi.crypto.application.DatabaseSpecDecrypt;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2026-03-02
 */
public final class PsqlUtil {
    private static final Config config;
    private static final HikariDataSource hikariDataSource;

    static {
        config = ConfigFactory.load("scale").resolve();
        List<? extends Config> writes = config.getConfigList("datasources.write.shards");
        Config write = writes.getFirst();
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", write.getString("hikari.dataSourceClassName"));
        String host = write.getString("db.host");
        int port = write.getInt("db.port");
        props.setProperty("dataSource.serverName", host);
        props.setProperty("dataSource.portNumber", String.valueOf(port));

        String entry = host + ":" + port;
        String user = DatabaseSpecDecrypt.decrypt(entry, write.getString("db.user"));
        String password = DatabaseSpecDecrypt.decrypt(entry, write.getString("db.password"));
        props.setProperty("dataSource.user", user);
        props.setProperty("dataSource.password", password);
        props.setProperty("dataSource.databaseName", write.getString("db.databaseName"));

        props.put("maximumPoolSize", write.hasPath("hikari.maximumPoolSize") ? write.getInt("hikari.maximumPoolSize") : 5);
        props.put("dataSource.logWriter", new PrintWriter(System.out));
        HikariConfig hikariConfig = new HikariConfig(props);
        System.out.println(props);
        hikariDataSource = new HikariDataSource(hikariConfig);
        /*
        List<? extends Config> reads = config.getConfigList("reads");
        for (Config read : reads) {
            Properties props = new Properties();
            props.setProperty("dataSource.serverName", read.getString("host"));
            props.setProperty("dataSource.portNumber", read.getString("port"));
            props.setProperty("dataSource.user", read.getString("user"));
            props.setProperty("dataSource.password", read.getString("password"));
            props.setProperty("dataSource.databaseName", read.getString("databaseName"));
            props.setProperty("dataSourceClassName", read.getString("hikari.dataSourceClassName"));
            props.put("maximumPoolSize", read.hasPath("hikari.maximumPoolSize") ? read.getInt("hikari.maximumPoolSize") : 5);
            props.put("dataSource.logWriter", new PrintWriter(System.out));
            HikariConfig hikariConfig = new HikariConfig(props);
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            readsSelector.add(new Selector.Divisor(read.hasPath("weight") ? read.getInt("weight") : 1, dataSource));
         */
    }

    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    public static void release(Connection connection) {
        if (hikariDataSource == null)
            return;
        hikariDataSource.evictConnection(connection);
    }
}
