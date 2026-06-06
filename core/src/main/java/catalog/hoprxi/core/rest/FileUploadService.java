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
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.common.multipart.MultipartFile;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Consumes;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
    private static final String UPLOAD_DIRECTORIES;

    static {
        Config config = ConfigFactory.load("core").getConfig("upload");
        UPLOAD_DIRECTORIES = config.hasPath("directory") ? config.getString("directory") : "uploads";
    }

    /**
     * 文件上传处理接口（支持多文件）
     * 客户端请求示例：POST /upload （Content-Type: multipart/form-data）
     * 文件字段名：file
     */
    @Post("/upload")
    @Consumes("multipart/form-data")
    public HttpResponse upload(ServiceRequestContext ctx, @Param("file") MultipartFile multipartFile,
                               @Param(value = "rename") @Nullable Boolean renameParam,
                               @Param("separateByDate") @Nullable Boolean separateByDateParam) {
        if (multipartFile == null || multipartFile.file().length() == 0) {
            //LOGGER.info("请选择要上传的文件");
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"请选择要上传的文件\"}");
        }
        boolean rename = (renameParam != null) ? renameParam : true;
        boolean separateByDate = (separateByDateParam != null) ? separateByDateParam : true;
        try {
            String filename = Paths.get(multipartFile.filename()).getFileName().toString();
            if (rename) {
                String suffix = "";
                int dotIndex = filename.lastIndexOf(".");
                if (dotIndex > 0) {
                    suffix = filename.substring(dotIndex);
                }
                filename = UUID.randomUUID().toString().replace("-", "") + suffix;
            }
            String date = separateByDate ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            Path target = Paths.get(UPLOAD_DIRECTORIES, date, filename);
            Files.createDirectories(target.getParent());

            Files.move(multipartFile.file().toPath(), target);

            String scheme = ctx.request().headers().get("X-Forwarded-Proto");
            if (scheme == null || scheme.isEmpty()) { // 如果没有经过 Nginx（例如本地直连测试），则回退到原始判断
                scheme = ctx.sessionProtocol().isTls() ? "https" : "http";
            }
            String host = ctx.request().authority();
            String accessUrl = String.format("%s://%s/uploads/%s/%s", scheme, host, date, filename);

            return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, String.format("""
                    {
                        "code":200,
                        "message":"success",
                        "url":"%s"
                    }""", accessUrl));
        } catch (IOException e) {
            LOGGER.error("文件上传发生严重错误，目标目录: {}", UPLOAD_DIRECTORIES, e);
            String json = String.format("{\"code\":500,\"message\":\"%s\"}", e.getMessage());
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8, json);
        }
    }

    @Post("/uploads")
    @Consumes("multipart/form-data")
    public HttpResponse upload(ServiceRequestContext ctx,
                               @Param("files") MultipartFile[] multipartFiles, // 1. 将参数改为数组，字段名建议改为 "files"
                               @Param(value = "rename") @Nullable Boolean renameParam,
                               @Param("separateByDate") @Nullable Boolean separateByDateParam) {
        // 2. 校验数组是否为空
        if (multipartFiles == null || multipartFiles.length == 0) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"请选择要上传的文件\"}");
        }
        boolean rename = (renameParam != null) ? renameParam : true;
        boolean separateByDate = (separateByDateParam != null) ? separateByDateParam : true;
        // 3. 用于收集所有成功上传文件的访问URL
        List<String> uploadedUrls = new ArrayList<>();

        // 4. 获取请求头中的协议和主机信息（只需获取一次，提高性能）
        String scheme = ctx.request().headers().get("X-Forwarded-Proto");
        if (scheme == null || scheme.isEmpty()) {
            scheme = ctx.sessionProtocol().isTls() ? "https" : "http";
        }
        String host = ctx.request().authority();

        try {
            // 5. 遍历每一个上传的文件
            for (MultipartFile multipartFile : multipartFiles) {
                // 过滤掉前端可能传过来的空文件
                if (multipartFile == null || multipartFile.file().length() == 0) {
                    continue;
                }

                // 处理文件名（重命名逻辑保持不变）
                String filename = Paths.get(multipartFile.filename()).getFileName().toString();
                if (rename) {
                    String suffix = "";
                    int dotIndex = filename.lastIndexOf(".");
                    if (dotIndex > 0) {
                        suffix = filename.substring(dotIndex);
                    }
                    filename = UUID.randomUUID().toString().replace("-", "") + suffix;
                }

                // 按日期生成路径并自动创建目录
                String date = separateByDate ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
                Path target = Paths.get(UPLOAD_DIRECTORIES, date, filename);
                Files.createDirectories(target.getParent());

                // 移动文件到目标路径
                Files.move(multipartFile.file().toPath(), target);

                // 拼接访问URL并加入列表
                String accessUrl = String.format("%s://%s/uploads/%s/%s", scheme, host, date, filename);
                uploadedUrls.add(accessUrl);
            }

            // 6. 返回包含所有文件URL的JSON数组
            // 这里使用简单的字符串拼接构建JSON，如果你的项目中有JSON库（如Jackson/Gson），建议替换
            String urlsJsonArray = String.join(",", uploadedUrls.stream()
                    .map(url -> "\"" + url + "\"")
                    .toArray(String[]::new));

            String jsonResponse = String.format("""
                {
                    "code": 200,
                    "message": "success",
                    "urls": [%s]
                }""", urlsJsonArray);

            return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, jsonResponse);

        } catch (IOException e) {
            LOGGER.error("多文件上传发生严重错误，目标目录: {}", UPLOAD_DIRECTORIES, e);
            String json = String.format("{\"code\":500,\"message\":\"%s\"}", e.getMessage());
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8, json);
        }
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
