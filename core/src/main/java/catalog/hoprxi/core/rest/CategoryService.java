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
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区
    private static final Pattern URI_REGEX = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");

    private static final CategoryQuery QUERY = new ESCategoryQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/categories/:id")
    @Description("Retrieves the category information by the given category ID.")
    public HttpResponse find(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.find(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer.release();
                stream.close();
            } catch (SearchException | IOException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release();
            }
        });
        return HttpResponse.of(stream);
    }

    @Get("/categories/{id}/children")
    public HttpResponse children(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.children(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer = null;
                stream.close();
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release();        // 确保buffer释放
            }
        });
        return HttpResponse.of(stream);
    }

    @Get("regex:^/categories/(?<id>.*)/(?:descendants|desc.)$")
    public HttpResponse descendants(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream is = QUERY.descendants(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, is);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer = null;
                stream.close();
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release();        // 确保buffer释放
            }
        });
        return HttpResponse.of(stream);
    }

    @Get("/categories/{id}/path")
    public HttpResponse path(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.path(id);
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer = null;
                stream.close();
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release();        // 确保buffer释放
            }
        });
        return HttpResponse.of(stream);
    }

    @Get("/categories/root")
    public HttpResponse root(ServiceRequestContext ctx, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled() || ctx.isTimedOut()) return;
            ByteBuf buffer = ctx.alloc().buffer(BATCH_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.root();
                if (pretty) gen.useDefaultPrettyPrinter();
                this.copyRaw(gen, source);
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer = null;
                stream.close();
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release();        // 确保buffer释放
            }
        });
        return HttpResponse.of(stream);
    }

    private void copyRaw(JsonGenerator generator, InputStream source) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(source);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
        generator.flush();
    }

    @Get("/categories")
    public HttpResponse search(ServiceRequestContext ctx, @Param("pretty") @Default("false") boolean pretty) {
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
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                buffer=null;
                stream.close();
            } catch (IOException e) {
                this.handleStreamError(stream, e);
            } finally {
                if (buffer != null) buffer.release();        // 确保buffer释放
            }
        });
        return HttpResponse.of(stream);
    }

    private void handleStreamError(StreamWriter<HttpObject> stream, Exception e) {
        stream.write(ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
        stream.write(HttpData.ofUtf8("{\"status\":\"error\",\"code\":500,\"message\":\"Error,it's %s\"}", e.getMessage()));
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
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A category created,it's %s\"}", category)));
            } catch (Exception e) {
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"code\":500,\"message\":\"Can't create a category,cause by %s\"}", e)));
            }
        });
        return HttpResponse.of(future);
    }

    private Category toCategory(JsonParser parser) throws IOException {
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
        //System.out.println(command);
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
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            this.update(body, id);
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.ofUtf8("{\"status\":\"success\",\"code\":201,\"message\":\"A category has update,it's %s\"}"));
            stream.close();
        });
        return HttpResponse.of(stream);
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

    @Delete("/categories/:id")
    public HttpResponse delete(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {

            CategoryDeleteCommand delete = new CategoryDeleteCommand(id);
            Handler<CategoryDeleteCommand, Boolean> handler = new CategoryDeleteHandler();
            handler.execute(delete);

            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.ofUtf8("{\"status\":\"success\",\"code\":200,\"message\":\"The category(id=%s) is deleted\"}", id));
            stream.close();
        });
        return HttpResponse.of(stream);
    }
}
