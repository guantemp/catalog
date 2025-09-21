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
import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESCategoryQuery;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.stream.StreamMessage;
import com.linecorp.armeria.common.stream.StreamWriter;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/20
 */

public class CategoryService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;

    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区

    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");

    private static final Pattern URI_REGEX = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    private static final CategoryQuery QUERY = new ESCategoryQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/categories/:id")
    @Description("Retrieves the category information by the given category ID.")
    public HttpResponse find(ServiceRequestContext ctx, @Param("id") @Default("-1") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.find(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
            } catch (SearchException | IOException e) {
                handleStreamError(stream, e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    @Get("/categories/{id}/children")
    public HttpResponse children(ServiceRequestContext ctx, @Param("id") @Default("-1") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.children(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
            } catch (IOException e) {
                handleStreamError(stream, e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    @Get("regex:^/categories/(?<id>.*)/(?:descendants|desc.)$")
    public HttpResponse descendants(ServiceRequestContext ctx, @Param("id") @Default("-1") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.descendants(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
            } catch (IOException e) {
                handleStreamError(stream, e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    @Get("/categories/{id}/path")
    public HttpResponse path(ServiceRequestContext ctx, @Param("id") @Default("-1") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.path(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    private void copyRaw(JsonGenerator generator, InputStream source) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(source);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Get("/categories")
    public HttpResponse query(ServiceRequestContext ctx, @Param("pretty") @Default("false") boolean pretty) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.blockingTaskExecutor().execute(() -> {
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, QUERY.search(search, offset, size));
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        });
        return HttpResponse.of(stream);
    }

    private void handleStreamError(StreamWriter<HttpObject> stream, Exception e) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
            gen.writeStartObject();
            gen.writeStringField("status", "fail");
            gen.writeNumberField("code", 500);
            gen.writeStringField("message", e.getMessage());
            gen.writeEndObject();
        } catch (IOException ex) {
            LOGGER.error("Json write error：{0}", e);
            //System.out.printf("Json write error：%s%n", e);
        }
        stream.write(ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
        stream.write(HttpData.wrap(buffer));
        stream.close();
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
                Category category = toCategory(parser);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A category created,it's %s\"}", "brand")));
            } catch (Exception e) {
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"code\":500,\"message\":\"Can't create a category,cause by {}\"}", e)));
            }
        });
        return HttpResponse.of(future);
    }

    private Category toCategory(JsonParser parser) throws IOException {
        String name = null, alias = null, description = null, icon = "";
        long parentId = 0;
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "parentId" -> parser.getValueAsLong();
                    case "name" -> name = parser.getValueAsString();
                    case "alias" -> alias = parser.getValueAsString();
                    case "description" -> description = parser.getValueAsString();
                    case "icon" -> icon = parser.getValueAsString();
                }
            }
        }
        CategoryCreateCommand command = new CategoryCreateCommand(parentId, name, alias, description, URI.create(icon));
        return null;
    }

}
