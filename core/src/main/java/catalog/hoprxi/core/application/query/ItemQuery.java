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
 * 定义物品（Item）查询操作的契约接口。
 * <p>
 * 该接口提供了同步和异步两种方式来查询单个物品或搜索物品列表。
 * 同步方法返回 {@link InputStream}，适用于阻塞式IO场景；
 * 异步方法返回 {@link reactor.core.publisher.Flux&lt;io.netty.buffer.ByteBuf&gt;}，
 * 基于 Project Reactor 实现非阻塞响应式数据流，适用于高并发场景。
 * </p>
 *
 * <p><strong>主要功能：</strong></p>
 * <ul>
 *     <li>通过 ID 或条形码查找单个物品。</li>
 *     <li>支持基于 {@link ItemQuerySpec} 的复杂条件搜索。</li>
 *     <li>支持分页（size/offset）、游标分页（searchAfter）及自定义排序。</li>
 *     <li>提供丰富的默认方法以简化常见查询场景的调用。</li>
 * </ul>
 *
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @version 0.0.1 builder 2025/10/13
 * @see ItemQuerySpec
 * @see SortFieldEnum
 * @see java.io.InputStream
 * @see reactor.core.publisher.Flux
 * @since JDK21
 */
public interface ItemQuery {
    /**
     * 根据物品 ID 同步查找物品。
     *
     * @param id 物品的唯一标识符。
     * @return 包含物品数据的输入流；如果未找到，可能返回空流或抛出异常（取决于具体实现）。
     */
    InputStream find(long id);

    /**
     * 根据物品 ID 异步查找物品。
     * <p>
     * 返回一个响应式数据流， emit 物品的二进制数据块。
     *
     * @param id 物品的唯一标识符。
     * @return 发射 {@link io.netty.buffer.ByteBuf} 的异步流。
     */
    Mono<ByteBuf> findAsync(long id);

    /**
     * 根据条形码同步查找物品。
     *
     * @param barcode 物品的条形码字符串。
     * @return 包含物品数据的输入流。
     */
    InputStream findByBarcode(String barcode);

    /**
     * 根据条形码异步查找物品。
     *
     * @param barcode 物品的条形码字符串。
     * @return 发射 {@link io.netty.buffer.ByteBuf} 的异步流。
     */
    Mono<ByteBuf> findByBarcodeAsync(String barcode);

    /**
     * 根据查询条件同步搜索物品列表（基于游标分页）。
     * <p>
     * 支持指定查询规范、返回数量、游标位置（searchAfter）以及排序字段。
     *
     * @param specs       查询条件规范数组，用于构建过滤逻辑。
     * @param size        期望返回的最大记录数。
     * @param searchAfter 游标值，表示从哪个记录之后开始获取（用于深度分页）。为空字符串表示从头开始。
     * @param sortField   排序字段枚举。
     * @return 包含搜索结果 JSON 或其他格式数据的输入流。
     */
    InputStream search(ItemQuerySpec[] specs, int size, String searchAfter, SortFieldEnum sortField);

    default InputStream search(ItemQuerySpec[] filters, int size) {
        return search(filters, size, "", SortFieldEnum._ID);
    }

    default InputStream search(int size, String searchAfter, SortFieldEnum sortField) {
        return search(new ItemQuerySpec[0], size, searchAfter, sortField);
    }

    default InputStream search(int size, SortFieldEnum sortField) {
        return search(new ItemQuerySpec[0], size, "", sortField);
    }

    /**
     * 根据查询条件异步搜索物品列表（基于游标分页）。
     * <p>
     * 以非阻塞方式返回响应式数据流。
     *
     * @param specs       查询条件规范数组。
     * @param size        期望返回的最大记录数。
     * @param searchAfter 游标值。
     * @param sortField   排序字段。
     * @return 发射 {@link io.netty.buffer.ByteBuf} 的异步流，代表搜索结果。
     */
    Flux<ByteBuf> searchAsync(ItemQuerySpec[] specs, int size, String searchAfter, SortFieldEnum sortField);

    default Flux<ByteBuf> searchAsync(ItemQuerySpec[] specs, int size) {
        return searchAsync(specs, size, "", SortFieldEnum._ID);
    }

    default Flux<ByteBuf> searchAsync(int size, String searchAfter, SortFieldEnum sortField) {
        return searchAsync(new ItemQuerySpec[0], size, searchAfter, sortField);
    }

    default Flux<ByteBuf> searchAsync(int size, SortFieldEnum sortField) {
        return searchAsync(new ItemQuerySpec[0], size, "", sortField);
    }

    /**
     * 根据查询条件同步搜索物品列表（基于偏移量分页）。
     * <p>
     * 适用于传统的 offset/limit 分页模式。注意：在大数据量下，基于游标的分页（searchAfter）性能通常更优。
     *
     * @param specs     查询条件规范数组。
     * @param offset    起始偏移量（跳过前 N 条记录）。
     * @param size      期望返回的最大记录数。
     * @param sortField 排序字段。
     * @return 包含搜索结果的输入流。
     */
    InputStream search(ItemQuerySpec[] specs, int offset, int size, SortFieldEnum sortField);

    default InputStream search(int offset, int size, SortFieldEnum sortField) {
        return search(new ItemQuerySpec[0], offset, size, sortField);
    }

    default InputStream search(int offset, int size) {
        return search(new ItemQuerySpec[0], offset, size, SortFieldEnum._ID);
    }

    /**
     * 根据查询条件异步搜索物品列表（基于偏移量分页）。
     *
     * @param specs   查询条件规范数组。
     * @param offset    起始偏移量。
     * @param size      期望返回的最大记录数。
     * @param sortField 排序字段。
     * @return 发射 {@link io.netty.buffer.ByteBuf} 的异步流。
     */
    Flux<ByteBuf> searchAsync(ItemQuerySpec[] specs, int offset, int size, SortFieldEnum sortField);

    default Flux<ByteBuf> searchAsync(int offset, int size, SortFieldEnum sortField) {
        return searchAsync(new ItemQuerySpec[0], offset, size, sortField);
    }

    default Flux<ByteBuf> searchAsync(int offset, int size) {
        return searchAsync(new ItemQuerySpec[0], offset, size, SortFieldEnum._ID);
    }
}
