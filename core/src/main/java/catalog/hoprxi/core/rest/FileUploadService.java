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
import com.linecorp.armeria.internal.shaded.guava.base.Optional;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Consumes;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

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
    public HttpResponse uploadFile(@Param("file") File file,
                                   @FormParam("renameFile") Boolean renameFile,
                                   @Param("rename") Optional<Boolean> rename,
                                   @Param("targetDir") Optional<String> targetDir) {
        if (FileUploadService.isValid(file)) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST,
                    MediaType.JSON_UTF_8,
                    "{\"error\": \" faltel file\"}");
        }
        boolean keepOriginal = rename.or(false);
        String subDir = targetDir.or("");
        try {
            String fileUrl = FileUploadService.upload(file, keepOriginal, subDir);
        } catch (IOException e) {
            LOGGER.error("Failed to store file", e);
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                    MediaType.JSON_UTF_8,
                    "Storage failed: " + e.getMessage());
        }
        return HttpResponse.of(HttpStatus.BAD_REQUEST,
                MediaType.JSON_UTF_8,
                "{\"error\": \" faltel file\"}");
    }

    private static String upload(File file, boolean keepOriginal, String subDir) throws IOException {
        Path uploadBasePath = Paths.get("./uploads").toAbsolutePath().normalize();
        Path tempPath = Paths.get("./tmp_uploads").toAbsolutePath().normalize();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path finalDir = uploadBasePath.resolve(subDir).resolve(dateDir);
        Files.createDirectories(finalDir);

        String fileName = file.getName();
        if (!keepOriginal) {
            String extension = "";//扩展名
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx >= 0) {
                extension = fileName.substring(dotIdx);
            }
            fileName = UUID.randomUUID() + extension;
        }

        Path targetPath = finalDir.resolve(fileName);
        Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        // 3. 生成访问 URL
        String relativePath = Paths.get(subDir, dateDir, fileName).toString().replace('\\', '/');
        return staticUrlPrefix + relativePath;
    }

    private static boolean isValid(File uploadedFile) {
        return true;
    }

}
