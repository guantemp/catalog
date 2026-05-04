/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.rest;


import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.multipart.AggregatedMultipart;
import com.linecorp.armeria.common.multipart.Multipart;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Consumes;
import com.linecorp.armeria.server.annotation.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/4/18
 */

public class FileUploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");

    /**
     * 文件上传处理接口（支持多文件）
     * 客户端请求示例：POST /upload （Content-Type: multipart/form-data）
     * 文件字段名：file
     */
    @Post("/upload")
    @Consumes("multipart/form-data")
    public HttpResponse upload(Multipart multipart) throws IOException {
        ServiceRequestContext ctx = ServiceRequestContext.current();
        return HttpResponse.of(CompletableFuture.supplyAsync(() -> {
            try {
                AggregatedMultipart agg = multipart.aggregate().join();
                System.out.println(agg.field("file").filename());
                byte[] bytes = agg.field("file").content().array();

                if (bytes == null) {
                    return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.PLAIN_TEXT_UTF_8, "No file");
                }

                File tempFile = File.createTempFile("upload", null);
                Files.write(tempFile.toPath(), bytes);

                String filename = UUID.randomUUID().toString().replace("-", "");
                Path target = Paths.get("./uploads", filename);
                Files.createDirectories(target.getParent());
                Files.move(tempFile.toPath(), target);


                return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8,
                        "{\"code\":200,\"message\":\"success\"}");
            } catch (Exception e) {
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, ctx.blockingTaskExecutor()).exceptionally(e ->
                HttpResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        MediaType.JSON_UTF_8,
                        String.format("{\"status\":\"error\",\"code\":500,\"message\":\"Delete failed: %s\"}", e.getMessage())
                )
        ));
    }

    private static boolean validateFile(File file) {
        return true;
    }
}
