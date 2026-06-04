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

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-09
 */
public class MaidenHandler implements EventHandler<ItemImportEvent> {
    private static final String AREA_URL;
    private static final CloseableHttpClient httpClient;

    static {
        Config areaUrl = ConfigFactory.load("import");
        AREA_URL = areaUrl.hasPath("made_in_url") ? areaUrl.getString("made_in_url") : "https://www.hoprxi.com/v1/areas";
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        // Allow TLSv1.2 protocol only
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext).setTlsVersions(TLS.V_1_2).build();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslSocketFactory)
                .build();
        //适配http以及https请求 通过new创建PoolingHttpClientConnectionManager
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        httpClientBuilder.setConnectionManager(connManager).evictExpiredConnections().evictIdleConnections(TimeValue.ofSeconds(5)).disableAutomaticRetries();
        httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .build());

        httpClient = httpClientBuilder.build();
    }

    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        String madeIn = itemImportEvent.map.get(ItemMapping.MADE_IN);
        if (madeIn == null || madeIn.isBlank()) {
            itemImportEvent.map.put(ItemMapping.MADE_IN, "{\"_class\":\"UNKNOWN\",\"code\":" + MadeIn.UNKNOWN.code() + ",\"madeIn\":\"" + MadeIn.UNKNOWN.madeIn() + "\"}");
            return;
        }
        ClassicHttpRequest httpGet = ClassicRequestBuilder.get(AREA_URL).build();
        // 表单参数
        List<NameValuePair> madeInList = new ArrayList<>();
        // GET 请求参数
        madeInList.add(new BasicNameValuePair("q", madeIn));
        madeInList.add(new BasicNameValuePair("filter", "city,country,county"));
        // 增加到请求 URL 中
        try {
            URI uri = new URIBuilder(new URI(AREA_URL))
                    .addParameters(madeInList)
                    .build();
            httpGet.setUri(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String result = httpClient.execute(httpGet, response -> {
            final HttpEntity entity = response.getEntity();
            return MaidenHandler.read(entity.getContent());
        });
        //System.out.println("result:" + result);
        itemImportEvent.map.put(ItemMapping.MADE_IN, result);
        //System.out.println("made_in:" + itemImportEvent.map.get(Corresponding.MADE_IN));
    }

    private static String read(InputStream inputStream) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(inputStream)) {
            while (parser.nextToken() != null) {
                if (JsonToken.START_ARRAY == parser.currentToken() && "areas".equals(parser.currentName())) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.END_ARRAY) break; // 退出 areas 数组
                        if (JsonToken.START_OBJECT == parser.currentToken() && parser.currentName() == null) {
                            MdeInView view = MaidenHandler.toMadeInView(parser);
                            if ("CITY".equals(view.level))
                                return "{\"_class\":\"Domestic\",\"code\":" + view.code + ",\"madeIn\":\"" + MaidenHandler.escapeJson(view.name) + "\"}";
                            if ("COUNTRY".equals(view.level) && 156 != view.code)
                                return "{\"_class\":\"Imported\",\"code\":" + view.code + ",\"madeIn\":\"" + (view.name.length() > 4 ? MaidenHandler.escapeJson(view.abbreviation) : MaidenHandler.escapeJson(view.name)) + "\"}";
                            if ("COUNTY".equals(view.level)) {
                                return MaidenHandler.processPreviousLevel(view.parentCode);
                                //System.out.println(view);
                            }
                        }
                    }
                }
            }
        }
        return "{\"_class\":\"UNKNOWN\",\"code\":" + MadeIn.UNKNOWN.code() + ",\"madeIn\":\"" + MadeIn.UNKNOWN.madeIn() + "\"}";
    }

    private static String processPreviousLevel(int parentCode) throws IOException {
        ClassicHttpRequest httpGet = ClassicRequestBuilder.get(AREA_URL + "/" + parentCode).build();
        return httpClient.execute(httpGet, response -> {
            final HttpEntity entity = response.getEntity();
            return MaidenHandler.read(entity.getContent());
        });
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static MdeInView toMadeInView(JsonParser parser) throws IOException {
        String name = null, abbreviation = null, level = null;
        int code = Integer.MIN_VALUE, parentCode = Integer.MIN_VALUE, position = 0;
        while (parser.nextToken() != null) {
            //System.out.println(parser.currentToken() + ":" + parser.currentName());
            if (parser.currentToken() == JsonToken.START_ARRAY && "sort".equals(parser.currentName())) break;
            String fieldName = parser.currentName();
            parser.nextToken();
            switch (fieldName) {
                case "parent_code" -> parentCode = parser.getIntValue();
                case "code" -> code = parser.getIntValue();
                case "name" -> {//end name也会进入一次
                    position += 1;
                    if (position == 2) {//name.name
                        name = parser.getValueAsString();
                    }
                    if (position == 4) {//level.name
                        level = parser.getValueAsString();
                    }
                }
                case "abbreviation" -> abbreviation = parser.getValueAsString();
            }

        }
        return new MdeInView(parentCode, code, name, abbreviation, level);
    }

    private record MdeInView(int parentCode, int code, String name, String abbreviation, String level) {
    }
}
