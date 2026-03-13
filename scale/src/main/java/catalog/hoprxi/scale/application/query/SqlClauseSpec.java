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

/**
 * 定义生成 SQL 查询子句的规格契约。
 * <p>
 * 实现此接口的类负责将特定的业务查询条件转换为 {@link SqlClause} 对象。
 * 该接口采用了“规格模式”（Specification Pattern）的设计思想，允许动态构建
 * 复杂的 SQL WHERE 子句。
 * </p>
 *
 * <p><strong>主要职责：</strong></p>
 * <ul>
 *     <li>通过 {@link #toClause()} 方法生成具体的 SQL 片段（包含 SQL 模板和参数）。</li>
 *     <li>通过 {@link #isSatisfied()} 方法判断当前规格是否有效（即是否包含实际的过滤条件）。</li>
 * </ul>
 *
 * <p><strong>使用场景：</strong></p>
 * <p>在构建动态查询时，可以组合多个 {@code SqlClauseSpec} 实例。
 * 只有当 {@code isSatisfied()} 返回 {@code true} 时，才将其生成的 {@link SqlClause}
 * 添加到最终的查询构建器中，从而避免生成空的或无效的 SQL 条件（例如 "AND 1=1" 或多余的逗号）。</p>
 *
 * <pre>{@code
 * // 示例用法
 * List<SqlClauseSpec> specs = Arrays.asList(statusSpec, nameSpec, dateSpec);
 * StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE 1=1");
 * List<Object> params = new ArrayList<>();
 *
 * for (SqlClauseSpec spec : specs) {
 *     if (spec.isSatisfied()) {
 *         SqlClause clause = spec.toClause();
 *         sql.append(" AND ").append(clause.getSql());
 *         params.addAll(clause.getParameters());
 *     }
 * }
 * }</pre>
 *
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.1 2026/3/6
 * @see SqlClause
 * @since JDK 21
 */
public interface SqlClauseSpec {
    /**
     * 将当前的查询规格转换为具体的 SQL 子句对象。
     * <p>
     * 返回的 {@link SqlClause} 通常包含两部分：
     * <ol>
     *     <li>SQL 片段字符串（例如："status = ?" 或 "name LIKE ?"）。</li>
     *     <li>对应的参数值列表，用于防止 SQL 注入。</li>
     * </ol>
     * <strong>注意：</strong> 调用此方法前通常建议先检查 {@link #isSatisfied()}，
     * 尽管实现类应当保证即使在不满足条件下也能返回一个安全的空对象或抛出异常（具体视实现而定）。
     *
     * @return 生成的 {@link SqlClause} 对象，包含 SQL 片段和绑定参数。
     */
    SqlClause toClause();

    /**
     * 判断当前规格是否“满足”条件，即是否包含有效的查询内容。
     * <p>
     * 如果规格内部的条件值为 null、空字符串或空集合，此方法应返回 {@code false}，
     * 表示该规格不应参与最终 SQL 的构建。
     * 反之，如果存在有效的过滤条件，则返回 {@code true}。
     * </p>
     *
     * @return 如果规格包含有效的查询条件则返回 {@code true}，否则返回 {@code false}。
     */
    boolean isSatisfied(); // 判断规格是否满足（即是否有内容）
}

