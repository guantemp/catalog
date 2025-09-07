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


import catalog.hoprxi.core.application.BrandAppService;
import catalog.hoprxi.core.application.command.BrandCreateCommand;
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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/8/27
 */
@PathPrefix("/v1")
public class BrandService {
    private static final int OFFSET = 0;
    private static final int SIZE = 64;
    private static final int BUFFER_SIZE = 4096; // 4KB缓冲区
    private static final PooledByteBufAllocator ALLOCATOR = PooledByteBufAllocator.DEFAULT;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final EnumSet<SortField> SUPPORT_SORT_FIELD = EnumSet.of(SortField._ID, SortField.ID, SortField.NAME, SortField._NAME);
    private static final BrandQuery QUERY = new ESBrandQuery();
    private final BrandAppService app = new BrandAppService();

    @Get("/brands/:id")
    @Description("Retrieves the brand information by the given brand ID.")
    public HttpResponse query(ServiceRequestContext ctx, @Param("id") @Default("-1") long id) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        ctx.whenRequestCancelled().thenAccept(stream::close);
        ctx.blockingTaskExecutor().execute(() -> {
            if (ctx.isCancelled()) return;
            ByteBuf buffer = ALLOCATOR.buffer(BUFFER_SIZE);
            try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os);) {
                QUERY.find(id);
                //OutputStream source = query.query(code);
                //copyRaw(gen, source);
                gen.close();
                stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
                stream.write(HttpData.wrap(buffer));
            } catch (IOException e) {
                //handleStreamError(stream, e);
            } finally {
                stream.close();
            }
        });
        return HttpResponse.of(stream);
    }

    @Get("/areas")
    public HttpResponse query(ServiceRequestContext ctx) {
        QueryParams params = ctx.queryParams();
        int offset = params.getInt("offset", OFFSET);
        int size = params.getInt("size", SIZE);
        SortField sortField = SortField.of(Optional.ofNullable(params.get("sort")).orElse(""));
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        if (!SUPPORT_SORT_FIELD.contains(sortField)) {

        }
        return HttpResponse.of(stream);
    }

    private void copyRaw(JsonGenerator generator, OutputStream source) throws IOException {
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) source).toByteArray());
        JsonParser parser = JSON_FACTORY.createParser(is);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
    }

    @Post("/brands")
    public HttpResponse create(ServiceRequestContext ctx, HttpRequest req, HttpData body) {
        RequestHeaders headers = req.headers();
        if (!(MediaType.JSON.is(Objects.requireNonNull(headers.contentType())) || MediaType.JSON_UTF_8.is(Objects.requireNonNull(headers.contentType()))))
            return HttpResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    MediaType.PLAIN_TEXT_UTF_8, "Expected JSON content");
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        ctx.blockingTaskExecutor().execute(() -> {
            try (JsonParser parser = JSON_FACTORY.createParser(body.toInputStream())) {
                Brand brand = jsonTo(parser);
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.CREATED, MediaType.JSON_UTF_8,
                        "{\"status\":\"success\",\"code\":201,\"message\":\"A brand created,it's %s\"}", brand)));
            } catch (Exception e) {
                ctx.eventLoop().execute(() -> future.complete(HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.JSON_UTF_8, "{\"status\":500,\"code\":500,\"message\":\"Can't create a area,cause by {}\"}", e)));
            }
        });
        return HttpResponse.of(future);
    }

    private Brand jsonTo(JsonParser parser) throws IOException, URISyntaxException {
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
        BrandCreateCommand brandCreateCommand = new BrandCreateCommand(name, alias, homepage, logo, since, story);
        return app.createBrand(brandCreateCommand);
    }
}
