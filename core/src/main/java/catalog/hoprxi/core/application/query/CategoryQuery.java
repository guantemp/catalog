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
 * @version 0.0.1 builder 2025/9/18
 * @since JDK21
 */

public interface CategoryQuery {
    /**
     * 获取分类树的根节点数据（同步阻塞）。
     * <p>
     * 返回的输入流中包含一个表示根分类的 JSON 对象，通常包含节点的所有属性（如 id、name、left、right 等）。
     * </p>
     *
     * @return 包含根节点 JSON 数据的输入流
     * @throws SearchException 如果查询失败或数据不存在
     */
    InputStream root();

    /**
     * 异步获取分类树的根节点数据（响应式流）。
     * <p>
     * 该方法不会阻塞调用线程，而是返回一个 {@link Flux} 数据流，下游可以通过订阅消费根节点的 JSON 数据块。
     * 适用于需要非阻塞 I/O 和高并发的场景。
     * </p>
     *
     * @return 包含根节点 JSON 数据块的响应式流
     */
    Flux<ByteBuf> rootAsync();

    /**
     * 根据分类 ID 查询单个分类节点（同步阻塞）。
     * <p>
     * 返回的输入流中包含指定分类的完整 JSON 对象。如果分类不存在，抛出 {@link SearchException}。
     * </p>
     *
     * @param id 分类的唯一标识符
     * @return 包含分类 JSON 数据的输入流
     * @throws SearchException 如果未找到指定 ID 的分类，或发生查询错误
     */
    InputStream find(long id) throws SearchException;

    /**
     * 异步根据分类 ID 查询单个分类节点（响应式流）。
     * <p>
     * 返回一个 {@link Mono} 异步单元素流，包含分类的 JSON 数据块。
     * </p>
     *
     * @param id 分类的唯一标识符
     * @return 包含分类 JSON 数据块的异步流
     */
    Mono<ByteBuf> findAsync(long id);

    /**
     * 获取指定分类的直接子节点列表（同步阻塞）。
     * <p>
     * 返回的输入流中包含一个 JSON 数组，每个元素为子分类的完整 JSON 对象。
     * 如果指定分类没有子节点，返回空数组。
     * </p>
     *
     * @param id 父分类的唯一标识符
     * @return 包含子节点 JSON 数组的输入流
     * @throws SearchException 如果父分类不存在或查询失败
     */
    InputStream children(long id);

    /**
     * 异步获取指定分类的直接子节点列表（响应式流）。
     * <p>
     * 返回一个 {@link Flux} 流，每个元素代表一个子分类的 JSON 数据块。
     * 下游可以按需消费，支持背压。
     * </p>
     *
     * @param id 父分类的唯一标识符
     * @return 包含子节点 JSON 数据块的响应式流
     */
    Flux<ByteBuf> childrenAsync(long id);

    /**
     * 获取指定分类的所有后代节点（同步阻塞）。
     * <p>
     * 包括子节点、孙子节点等全部下级节点，以扁平的数组形式返回。
     * 适用于需要导出整个子树或批量处理的场景。
     * </p>
     *
     * @param id 祖先分类的唯一标识符
     * @return 包含所有后代节点 JSON 数组的输入流
     * @throws SearchException 如果分类不存在或查询失败
     */
    InputStream descendants(long id);

    /**
     * 异步获取指定分类的所有后代节点（响应式流）。
     * <p>
     * 返回一个 {@link Flux} 流，每个元素代表一个后代节点的 JSON 数据块。
     * 节点顺序通常按 left 值升序排列。
     * </p>
     *
     * @param id 祖先分类的唯一标识符
     * @return 包含后代节点 JSON 数据块的响应式流
     */
    Flux<ByteBuf> descendantsAsync(long id);

    /**
     * 根据关键词分页搜索分类（同步阻塞）。
     * <p>
     * 在分类名称或其他可搜索字段上进行模糊匹配，返回匹配的分类列表。
     * 结果集以 JSON 数组形式返回，支持分页控制。
     * </p>
     *
     * @param key    搜索关键词
     * @param offset 起始位置（从 0 开始）
     * @param size   每页返回的最大记录数
     * @return 包含匹配分类 JSON 数组的输入流
     * @throws SearchException 如果搜索请求失败
     */
    InputStream search(String key, int offset, int size);

    default InputStream search(String key) {
        return this.search(key, 0, 64);
    }

    /**
     * 异步根据关键词分页搜索分类（响应式流）。
     * <p>
     * 返回一个 {@link Flux} 流，每个元素代表一个匹配分类的 JSON 数据块。
     * 支持背压和异步处理，适用于高并发搜索场景。
     * </p>
     *
     * @param key    搜索关键词
     * @param offset 起始位置
     * @param size   每页大小
     * @return 包含匹配分类 JSON 数据块的响应式流
     */
    Flux<ByteBuf> searchAsync(String key, int offset, int size);

    default Flux<ByteBuf> searchAsync(String key) {
        return this.searchAsync(key, 0, 64);
    }

    InputStream siblings(long id);

    InputStream path(long id);

    Flux<ByteBuf> pathAsync(long id);
}
