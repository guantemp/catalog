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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Sinks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/12/26
 */

public class ChunkedByteBufOutputStream extends java.io.OutputStream {
    private final FluxSink<ByteBuf> sink;
    private final byte[] buffer;
    private int pos = 0;
    private boolean closed = false;

    public ChunkedByteBufOutputStream(FluxSink<ByteBuf> sink, int chunkSize) {
        this.sink = sink;
        this.buffer = new byte[chunkSize];
    }

    @Override
    public synchronized void write(int b) throws IOException {
        ensureOpen();
        if (pos >= buffer.length) flushInternal();
        buffer[pos++] = (byte) b;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            int space = buffer.length - pos;
            if (space == 0) {
                flushInternal();
                space = buffer.length;
            }
            int toCopy = Math.min(remaining, space);
            System.arraycopy(b, offset, buffer, pos, toCopy);
            pos += toCopy;
            offset += toCopy;
            remaining -= toCopy;
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        if (!closed) flushInternal();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            flushInternal();
            closed = true;
        }
    }

    private void flushInternal() throws IOException {
        if (pos > 0) {
            ByteBuf buf = Unpooled.wrappedBuffer(buffer, 0, pos);
            sink.next(buf);
            pos = 0;
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("Stream closed");
    }
}
