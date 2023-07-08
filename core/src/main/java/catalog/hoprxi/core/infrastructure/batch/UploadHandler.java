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

package catalog.hoprxi.core.infrastructure.batch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.lmax.disruptor.EventHandler;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-07-08
 */
public class UploadHandler implements EventHandler<ItemImportEvent> {
    //https://hoprxi.tooo.top/catalog/core/v1/upload
    private static final String UPLOAD_URL = "http://127.0.0.1:8080/catalog/core/v1/upload";
    private static CloseableHttpClient httpClient;

    static {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLContext sslContext = null;
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

    private final JsonFactory jasonFactory = JsonFactory.builder().build();
    private AtomicInteger number = new AtomicInteger(0);

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        EnumMap<Corresponding, String> map = itemImportEvent.map;
        if (itemImportEvent.verify == Verify.OK) {
            String barcode = map.get(Corresponding.BARCODE);
            barcode = barcode.substring(1, barcode.length() - 1);
            File file = new File("F:\\developer\\catalog\\barcode\\" + barcode + ".jpg");
            System.out.println(barcode + ":" + file.getCanonicalPath() + ":" + file.exists());
            if (file.exists())
                number.incrementAndGet();
        }
        if (map.get(Corresponding.LAST_ROW) != null) {
            System.out.println("Exists:" + number);
        }

   /*
        File file = new File("F:\\developer\\catalog\\barcode\\077330012416.jpg");
        uplaod(file);
          */

    }

    private void uplaod(File file) {
        try {
            // 创建httpget.
            HttpPost httpPost = new HttpPost(UPLOAD_URL);

            //setConnectTimeout：设置连接超时时间，单位毫秒。setConnectionRequestTimeout：设置从connect Manager获取Connection 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。setSocketTimeout：请求获取数据的超时时间，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
            //RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectionRequestTimeout(5, TimeUnit.SECONDS).build();
            //httpPost.setConfig(defaultRequestConfig);

            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.setCharset(StandardCharsets.UTF_8);

            //ContentType type = ContentType.create("text/plain", StandardCharsets.UTF_8);
            //multipartEntityBuilder.addTextBody("randomFileName", "on", ContentType.TEXT_PLAIN);

            multipartEntityBuilder.addBinaryBody("file", file);

            HttpEntity httpEntity = multipartEntityBuilder.build();
            httpPost.setEntity(httpEntity);

            String show = httpClient.execute(httpPost, response -> {
                //System.out.println(response.getCode() + " " + response.getReasonPhrase() + " " + response.getVersion());
                final HttpEntity entity = response.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                //System.out.println(EntityUtils.toString(entity));
                //EntityUtils.consume(entity);
                return processUploadResult(entity.getContent());
            });
            System.out.println(show);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭连接,释放资源
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String processUploadResult(InputStream inputStream) throws IOException {
        String url = null;
        JsonParser parser = jasonFactory.createParser(inputStream);
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "status":
                        break;
                    case "url":
                        url = parser.getValueAsString();
                        break;
                    default:
                        break;
                }
            }
        }
        return "'{\"images\":[\"" + url + "\"]}'";
    }
}
