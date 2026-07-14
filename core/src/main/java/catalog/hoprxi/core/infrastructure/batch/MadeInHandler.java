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

package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 产地解析处理器（手工 JsonParser 解析，带缓存）
 */
public class MadeInHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {
    private static final String MADE_IN_URL;
    private static final CloseableHttpClient httpClient;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    // 缓存
    private static final Cache<String, String> madeInCache;

    static {
        madeInCache = CacheFactory.build("madeIn");
        Config config = ConfigFactory.load("import").getConfig("madin_in");
        MADE_IN_URL = config.hasPath("url") ? config.getString("url") : "https://www.hoprxi.com/v1/areas";
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext).setTlsVersions(TLS.V_1_2).build();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslSocketFactory)
                .build();
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connManager.setMaxTotal(50);
        connManager.setDefaultMaxPerRoute(20);
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .disableAutomaticRetries()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .setResponseTimeout(Timeout.ofSeconds(10))
                        .build())
                .build();
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }

    @Override
    public void onEvent(ItemImportEvent event, long sequence, boolean endOfBatch) throws Exception {
        event.madeInJson = MadeInHandler.buildUnknownJson();
        String madeInText = event.map.get(ItemMapping.MADE_IN);
        if (madeInText == null || madeInText.isBlank())
            return;

        String trimmed = madeInText.trim();
        // 尝试从缓存获取
        String cached = madeInCache.get(trimmed);
        if (cached != null) {
            event.madeInJson = cached;
            return;
        }

        // 缓存未命中，发起网络请求
        try {
            String result = MadeInHandler.fetchFromApi(trimmed);
            madeInCache.put(trimmed, result);
            event.madeInJson = result;
        } catch (Exception e) {
            System.err.println("Failed to resolve madeIn: " + trimmed + ", error: " + e.getMessage());
        }
    }

    /**
     * 发起 HTTP 请求并解析返回结果
     */
    private static String fetchFromApi(String query) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("q", query));
        params.add(new BasicNameValuePair("filter", "city,country,county"));
        URI uri;
        try {
            uri = new URIBuilder(new URI(MADE_IN_URL)).addParameters(params).build();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI", e);
        }
        //System.out.println("Url:" + uri.toURL().toExternalForm());

        ClassicHttpRequest httpGet = ClassicRequestBuilder.get(uri).build();

        return httpClient.execute(httpGet, response -> {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return MadeInHandler.buildUnknownJson();
            }
            try (InputStream content = entity.getContent()) {
                return parseResponse(content);
            }
        });
    }

    /**
     * 手工解析 JSON 响应（使用 JsonParser）
     */
    private static String parseResponse(InputStream inputStream) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(inputStream)) {
            // 跳过根对象，直到遇到 "areas" 数组
            while (parser.nextToken() != null) {
                if (JsonToken.START_ARRAY == parser.currentToken() && "areas".equals(parser.currentName())) {
                    // 开始遍历 areas 数组
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.END_ARRAY) {
                            break; // 数组结束
                        }
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            // 解析每个 area 对象
                            AreaInfo area = MadeInHandler.parseArea(parser);
                            //System.out.println("area:"+area);
                            if (area == null) continue;

                            String level = area.level;
                            if ("CITY".equals(level)) {
                                return MadeInHandler.buildDomesticJson(area.code, area.name);
                            }
                            if ("COUNTRY".equals(level) && area.code != 156) {
                                return MadeInHandler.buildImportedJson(area.code, area.name, area.abbreviation);
                            }
                            if ("COUNTY".equals(level)) {
                                // 向上查询父级
                                AreaInfo parent = MadeInHandler.fetchParent(area.parentCode);
                                if (parent != null) {
                                    String parentLevel = parent.level;
                                    if ("CITY".equals(parentLevel)) {
                                        return MadeInHandler.buildDomesticJson(parent.code, parent.name);
                                    }
                                    if ("COUNTRY".equals(parentLevel) && parent.code != 156) {
                                        return MadeInHandler.buildImportedJson(parent.code, parent.name, parent.abbreviation);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return MadeInHandler.buildUnknownJson();
    }

    /**
     * 解析单个 area 对象，返回 AreaInfo
     */
    private static AreaInfo parseArea(JsonParser parser) throws IOException {
        int parentCode = Integer.MIN_VALUE;
        int code = Integer.MIN_VALUE;
        String name = null;
        String abbreviation = null;
        String level = null;

        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.END_OBJECT) {
                break;
            }
            String fieldName = parser.currentName();
            if (fieldName == null) continue;

            parser.nextToken(); // move to value

            switch (fieldName) {
                case "parent_code":
                    parentCode = parser.getIntValue();
                    break;
                case "code":
                    code = parser.getIntValue();
                    break;
                case "name":
                    if (parser.currentToken() == JsonToken.START_OBJECT) {
                        while (parser.nextToken() != null) {
                            if (parser.currentToken() == JsonToken.END_OBJECT) break;
                            String subField = parser.currentName();
                            parser.nextToken();
                            if ("name".equals(subField)) {
                                name = parser.getValueAsString();
                            } else if ("abbreviation".equals(subField)) {
                                abbreviation = parser.getValueAsString();
                            } else {
                                parser.skipChildren(); // 跳过可能嵌套的结构
                            }
                        }
                    } else {
                        parser.skipChildren(); // 跳过可能嵌套的结构
                    }
                    break;
                case "level":
                    if (parser.currentToken() == JsonToken.START_OBJECT) {
                        while (parser.nextToken() != null) {
                            if (parser.currentToken() == JsonToken.END_OBJECT) break;
                            String subField = parser.currentName();
                            parser.nextToken();
                            if ("name".equals(subField)) {
                                level = parser.getValueAsString();
                            } else {
                                parser.skipChildren(); // 跳过可能嵌套的结构
                            }
                        }
                    } else {
                        parser.skipChildren(); // 跳过可能嵌套的结构
                    }
                    break;
                default:
                    // 其他字段（如 zipcode, telephone_code, location, sort）直接跳过整个值
                    parser.skipChildren();
                    break;
            }
        }

        if (code == Integer.MIN_VALUE || name == null || level == null) {
            return null;
        }
        return new AreaInfo(parentCode, code, name, abbreviation, level);
    }

    /**
     * 根据 parentCode 查询父级地区（用于 COUNTY 追溯）
     */
    private static AreaInfo fetchParent(int parentCode) throws IOException {
        String url = MADE_IN_URL + "/" + parentCode;
        ClassicHttpRequest httpGet = ClassicRequestBuilder.get(url).build();
        return httpClient.execute(httpGet, response -> {
            HttpEntity entity = response.getEntity();
            if (entity == null) return null;
            try (InputStream content = entity.getContent()) {
                try (JsonParser parser = JSON_FACTORY.createParser(content)) {
                    // 直接解析根对象，无需查找 areas 数组
                    if (parser.nextToken() == JsonToken.START_OBJECT) {
                        return MadeInHandler.parseArea(parser);
                    }
                }
                return null;
            }
        });
    }

    // ---------- 辅助构造 JSON ----------
    private static String buildDomesticJson(int code, String name) {
        return String.format("'{\"_class\":\"Domestic\",\"code\":%d,\"madeIn\":\"%s\"}'",
                code, escapeJson(name));
    }

    private static String buildImportedJson(int code, String name, String abbreviation) {
        if (name.length() > 4 && abbreviation != null && !abbreviation.isEmpty()) {
            name = abbreviation;
        }
        return String.format("'{\"_class\":\"Imported\",\"code\":%d,\"madeIn\":\"%s\"}'",
                code, escapeJson(name));
    }

    private static String buildUnknownJson() {
        return String.format("'{\"_class\":\"UNORIGINATED\",\"code\":%s,\"madeIn\":\"%s\"}'",
                MadeIn.UNORIGINATED.code(), MadeIn.UNORIGINATED.madeIn());
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ---------- 内部数据类 ----------
    private static class AreaInfo {
        int parentCode;
        int code;
        String name;
        String abbreviation;
        String level;

        AreaInfo(int parentCode, int code, String name, String abbreviation, String level) {
            this.parentCode = parentCode;
            this.code = code;
            this.name = name;
            this.abbreviation = abbreviation;
            this.level = level;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", AreaInfo.class.getSimpleName() + "[", "]")
                    .add("parentCode=" + parentCode)
                    .add("code=" + code)
                    .add("name='" + name + "'")
                    .add("abbreviation='" + abbreviation + "'")
                    .add("level='" + level + "'")
                    .toString();
        }
    }

    // ---------- 缓存接口（假设 CacheFactory 提供） ----------
    interface Cache<K, V> {
        V get(K key);

        void put(K key, V value);
    }

    static class CacheFactory {
        public static <K, V> Cache<K, V> build(String name) {
            // 实际实现可能是 Guava Cache 或其它，这里仅示意
            return new InMemoryCache<>();
        }
    }

    static class InMemoryCache<K, V> implements Cache<K, V> {
        private final java.util.concurrent.ConcurrentHashMap<K, V> map = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public V get(K key) {
            return map.get(key);
        }

        @Override
        public void put(K key, V value) {
            map.put(key, value);
        }
    }
}