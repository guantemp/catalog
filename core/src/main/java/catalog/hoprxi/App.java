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

package catalog.hoprxi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteBufferFeeder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * Hello world!
 */
public class App {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        for (int i = 0, j = args.length; i < j; i++) {
            if ("-iv".equals(args[i])) {
                System.out.println(args[i + 1]);
            }
        }
        String esQuery = """
                {
                    "query": { "match_all": {} },
                    "size": 1000
                }
                """;

        // 2. 创建 HttpClient（使用 HTTP，因为之前 SSL 报错，且原客户端可能是 HTTP）
        HttpClient client = HttpClient.create()
                .secure(spec -> {
                    try {
                        spec.sslContext(SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE).build());
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .baseUrl("https://slave.tooo.top:9200")   // 注意 https
                .headers(h -> {
                    h.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                    h.set(HttpHeaderNames.AUTHORIZATION, "Basic ZWxhc3RpYzpRd2UxMjM0NjU=");
                });

        // 3. 发送请求（使用 GET 方法 + body，模仿原 elasticsearch-rest-client 行为）
        Flux<ByteBuf> responseChunks = client
                .request(HttpMethod.GET)            // 显式 GET，允许带 body
                .uri("/item/_search")           // 请确认该路径是否正确
                .send(ByteBufFlux.fromString(Mono.just(esQuery)))   // 请求体
                .response((res, bodyFlux) -> {
                    System.out.println("ES response status: " + res.status());
                    if (res.status().code() != 200) {
                        // 非 200 状态码，返回错误
                        return Flux.error(new RuntimeException("ES error, status: " + res.status()));
                    }
                    return bodyFlux;   // 流式返回 ByteBuf
                });
        final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
        JsonParser nonBlockingParser=JSON_FACTORY.createNonBlockingByteBufferParser();
        ByteBufferFeeder feeder = (ByteBufferFeeder) nonBlockingParser.getNonBlockingInputFeeder();

        // 打印每个接收到的块（不聚合）
        responseChunks
                .doOnNext(bb -> {
                    System.out.printf("[Chunk] size: %d bytes, content preview: %s\n",
                            bb.readableBytes(),
                            bb.toString(0, Math.min(100, bb.readableBytes()), StandardCharsets.UTF_8));
                    System.out.println(bb.toString(StandardCharsets.UTF_8));
                    System.out.println();
                    // 注意：不要在这里 release，Reactor Netty 会自动管理
                })
                .doOnComplete(() -> System.out.println("All chunks received"))
                .doOnError(e -> System.err.println("Error: " + e))
                .subscribe();

        Thread.sleep(15000); // 等待完成
    }
}
