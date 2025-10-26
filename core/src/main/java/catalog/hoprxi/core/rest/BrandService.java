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


import catalog.hoprxi.core.application.command.BrandCreateCommand;
import catalog.hoprxi.core.application.command.BrandDeleteCommand;
import catalog.hoprxi.core.application.command.BrandUpdateCommand;
import catalog.hoprxi.core.application.handler.BrandCreateHandler;
import catalog.hoprxi.core.application.handler.BrandDeleteHandler;
import catalog.hoprxi.core.application.handler.BrandUpdateHandler;
import catalog.hoprxi.core.application.handler.Handler;
import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESBrandQuery;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/8/27
 */
public class BrandService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;

    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;// 8KB缓冲区
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Brand");

    private static final EnumSet<SortField> SUPPORT_SORT_FIELD = EnumSet.of(SortField._ID, SortField.ID, SortField.NAME, SortField._NAME);
    private static final BrandQuery QUERY = new ESBrandQuery();
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/brands/{id}")
    @Description("Retrieves the brand information by the given brand ID.")
    public HttpResponse find(ServiceRequestContext ctx, @Param("id") long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                InputStream source = QUERY.find(id);
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

    @Get("/brands")
    public HttpResponse search(ServiceRequestContext ctx, @Param("pretty") @Default("false") boolean pretty) {
        QueryParams params = ctx.queryParams();
        String search = params.get("s", "");
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        String cursor = params.get("cursor", "");
        SortField sortField = SortField.of(params.get("sort", ""));
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        if (!SUPPORT_SORT_FIELD.contains(sortField)) {//not support sort
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                gen.writeStartObject();
                gen.writeStringField("status", "fail");
                gen.writeNumberField("code", 400);
                gen.writeStringField("message", "Not support sort filed");
                gen.writeEndObject();
            } catch (IOException e) {
                System.out.printf("Json write error：%s%n", e);
            }
            stream.write(ResponseHeaders.of(HttpStatus.BAD_REQUEST, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(buffer));
            stream.close();
        } else {
            ctx.blockingTaskExecutor().execute(() -> {
                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
                try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                    if (pretty) gen.useDefaultPrettyPrinter();
                    if (cursor.isEmpty()) {
                        copyRaw(gen, QUERY.search(search, offset, size, sortField));
                    } else {
                        copyRaw(gen, QUERY.search(search, size, cursor, sortField));
                    }
                } catch (IOException e) {
                    handleStreamError(stream, e);
                }
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                stream.close();
            });
        }
        return HttpResponse.of(stream);
    }

    private void copyRaw(JsonGenerator generator, InputStream source) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(source);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Post("/brands")
    public HttpResponse create(ServiceRequestContext ctx, HttpData body, @Param("pretty") @Default("false") boolean pretty) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ctx.blockingTaskExecutor().execute(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Brand brand = toBrand(parser);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A brand created,it's %s\"}", brand)));
            } catch (Exception e) {
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8,
                        "{\"status\":500,\"code\":500,\"message\":\"Can't create a brand,cause by {}\"}", e)));
            }
        });
        return HttpResponse.of(future);
    }

    private Brand toBrand(JsonParser parser) throws IOException, URISyntaxException {
        String name = null, alias = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
        while (parser.nextToken() != null) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.currentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name" -> name = parser.getValueAsString();
                    case "alias" -> alias = parser.getValueAsString();
                    case "story" -> story = parser.getValueAsString();
                    case "homepage" -> homepage = new URI(parser.getValueAsString()).toURL();
                    case "logo" -> logo = new URI(parser.getValueAsString()).toURL();
                    case "since" -> since = Year.of(parser.getIntValue());
                }
            }
        }
        BrandCreateCommand createCommand = new BrandCreateCommand(name, alias, homepage, logo, since, story);
        Handler<BrandCreateCommand, Brand> handler = new BrandCreateHandler();
        return handler.execute(createCommand);
    }

    @StatusCode(201)
    @Put("/brands/{id}")
    public HttpResponse update(ServiceRequestContext ctx, HttpData body, @Param("id")  long id, @Param("pretty") @Default("false") boolean pretty) {
        RequestHeaders headers = ctx.request().headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            this.update(body, id);
        });
        return HttpResponse.of(stream);
    }

    private void update(HttpData body, long id) {
        String name = null, alias = null, story = null;
        URL logo = null, homepage = null;
        Year since = null;
        try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
            while (parser.nextToken() != null) {
                if (JsonToken.FIELD_NAME == parser.currentToken()) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "name" -> name = parser.getValueAsString();
                        case "alias" -> alias = parser.getValueAsString();
                        case "story" -> story = parser.getValueAsString();
                        case "homepage" -> homepage = new URI(parser.getValueAsString()).toURL();
                        case "logo" -> logo = new URI(parser.getValueAsString()).toURL();
                        case "since" -> since = Year.of(parser.getIntValue());
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("The process error", e);
            //System.out.println(e);
        }
        BrandUpdateCommand command = new BrandUpdateCommand(id);
        if (name != null || alias != null)
            command.setName(name, alias);
        if (story != null || homepage != null || logo != null || since != null)
            command.setAbout(logo, homepage, since, story);
        Handler<BrandUpdateCommand, Brand> handler = new BrandUpdateHandler();
        Brand brand = handler.execute(command);
    }

    @Delete("/brands/:id")
    public HttpResponse delete(ServiceRequestContext ctx, @Param("id")  long id, @Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {

            BrandDeleteCommand delete = new BrandDeleteCommand(id);
            Handler<BrandDeleteCommand, Boolean> handler = new BrandDeleteHandler();
            handler.execute(delete);

            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                if (pretty) gen.useDefaultPrettyPrinter();
                gen.writeStartObject();
                gen.writeStringField("status", "success");
                gen.writeNumberField("code", 200);
                gen.writeStringField("message", "The brand is deleted");
                gen.writeEndObject();
                gen.close();
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
                stream.close();
            } catch (IOException e) {
                handleStreamError(stream, e);
            }
        });
        return HttpResponse.of(stream);
    }

    private void handleStreamError(StreamWriter<HttpObject> stream, IOException e) {
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
}
