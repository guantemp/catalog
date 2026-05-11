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


import catalog.hoprxi.core.application.command.BrandCreateCommand;
import catalog.hoprxi.core.application.command.BrandDeleteCommand;
import catalog.hoprxi.core.application.command.BrandUpdateCommand;
import catalog.hoprxi.core.application.handler.BrandCreateHandler;
import catalog.hoprxi.core.application.handler.BrandDeleteHandler;
import catalog.hoprxi.core.application.handler.BrandUpdateHandler;
import catalog.hoprxi.core.application.handler.Handler;
import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.application.query.NotFoundException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESBrandQuery;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2026/4/5
 */
public class BrandService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final EnumSet<SortFieldEnum> SUPPORT_SORT_FIELD = EnumSet.of(SortFieldEnum._ID, SortFieldEnum.ID, SortFieldEnum.NAME, SortFieldEnum._NAME);
    private static final BrandQuery QUERY = new ESBrandQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();

    @Get("/brands/{id}")
    @Description("Retrieves the brand information by the given brand ID.")
    public HttpResponse find(@Param("id") long id) {
        Mono<ByteBuf> dataMono = QUERY.findAsync(id);
        // 将 Mono<ByteBuf> 转换为 Flux<HttpObject>
        Flux<HttpObject> responseFlux = dataMono
                .flatMapMany(byteBuf -> Flux.using(
                        () -> byteBuf,
                        buf -> Flux.just(// 业务逻辑：正常响应
                                ResponseHeaders.builder(HttpStatus.OK)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build(),
                                HttpData.wrap(buf) // Armeria 接管生命周期
                        ),
                        ReferenceCountUtil::release // 资源清理：仅在异常路径执行
                ))
                .onErrorResume(error -> {
                    if (error instanceof NotFoundException) {
                        return Flux.just(
                                ResponseHeaders.builder(HttpStatus.NOT_FOUND)
                                        .contentType(MediaType.JSON_UTF_8)
                                        .build(),
                                HttpData.ofUtf8(String.format("{\"Error\":\"Brand not found: %d\"}", id))
                        );
                    } else {
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR),
                                HttpData.ofUtf8("{\"Error\":\"Internal server error\"}")
                        );
                    }
                });
        return HttpResponse.of(responseFlux);
    }

    @Get("/brands")
    public HttpResponse search(ServiceRequestContext ctx) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        String cursor = params.get("cursor", "");
        SortFieldEnum sortField = SortFieldEnum.of(params.get("sort", "_ID"));
        // 1. 前置校验：不支持的排序 → 直接返回 错误 Flux
        if (!SUPPORT_SORT_FIELD.contains(sortField)) {
            return HttpResponse.of(
                    ResponseHeaders.builder(HttpStatus.BAD_REQUEST)  // 建议用400
                            .contentType(MediaType.JSON_UTF_8)
                            .build(),
                    HttpData.ofUtf8("{\"status\":\"error\",\"code\":400,\"message\":\"Not support sort field\"}"));
        }

        Flux<ByteBuf> dataFlux = cursor.isBlank()
                ? QUERY.searchAsync(search, offset, size, sortField)
                : QUERY.searchAsync(search, size, cursor, sortField);
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
                        HttpData.ofUtf8(String.format("{\"Warn\":\"Category not found for id : %s\"}", search))
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

    @Post("/brands")
    public HttpResponse create(ServiceRequestContext ctx, HttpData body) {
        RequestHeaders headers = ctx.request().headers();
        MediaType contentType = headers.contentType();
        if (contentType == null || !(MediaType.JSON.is(contentType) || MediaType.JSON_UTF_8.is(contentType)))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MediaType.JSON_UTF_8,
                    "{\"status\":415,\"code\":415,\"message\":\"Expected JSON content\"}");
        CompletableFuture<HttpResponse> future = CompletableFuture.supplyAsync(() -> {
            byte[] content = body.array();
            if (content.length == 0) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"status\":400,\"code\":400,\"message\":\"Empty request body\"}");
            }
            try (JsonParser parser = JSON_FACTORY.createParser(content)) {
                Brand brand = BrandService.createBrand(parser);
                return HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A brand created,it's %s\"}", brand);
            } catch (JsonProcessingException e) {
                // 5. 统一异常处理，避免泄露敏感信息
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"status\":400,\"message\":\"Invalid JSON format\"}");
            } catch (Exception e) {
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"message\":\"Internal server error\"}");
            }
        }, VIRTUAL_EXECUTOR);
        return HttpResponse.of(future);
    }

    private static Brand createBrand(JsonParser parser) throws IOException, URISyntaxException {
        String name = null, alias = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected JSON object");
        }
        while (parser.nextToken() != null) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name" -> name = parser.getValueAsString();
                    case "alias" -> alias = parser.getValueAsString();
                    case "story" -> story = parser.getValueAsString();
                    case "homepage" -> {
                        String urlStr = parser.getValueAsString();
                        if (urlStr != null) homepage = new URI(urlStr).toURL();
                    }
                    case "logo" -> {
                        String urlStr = parser.getValueAsString();
                        if (urlStr != null) logo = new URI(urlStr).toURL();
                    }
                    case "since" -> {
                        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            since = Year.of(parser.getIntValue());
                        }
                    }
                    //default -> parser.skipChildren();
                }
            }
        }
        BrandCreateCommand command = new BrandCreateCommand(name, alias, homepage, logo, since, story);
        Handler<BrandCreateCommand, Brand> handler = new BrandCreateHandler();
        return handler.execute(command);
    }

    @StatusCode(201)
    @Put("/brands/{id}")
    public HttpResponse update(ServiceRequestContext ctx, HttpData body, @Param("id") long id) {
        RequestHeaders headers = ctx.request().headers();
        MediaType contentType = headers.contentType();
        if (contentType == null || !(MediaType.JSON.is(contentType) || MediaType.JSON_UTF_8.is(contentType)))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MediaType.JSON_UTF_8,
                    "{\"status\":415,\"code\":415,\"message\":\"Expected JSON content\"}");
        CompletableFuture<HttpResponse> future = CompletableFuture.supplyAsync(() -> {
            byte[] content = body.array();
            if (content.length == 0) {
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"status\":400,\"code\":400,\"message\":\"Empty request body\"}");
            }
            try (JsonParser parser = JSON_FACTORY.createParser(content)) {
                Brand brand = BrandService.update(parser, id);
                return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":200,\"message\":\"A brand updated,it's %s\"}", brand);
            } catch (JsonProcessingException e) {
                // 5. 统一异常处理，避免泄露敏感信息
                return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.JSON_UTF_8,
                        "{\"status\":400,\"message\":\"Invalid JSON format\"}");
            } catch (Exception e) {
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"message\":\"Internal server error\"}");
            }
        }, VIRTUAL_EXECUTOR);
        return HttpResponse.of(future);
    }

    private static Brand update(JsonParser parser, long id) throws IOException, URISyntaxException {
        String name = null, shortName = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected JSON object");
        }
        while (parser.nextToken() != null) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name" -> name = parser.getValueAsString();
                    case "shortName" -> shortName = parser.getValueAsString();
                    case "story" -> story = parser.getValueAsString();
                    case "homepage" -> {
                        String urlStr = parser.getValueAsString();
                        if (urlStr != null) homepage = new URI(urlStr).toURL();
                    }
                    case "logo" -> {
                        String urlStr = parser.getValueAsString();
                        if (urlStr != null) logo = new URI(urlStr).toURL();
                    }
                    case "since" -> {
                        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            since = Year.of(parser.getIntValue());
                        }
                    }
                    //default -> parser.skipChildren();
                }
            }
        }
        BrandUpdateCommand command = new BrandUpdateCommand(id);
        if (name != null || shortName != null)
            command.setName(name, shortName);
        if (story != null || homepage != null || logo != null || since != null)
            command.setAbout(logo, homepage, since, story);
        System.out.println(command);
        Handler<BrandUpdateCommand, Brand> handler = new BrandUpdateHandler();
        return handler.execute(command);
    }

    @Delete("/brands/:id")
    public HttpResponse delete(ServiceRequestContext ctx, @Param("id") long id) {
        return HttpResponse.of(CompletableFuture.supplyAsync(() -> {
                    BrandDeleteCommand delete = new BrandDeleteCommand(id);
                    Handler<BrandDeleteCommand, Boolean> handler = new BrandDeleteHandler();
                    handler.execute(delete);

                    String message = String.format("Brand(id=%d) deleted successfully", id);
                    return HttpResponse.of(
                            HttpStatus.OK,
                            MediaType.JSON_UTF_8,
                            String.format("{\"status\":\"success\",\"code\":200,\"message\":\"%s\"}", message)
                    );
                }, ctx.blockingTaskExecutor()).exceptionally(e ->
                        HttpResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                MediaType.JSON_UTF_8,
                                "{\"status\":\"error\",\"code\":500,\"message\":\"Delete failed due to internal error\"}")
                )
        );
    }
}
