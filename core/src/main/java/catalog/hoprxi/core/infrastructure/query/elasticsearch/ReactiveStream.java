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


import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.infrastructure.ESUtil;
import catalog.hoprxi.core.infrastructure.query.JsonByteBufOutputStream;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elasticsearch 响应流式处理工具类。
 * <p>
 * 提供将 Elasticsearch 同步/异步请求结果转换为 Reactor 响应式流（{@link Mono} / {@link Flux}）
 * 或同步阻塞流（{@link ByteBufInputStream}）的能力。所有内存操作均基于 Netty 的 {@link ByteBuf}
 * 进行管理，支持背压控制和高并发场景下的内存安全。
 * </p>
 *
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @version 0.0.2 builder 2026/5/14
 * @since JDK21
 */

public final class ReactiveStream {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final ExecutorService TRANSFORM_POOL = Executors.newVirtualThreadPerTaskExecutor();
    private static final int SINGLE_BUFFER_SIZE = 2 * 1024;// 2KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    /**
     * Jackson JSON 工厂，禁用字段名缓存以减少内存开销。
     */
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
            .build();

    @FunctionalInterface
    private interface ExtractFunction {
        void extract(JsonParser parser, JsonGenerator generator) throws IOException;
    }

    /**
     * 同步阻塞方式获取单个 ByteBuf 输入流。
     * <p>
     * 该方法会阻塞当前线程直到 ES 请求完成并将数据写入缓冲区。
     * 适用于非反应式环境或必须使用同步流的场景。
     *
     * @param request Elasticsearch 请求对象
     * @param tips    用于日志和异常信息的上下文标识
     * @return 包含 ES 响应数据的 ByteBufInputStream
     * @throws SearchException 当 ES 返回 404 或其他错误，或发生 IO 异常时抛出
     */
    public static ByteBufInputStream toSingleByteBufInputStream(Request request, String tips) {
        Response response;
        try {
            response = ESUtil.restClient().performRequest(request);
        } catch (ResponseException e) {
            LOGGER.error("No search was found for anything resembling name({}) brand", tips, e);
            throw new SearchException(String.format("No search was found for anything resembling name(%s) brand", tips), e);
        } catch (IOException e) {
            // 3. 处理网络/IO 异常
            LOGGER.error("I/O failed for request: {}", tips, e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        }
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        try (InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is);
             OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            Extract.extractSourceSkipMeta(parser, generator);
            success = true;
            return new ByteBufInputStream(buffer, true);
        } catch (IOException e) {
            LOGGER.error("Failed to process response stream for: {}", tips, e);
            throw new SearchException("Error processing Elasticsearch response stream", e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (!success) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    /**
     * 同步执行 ES 请求并将结果转换为 ByteBufInputStream。
     * <p>
     * 该方法专门用于处理包含 "brands" 聚合的查询结果（基于 Extract.extractWithoutAggs 的逻辑）。
     * 使用 Netty 的 PooledByteBufAllocator 分配内存，并在发生异常时确保内存被正确释放。
     *
     * @param request ES 请求对象
     * @param tips    用于日志记录的上下文信息（如搜索关键词）
     * @return 包含解析后数据的 ByteBufInputStream
     * @throws SearchException 当 ES 请求失败或发生 IO 异常时抛出
     */
    public static ByteBufInputStream toByteBufInputStream(Request request, String objectName, String tips) {
        Response response;
        try {
            response = ESUtil.restClient().performRequest(request);
        } catch (ResponseException e) {
            LOGGER.error("No search was found for anything resembling name ({})", tips, e);
            throw new SearchException(String.format("No search was found for anything resembling name(%s)", tips), e);
        } catch (IOException e) {
            // 3. 处理网络/IO 异常
            LOGGER.error("I/O failed for request: {}", tips, e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        }
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer(BATCH_BUFFER_SIZE);
        boolean success = false;
        try (InputStream is = response.getEntity().getContent(); JsonParser parser = JSON_FACTORY.createParser(is);
             OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            Extract.extract(parser, generator, objectName);
            success = true;
            return new ByteBufInputStream(buffer, true);
        } catch (IOException e) {
            LOGGER.error("Failed to process response stream for: {}", tips, e);
            throw new SearchException("Error processing Elasticsearch response stream", e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            if (!success) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    /**
     * 将 Elasticsearch 请求转换为 Mono<ByteBuf>。
     * <p>
     * 此方法适用于返回单个结果集的场景。它异步执行 ES 请求，将响应体中的 JSON 数据
     * 经过转换（去除元数据）后写入一个 Direct ByteBuf，并通过 Mono 返回。
     *
     * @param request Elasticsearch 的请求对象
     * @param tips    用于日志记录或异常信息的上下文标识（如查询ID、索引名等）
     * @return 包含处理后的 ByteBuf 的 Mono。如果请求失败或取消，Mono 将终止。
     */
    public static Mono<ByteBuf> toMonoByteBuf(Request request, String... tips) {
        return Mono.create((MonoSink<ByteBuf> sink) -> {
                    final AtomicBoolean isCancelled = new AtomicBoolean(false);
                    // 取消监听
                    sink.onCancel(() -> isCancelled.set(true));
                    // ES 异步请求
                    ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            if (isCancelled.get()) {
                                EntityUtils.consumeQuietly(response.getEntity());
                                return;
                            }
                            final InputStream content;
                            try {
                                content = response.getEntity().getContent();
                            } catch (IOException e) {
                                sink.error(MapException.mapException(e, tips));
                                EntityUtils.consumeQuietly(response.getEntity());
                                return;
                            }
                            // 线程池处理
                            TRANSFORM_POOL.execute(() -> {
                                if (isCancelled.get()) {
                                    try {
                                        if (content != null) content.close();
                                    } catch (Exception ignore) {
                                    }
                                    EntityUtils.consumeQuietly(response.getEntity());
                                    return;
                                }
                                ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(SINGLE_BUFFER_SIZE);
                                try (content; JsonParser parser = JSON_FACTORY.createParser(content);
                                     OutputStream os = new ByteBufOutputStream(buf); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                                    Extract.extractSourceSkipMeta(parser, generator);
                                    generator.close();
                                    if (!isCancelled.get()) {
                                        sink.success(buf);
                                    } else {
                                        ReferenceCountUtil.release(buf);
                                    }
                                } catch (IOException e) {
                                    ReferenceCountUtil.release(buf);
                                    if (!isCancelled.get()) {
                                        sink.error(MapException.mapException(e, tips));
                                    }
                                }
                                //如果 try 块正常执行完，content 已经被读取并关闭了，此时再调用 consumeQuietly 可能会尝试读取已经关闭的流（虽然 Quietly 会吞掉异常，但这是一种“坏味道”）
                            });
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            if (!isCancelled.get()) {
                                sink.error(MapException.mapException(exception, tips));
                            }
                        }
                    });
                })
                .doOnTerminate(() -> LOGGER.debug("Request terminated from {}", (Object[]) tips))
                .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease);
    }


    /**
     * 将 Elasticsearch 请求转换为 Flux<ByteBuf>。
     * <p>
     * 此方法适用于处理大量数据或流式数据的场景。它使用自定义的 OutputStream 将 JSON 解析过程
     * 产生的数据块直接推送到 Flux 下游，支持背压（Backpressure）。
     *
     * @param request     Elasticsearch 的请求对象
     * @param objectsName 对象名称，用于 JSON 解析时的特定处理逻辑
     * @param tips        用于日志记录或异常信息的上下文标识
     * @return 包含 ByteBuf 数据块的 Flux 流
     */
    public static Flux<ByteBuf> toFluxByteBuf(Request request, String objectsName, String tips) {
        return toFluxByteBufInternal(request, tips,
                (parser, generator) -> Extract.extract(parser, generator, objectsName));
    }

    /**
     * 将 Elasticsearch 请求转换为 Flux&lt;ByteBuf&gt;，并将结果解析为树形结构（基于 left/right 嵌套集模型）。
     * <p>
     * 此方法专门用于处理带有 left/right 闭区间字段的 ES 数据（如目录树、组织架构等）。
     * 它会将扁平的结果集动态重组为带有 "children" 数组的嵌套 JSON 树，并通过 Flux 流式输出。
     * 内部使用 {@link Extract#extractAsTree(JsonParser, JsonGenerator, String)} 完成树形转换。
     * </p>
     *
     * @param request Elasticsearch 的请求对象
     * @param title   输出树形结构的根字段名称，该字段的值即为整个树对象；不可为 {@code null} 或空白
     * @param tips    用于日志记录或异常信息的上下文标识
     * @return 包含树形 JSON 数据块（ByteBuf）的 Flux 流
     */
    public static Flux<ByteBuf> toTreeFluxByteBuf(Request request, String title, String tips) {
        return toFluxByteBufInternal(request, tips,
                (parser, generator) -> Extract.extractAsTree(parser, generator, title));
    }

    private static Flux<ByteBuf> toFluxByteBufInternal(Request request, String tips, ExtractFunction extractor) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                if (isCancelled.get()) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    return;
                }
                final InputStream content;
                try {
                    content = response.getEntity().getContent();
                } catch (IOException e) {
                    sink.tryEmitError(MapException.mapException(e, tips));
                    EntityUtils.consumeQuietly(response.getEntity());
                    return;
                }
                TRANSFORM_POOL.execute(() -> {
                    if (isCancelled.get()) {
                        try {
                            if (content != null) content.close();
                        } catch (Exception ignore) {
                        }
                        EntityUtils.consumeQuietly(response.getEntity());
                        return;
                    }
                    try (content; JsonParser parser = JSON_FACTORY.createParser(content);
                         OutputStream os = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                        extractor.extract(parser, generator);
                        Sinks.EmitResult result = sink.tryEmitComplete();
                        if (result.isFailure() && result != Sinks.EmitResult.FAIL_TERMINATED) {
                            LOGGER.warn("Failed to emit complete: {}", result);
                        }
                    } catch (IOException e) {
                        Sinks.EmitResult result = sink.tryEmitError(e);
                        if (result.isFailure() && result != Sinks.EmitResult.FAIL_TERMINATED) {
                            LOGGER.warn("emitError failed: {}", result);
                        }
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                });
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isCancelled.get()) {
                    sink.tryEmitError(MapException.mapException(exception, tips));
                }
            }
        });

        return sink.asFlux()
                .timeout(Duration.ofSeconds(20), Mono.error(new TimeoutException("Request timed out for : " + tips)))
                .doOnCancel(() -> isCancelled.set(true)).doOnTerminate(() -> LOGGER.debug("Request terminated for {}", tips))
                .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease);
    }
