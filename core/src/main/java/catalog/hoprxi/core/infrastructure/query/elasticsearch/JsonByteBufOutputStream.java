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
 * @version 0.0.1 builder 2026/1/6
 */

public class JsonByteBufOutputStream extends OutputStream {
    private static final int BUFFER_SIZE = 8192;
    private final Sinks.Many<ByteBuf> sink;
    private final AtomicBoolean isCancelled;
    private final ByteBuf byteBuf;
    private int position = 0;

    public JsonByteBufOutputStream(Sinks.Many<ByteBuf> sink, AtomicBoolean isCancelled, int bufferSize) {
        this.sink = sink;
        this.isCancelled = isCancelled;
        this.byteBuf = PooledByteBufAllocator.DEFAULT.buffer(bufferSize);
    }

    public JsonByteBufOutputStream(Sinks.Many<ByteBuf> sink, AtomicBoolean isCancelled) {
      this(sink,isCancelled, BUFFER_SIZE);
    }

    @Override
    public void write(int b) throws IOException {
        checkCancelled();
        if (position >= BUFFER_SIZE) {
            flushBuffer();
        }
        byteBuf.writeByte(b);
        position++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkCancelled();
        while (len > 0) {
            int spaceLeft = BUFFER_SIZE - position;
            int toWrite = Math.min(len, spaceLeft);

            byteBuf.writeBytes(b, off, toWrite);
            position += toWrite;
            off += toWrite;
            len -= toWrite;

            if (position >= BUFFER_SIZE) {
                flushBuffer();
            }
        }
    }


    @Override
    public void flush() throws IOException {
        checkCancelled();
        flushBuffer(); // 真正 flush 到 sink
    }

    @Override
    public void close() throws IOException {
        try {
            flushBuffer(); // 发送最后一块
            if (!isCancelled.get()) {
                sink.tryEmitComplete();
            }
        } finally {
            if (byteBuf.refCnt() > 0) {
                byteBuf.release(); // 释放内部 buffer
            }
            super.close();
        }
    }

    private void checkCancelled() throws IOException {
        if (isCancelled.get()) {
            throw new IOException("Processing cancelled");
        }
    }

    private void flushBuffer() {
        if (position > 0 && !isCancelled.get()) {
            ByteBuf slice = byteBuf.slice(0, position).retain();
            Sinks.EmitResult result = sink.tryEmitNext(slice);
            if (result.isFailure()) {
                slice.release();
                isCancelled.set(true);
                sink.tryEmitError(new IOException("Backpressure overflow, stream cancelled"));// 发射失败时释放资源
            }

            byteBuf.clear();
            position = 0;
        }
    }
}
