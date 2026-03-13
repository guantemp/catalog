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

package catalog.hoprxi.scale.application.query;


import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.scale.domain.model.Plu;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

/**
 * 商品规模表 (scale) 的响应式查询接口。
 * <p>
 * 本接口基于 R2DBC 实现，提供非阻塞的数据库访问能力。
 * 所有方法均返回 {@link Flux} 响应式流，支持背压（Backpressure）控制，
 * 适用于高并发场景下的数据检索与流式传输。
 * </p>
 *
 * <h3>设计说明：</h3>
 * <ul>
 *     <li><b>二进制流输出</b>：方法返回 {@link ByteBuf} 而非实体对象，旨在将序列化逻辑（如 JSON 格式化）
 *     下沉至数据库层或特定的编码处理器，减少应用层内存占用，适合直接写入 HTTP 响应流。</li>
 *     <li><b>动态查询</b>：{@code searchAsync} 支持通过 {@link SqlClauseSpec} 数组动态构建复杂的
 *     WHERE 子句（包括全文检索、范围过滤、多字段组合等）。</li>
 * </ul>
 *
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.1 2026/3/6
 * @see SqlClauseSpec
 * @see SortFieldEnum
 * @since JDK 21
 */
public interface ScaleQuery {
    /**
     * 根据商品编码 (PLU) 异步查找单个商品的二进制数据流。
     * <p>
     * 该方法用于精确匹配查询。由于 PLU 是主键，理论上结果集最多包含一条记录。
     * 返回的 {@link Flux} 将发射单个 {@link ByteBuf} 元素（代表该商品的序列化数据），
     * 若未找到则完成为空（empty）。
     * </p>
     *
     * <p><b>性能特征：</b>
     * 利用主键索引进行 O(1) 复杂度的快速查找。
     * </p>
     *
     * @param plu 商品编码 (Price Look-Up code)，不能为 null。
     * @return 发射商品数据二进制流的 {@link Flux}。若商品不存在，则返回空流。
     * @throws IllegalArgumentException 如果 plu 为 null。
     */
    Flux<ByteBuf> findAsync(Plu plu);

    /**
     * 执行动态条件搜索，并返回分页排序后的商品二进制数据流。
     * <p>
     * 该方法支持复杂的组合查询，包括但不限于：
     * <ul>
     *     <li><b>全文检索</b>：基于 PostgreSQL GIN 索引的中文分词搜索（通过 {@code SqlClauseSpec} 构建 {@code @@ to_tsquery}）。</li>
     *     <li><b>多维过滤</b>：价格范围、分类 ID、品牌 ID、产地等字段的精确或范围匹配。</li>
     *     <li><b>动态排序</b>：支持按指定字段升序/降序排列。</li>
     * </ul>
     * </p>
     *
     * <p><b>参数说明：</b></p>
     * <ul>
     *     <li>{@code specs}：动态 SQL 子句规范数组。每个元素代表一个 WHERE 条件片段。
     *         框架会将这些片段安全地拼接并参数化，防止 SQL 注入。</li>
     *     <li>{@code offset}：分页偏移量（从 0 开始）。</li>
     *     <li>{@code size}：每页返回的最大记录数。</li>
     *     <li>{@code sortField}：排序字段枚举，定义结果集的排序规则。</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>返回的 {@link Flux} 是冷流（Cold Flux），仅在订阅时触发数据库查询。</li>
     *     <li>调用者应合理设置 {@code size} 以避免一次性加载过多数据导致内存压力。</li>
     *     <li>若 {@code specs} 为空数组，则代表查询所有记录（受限于 offset 和 size）。</li>
     * </ul>
     *
     * @param specs     动态查询条件数组，允许为空（表示无过滤条件）。
     * @param offset    分页偏移量，必须 >= 0。
     * @param size      分页大小，必须 > 0。
     * @param sortField 排序字段定义，若为 null 则使用默认排序（通常为主键或相关性得分）。
     * @return 发射匹配商品数据二进制流的 {@link Flux}。
     * @throws IllegalArgumentException 如果 offset < 0 或 size <= 0。
     */
    Flux<ByteBuf> searchAsync(SqlClauseSpec[] specs, int offset, int size, SortFieldEnum sortField);
}
