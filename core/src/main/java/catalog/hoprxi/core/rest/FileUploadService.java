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
import com.linecorp.armeria.common.multipart.MultipartFile;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Consumes;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.to.ByteToHex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    public HttpResponse upload(ServiceRequestContext ctx, @Param("file") MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.file().length() == 0) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"请选择要上传的文件\"}");
        }
        String filename = multipartFile.filename();
        filename = Paths.get(filename).getFileName().toString();
        if (true) {
            String suffix = "";
            int dotIndex = filename.lastIndexOf(".");
            if (dotIndex > 0) {
                suffix = filename.substring(dotIndex);
            }
            filename = UUID.randomUUID().toString().replace("-", "") + suffix;
        }
        File file = multipartFile.file();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path target = Paths.get("./uploads", today, filename);
        Files.createDirectories(target.getParent());
        Files.move(file.toPath(), target);

        String scheme = ctx.sessionProtocol().isTls() ? "https" : "http";
        String host = ctx.request().authority();
        String accessUrl = String.format("%s://%s/uploads/%s/%s", scheme, host, today, filename);

        return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, String.format("""
                {
                    "code":200,
                    "message":"success",
                    "url":"%s"
                }""", accessUrl));
        /*
        ServiceRequestContext ctx = ServiceRequestContext.current();
        return HttpResponse.of(CompletableFuture.supplyAsync(() -> {
            AggregatedMultipart agg = multipart.aggregate().join();
            AggregatedBodyPart bodyPart = agg.field("file");
            // 2. 校验：文件是否存在
            if (bodyPart == null) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"未上传文件\"}");
            }
            try {

                String filename = bodyPart.filename();
                byte[] bytes = bodyPart.content().array();

                File tempFile = File.createTempFile("upload", null);
                Files.write(tempFile.toPath(), bytes);

                // 文件名处理
                String suffix = "";
                int dotIndex = filename.lastIndexOf(".");
                if (dotIndex > 0) {
                    suffix = filename.substring(dotIndex);
                }
                filename = UUID.randomUUID().toString() + suffix;

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
         */
    }

    @Post("/uploads")
    @Consumes("multipart/form-data")
    public HttpResponse uploads(ServiceRequestContext ctx, @Param("file") MultipartFile[] multipartFiles) throws IOException {
        if (multipartFiles == null || multipartFiles.length == 0) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"请选择要上传的文件\"}");
        }
        // 日期目录 yyyy-MM-dd
        String dateDir = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String scheme = ctx.sessionProtocol().isTls() ? "https" : "http";
        String domain = ctx.request().authority();
        List<Map<String, String>> successList = new ArrayList<>();
        List<String> failMsgList = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            String originalName = multipartFile.filename();
            if (originalName.isBlank()) {
                failMsgList.add("存在空文件名文件");
                continue;
            }
            // 1. 防路径穿越：只保留纯文件名
            originalName = Paths.get(originalName).getFileName().toString();
            File file = multipartFile.file();
            if (validFormat(file)) {
                // 3. 截取后缀，生成UUID唯一文件名
                String suffix = "";
                int dotIdx = originalName.lastIndexOf(".");
                if (dotIdx > 0) {
                    suffix = originalName.substring(dotIdx).toLowerCase();
                }
                String saveName = UUID.randomUUID() + suffix;
                // 4. 目标路径：uploads/日期/文件名
                Path target = Paths.get("./uploads", dateDir, saveName);
                Files.createDirectories(target.getParent());
                Files.move(file.toPath(), target);
                String url = String.format("%s://%s/uploads/%s/%s",
                        scheme, domain, dateDir, saveName);
                Map<String, String> item = new HashMap<>();
                item.put("originalName", originalName);
                item.put("url", url);
                successList.add(item);
            }
        }
        // 统一返回JSON
        String json = """
                {
                    "code": 200,
                    "message": "处理完成",
                    "successCount": %d,
                    "failCount": %d,
                    "fileList": %s,
                    "failList": %s
                }
                """.formatted(
                successList.size(),
                failMsgList.size(),
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(successList),
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(failMsgList)
        );
        return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, json);
    }


    private static boolean validFormat(File file) {
        //jpg,png,gif,pdf,doc,docx
        String[] heads = new String[]{"FFD8FF", "89504E47", "47494638", "25504446", "D0CF11E0", "504B0304"};
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int readLen = fis.read(header);
            if (readLen < 4) return false;

            String hex = ByteToHex.toHexStr(header).toUpperCase();
            for (String head : heads)
                if (head.equals(hex))
                    return true;
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
