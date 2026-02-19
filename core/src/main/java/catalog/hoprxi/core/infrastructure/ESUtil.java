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
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
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
    private static final RestClient restClient;

    static {
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            if (database.getString("provider").equals("elasticsearch") && (database.getString("type").equals("read") || database.getString("type").equals("R"))) {
                props.put("host", database.getString("host"));
                props.put("port", database.getInt("port"));
                String entry = database.getString("host") + ":" + database.getString("port");
                props.put("user", DecryptUtil.decrypt(entry, database.getString("user")));
                props.put("password", DecryptUtil.decrypt(entry, database.getString("password")));
                props.put("databaseName", config.getString("databaseName"));
                props.put("customized", config.hasPath("customized") ? config.getString("customized") : "");

                props.put("ConnectTimeout", config.hasPath("ConnectTimeout") ? config.getString("ConnectTimeout") : 5000);
                props.put("ConnectionRequestTimeout", config.hasPath("ConnectionRequestTimeout") ? config.getString("ConnectionRequestTimeout") : 1000);
                props.put("SocketTimeout", config.hasPath("SocketTimeout") ? config.getString("ConnectTimeout") : 3000);
            }
        }

        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((props.get("user") + ":" + props.get("password")).getBytes(StandardCharsets.UTF_8)))
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
       /*
        builder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory
                        .HeapBufferedResponseConsumerFactory(64 * 1024 * 1024));//64MB
        */

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(3000)// 从连接池获取连接超时（3秒）
                .setConnectTimeout(5000) // 建立 TCP 连接超时（5秒）
                .setSocketTimeout(30000).build(); // 读取响应超时（30秒）
        builder.setRequestConfig(requestConfig);
        COMMON_OPTIONS = builder.build();

        restClient = RestClient.builder(new HttpHost(props.getProperty("host", DEFAULT_HOST), Integer.parseInt(props.getProperty("port", "9200")), "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    // 设置连接池
                    httpClientBuilder.setMaxConnTotal(100);   // 整个连接池最大连接数
                    httpClientBuilder.setMaxConnPerRoute(40);//每个 host 最多 80 连接

                    // 如果需要跳过证书验证（仅测试环境！）不推荐生产使用
                    /*
                    SSLContext sslContext = SSLContextBuilder.create()
                            .loadTrustMaterial(null, (chain, authType) -> true)
                            .build();
                    httpClientBuilder.setSSLContext(sslContext);
                    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
*/
                    return httpClientBuilder;
                }).build();
    }

    public static String host() {
        return props.getProperty("host", DEFAULT_HOST);
    }

    public static String database() {
        return props.getProperty("databaseName", DEFAULT_DATABASE_NAME);
    }

    public static String customized() {
        return props.getProperty("customized", "");
    }

    public static int port() {
        return Integer.parseInt(props.getProperty("port", "9200"));
    }

    public static RequestOptions requestOptions() {
        return COMMON_OPTIONS;
    }

    public static RestClient restClient() {
        return restClient;
    }

    public static void release(RestClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
