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

package catalog.hoprxi.core.infrastructure.query;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/4/13
 */

public class FluxByteBufOutputStream extends OutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024; // 16kb 流式块
    private final FluxSink<ByteBuf> sink;
    private final AtomicBoolean cancelled;
    private final ByteBuf buffer;
    private volatile boolean closed = false;

    // 传入 Flux.create 的原生 sink
    public FluxByteBufOutputStream(FluxSink<ByteBuf> sink, AtomicBoolean cancelled) {
        this(sink, cancelled, DEFAULT_BUFFER_SIZE);
    }

    public FluxByteBufOutputStream(FluxSink<ByteBuf> sink, AtomicBoolean cancelled, int bufferSize) {
        this.sink = Objects.requireNonNull(sink, "FluxSink must not be null");
        this.cancelled = Objects.requireNonNull(cancelled, "cancelled flag must not be null");
        this.buffer = PooledByteBufAllocator.DEFAULT.buffer(bufferSize);
    }

    @Override
    public void write(int b) throws IOException {
        ensureWritable(1);
        buffer.writeByte(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) return;
        checkCancelled();

        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            if (buffer.writableBytes() == 0) flush();

            int write = Math.min(buffer.writableBytes(), remaining);
            buffer.writeBytes(b, offset, write);
            offset += write;
            remaining -= write;
        }
    }

    @Override
    public void flush() throws IOException {
        checkCancelled();
        if (buffer.readableBytes() == 0) return;
        // 零拷贝切片发送
        ByteBuf slice = buffer.readRetainedSlice(buffer.readableBytes());
        try {
            sink.next(slice);
            buffer.clear();
        } catch (Throwable e) {
            slice.release();
            throw new IOException("Sink emit failed", e);
        }
    }

    @Override
    public void close() throws IOException {
        checkCancelled();
        if (buffer.readableBytes() == 0) return;

        try {
            // 1. 申请一个新的 Heap Buffer (或者 Direct Buffer，取决于你的场景)
            // 这一步是关键：分配新的内存，切断与内部 buffer 的联系
            ByteBuf safeBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(buffer.readableBytes());

            // 2. 将当前 buffer 的数据 拷贝 到新 buffer 中
            safeBuf.writeBytes(buffer);

            // 3. 发送给下游
            // 此时下游拿到的是 safeBuf，与我们内部的 buffer 完全无关
            sink.next(safeBuf);

            // 4. 清理内部 buffer，准备复用
            buffer.clear();

        } catch (Throwable e) {
            // 注意：这里不需要 release(safeBuf)，因为 sink.next 成功后 ownership 已经转移
            // 只有在 new 失败时才需要处理，但通常这里直接抛异常即可
            throw new IOException("Sink emit failed", e);
        }
    }

    private void ensureWritable(int size) throws IOException {
        checkCancelled();
        if (buffer.writableBytes() < size) flush();
    }

    private void checkCancelled() throws IOException {
        if (cancelled.get()) throw new IOException("Stream cancelled by downstream");
    }
}
