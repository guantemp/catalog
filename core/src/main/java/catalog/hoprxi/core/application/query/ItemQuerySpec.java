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

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * 定义生成查询子句的契约（接口）。
 * <p>
 * 实现此接口的类负责定义构建特定查询条件或过滤器的逻辑。
 * 生成的输出将直接写入提供的 {@link JsonGenerator}，从而能够灵活地集成到
 * 基于 JSON 的查询结构中（例如 Elasticsearch DSL、MongoDB 查询或自定义 JSON API）。
 * </p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * public class StatusQuerySpec implements ItemQuerySpec {
 *     private final String status;
 *
 *     public StatusQuerySpec(String status) {
 *         this.status = status;
 *     }
 *
 *     @Override
 *     public void queryClause(JsonGenerator generator) throws IOException {
 *         generator.writeStartObject();
 *         generator.writeStringField("term", "status");
 *         generator.writeStringField("value", status);
 *         generator.writeEndObject();
 *     }
 * }
 * }</pre>
 *
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025-01-03
 */
public interface ItemQuerySpec {
    /**
     * 使用提供的 JSON 生成器生成查询子句的表示形式。
     * <p>
     * 该方法应写入定义查询条件所需的 JSON 字段和结构。
     * 实现类有责任确保生成有效的 JSON 输出。
     * </p>
     *
     * @param generator 用于写入查询子句内容的 {@link JsonGenerator}。
     *                  不能为 null。
     * @throws IOException 如果在写入生成器时发生 I/O 错误。
     */
    void queryClause(JsonGenerator generator) throws IOException;
}
