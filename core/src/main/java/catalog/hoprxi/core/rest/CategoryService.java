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


import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.handler.*;
import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.NotFoundException;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESCategoryQuery;
import com.fasterxml.jackson.core.*;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final Pattern URI_REGEX = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final CategoryQuery QUERY = new ESCategoryQuery();
    private static final CategoryRepository REPOSITORY = new PsqlCategoryRepository();

    @Get("/categories/{id}")
    @Description("Retrieves the category information by the given category ID.")
    public Mono<HttpResponse> find(@Param("id") long id) {
        return QUERY.findAsync(id)
                .map(byteBuf -> HttpResponse.of(
                        ResponseHeaders.builder(HttpStatus.OK)
                                .contentType(MediaType.JSON_UTF_8)
                                .build(),
                        HttpData.wrap(byteBuf)
                ))
                .onErrorResume(NotFoundException.class, error ->
                        Mono.just(HttpResponse.of(
                                ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build(),
                                HttpData.ofUtf8(String.format("{\"Error\":\"Category not found: %d\"}", id))
                        ))
                )
                .onErrorResume(error ->
                        Mono.just(HttpResponse.of(
                                ResponseHeaders.builder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build(),
                                HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                        ))
                );
    }

    @Get("/categories/{id}/children")
    public HttpResponse children(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.childrenAsync(id)
                .onErrorResume(NotFoundException.class, err ->
                        Flux.error(new NotFoundException("Category not found: " + id))); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> stream = dataFlux
                .map(HttpData::wrap)
                .transform(flux -> {
                    AtomicBoolean headersSent = new AtomicBoolean(false);
                    return flux.concatMap(data -> {
                        if (!headersSent.getAndSet(true)) {
                            // 第一个数据：先响应头 200，再发数据
                            return Flux.concat(
                                    Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build()),
                                    Flux.just(data)
                            );
                        }
                        return Flux.just(data);
                    });
                })
                .switchIfEmpty(Flux.just(
                        ResponseHeaders.of(HttpStatus.NOT_FOUND),
                        HttpData.ofUtf8("{\"Warn\":\"Category not found for\"}")
                ))
                .onErrorResume(throwable -> {
                    //log.error("Data stream error", throwable);
                    return Flux.just(
                            ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                            HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                    );
                });
        return HttpResponse.of(stream);
    }

    @Get("/categories/{id}/descendants")
    public HttpResponse descendants(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.descendantsAsync(id); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> stream = dataFlux
                .map(HttpData::wrap)
                .transform(flux -> {
                    AtomicBoolean headersSent = new AtomicBoolean(false);
                    return flux.concatMap(data -> {
                        if (!headersSent.getAndSet(true)) {
                            // 第一个数据：先响应头 200，再发数据
                            return Flux.concat(
                                    Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build()),
                                    Flux.just(data)
                            );
                        }
                        return Flux.just(data);
                    });
                })
                .switchIfEmpty(Flux.just(
                        ResponseHeaders.of(HttpStatus.NOT_FOUND),
                        HttpData.ofUtf8("{\"Warn\":\"Category not found for : %s\"}")
                ))
                .onErrorResume(throwable -> {
                    //log.error("Data stream error", throwable);
                    return Flux.just(
                            ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                            HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                    );
                });
        return HttpResponse.of(stream);
    }

    @Get("/categories/{id}/desc.")
    public HttpResponse desc(@Param("id") long id) {
        return descendants(id);
    }


    @Get("/categories/{id}/path")
    public HttpResponse path(@Param("id") long id) {
        Flux<ByteBuf> dataFlux = QUERY.pathAsync(id); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> stream = dataFlux
                .map(HttpData::wrap)
                .transform(flux -> {
                    AtomicBoolean headersSent = new AtomicBoolean(false);
                    return flux.concatMap(data -> {
                        if (!headersSent.getAndSet(true)) {
                            // 第一个数据：先响应头 200，再发数据
                            return Flux.concat(
                                    Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build()),
                                    Flux.just(data)
                            );
                        }
                        return Flux.just(data);
                    });
                })
                .switchIfEmpty(Flux.just(
                        ResponseHeaders.of(HttpStatus.NOT_FOUND),
                        HttpData.ofUtf8("{\"Warn\":\"Not found path\"}")
                ))
                .onErrorResume(throwable -> {
                    //log.error("Data stream error", throwable);
                    return Flux.just(
                            ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                            HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                    );
                });
        return HttpResponse.of(stream);
    }

    @Get("/categories/root")
    public HttpResponse root() {
        Flux<ByteBuf> dataFlux = QUERY.rootAsync(); // 假设返回 Flux<ByteBuf>
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> stream = dataFlux
                .map(HttpData::wrap)
                .transform(flux -> {
                    AtomicBoolean headersSent = new AtomicBoolean(false);
                    return flux.concatMap(data -> {
                        if (!headersSent.getAndSet(true)) {
                            // 第一个数据：先响应头 200，再发数据
                            return Flux.concat(
                                    Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build()),
                                    Flux.just(data)
                            );
                        }
                        return Flux.just(data);
                    });
                })
                .switchIfEmpty(Flux.just(
                        ResponseHeaders.of(HttpStatus.NOT_FOUND),
                        HttpData.ofUtf8("{\"Warn\":\"Category not found\"}")
                ))
                .onErrorResume(throwable -> {
                    //log.error("Data stream error", throwable);
                    return Flux.just(
                            ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                            HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                    );
                });
        return HttpResponse.of(stream);
    }

    @Get("/categories")
    public HttpResponse search(ServiceRequestContext ctx) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        Flux<ByteBuf> dataFlux = QUERY.searchAsync(search, offset, size);
        //dataFlux.count().subscribe(count -> System.out.println("总共收到 " + count + " 个 ByteBuf"));
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> stream = dataFlux
                .map(HttpData::wrap)
                .transform(flux -> {
                    AtomicBoolean headersSent = new AtomicBoolean(false);
                    return flux.concatMap(data -> {
                        if (!headersSent.getAndSet(true)) {
                            // 第一个数据：先响应头 200，再发数据
                            return Flux.concat(
                                    Flux.just(ResponseHeaders.builder(HttpStatus.OK)
                                            .contentType(MediaType.JSON_UTF_8)
                                            .build()),
                                    Flux.just(data)
                            );
                        }
                        return Flux.just(data);
                    });
                })
                .switchIfEmpty(Flux.just(
                        ResponseHeaders.of(HttpStatus.NOT_FOUND),
                        HttpData.ofUtf8(String.format("{\"Warn\":\"Category not found for : %s\"}", search))
                ))
                .onErrorResume(throwable -> {
                    //log.error("Data stream error", throwable);
                    return Flux.just(
                            ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                            HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                    );
                });
        return HttpResponse.of(stream);
    }

    @Post("/categories")
    public HttpResponse create(ServiceRequestContext ctx, HttpData body) {
        RequestHeaders headers = ctx.request().headers();
        MediaType contentType = headers.contentType();
        if (contentType == null || !(MediaType.JSON.is(contentType) || MediaType.JSON_UTF_8.is(contentType)))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MediaType.JSON_UTF_8,
                    "{\"status\":415,\"message\":\"Expected JSON content\"}");
        if (body.isEmpty())
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"status\":400,\"message\":\"Empty request body\"}");
        CompletableFuture<HttpResponse> future = CompletableFuture.supplyAsync(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream());
                 ByteArrayOutputStream baos = new ByteArrayOutputStream(); JsonGenerator generator = JSON_FACTORY.createGenerator(baos, JsonEncoding.UTF8)) {
                Category category = CategoryService.createCategory(parser);
                CategoryService.writeCategoryResponse(generator, category);
                LOGGER.info("category created: {}", category);
                return HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        HttpData.wrap(baos.toByteArray()));
            } catch (JsonProcessingException e) {
                // 5. 统一异常处理，避免泄露敏感信息
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"status\":400,\"message\":\"Invalid JSON format\"}");
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"message\":\"Internal server error\"}");
            }
        }, VIRTUAL_EXECUTOR);
        return HttpResponse.of(future.exceptionally(throwable -> {
            LOGGER.error("Unexpected error in async task", throwable);
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                    "{\"status\":500,\"message\":\"Unexpected error\"}");
        }));
    }

    private static void writeCategoryResponse(JsonGenerator generator, Category category) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("status", "success");
        generator.writeStringField("message", "A category created or update, it's " + category.name().name());

        generator.writeObjectFieldStart("category");
        generator.writeNumberField("parentId", category.parentId());
        generator.writeNumberField("id", category.id());

        generator.writeObjectFieldStart("name");
        generator.writeStringField("name", category.name().name());
        generator.writeStringField("shortName", category.name().shortName());
        generator.writeEndObject();
        // 优化 null 值处理，语义更清晰
        if (category.icon() != null) {
            generator.writeStringField("icon", category.icon().toExternalForm());
        } else {
            generator.writeNullField("icon");
        }

        generator.writeStringField("description", category.description());
        generator.writeEndObject();

        generator.writeEndObject();
        generator.flush();
    }


    private static Category createCategory(JsonParser parser) throws IOException {
        String name = null, shortName = null, description = null;
        URL icon = null;
        long parentId = 0L;
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "parent_id" -> parentId = parser.getValueAsLong();
                    case "name" -> name = parser.getValueAsString();
                    case "shortName" -> shortName = parser.getValueAsString();
                    case "description" -> description = parser.getValueAsString();
                    case "icon" -> icon = URI.create(parser.getValueAsString()).toURL();
                }
            }
        }
        CategoryCreateCommand command = new CategoryCreateCommand(parentId, name, shortName, description, icon);
        Handler<CategoryCreateCommand, Category> handler = new CategoryCreateHandler();
        return handler.execute(command);
    }

    @Put("/categories/{id}")
    public HttpResponse update(ServiceRequestContext ctx, HttpData body, @Param("id") long id) {
        RequestHeaders headers = ctx.request().headers();
        MediaType contentType = headers.contentType();
        if (contentType == null || !(MediaType.JSON.is(contentType) || MediaType.JSON_UTF_8.is(contentType)))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MediaType.JSON_UTF_8,
                    "{\"status\":415,\"code\":415,\"message\":\"Expected JSON content\"}");
        if (body.isEmpty())
            return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                    "{\"status\":400,\"code\":400,\"message\":\"Empty request body\"}");
        CompletableFuture<HttpResponse> future = CompletableFuture.supplyAsync(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Category category = CategoryService.update(parser, id);
                return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8,
                        String.format("{\"status\":\"success\",\"code\":200,\"message\":\"A category has updated, it's %s\"}", category));
            } catch (JsonProcessingException e) {
                // 精细异常捕获：JSON 格式错误返回 400
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"status\":400,\"message\":\"Invalid JSON format\"}");
            } catch (Exception e) {
                // 其他业务或系统异常返回 500
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"message\":\"Internal server error\"}");
            }
        }, VIRTUAL_EXECUTOR);
        return HttpResponse.of(future.exceptionally(throwable -> {
            LOGGER.error("Unexpected error in async task", throwable);
            return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                    "{\"status\":500,\"message\":\"Unexpected error\"}");
        }));
    }

    private static Category update(JsonParser parser, long id) throws IOException {
        Category category = REPOSITORY.find(id);
        if (category == null) {
            return null;
        }
        UnitOfWork<Category> uow = new UnitOfWork<>();
        MacroInvoker<Category> invoker = new MacroInvoker<>(uow);
        String name = null, shortName = null;
        URL icon = null;
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "parent_id" ->
                            invoker.addCommand(new CategoryMoveCommand(id, parser.getValueAsLong())).bind(CategoryMoveCommand.class, new CategoryMoveHandler(uow));
                    case "name" -> name = parser.getValueAsString();
                    case "shortName" -> shortName = parser.getValueAsString();
                    case "description" ->
                            invoker.addCommand(new CategoryChangDescriptionCommand(id, parser.getValueAsString())).bind(CategoryChangDescriptionCommand.class, new CategoryDescriptionHandler(uow));
                    case "icon_url" -> URI.create(parser.getValueAsString()).toURL();
                }
            }
        }
        if (name != null || shortName != null) {
            invoker.addCommand(new CategoryRenameCommand(id, name, shortName)).bind(CategoryRenameCommand.class,new CategoryRenameHandler(uow));
        }
        category = invoker.execute(category, REPOSITORY::save);
        //System.out.println(category);
        return category;
    }

    private boolean validate(JsonGenerator generator, boolean root, String parentId, String name, String shortName, String description, String uri) throws IOException {
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
        if (shortName != null && shortName.length() > 256) {
            generator.writeStringField("status", "FAIL");
            generator.writeStringField("code", "10_05_03");
            generator.writeStringField("message", "shortName length rang is 1-256");
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
    public HttpResponse delete(@Param("id") long id) {
        return HttpResponse.of(CompletableFuture.supplyAsync(() -> {
            try {
                CategoryDeleteCommand delete = new CategoryDeleteCommand(id);
                Handler<CategoryDeleteCommand, Boolean> handler = new CategoryDeleteHandler();
                handler.execute(delete);
                // 1. 删除成功，推荐使用 204 No Content
                return HttpResponse.of(HttpStatus.NO_CONTENT);
            } catch (NotFoundException e) {
                // 2. 精细化异常处理：资源不存在返回 404
                return HttpResponse.of(HttpStatus.NOT_FOUND, MediaType.JSON_UTF_8,
                        String.format("{\"status\":\"error\",\"code\":404,\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                // 3. 其他未知异常返回 500
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":\"error\",\"code\":500,\"message\":\"Delete failed due to internal error\"}");
            }
        }, VIRTUAL_EXECUTOR)); // 4. 推荐使用虚拟线程替代 blockingTaskExecutor
    }
}