/*
    private static Flux<ByteBuf> byteBufFlux(String tips, Request request, boolean alone) {
        return Flux.<ByteBuf>create(sink -> {
                    final AtomicBoolean isCancelled = new AtomicBoolean(false);
                    sink.onCancel(() -> isCancelled.set(true));
                    // 1. 发起 ES 请求 (运行在 ES I/O 线程)
                    ESUtil.restClient().performRequestAsync(request, new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            if (isCancelled.get()) {
                                EntityUtils.consumeQuietly(response.getEntity());
                                return;
                            }
                            final InputStream content;
                            try {
                                content = response.getEntity().getContent();
                            } catch (IOException e) {
                                sink.error(MapException.mapException(e, tips));
                                EntityUtils.consumeQuietly(response.getEntity());
                                return;
                            }
                            // 2. 切换到业务线程池处理耗时逻辑
                            // 注意：这里直接在线程池里操作 sink，Reactor 会自动处理线程安全
                            TRANSFORM_POOL.execute(() -> {
                                if (isCancelled.get()) {
                                    try {
                                        if (content != null) content.close();
                                    } catch (Exception ignore) {
                                    }
                                    EntityUtils.consumeQuietly(response.getEntity());
                                    return;
                                }
                                try (content; JsonParser parser = JSON_FACTORY.createParser(content);
                                     OutputStream os = new FluxByteBufOutputStream(sink, isCancelled); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
                                    // 禁用 Jackson 自动刷新，交给Netty 的 ByteBuf控制，大幅减少IO次数
                                    //generator.disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
                                    // 执行解析，数据会通过 os 自动 emit 给下游
                                    if (alone) {
                                        Extract.extractSourceSkipMeta(parser, generator);
                                    } else {
                                        Extract.extract(parser, generator, "items");
                                    }
                                    sink.complete();
                                } catch (IOException e) {
                                    sink.error(MapException.mapException(e, tips));
                                } finally {
                                    EntityUtils.consumeQuietly(response.getEntity());
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            sink.error(MapException.mapException(exception, tips));
                        }
                    });
                }, FluxSink.OverflowStrategy.BUFFER) // 这里保持BUFFER，但必须配合上游limitRate，否则可能oom
                .doOnTerminate(() -> {
                    String threadName = Thread.currentThread().getName();
                    // 这里的 this 指的是 doOnTerminate 这个操作符内部的上下文，或者直接打印 tips 对应的唯一请求标识
                    LOGGER.debug("Request terminated for id: {}, Thread: {}, FluxIdentity: {}",
                            tips, threadName, System.identityHashCode(tips));
                })
                .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease);
    }
 */
}
