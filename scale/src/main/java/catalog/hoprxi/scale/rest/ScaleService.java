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

package catalog.hoprxi.scale.rest;

import catalog.hoprxi.scale.application.query.ScaleQuery;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.infrastructure.query.postgresql.PsqlScaleQuery;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Description;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/4/3
 * @since JDK 21
 */

public class ScaleService  {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Item");
    private static final ScaleQuery QUERY=new PsqlScaleQuery();

    @Get("/scale/:plu")
    @Description("Retrieves the item information by the given ID.")
    public HttpResponse find(ServiceRequestContext ctx, @Param("plu") int plu) {
        Flux<ByteBuf> dataFlux = QUERY.findAsync(new Plu(plu)); // 假设返回 Flux<ByteBuf>
        Flux<HttpObject> bodyStream = dataFlux.map(HttpData::wrap);// 先不发 headers！等第一个数据到来再发
        // 使用 switchMap：一旦有第一个元素，就前置 headers
        Flux<HttpObject> responseStream = bodyStream
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) { // 第一个信号就是错误
                        return handleErrorResponse(ctx, plu, signal.getThrowable());
                    } else if (signal.isOnComplete()) {
                        // 空流？返回 404 或 204
                        return Flux.just(
                                ResponseHeaders.of(HttpStatus.NOT_FOUND),
                                HttpData.ofUtf8("{\"error\":\"Scale not found\",\"plu\":" + plu + "}")
                        );
                    } else {
                        return Flux.concat(// 有数据：前置 200 headers
                                Flux.just(ResponseHeaders.of(HttpStatus.OK)),
                                flux // 原始流（包含第一个元素）
                        );
                    }
                })
                .onErrorResume(e -> handleErrorResponse(ctx, plu, e));

        return HttpResponse.of(responseStream);
    }
    private Publisher<? extends HttpObject> handleErrorResponse(ServiceRequestContext ctx, int plu, Throwable cause) {
        LOGGER.warn("Error for plu={}", plu, cause);
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeCharSequence("{\"error\":\"Scale not found\",\"plu\":" + plu + "}", StandardCharsets.UTF_8);
        return Flux.just(
                ResponseHeaders.of(HttpStatus.NOT_FOUND),
                HttpData.wrap(buf)
        );
    }

    @Get("/scales")
    public HttpResponse search(ServiceRequestContext ctx) {
        return null;
    }
}
