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


import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2026/1/31
 */

public class JsonByteBufOutputStream extends OutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private final Sinks.Many<ByteBuf> sink;
    private final AtomicBoolean isCancelled;
    private final ByteBuf byteBuf;

    public JsonByteBufOutputStream(Sinks.Many<ByteBuf> sink, AtomicBoolean isCancelled, int bufferSize) {
        this.sink = sink;
        this.isCancelled = isCancelled;
        this.byteBuf = PooledByteBufAllocator.DEFAULT.buffer(bufferSize);
    }

    public JsonByteBufOutputStream(Sinks.Many<ByteBuf> sink, AtomicBoolean isCancelled) {
        this(sink, isCancelled, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void write(int b) throws IOException {
        ensureWritable(1);
        byteBuf.writeByte(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) return;
        checkCancelled(); // 提前检查取消

        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            // 确保至少有 1 字节空间（防止 buffer 满了卡住）
            if (byteBuf.writableBytes() == 0) {
                flush();
            }
            int toWrite = Math.min(byteBuf.writableBytes(), remaining);
            byteBuf.writeBytes(b, offset, toWrite);
            offset += toWrite;
            remaining -= toWrite;
        }
    }

    @Override
    public void flush() throws IOException {
        checkCancelled();
        if (byteBuf.readableBytes() > 0) {
            ByteBuf slice = byteBuf.readRetainedSlice(byteBuf.readableBytes());
            //System.out.println("[" + System.currentTimeMillis() + "] >>> Flushing ByteBuf of size: " + slice.readableBytes());
            Sinks.EmitResult result = sink.tryEmitNext(slice);
            if (result.isFailure()) {
                slice.release();
                cancelAndEmitError(new IOException("Backpressure overflow, stream cancelled"));
            }
        }
        byteBuf.clear();//非常重要，没有这个在这里卡了1天
    }

    @Override
    public void close() throws IOException {
        IOException flushError = null;
        try {
            //System.out.println(">>> Closing JsonByteBufOutputStream");
            flush();
        } catch (IOException e) {
            flushError = e;
        } finally {
            if (byteBuf.refCnt() > 0) {
                byteBuf.release();
            }
        }
        if (flushError != null) {
            throw flushError;
        }
    }

    private void ensureWritable(int required) throws IOException {
        checkCancelled();
        if (byteBuf.writableBytes() < required) {
            flush();
        }
    }

    private void checkCancelled() throws IOException {
        if (isCancelled.get()) {
            throw new IOException("Processing cancelled");
        }
    }

    private void cancelAndEmitError(IOException error) {
        isCancelled.set(true);
        sink.tryEmitError(error);
    }
}
