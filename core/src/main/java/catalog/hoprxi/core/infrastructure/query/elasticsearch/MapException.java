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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;

import catalog.hoprxi.core.application.query.NotFoundException;
import catalog.hoprxi.core.application.query.SearchException;
import io.netty.buffer.ByteBuf;
import org.elasticsearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/4/4
 * @since JDK 21
 */

public final class MapException {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");

    /**
     * 将通用 Exception 映射为业务自定义异常
     * <p>主要处理 HTTP 调用异常、IO 异常及未知异常，统一转换为服务内部可识别的异常类型</p>
     *
     * @param e          原始异常（如 HTTP 调用异常、IO 异常、运行时异常）
     * @param identifier 异常关联的资源标识（ID / 关键字），用于日志与错误提示
     * @return 转换后的业务异常（NotFoundException / SearchException）
     */
    public static Throwable mapException(Exception e, Object identifier) {
        if (e instanceof ResponseException) {
            int status = ((ResponseException) e).getResponse().getStatusLine().getStatusCode();
            if (status == 404) {
                return new NotFoundException("Not found for: " + identifier);
            } else if (status >= 400 && status < 500) {
                return new SearchException("Client error: " + status);
            } else {
                return new SearchException("Server error: " + status);
            }
        } else if (e instanceof IOException) {
            LOGGER.error("I/O error for id={}", identifier, e);
            return new SearchException("Network error", e);
        }
        return new SearchException("Unexpected error", e);
    }

    /**
     * 异常统一处理并向响应流发射错误信号
     * <p>处理异步流中的异常，自动解包 UncheckedIOException，区分 IO 异常与系统错误，
     * 并根据取消状态决定是否发送错误或完成信号。</p>
     *
     * @param sink        Reactor 响应流发射器，用于向下游发送错误/完成信号
     * @param err         待处理的异常（可为 null）
     * @param isCancelled 取消状态标记，若为 true 则不发送任何信号
     * @param key         当前处理的资源唯一标识（ID），用于日志定位
     */
    public static void mapExceptionAndEmit(Sinks.Many<ByteBuf> sink, Throwable err, AtomicBoolean isCancelled, Object key) {
        if (err != null && !isCancelled.get()) {
            Throwable cause = err;
            if (err instanceof UncheckedIOException) {
                cause = err.getCause(); // 解包 IO 异常
            }
            if (cause instanceof IOException) { // 如果是普通 RuntimeException，cause 就是 err 本身，保持不动
                LOGGER.warn("Transform failed due to IO error for id={}", key, cause);
                sink.tryEmitError(new SearchException("Transform failed: IO error", cause));
            } else {//【修复空指针】：确保 cause 不为 null 再调用 getMessage()
                LOGGER.error("Unexpected system error (Bug) while fetching id={}", key, cause);
                sink.tryEmitError(new SearchException("Unexpected error: " + cause.getMessage(), cause));
            }
        } else if (!isCancelled.get()) {
            sink.tryEmitComplete();
        }
    }
}
