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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-07-08
 */
public class UploadHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {

    private static final String UPLOAD_URI;
    private static final String LOCAL_IMG_DIR;          // 本地图片搜索目录
    private static final String SERVER_BARCODE_DIR;        // 上传后服务器保存目录（相对路径）
    private static final int MAX_SEQUENCE;
    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".ico", ".tiff"};
    private static final CloseableHttpClient HTTP_CLIENT;
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static volatile boolean closed = false;

    static {
        Config config = ConfigFactory.load("import").getConfig("upload");
        UPLOAD_URI = config.hasPath("uri") ? config.getString("uri") : "https://www.hoprxi.com/catalog/v1/uploads";
        LOCAL_IMG_DIR = config.hasPath("local_img_dir") ? config.getString("local_img_dir") : "";
        SERVER_BARCODE_DIR = config.hasPath("server_barcode_dir") ? config.getString("server_barcode_dir") : "uploads/barcode";
        MAX_SEQUENCE = config.hasPath("max_sequence") ? config.getInt("max_sequence") : 10;

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
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
        httpClientBuilder.setConnectionManager(connManager)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(5))
                .disableAutomaticRetries();
        httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .build());
        HTTP_CLIENT = httpClientBuilder.build();
    }

    private final URI uri;
    private final AtomicInteger totalSuccessCount = new AtomicInteger(0);

    public UploadHandler() {
        this.uri = URI.create(UPLOAD_URI);
    }

    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) throws Exception {
        if (!event.hasWrong()) {
            String barcodeRaw = event.barcode;
            if (barcodeRaw != null && barcodeRaw.length() > 2) {
                String barcode = barcodeRaw.substring(1, barcodeRaw.length() - 1); // 去掉引号
                List<File> imageFiles = UploadHandler.findImageFiles(barcode);
                if (!imageFiles.isEmpty()) {
                    List<String> uploadedUrls = UploadHandler.uploadFiles(imageFiles);
                    if (!uploadedUrls.isEmpty()) {
                        // 使用 JsonGenerator 将 URL 列表序列化为 JSON 数组字符串
                        String json = UploadHandler.serializeUrlsToJson(uploadedUrls);
                        if (json != null)
                            event.show = "'" + json + "'";
                        totalSuccessCount.addAndGet(uploadedUrls.size());
                    }
                    // 全部失败则 SHOW 保持 null
                }
            }
        }

        if (event.map.get(ItemMapping.LAST_ROW) != null) {
            synchronized (UploadHandler.class) {
                if (!closed) {
                    try {
                        HTTP_CLIENT.close();
                        closed = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Total uploaded files: " + totalSuccessCount.get());
                }
            }
        }
    }

    /**
     * 查找条形码对应的所有图片文件（主文件 + 序号文件），支持多种后缀
     */
    private static List<File> findImageFiles(String barcode) {
        if (LOCAL_IMG_DIR.isEmpty()) {
            return Collections.emptyList();
        }
        File baseDir = new File(LOCAL_IMG_DIR);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return Collections.emptyList();
        }

        List<File> found = new ArrayList<>();
        // 主文件 (不带序号)
        for (String ext : SUPPORTED_EXTENSIONS) {
            File f = new File(baseDir, barcode + ext);
            if (f.exists() && f.isFile()) {
                found.add(f);
                break;
            }
        }
        // 带序号的 (1 ~ MAX_SEQUENCE)
        for (int i = 1; i <= MAX_SEQUENCE; i++) {
            String prefix = barcode + "_" + i;
            for (String ext : SUPPORTED_EXTENSIONS) {
                File f = new File(baseDir, prefix + ext);
                if (f.exists() && f.isFile()) {
                    found.add(f);
                    break;
                }
            }
        }
        return found;
    }

    /**
     * 上传文件列表到服务器，不重命名，不按日期分隔，保存到指定目录
     */
    private static List<String> uploadFiles(List<File> files) {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        HttpPost httpPost = new HttpPost(UPLOAD_URI);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(StandardCharsets.UTF_8);

        // 添加文件部分，字段名 "files"
        for (File file : files) {
            builder.addBinaryBody("files", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }

        // 添加文本参数：rename=false（不重命名）, directory=配置的服务器保存目录
        builder.addTextBody("rename", "false", ContentType.TEXT_PLAIN);
        builder.addTextBody("separateByDate", "false", ContentType.TEXT_PLAIN);
        builder.addTextBody("directory", SERVER_BARCODE_DIR, ContentType.TEXT_PLAIN);

        httpPost.setEntity(builder.build());

        try {
            return HTTP_CLIENT.execute(httpPost, response -> {
                final HttpEntity entity = response.getEntity();
                if (HttpStatus.SC_OK == response.getCode() && entity != null) {
                    return UploadHandler.parseUploadResponse(entity.getContent());
                } else {
                    return Collections.emptyList();
                }
            });
        } catch (IOException e) {
            System.err.println("Upload failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<String> parseUploadResponse(InputStream content) throws IOException {
        List<String> urls = new ArrayList<>();
        try (JsonParser parser = JSON_FACTORY.createParser(content)) {
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == JsonToken.FIELD_NAME && "results".equals(parser.currentName())) {
                    parser.nextToken(); // 进入数组
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            String field = parser.currentName();
                            parser.nextToken();
                            if ("url".equals(field)) {
                                String url = parser.getValueAsString();
                                if (url != null && !url.isEmpty()) {
                                    urls.add(url);
                                }
                            } else {
                                // 跳过不需要的字段值（即使它是嵌套对象或数组）
                                parser.skipChildren();
                            }
                        }
                    }
                }
            }
            return urls;
        }
    }

    private static String serializeUrlsToJson(List<String> urls) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonGenerator gen = JSON_FACTORY.createGenerator(baos)) {
            gen.writeStartArray();
            for (String url : urls) {
                gen.writeString(url);
            }
            gen.writeEndArray();
            gen.flush();
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }
}
