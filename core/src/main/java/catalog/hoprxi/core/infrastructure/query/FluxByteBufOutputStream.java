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
import io.netty.util.ReferenceCountUtil;
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
    private final FluxSink<ByteBuf> sink;
    private final AtomicBoolean isCancelled;
    private ByteBuf buffer;
    private final int chunkSize;
    private volatile boolean closed = false;

    // 传入 Flux.create 的原生 sink
    public FluxByteBufOutputStream(FluxSink<ByteBuf> sink, AtomicBoolean isCancelled) {
        this(sink, isCancelled, 16 * 1024);
    }

    public FluxByteBufOutputStream(FluxSink<ByteBuf> sink, AtomicBoolean isCancelled, int chunkSize) {
        this.sink = Objects.requireNonNull(sink, "FluxSink must not be null");
        this.isCancelled = Objects.requireNonNull(isCancelled, "cancelled flag must not be null");
        this.chunkSize = chunkSize;
        this.buffer = PooledByteBufAllocator.DEFAULT.buffer(chunkSize);
    }

    @Override
    public void write(int b) throws IOException {
        ensureWritable();
        if (buffer == null) buffer = PooledByteBufAllocator.DEFAULT.buffer(chunkSize);
        if (!buffer.isWritable()) {
            // 满了就发
            if (buffer != null && buffer.readableBytes() > 0) {
                // 发送当前缓冲区（所有权转移给下游）
                sink.next(buffer);
                buffer = null;  // 生产者不再持有
            }
            if (buffer == null) buffer = PooledByteBufAllocator.DEFAULT.buffer(chunkSize);
        }
        buffer.writeByte(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureWritable();
        if (len == 0) return;
        if (buffer == null) buffer = PooledByteBufAllocator.DEFAULT.buffer(chunkSize);

        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            // 如果当前容量不足，先发送再分配新缓冲区
            if (buffer.writableBytes() == 0) {
                // 满了就发
                if (buffer != null && buffer.readableBytes() > 0) {
                    // 发送当前缓冲区（所有权转移给下游）
                    sink.next(buffer);
                    buffer = null;  // 生产者不再持有
                }
                if (buffer == null) buffer = PooledByteBufAllocator.DEFAULT.buffer(chunkSize);
            }
            int toWrite = Math.min(buffer.writableBytes(), remaining);
            buffer.writeBytes(b, offset, toWrite);
            offset += toWrite;
            remaining -= toWrite;
        }
    }

    @Override
    public void flush() throws IOException {
        if (isCancelled.get()) throw new IOException("Stream cancelled");
        if (buffer != null && buffer.readableBytes() > 0) {
            sink.next(buffer);
            buffer = null;
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        try {
            // 发送最后一块（如果有数据）
            flush();
        } finally {
            closed = true;
            // 释放当前缓冲区（如果没有发送且还有数据，flush 已经发送则 buffer=null；如果没数据则直接释放）
            if (buffer != null) {
                ReferenceCountUtil.release(buffer);
                buffer = null;
            }
        }
    }

    private void ensureWritable() throws IOException {
        if (isCancelled.get()) throw new IOException("Stream cancelled");
        if (closed) throw new IOException("Stream closed");
    }
}
