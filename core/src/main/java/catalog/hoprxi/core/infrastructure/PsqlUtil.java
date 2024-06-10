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
import salt.hoprxi.crypto.util.AESUtil;
import salt.hoprxi.utils.Selector;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-25
 */
public final class PsqlUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlUtil.class);
    private static final Config config;
    private static final Selector readsSelector = new Selector();
    private static HikariDataSource hikariDataSource;

    static {
        Config core = ConfigFactory.load("core");
        Config databases = ConfigFactory.load("database");
        config = core.withFallback(databases);
        List<? extends Config> writes = config.getConfigList("writes");
        for (Config write : writes) {
            if (write.getString("provider").equals("postgresql") || write.getString("provider").equals("psql") || write.getString("provider").equals("mysql")) {
                Properties props = new Properties();
                props.setProperty("dataSourceClassName", write.getString("hikari.dataSourceClassName"));
                props.setProperty("dataSource.serverName", write.getString("host"));
                props.setProperty("dataSource.portNumber", write.getString("port"));
                props.setProperty("dataSource.user", write.getString("user"));
                props.setProperty("dataSource.password", write.getString("password"));
                props.setProperty("dataSource.databaseName", write.getString("databaseName"));
                props.put("maximumPoolSize", config.hasPath("hikari.maximumPoolSize") ? config.getInt("hikari.maximumPoolSize") : Runtime.getRuntime().availableProcessors() * 2 + 1);
                props.put("dataSource.logWriter", new PrintWriter(System.out));
                HikariConfig hikariConfig = new HikariConfig(props);
                hikariDataSource = new HikariDataSource(hikariConfig);
            }
        }
        List<? extends Config> reads = config.getConfigList("reads");
        for (Config read : reads) {
            if (read.getString("provider").equals("postgresql") || read.getString("provider").equals("mysql")) {
                Properties props = new Properties();
                props.setProperty("dataSource.serverName", read.getString("host"));
                props.setProperty("dataSource.portNumber", read.getString("port"));
                props.setProperty("dataSource.user", read.getString("user"));
                props.setProperty("dataSource.password", read.getString("password"));
                props.setProperty("dataSource.databaseName", read.getString("databaseName"));
                props.setProperty("dataSourceClassName", read.getString("hikari.dataSourceClassName"));
                props.put("maximumPoolSize", read.hasPath("hikari.maximumPoolSize") ? read.getInt("hikari.maximumPoolSize") : Runtime.getRuntime().availableProcessors() * 2 + 1);
                props.put("dataSource.logWriter", new PrintWriter(System.out));
                HikariConfig hikariConfig = new HikariConfig(props);
                HikariDataSource dataSource = new HikariDataSource(hikariConfig);
                readsSelector.add(new Selector.Divisor<>(read.hasPath("weight") ? read.getInt("weight") : 1, dataSource));
            }
        }
    }

    public static Connection getConnection(String databaseName) throws SQLException {
        return hikariDataSource.getConnection();
    }

    public static Connection getConnection() throws SQLException {
        return readsSelector.<HikariDataSource>select().getConnection();
    }

    public static Connection getReadConnection() throws SQLException {
        return readsSelector.<HikariDataSource>select().getConnection();
    }

    public static void release(Connection connection) {
        if (hikariDataSource == null)
            return;
        hikariDataSource.evictConnection(connection);
    }

    private String decrypt(String securedPlainText, String entry, String entryPasswd, String fileName, String protectedPasswd) {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(fis, protectedPasswd.toCharArray());
            if (keyStore.containsAlias(entry)) {
                SecretKey secKey = (SecretKey) keyStore.getKey(entry, entryPasswd.toCharArray());
                byte[] result = AESUtil.decryptSpec(Base64.getDecoder().decode(securedPlainText), secKey);
                return Base64.getEncoder().encodeToString(result);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException |
                 IOException e) {
            LOGGER.error("Not get database user password,system will exit");
            throw new RuntimeException(e);
        }
        return "";
    }
}
