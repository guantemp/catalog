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

package catalog.hoprxi.core.rest;


import catalog.hoprxi.core.application.command.CategoryCreateCommand;
import catalog.hoprxi.core.application.command.CategoryDeleteCommand;
import catalog.hoprxi.core.application.command.CategoryMoveNodeCommand;
import catalog.hoprxi.core.application.command.CategoryRenameCommand;
import catalog.hoprxi.core.application.handler.CategoryCreateHandler;
import catalog.hoprxi.core.application.handler.CategoryDeleteHandler;
import catalog.hoprxi.core.application.handler.Handler;
import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.NotFoundException;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESCategoryQuery;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/20
 */
public class CategoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区
    private static final Pattern URI_REGEX = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");

    private static final CategoryQuery QUERY = new ESCategoryQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/categories/{id}")
    @Description("Retrieves the category information by the given category ID.")
    public HttpResponse find(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.findAsync(id); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                HttpData.ofUtf8("{\"Error\":\"Internal server error\"}"));
                    }
                    if (signal.hasValue()) { // 2. 有数据 → 先响应头
                        return Flux.concat(
                                Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build()), flux);
                    }
                    return Flux.just(
                            ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.JSON_UTF_8)
                                    .build(),
                            HttpData.ofUtf8(String.format("{\"Warn\":\"Category not found for id : %d }\"", id)));
                });
        return HttpResponse.of(responseStream);
    }

    @Get("/categories/{id}/children")
    public HttpResponse children(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.childrenAsync(id)
                .onErrorResume(NotFoundException.class, err ->
                        Flux.error(new NotFoundException("Category not found: " + id))); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        Throwable error = signal.getThrowable();
                        System.out.println(error);
                        if (error instanceof NotFoundException) {
                            return Flux.just(
                                    ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build(),
                                    HttpData.ofUtf8(String.format("{\"Error\":\"Category not found: %d\"}", id)));
                        } else {
                            return Flux.just(
                                    ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                    HttpData.ofUtf8("{\"Error\":\"Internal server error\"}"));
                        }
                    }
                    if (signal.hasValue()) { // 2. 有数据 → 先响应头
                        return Flux.concat(
                                Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build()), flux);
                    }
                    return Flux.just(
                            ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.JSON_UTF_8)
                                    .build(),
                            HttpData.ofUtf8(String.format("{\"Warn\":\"Not found category of id : %d }\"", id)));
                });
        return HttpResponse.of(responseStream);
    }

    @Get("/categories/{id}/descendants")
    public HttpResponse descendants(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.descendantsAsync(id); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        Throwable error = signal.getThrowable();
                        //System.out.println(error);
                        if (error instanceof NotFoundException) {
                            return Flux.just(
                                    ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build(),
                                    HttpData.ofUtf8(String.format("{\"Error\":\"Category not found: %d\"}", id)));
                        } else {
                            return Flux.just(
                                    ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                    HttpData.ofUtf8("{\"Error\":\"Internal server error\"}"));
                        }
                    }
                    if (signal.hasValue()) { // 2. 有数据 → 先响应头
                        return Flux.concat(
                                Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build()), flux);
                    }
                    return Flux.just(
                            ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.JSON_UTF_8)
                                    .build(),
                            HttpData.ofUtf8(String.format("{\"Warn\":\"Not found category of id : %d }\"", id)));
                });
        return HttpResponse.of(responseStream);
    }

    @Get("/categories/{id}/desc.")
    public HttpResponse desc(@Param("id") long id) {
        return descendants(id);
    }


    @Get("/categories/{id}/path")
    public HttpResponse path(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.pathAsync(id); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        Throwable error = signal.getThrowable();
                        //System.out.println(error);
                        if (error instanceof NotFoundException) {
                            return Flux.just(
                                    ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build(),
                                    HttpData.ofUtf8(String.format("{\"Error\":\"Category not found: %d\"}", id)));
                        } else {
                            return Flux.just(
                                    ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                    HttpData.ofUtf8("{\"Error\":\"Internal server error\"}"));
                        }
                    }
                    if (signal.hasValue()) { // 2. 有数据 → 先响应头
                        return Flux.concat(
                                Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build()), flux);
                    }
                    return Flux.just(
                            ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.JSON_UTF_8)
                                    .build(),
                            HttpData.ofUtf8(String.format("{\"Warn\":\"Not found category of id : %d }\"", id)));
                });
        return HttpResponse.of(responseStream);
    }

    @Get("/categories/root")
    public HttpResponse root() {
        Flux<ByteBuf> dataFlux = QUERY.rootAsync(); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                HttpData.ofUtf8("{\"Error\":\"Internal server error\"}"));
                    }
                    if (signal.hasValue()) { // 2. 有数据 → 先响应头
                        return Flux.concat(
                                Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build()), flux);
                    }
                    return Flux.just(
                            ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.JSON_UTF_8)
                                    .build(),
                            HttpData.ofUtf8("{\"Warn\":\"Not found category root}\""));
                });
        return HttpResponse.of(responseStream);
    }

    @Get("/categories")
    public HttpResponse search(ServiceRequestContext ctx) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);

        Flux<ByteBuf> dataFlux = QUERY.searchAsync(search, offset, size);
        Flux<HttpObject> responseStream = dataFlux
                .map(HttpData::wrap)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) {// 信号->错误
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                HttpData.ofUtf8("{\"Error\":\"Internal server error\"}"));
                    }
                    if (signal.hasValue()) { // 2. 有数据 → 先响应头
                        return Flux.concat(
                                Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build()), flux);
                    }
                    return Flux.just(
                            ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.JSON_UTF_8)
                                    .build(),
                            HttpData.ofUtf8("{\"warn\":\"Not found\"}")
                    );
                });

        return HttpResponse.of(responseStream);
    }

    @Post("/categories")
    public HttpResponse create(ServiceRequestContext ctx, HttpData body) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ctx.blockingTaskExecutor().execute(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Category category = CategoryService.createCategory(parser);
                future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        String.format("{\"status\":\"success\",\"code\":201,\"message\":\"A category created,it's %s\"}", category)));
            } catch (Exception e) {
                future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        String.format("{\"status\":500,\"code\":500,\"message\":\"Can't create a category,cause by %s\"}", e.getMessage())));
            }
        });
        return HttpResponse.of(future);
    }

    private static Category createCategory(JsonParser parser) throws IOException {
        String name = null, alias = null, description = null;
        URL icon = null;
        long parentId = 0;
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "parent_id" -> parentId = parser.getValueAsLong();
                    case "name" -> name = parser.getValueAsString();
                    case "alias" -> alias = parser.getValueAsString();
                    case "description" -> description = parser.getValueAsString();
                    case "icon" -> icon = URI.create(parser.getValueAsString()).toURL();
                }
            }
        }
        CategoryCreateCommand command = new CategoryCreateCommand(parentId, name, alias, description, icon);
        Handler<CategoryCreateCommand, Category> handler = new CategoryCreateHandler();
        return handler.execute(command);
    }

    @StatusCode(201)
    @Put("/categories/{id}")
    public HttpResponse update(ServiceRequestContext ctx, HttpData body, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");

        return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8,
                "{\"status\":\"success\",\"code\":200,\"message\":\"A brand updated,it\"}");
    }

    private void update(HttpData body, long id) {
        String name = null, alias = null, description = null;
        URL icon = null;
        long parentId = -1L;
        try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "parent_id" -> parentId = parser.getValueAsLong();
                        case "name" -> name = parser.getValueAsString();
                        case "alias" -> alias = parser.getValueAsString();
                        case "description" -> description = parser.getValueAsString();
                        case "icon" -> icon = URI.create(parser.getValueAsString()).toURL();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (name != null || alias != null) {
            CategoryRenameCommand renameComm = new CategoryRenameCommand(id, name, alias);
        }
        if (parentId != Long.MIN_VALUE)
            new CategoryMoveNodeCommand(id, parentId);
         /*
            if (description != null)
                commands.add(new CategoryChangeDescriptionCommand(id, description));
            if (icon != null)
                commands.add(new CategoryChangeIconCommand(id, URI.create(icon).toURL()));
             */
    }

    private boolean validate(JsonGenerator generator, boolean root, String parentId, String name, String alias, String description, String uri) throws IOException {
        boolean result = true;
        if (!root && (parentId == null || parentId.isEmpty())) {
            generator.writeStartObject();
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_01");
            generator.writeStringField("message", "ParenId is required");
            generator.writeEndObject();
            result = false;
        }
        if (name == null || name.isEmpty()) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_02");
            generator.writeStringField("message", "name is required");
        }
        if (alias != null && alias.length() > 256) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_03");
            generator.writeStringField("message", "alias length rang is 1-256");
        }
        if (description != null && description.length() > 512) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_04");
            generator.writeStringField("message", "description length rang is 1-512");
        }
        if (uri != null && !URI_REGEX.matcher(uri).matches()) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_05");
            generator.writeStringField("message", "description length rang is 1-512");
        }
        return result;
    }

    @Delete("/categories/{id}")
    public HttpResponse delete(ServiceRequestContext ctx, @Param("id") long id) {
        return HttpResponse.of(CompletableFuture.supplyAsync(() -> {
            CategoryDeleteCommand command = new CategoryDeleteCommand(id);
            Handler<CategoryDeleteCommand, Boolean> handler = new CategoryDeleteHandler();
            handler.execute(command);

            return HttpResponse.of(
                    HttpStatus.OK,
                    MediaType.JSON_UTF_8,
                    String.format("{\"status\":\"success\",\"code\":200,\"message\":\"The category(id=%d) is moved to the recycle bin, you can retrieve it later in the recycle bin!\"}", id)
            );
        }, ctx.blockingTaskExecutor()).exceptionally(e ->
                HttpResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        MediaType.JSON_UTF_8,
                        String.format("{\"status\":\"error\",\"code\":500,\"message\":\"Delete failed: %s\"}", e.getMessage())
                )
        ));
    }
}

