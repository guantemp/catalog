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

import java.util.Collections;
import java.util.List;

/**
 * 表示一个完整的 SQL 查询片段及其对应的参数列表。
 * <p>
 * 该记录类通常用于动态构建 SQL 查询的场景（如规格模式 Specification Pattern）。
 * 它将 SQL 模板字符串（包含占位符 {@code ?}）与具体的参数值绑定在一起，
 * 以便安全地传递给 JDBC 驱动或数据库客户端执行，从而有效防止 SQL 注入攻击。
 * </p>
 *
 * <h3>结构说明</h3>
 * <ul>
 *     <li><strong>sql</strong>: SQL 语句片段。例如："age > ? AND status = ?"。
 *         如果构造时传入 null，将自动初始化为空字符串。</li>
 *     <li><strong>params</strong>: 与 SQL 片段中占位符顺序对应的参数值列表。
 *         如果构造时传入 null，将自动初始化为空列表（{@link Collections#emptyList()}）。</li>
 * </ul>
 *
 * <h3>不变量 (Invariants)</h3>
 * <p>通过紧凑构造函数（Compact Constructor）保证了以下不变量：</p>
 * <ul>
 *     <li>{@code sql} 永远不会为 {@code null}。</li>
 *     <li>{@code params} 永远不会为 {@code null}。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 构建一个查询条件：name LIKE ? AND age > ?
 * List<Object> params = new ArrayList<>();
 * params.add("%John%");
 * params.add(18);
 *
 * SqlClause clause = new SqlClause("name LIKE ? AND age > ?", params);
 *
 * // 获取 SQL 模板
 * String sqlTemplate = clause.sql(); // "name LIKE ? AND age > ?"
 *
 * // 获取参数列表
 * List<Object> boundParams = clause.params(); // ["%John%", 18]
 *
 * // 组合多个子句
 * SqlClause clause1 = new SqlClause("status = ?", List.of("ACTIVE"));
 * SqlClause clause2 = new SqlClause("type IN (?, ?)", List.of("A", "B"));
 *
 * String combinedSql = "SELECT * FROM table WHERE 1=1 AND " + clause1.sql() + " AND " + clause2.sql();
 * List<Object> combinedParams = new ArrayList<>();
 * combinedParams.addAll(clause1.params());
 * combinedParams.addAll(clause2.params());
 * }</pre>
 *
 * @param sql    SQL 语句片段，包含占位符（通常为 '?'）。
 * @param params 与占位符对应的参数值列表。
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/7
 * @see java.sql.PreparedStatement
 * @since JDK 21
 */
public record SqlClause(String sql, List<Object> params) {
    public SqlClause {
        params = params != null ? params : Collections.emptyList();
        sql = sql != null ? sql : "";
    }
}
