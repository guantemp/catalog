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


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.common.multipart.MultipartFile;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Consumes;
import com.linecorp.armeria.server.annotation.Delete;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.to.ByteToHex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
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
                               @Param(value = "directory") @Nullable String directoryParam,
                               @Param("separateByDate") @Nullable Boolean separateByDateParam,
                               @Param(value = "validSpec") @Nullable String validSpecParam) {
        if (multipartFile == null || multipartFile.file().length() == 0) {
            //LOGGER.info("请选择要上传的文件");
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"请选择要上传的文件\"}");
        }
        // 验证文件格式（头信息）
        EnumSet<FileValidSpec> specs = EnumSet.noneOf(FileValidSpec.class);
        if (validSpecParam != null && !validSpecParam.trim().isEmpty()) {
            try {
                // 将传入的字符串转换为对应的枚举值
                FileValidSpec spec = FileValidSpec.valueOf(validSpecParam.trim().toUpperCase());
                specs.add(spec);
            } catch (IllegalArgumentException e) {
                // 如果传入的枚举名称无效，返回错误
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"无效的文件校验规格: " + validSpecParam + "\"}");
            }
        }
        // 获取所有支持的后缀，用于错误提示
        Set<String> allSupportedExtensions = new LinkedHashSet<>();
        for (FileValidSpec spec : specs) {
            allSupportedExtensions.addAll(Arrays.asList(spec.support()));
        }
        String supportedExtensions = String.join(", ", allSupportedExtensions);

        File tempFile = multipartFile.file();
        // ★ 关键：任意一个验证器通过即可（OR 逻辑）
        boolean isValid = true;
        // 只有当存在验证器时，才进行校验
        if (!specs.isEmpty()) {
            isValid = false; // 只要有验证器，初始状态先设为 false
            for (FileValidSpec spec : specs) {
                if (spec.validFormat(tempFile)) {
                    isValid = true;
                    break; // 只要有一个通过，立即跳出循环，判定为合法
                }
            }
        }
        if (!isValid) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"不支持的文件格式，允许的格式： " + supportedExtensions + "\"}");
        }

        boolean rename = (renameParam != null) ? renameParam : true;
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

            Path basePath = Paths.get(UPLOAD_DIRECTORIES);
            if (directoryParam != null && !directoryParam.trim().isEmpty()) {
                // 简单安全过滤：只允许字母、数字、下划线、连字符，防止路径遍历
                String safeDir = directoryParam.trim()
                        .replaceAll("[^a-zA-Z0-9_\\-]", "")  // 移除非法字符
                        .replaceAll("/+", "");               // 移除多余斜杠
                if (!safeDir.isEmpty()) {
                    basePath = basePath.resolve(safeDir);
                }
            }

            boolean separateByDate = (separateByDateParam != null) ? separateByDateParam : true;
            String date = separateByDate ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            if (separateByDate && !date.isEmpty()) {
                basePath = basePath.resolve(date);
            }
            // 4. 创建目录并移动文件
            Path target = basePath.resolve(filename);
            Files.createDirectories(target.getParent());
            Files.move(multipartFile.file().toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            String scheme = ctx.request().headers().get("X-Forwarded-Proto");
            if (scheme == null || scheme.isEmpty()) { // 如果没有经过 Nginx（例如本地直连测试），则回退到原始判断
                scheme = ctx.sessionProtocol().isTls() ? "https" : "http";
            }
            String host = ctx.request().authority();
            // ---- 修改访问 URL 构造 ----
            // 使用 basePath 和 filename 生成相对路径，并转换为 URL 格式
            Path urlPath = basePath.resolve(filename);  // 与 target 相同，但 target 可能含绝对路径（若 UPLOAD_DIRECTORIES 是绝对路径）
            String urlPathStr = urlPath.toString().replace('\\', '/');  // 统一使用正斜杠
            if (!urlPathStr.startsWith("/")) {
                urlPathStr = "/" + urlPathStr;  // 确保以斜杠开头（若 UPLOAD_DIRECTORIES 不是以 / 开头）
            }
            String accessUrl = String.format("%s://%s%s", scheme, host, urlPathStr);

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
                               @Param("files") @Nullable List<MultipartFile> multipartFiles,
                               @Param(value = "rename") @Nullable Boolean renameParam,
                               @Param(value = "separateByDate") @Nullable Boolean separateByDateParam,
                               @Param(value = "directory") @Nullable String directoryParam,
                               @Param(value = "validSpec") @Nullable String validSpecParam) {
        System.out.println("Armeria 收到的原始请求: " + ctx.request().path());
        // 1. 校验文件列表
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"请选择要上传的文件\"}");
        }

        boolean rename = (renameParam != null) ? renameParam : true;
        boolean separateByDate = (separateByDateParam != null) ? separateByDateParam : true;
        // 2. 构建基础路径
        String date = separateByDate ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        Path basePath = Paths.get(UPLOAD_DIRECTORIES);
        if (directoryParam != null && !directoryParam.trim().isEmpty()) {
            String safeDir = directoryParam.trim()
                    .replaceAll("[^a-zA-Z0-9_\\-]", "")
                    .replaceAll("/+", "");
            if (!safeDir.isEmpty()) {
                basePath = basePath.resolve(safeDir);
            }
        }
        if (separateByDate && !date.isEmpty()) {
            basePath = basePath.resolve(date);
        }

        EnumSet<FileValidSpec> specs = EnumSet.noneOf(FileValidSpec.class);
        System.out.println("validSpecParam:"+validSpecParam);
        if (validSpecParam != null && !validSpecParam.trim().isEmpty()) {
            try {
                // 将传入的字符串转换为对应的枚举值
                FileValidSpec spec = FileValidSpec.valueOf(validSpecParam.trim().toUpperCase());
                specs.add(spec);
            } catch (IllegalArgumentException e) {
                // 如果传入的枚举名称无效，返回错误
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"无效的文件校验规格: " + validSpecParam + "\"}");
            }
        }
        // 动态获取所有支持的后缀，用于错误提示
        Set<String> allSupportedExtensions = new LinkedHashSet<>();
        for (FileValidSpec spec : specs) {
            allSupportedExtensions.addAll(Arrays.asList(spec.support()));
        }
        String supportedExtensions = String.join(", ", allSupportedExtensions);
        System.out.println(specs);

        // 3. 获取协议和主机
        String scheme = ctx.request().headers().get("X-Forwarded-Proto");
        if (scheme == null || scheme.isEmpty()) {
            scheme = ctx.sessionProtocol().isTls() ? "https" : "http";
        }
        String host = ctx.request().authority();
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0, failCount = 0;
        // 4. 逐个处理文件
        for (MultipartFile multipartFile : multipartFiles) {
            Map<String, Object> result = new LinkedHashMap<>();
            String originalFilename = multipartFile.filename();
            result.put("filename", originalFilename);

            // 检查文件是否为空
            if (multipartFile.file().length() == 0) {
                result.put("status", "failed");
                result.put("error", "文件为空");
                results.add(result);
                failCount++;
                continue;
            }

            // ★ 关键：任意一个验证器通过即可（OR 逻辑）
            boolean isValid = true;
            // 只有当存在验证器时，才进行校验
            if (!specs.isEmpty()) {
                isValid = false; // 只要有验证器，初始状态先设为 false
                for (FileValidSpec spec : specs) {
                    if (spec.validFormat(multipartFile.file())) {
                        isValid = true;
                        break; // 只要有一个通过，立即跳出循环，判定为合法
                    }
                }
            }
            if (!isValid) {
                result.put("status", "failed");
                result.put("error", "不支持的文件格式，允许的格式：" + supportedExtensions);
                results.add(result);
                failCount++;
                continue;
            }

            try { // 处理文件名
                String filename = Paths.get(originalFilename).getFileName().toString();
                if (rename) {
                    String suffix = "";
                    int dotIndex = filename.lastIndexOf('.');
                    if (dotIndex > 0) {
                        suffix = filename.substring(dotIndex);
                    }
                    filename = UUID.randomUUID().toString().replace("-", "") + suffix;
                }

                Path target = basePath.resolve(filename);
                Files.createDirectories(target.getParent());
                Files.move(multipartFile.file().toPath(), target);

                // 生成访问 URL
                Path urlPath = basePath.resolve(filename);
                String urlPathStr = urlPath.toString().replace('\\', '/');
                if (!urlPathStr.startsWith("/")) {
                    urlPathStr = "/" + urlPathStr;
                }
                String accessUrl = String.format("%s://%s%s", scheme, host, urlPathStr);

                result.put("status", "success");
                result.put("url", accessUrl);
                successCount++;

            } catch (FileAlreadyExistsException e) {
                result.put("status", "failed");
                result.put("error", "文件已存在: " + e.getFile());
                failCount++;
            } catch (IOException e) {
                result.put("status", "failed");
                result.put("error", e.getMessage());
                failCount++;
            } catch (Exception e) {
                result.put("status", "failed");
                result.put("error", "未知错误: " + e.getMessage());
                failCount++;
            }
            results.add(result);
        }

        // 5. 构建 JSON 响应
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonGenerator gen = JSON_FACTORY.createGenerator(baos)) {
            gen.writeStartObject();
            gen.writeNumberField("code", 200);
            gen.writeStringField("message", "上传完成，成功 " + successCount + " 个，失败 " + failCount + " 个");
            gen.writeArrayFieldStart("results");
            for (Map<String, Object> res : results) {
                gen.writeStartObject();
                gen.writeStringField("filename", res.get("filename").toString());
                gen.writeStringField("status", res.get("status").toString());
                if (res.containsKey("url")) {
                    gen.writeStringField("url", res.get("url").toString());
                }
                if (res.containsKey("error")) {
                    gen.writeStringField("error", res.get("error").toString());
                }
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeEndObject();
            gen.flush();
        } catch (IOException e) {
            LOGGER.error("生成响应 JSON 失败", e);
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                    "{\"code\":500,\"message\":\"响应生成失败\"}");
        }

        return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, baos.toString(StandardCharsets.UTF_8));
    }


    private static boolean validFormat(File file) {
        String[] heads = new String[]{"FFD8FF",      // JPEG
                "89504E47",    // PNG
                "47494638",    // GIF
                "25504446",    // PDF
                "D0CF11E0",    // DOC, XLS, PPT (老版 Office)
                "504B0304",    // DOCX, XLSX, PPTX (新版 Office)
                "424D",        // BMP
                "52494646",    // WEBP (RIFF 头，需再校验，但此处只做初筛)
                "00000100",    // ICO
                "49492A00",    // TIFF (小端)
                "4D4D002A"     // TIFF (大端)};
        };
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int readLen = fis.read(header);
            if (readLen < 4) return true;

            String hex = ByteToHex.toHexStr(header).toUpperCase();
            for (String head : heads) {
                if (hex.startsWith(head)) {  // 改为前缀匹配
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException("读取文件头失败", e);
        }
    }

    @Delete("/upload")
    @Consumes("application/json")
    public HttpResponse deleteFile(@Param("url") String fileUrl) {
        // 1. 参数校验
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"缺少文件 URL 参数\"}");
        }

        try {
            // 2. 解析完整 URL，提取路径部分（例如 /uploads/logo/2025-01-01/abc.png）
            java.net.URL url = new java.net.URL(fileUrl);
            String path = url.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"无效的文件 URL\"}");
            }

            // 3. 安全过滤：移除 ..、/./ 等危险字符，防止路径遍历
            String safePath = path
                    .replaceAll("\\\\.\\\\.", "")      // 移除 ..
                    .replaceAll("/\\\\./", "/")       // 移除 /.
                    .replaceAll("//+", "/");           // 合并多余斜杠
            if (!safePath.matches("^/[a-zA-Z0-9_\\-./]+$")) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"URL 路径包含非法字符\"}");
            }

            // 4. 获取上传基础目录（假设 UPLOAD_DIRECTORIES 为 "uploads" 或类似）
            String uploadDir = UPLOAD_DIRECTORIES; // 例如 "uploads"
            String baseDirName = uploadDir.replaceAll("^/|/$", "");
            if (baseDirName.isEmpty()) {
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"code\":500,\"message\":\"服务器配置错误：上传目录未设置\"}");
            }

            // 检查路径是否以 "/{baseDirName}/" 开头
            String prefix = "/" + baseDirName + "/";
            if (!safePath.startsWith(prefix)) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"URL 路径格式不正确，缺少基础目录前缀\"}");
            }

            // 提取相对路径（去掉前缀）
            String relativePath = safePath.substring(prefix.length());
            if (relativePath.isEmpty()) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"code\":400,\"message\":\"无法确定文件相对路径\"}");
            }

            // 5. 构建物理文件路径
            Path baseDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetFile = baseDirPath.resolve(relativePath).normalize();

            // 6. 二次安全校验：确保文件在基础目录下（防止符号链接越权）
            if (!targetFile.startsWith(baseDirPath)) {
                return HttpResponse.of(HttpStatus.FORBIDDEN, MediaType.JSON_UTF_8,
                        "{\"code\":403,\"message\":\"无权访问该文件\"}");
            }

            // 7. 检查文件是否存在且不是目录
            if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
                return HttpResponse.of(HttpStatus.NOT_FOUND, MediaType.JSON_UTF_8,
                        "{\"code\":404,\"message\":\"文件不存在\"}");
            }

            // 8. 执行删除
            Files.delete(targetFile);
            return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8,
                    "{\"code\":200,\"message\":\"删除成功\"}");
        } catch (java.net.MalformedURLException e) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"code\":400,\"message\":\"无效的 URL 格式\"}");
        } catch (IOException e) {
            LOGGER.error("删除文件失败: {}", fileUrl, e);
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                    String.format("{\"code\":500,\"message\":\"删除失败: %s\"}", e.getMessage()));
        }
    }
}
