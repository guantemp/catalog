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

package catalog.hoprxi.core.application.query;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;

/**
 * Query interface for category information.
 * Provides both synchronous and reactive asynchronous operations for category tree queries,
 * including root node lookup, detail query by ID, children/descendants query, search, path query, etc.
 * <p>
 * Synchronous methods return {@link InputStream}, while reactive async methods return {@link Flux<ByteBuf>}.
 * </p>
 *
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/18
 */

public interface CategoryQuery {
    /**
     * @return category root node
     */
    InputStream root();

    /**
     * 异步查询分类根节点数据
     * 基于响应式编程，返回ByteBuf数据流
     *
     * @return 分类根节点响应式数据流
     */
    Flux<ByteBuf> rootAsync();

    /**
     * @param id category id
     * @return category where id is correct
     */
    InputStream find(long id) throws SearchException;

    Mono<ByteBuf> findAsync(long id);

    /**
     * @param id category id
     * @return children of category id
     */
    InputStream children(long id);

    Flux<ByteBuf> childrenAsync(long id);

    InputStream descendants(long id);

    Flux<ByteBuf> descendantsAsync(long id);

    InputStream search(String key, int offset, int size);

    default InputStream search(String key) {
        return this.search(key, 0, 64);
    }

    Flux<ByteBuf> searchAsync(String key, int offset, int size);

    default Flux<ByteBuf> searchAsync(String key) {
        return this.searchAsync(key, 0, 64);
    }

    InputStream searchSiblings(long id);

    InputStream path(long id);

    Flux<ByteBuf> pathAsync(long id);
}
