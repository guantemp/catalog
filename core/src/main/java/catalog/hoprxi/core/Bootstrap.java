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

package catalog.hoprxi.core;

import catalog.hoprxi.core.rest.BrandService;
import catalog.hoprxi.core.rest.CategoryService;
import catalog.hoprxi.core.rest.ItemService;
import catalog.hoprxi.core.rest.UnitService;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.encoding.EncodingService;
import com.linecorp.armeria.server.logging.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2025-02-23
 */
public class Bootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);
    private static final Pattern EXCLUDE = Pattern.compile("^-{1,}.*");
    private static final int PORT = 9002;

    public static void main(String[] args) {
        String fileName = "keystore.jks", fileProtectedPasswd = "";
        Set<String> entries = new HashSet<>();
        for (int i = 0, j = args.length; i < j; i++) {
            switch (args[i]) {
                case "-f":
                case "--file":
                    if (j > i + 1) {
                        if (EXCLUDE.matcher(args[i + 1]).matches())
                            break;
                        else
                            fileName = args[i + 1];
                    }
                    if (j > i + 2) {
                        if (EXCLUDE.matcher(args[i + 2]).matches())
                            break;
                        else
                            fileProtectedPasswd = args[i + 2];
                    }
                    break;
                case "-e":
                case "--entries":
                    int k = i + 1;
                    while (k < j) {
                        if (EXCLUDE.matcher(args[k]).matches())//下一参数开始
                            break;
                        else
                            entries.add(args[k]);
                        k++;
                    }
                    break;
                case "-h":
                case "--help":
                    System.out.println("""
                            Non-option arguments:
                            command             \s
                            
                            Option                         Description       \s
                            ------                         -----------       \s
                            -f, --file <filename>          A file that stores the key
                            -e <KeyValuePair>              encrypt a passwd
                            -l, --list                     entries in the keystore
                            -h, --help                     Show help         \s
                            """);
                    break;
            }
        }
        StoreKeyLoad.loadSecretKey(fileName, fileProtectedPasswd, entries.toArray(new String[0]));
        //System.out.println(StoreKeyLoad.SECRET_KEY_PARAMETER);

        ServerBuilder sb = Server.builder();
        //http2 配置
        sb.http2MaxFrameSize(16384) // 16KB帧大小
                .http2InitialConnectionWindowSize(1024 * 1024) // 1MB连接窗口
                .http2InitialStreamWindowSize(512 * 1024);// 512KB流窗口
        //  添加装饰器（中间件）
        sb.decorator(LoggingService.newDecorator()); // 日志记录
        sb.decorator(EncodingService.newDecorator()); // 压缩
/*
        FileServiceBuilder fsb =
                FileService.builder(Paths.get(System.getProperty("user.dir"), "/html"));
        fsb.autoIndex(true);
        FileService fs = fsb.build();
        sb.serviceUnder("/html", fs);
        HttpFile index = HttpFile.of(Paths.get(System.getProperty("user.dir"), "/html/upload.html"));
        sb.serviceUnder("/", index.asService());//相当于缺省index.html
 */
        // 设置默认首页：访问 / 返回 HTML
        sb.service("/", (ctx, req) ->
                HttpResponse.of(HttpStatus.OK, MediaType.HTML_UTF_8,
                        "<html><body><h1>Welcome to My Catalog Service!</h1><h1><a href=\"../docs\">View the document</a></h1></body></html>")
        );
        //添加文档服务
        sb.serviceUnder("/docs", DocService.builder()
                //.exampleRequests("/v1/brands", "query")
                .build());
        //ssl
        //sb.https(8443).tls(new File("certificate.crt"), new File("private.key"), "myPassphrase");
        //sb.contextPath("/catalog/core/v1");测试没作用

        Server server = sb.http(PORT)
                .annotatedService("/catalog/core/v1", new UnitService())
                .annotatedService("/catalog/core/v1", new BrandService())
                .annotatedService("/catalog/core/v1", new CategoryService())
                .annotatedService("/catalog/core/v1", new ItemService())
                .build();
        server.closeOnJvmShutdown();
        server.start().join();
        System.out.printf("Server has been started. Serving dummy service at http://127.0.0.1:%d%n", server.activeLocalPort());
        LOGGER.info("Server has been started. Serving dummy service at http://127.0.0.1:{}", server.activeLocalPort());

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            LOGGER.info("Server stopped");
        }));
    }
}
