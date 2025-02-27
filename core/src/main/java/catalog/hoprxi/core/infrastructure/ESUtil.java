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

package catalog.hoprxi.core.infrastructure;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.HttpHeaders;
import org.elasticsearch.client.RequestOptions;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-06-15
 */
public class ESUtil {
    private static final String DEFAULT_HOST = "slave.tooo.top";
    private static final String DEFAULT_DATABASE_NAME = "catalog";
    private static final Properties props = new Properties();
    private static final RequestOptions COMMON_OPTIONS;

    static {
        //System.out.println(StoreKeyLoad.SECRET_KEY_PARAMETER);
        Config config = ConfigFactory.load("databases");
        props.put("databaseName", config.getString("databaseName"));
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            if (database.getString("provider").equals("elasticsearch") && (database.getString("type").equals("read") || database.getString("type").equals("R"))) {
                props.put("host", database.getString("host"));
                props.put("port", database.getInt("port"));
                String entry = database.getString("host") + ":" + database.getString("port");
                //System.out.println(database.getString("user"));
                props.put("user", StoreKeyLoad.decrypt(entry, database.getString("user")));
                props.put("password", StoreKeyLoad.decrypt(entry, database.getString("password")));
            }
        }

        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((props.get("user") + ":" + props.get("password")).getBytes(StandardCharsets.UTF_8)))
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        //builder.setHttpAsyncResponseConsumerFactory(
        //new HttpAsyncResponseConsumerFactory
        //.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    public static String host() {
        return props.getProperty("host", DEFAULT_HOST);
    }

    public static String databasePrefix() {
        return props.getProperty("databaseName", DEFAULT_DATABASE_NAME);
    }

    public static int port() {
        return Integer.parseInt(props.getProperty("port", "9200"));
    }

    public static RequestOptions requestOptions() {
        return COMMON_OPTIONS;
    }
}
