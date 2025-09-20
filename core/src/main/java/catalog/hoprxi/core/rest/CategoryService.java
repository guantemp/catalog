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


import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESCategoryQuery;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.stream.StreamMessage;
import com.linecorp.armeria.common.stream.StreamWriter;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Description;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/20
 */

public class CategoryService {
    private static final int OFFSET = 0;
    private static final int SIZE = 128;

    private static final int SINGLE_BUFFER_SIZE = 512; // 0.5KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 8192;
    /// / 8KB缓冲区

    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");

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

    @Get("regex:^/categories/(?<id>.*)/(?:ch|children)$")
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


    private void copyRaw(JsonGenerator generator, InputStream source) throws IOException {
        JsonParser parser = JSON_FACTORY.createParser(source);
        while (parser.nextToken() != null) {
            generator.copyCurrentEvent(parser);
        }
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
