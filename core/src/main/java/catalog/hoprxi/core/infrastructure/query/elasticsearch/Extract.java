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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/2/17
 * @since JDK 21
 */

public final class Extract {

    /**
     * 从当前解析器位置开始，搜索第一个 {@code _source} 对象，并将其内容复制到输出生成器中，
     * 同时跳过 {@code _meta} 字段（如果存在）。
     *
     * <p>该方法不依赖任何特定的外部结构（如聚合或 hits），只要遇到顶层或嵌套的 {@code _source} 字段，
     * 就会将其整个对象内容复制到输出，但会过滤掉内部的 {@code _meta} 字段。
     *
     * <p><b>处理逻辑：</b>
     * <ul>
     *   <li>遍历所有 token，直到找到名为 {@code _source} 的字段；</li>
     *   <li>进入该字段的值（必须是 {@code START_OBJECT}）；</li>
     *   <li>开始输出一个对象（{@code gen.writeStartObject()}）；</li>
     *   <li>遍历 {@code _source} 对象内部的所有字段：</li>
     *   <ul>
     *     <li>如果字段名是 {@code _meta}，则跳过其整个值（不输出）；</li>
     *     <li>否则，先复制字段名，然后复制对应的整个值（支持嵌套结构）；</li>
     *   </ul>
     *   <li>输出对象结束标记；</li>
     *   <li>关闭生成器。</li>
     * </ul>
     *
     * <p><b>典型用途：</b>从 Elasticsearch 的 {@code _source} 中提取文档字段，同时丢弃元数据字段 {@code _meta}。
     *
     * @param parser JSON 解析器，应指向任意位置（方法会向前扫描直到找到 {@code _source}）
     * @param gen    JSON 生成器，用于输出处理后的对象
     * @throws IOException 如果解析或生成过程中发生 I/O 错误
     * @throws IllegalStateException 如果找到的 {@code _source} 字段的值不是对象（START_OBJECT）
     *
     * @see JsonParser#skipChildren()
     * @see JsonGenerator#copyCurrentStructure(JsonParser)
     */
    public static void extractSourceSkipMeta(JsonParser parser, JsonGenerator gen) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.currentName())) {
                parser.nextToken(); // move to value (START_OBJECT)
                if (parser.currentToken() != JsonToken.START_OBJECT) {
                    throw new IllegalStateException("_source is not an object");
                }
                gen.writeStartObject();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName())) {
                        parser.nextToken();
                        parser.skipChildren(); // skip value of _meta
                    } else {
                        gen.copyCurrentEvent(parser); // copy field name
                        parser.nextToken();
                        gen.copyCurrentStructure(parser); // copy entire value (handles nested)
                    }
                }
                gen.writeEndObject();
            }
        }
        gen.close();
    }


    /**
     * 从 Elasticsearch 搜索或聚合响应中提取关键信息，生成一个简化的结果对象。
     *
     * <p>该方法解析标准 ES 搜索响应（包含 {@code hits} 和可选的 {@code aggregations}），提取：
     * <ul>
     *   <li>总命中数（{@code hits.total.value}），写入字段 {@code total}；</li>
     *   <li>文档列表，写入以给定 {@code objectName} 为名称的数组字段；</li>
     *   <li>聚合结果（如果存在），原样复制到字段 {@code aggregations}。</li>
     * </ul>
     * 对于每个命中文档（{@code hits.hits} 数组中的元素），该方法提取 {@code _source} 对象中的所有字段
     * （但会跳过 {@code _meta} 字段），同时还会提取顶层的 {@code sort} 字段（如果存在），
     * 并将其与 {@code _source} 内容合并输出到同一个对象中。
     *
     * <p><b>输出结构示例：</b>
     * <pre>
     * {
     *   "total": 123,
     *   "items": [
     *     {
     *       "id": 1,
     *       "name": "商品",
     *       "sort": [1234567890]
     *     },
     *     ...
     *   ],
     *   "aggregations": { ... }   // 如果原响应中包含 aggregations
     * }
     * </pre>
     *
     * <p><b>处理逻辑：</b>
     * <ol>
     *   <li>读取根对象的各个字段：</li>
     *   <ul>
     *     <li>如果是 {@code hits} 字段，进入并解析：</li>
     *     <ul>
     *       <li>从 {@code hits.total.value} 提取总数，写入 {@code total}；</li>
     *       <li>从 {@code hits.hits} 数组中提取每个命中的 {@code _source} 和 {@code sort} 字段，</li>
     *       <li>以 {@code objectName} 为数组名输出这些对象；</li>
     *     </ul>
     *     <li>如果是 {@code aggregations} 字段，原样复制到输出；</li>
     *     <li>其他根级字段（如 {@code took}、{@code _shards}、{@code timed_out}）直接跳过；</li>
     *   </ul>
     *   <li>如果响应中没有 {@code hits} 字段或 {@code hits.hits} 数组为空，则输出 {@code total: 0} 和空数组；</li>
     *   <li>关闭生成器。</li>
     * </ol>
     *
     * @param parser     JSON 解析器，应指向 Elasticsearch 响应对象的开始（通常为 {@code START_OBJECT}）
     * @param gen        JSON 生成器，用于输出处理后的结果
     * @param objectName 文档数组的字段名，例如 {@code "items"}、{@code "documents"} 等，不能为空
     * @throws NullPointerException 如果 {@code objectName} 为 {@code null}
     * @throws IOException 如果解析或生成过程中发生 I/O 错误
     * @throws IllegalStateException 如果 JSON 结构不符合 Elasticsearch 响应的预期格式
     *                               （例如 {@code hits} 或 {@code hits.hits} 的类型错误）
     *
     * @apiNote 该方法会修改解析器的位置，调用前请确保解析器处于响应对象的开始处或任意位置（方法会向前扫描）。
     *          {@code _source} 中的 {@code _meta} 字段会被自动跳过，不包含在输出中。
     *          {@code sort} 字段如果存在，会与 {@code _source} 的内容合并到同一个输出对象中。
     */
    public static void extract(JsonParser parser, JsonGenerator gen, String objectName) throws IOException {
        Objects.requireNonNull(objectName, "objectName is required");
        boolean hitsFound = false;
        gen.writeStartObject();
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                if ("hits".equals(name)) {
                    parser.nextToken(); // should be START_OBJECT
                    if (parser.currentToken() != JsonToken.START_OBJECT) {
                        throw new IllegalStateException("Hits' must be an object");
                    }
                    hitsFound = true;
                    boolean hasHitsArray = false;
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        if (parser.currentToken() != JsonToken.FIELD_NAME) continue;
                        String hitsField = parser.currentName();
                        if ("total".equals(hitsField)) {
                            parser.nextToken(); // enter total object
                            if (parser.currentToken() != JsonToken.START_OBJECT) {
                                throw new IllegalStateException("Total must be an object");
                            }
                            while (parser.nextToken() != JsonToken.END_OBJECT) { // Extract only "value"
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                                    parser.nextToken();
                                    gen.writeNumberField("total", parser.getValueAsLong());
                                } else {
                                    parser.skipChildren();
                                }
                            }
                        } else if ("hits".equals(hitsField)) {
                            parser.nextToken(); // should be START_ARRAY
                            if (parser.currentToken() != JsonToken.START_ARRAY) {
                                throw new IllegalStateException("ES7 'hits.hits' must be an array");
                            }

                            gen.writeArrayFieldStart(objectName);//start arrays
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
                                    parser.skipChildren();
                                    continue;
                                }
                                gen.writeStartObject();
                                while (parser.nextToken() != JsonToken.END_OBJECT) { // Process one hit
                                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.currentName())) {
                                        parser.nextToken(); // enter _source
                                        if (parser.currentToken() != JsonToken.START_OBJECT) {
                                            parser.skipChildren();
                                            continue;
                                        }
                                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                                            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                                String srcField = parser.currentName();
                                                if ("_meta".equals(srcField)) {
                                                    parser.nextToken();
                                                    parser.skipChildren();
                                                } else {
                                                    gen.writeFieldName(srcField);
                                                    parser.nextToken();
                                                    gen.copyCurrentStructure(parser);
                                                }
                                            }
                                        }
                                    } else if (parser.currentToken() == JsonToken.FIELD_NAME && "sort".equals(parser.currentName())) {
                                        gen.writeFieldName("sort");
                                        parser.nextToken();
                                        gen.copyCurrentStructure(parser);
                                    } else {
                                        parser.skipChildren(); // skip _id, _index, sort, _score, etc.
                                    }
                                }
                                gen.writeEndObject();//end source + sort
                            }
                            gen.writeEndArray();//end objectName array
                            hasHitsArray = true;//have object
                        } else {
                            parser.skipChildren(); // skip max_score, etc.
                        }
                    }

                    if (!hasHitsArray) {//no objectName array,write empty array
                        gen.writeArrayFieldStart(objectName);
                        gen.writeEndArray();
                    }
                } else if ("aggregations".equals(name)) {
                    parser.nextToken();
                    gen.writeFieldName("aggregations");
                    gen.copyCurrentStructure(parser);

                } else {
                    parser.skipChildren(); // skip took, timed_out, _shards, etc.
                }
            }
        }
        if (!hitsFound) {
            gen.writeNumberField("total", 0);
            gen.writeArrayFieldStart(objectName);
            gen.writeEndArray();
        }
        gen.writeEndObject();
        gen.close();
    }

    /**
     * 从 Elasticsearch 聚合响应中提取建议结果。
     *
     * <p>该方法解析输入的 JSON 流，定位到 {@code buckets} 数组，遍历每个 bucket 对象，
     * 查找其中的 {@code _source} 对象，并从 {@code _source} 中提取 {@code id}、{@code barcode}
     * 和 {@code name} 字段（{@code name} 字段支持直接字符串或形如 {@code {"name": "实际值"}} 的嵌套对象）。
     * 提取后的数据以 JSON 对象数组的形式写入输出生成器，每个对象包含 {@code id}、{@code barcode}、{@code name} 字段。
     *
     * <p>该方法是纯流式处理，不创建中间 DOM 树，内存效率高。对于非 {@code _source} 的字段，
     * 使用 {@link JsonParser#skipChildren()} 快速跳过，确保解析性能。
     *
     * @param parser JSON 解析器，应指向聚合响应的根节点或任意位置（方法会自动定位到 {@code buckets} 数组）
     * @param gen    JSON 生成器，用于输出结果数组
     * @throws IOException 如果解析或生成过程中发生 I/O 错误
     * @throws IllegalStateException 如果未找到 {@code buckets} 数组或 JSON 结构不符合预期
     *
     * @implNote 该方法假设输入的 JSON 符合 Elasticsearch 聚合响应格式，例如：
     * <pre>
     * {
     *   "aggregations": {
     *     "sampler": {
     *       "suggests": {
     *         "buckets": [
     *           {
     *             "key": "...",
     *             "doc_count": 1,
     *             "hit": {
     *               "hits": {
     *                 "hits": [
     *                   {
     *                     "_source": {
     *                       "id": 123,
     *                       "barcode": "123456",
     *                       "name": { "name": "商品名称" }
     *                     }
     *                   }
     *                 ]
     *               }
     *             }
     *           }
     *         ]
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * <p>处理逻辑要点：
     * <ul>
     *   <li>跳过 {@code buckets} 之前的所有 token；</li>
     *   <li>对每个 bucket 对象使用深度计数器 {@code depth} 确保完整消费；</li>
     *   <li>遇到 {@code _source} 字段时，独立循环解析其内部，不依赖外部深度计数器增减；</li>
     *   <li>{@code name} 字段如果为对象，则进一步解析内层 {@code name} 字段；</li>
     *   <li>非 {@code _source} 的字段统一用 {@code skipChildren()} 跳过；</li>
     *   <li>输出结果数组，每个元素包含 id, barcode, name（仅当相应字段存在时写入）。</li>
     * </ul>
     *
     * @see JsonParser#skipChildren()
     * @see JsonGenerator#writeStartArray()
     * @see JsonGenerator#writeEndArray()
     */
    public static void extractSuggest(JsonParser parser, JsonGenerator gen) throws IOException {
        // 1. 定位到 buckets 数组
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME && "buckets".equals(parser.currentName())) {
                parser.nextToken(); // 进入数组 START_ARRAY
                break;
            }
        }
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("未找到 buckets 数组");
        }

        gen.writeStartArray(); // 输出外层数组
        // 2. 遍历每个 bucket
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() != JsonToken.START_OBJECT) continue;

            long id = 0L;
            String name = null, barcode = null;
            // 当前 bucket 对象的嵌套深度（从1开始，因为当前在 START_OBJECT）
            int depth = 1;
            //System.out.println("\nstart:" + depth);
            // 只要 depth > 0 就一直读，保证完整消耗当前 bucket 对象
            while (depth > 0) {
                JsonToken token = parser.nextToken();
                if (token == null) break;
                switch (token) {
                    case START_OBJECT:
                    case START_ARRAY:
                        depth++;
                        break;
                    case END_OBJECT:
                    case END_ARRAY:
                        depth--;
                        break;
                    case FIELD_NAME:
                        if ("_source".equals(parser.currentName())) {
                            // 找到 _source 字段，读取它的值
                            token = parser.nextToken();
                            if (token == JsonToken.START_OBJECT) {
                                // 不能增加深度，因为 _source 对象会有一个 END_OBJECT 来减少深度使depth==0来退出对象循环
                                //depth++;
                                // 解析 _source 对象内部
                                while (parser.nextToken() != JsonToken.END_OBJECT) {
                                    if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                        String srcField = parser.currentName();

                                        parser.nextToken(); // 进入值
                                        switch (srcField) {
                                            case "barcode" -> barcode = parser.getValueAsString();
                                            case "id" -> id = parser.getLongValue();
                                            case "name" -> {
                                                // name 可能是字符串或对象 {"name":"xxx"}
                                                if (parser.currentToken() == JsonToken.START_OBJECT) {
                                                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                                                        if (parser.currentToken() == JsonToken.FIELD_NAME && "name".equals(parser.currentName())) {
                                                            parser.nextToken();
                                                            name = parser.getValueAsString();
                                                        } else {
                                                            parser.skipChildren();
                                                        }
                                                    }
                                                }
                                            }
                                            case null, default -> parser.skipChildren(); // 跳过其他字段
                                        }
                                    }
                                }
                                // 此时 parser 在 _source 的 END_OBJECT 上，但该 END_OBJECT 尚未被外层 switch 处理
                                // 由于我们已经手动 depth++，并且这个 END_OBJECT 会在下一次循环开始时被外层捕获并 depth--
                                // 为了避免重复减，我们在这里不额外处理，让外层循环自然处理
                            } else {
                                // _source 不是对象，直接跳过整个值
                                parser.skipChildren();
                            }
                            //System.out.println("_source:" + depth);
                        }
                        break;
                    default:    // 其他 token（如字符串、数字等）忽略
                        break;
                }
                //System.out.println(parser.currentToken() + ":" + parser.currentName());
                //System.out.println("_bukes:" + depth);
            }
            // 当前 bucket 处理完毕，输出提取的结果
            if (barcode != null || name != null || id != 0L) {
                gen.writeStartObject();
                if (id != 0L) gen.writeNumberField("id", id);
                if (barcode != null) gen.writeStringField("barcode", barcode);
                if (name != null) gen.writeStringField("name", name);
                gen.writeEndObject();
            }
        }

        gen.writeEndArray();
        gen.close();
    }

    /**
     * 从 Elasticsearch 响应流中提取树形结构数据，并将结果以 JSON 格式输出。
     * <p>
     * 此方法假定输入 JSON 流包含 Elasticsearch 的标准搜索响应（带有 {@code hits.hits} 数组），
     * 其中每个命中的 {@code _source} 必须包含 {@code left} 和 {@code right} 整数字段，用于表示嵌套集（Nested Set）模型。
     * 方法会遍历所有命中，根据 {@code left}/{@code right} 值动态重组树形层次结构，生成嵌套的 JSON 对象，
     * 并在根节点下以指定的 {@code title} 作为字段名输出该树。
     * </p>
     * <p>
     * 输出 JSON 结构示例（假设 title = "category"）：
     * <pre>
     * {
     *   "category": {
     *     "name": "root",
     *     "left": 1,
     *     "right": 10,
     *     "children": [
     *       { "name": "child1", "left": 2, "right": 3 },
     *       { "name": "child2", "left": 4, "right": 9, "children": [...] }
     *     ]
     *   }
     * }
     * </pre>
     * </p>
     *
     * @param parser Jackson JSON 解析器，已指向 Elasticsearch 响应的起始位置
     * @param gen    Jackson JSON 生成器，用于输出重组后的树形 JSON
     * @param title  输出树形结构的根字段名称，该字段的值即为整个树对象；不可为 {@code null} 或空白
     * @throws IOException           如果解析或生成过程中发生 I/O 错误
     * @throws IllegalStateException 如果输入 JSON 结构不符合预期（例如缺少 {@code hits.hits} 数组，
     *                               或 {@code _source} 中缺少 {@code left}/{@code right} 字段）
     */
    public static void extractAsTree(JsonParser parser, JsonGenerator gen, String title) throws IOException {
        Deque<Integer> rightValueStack = new ArrayDeque<>();
        gen.writeStartObject();//start
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                if ("hits".equals(name)) {//first hits
                    parser.nextToken(); // should be START_OBJECT
                    if (parser.currentToken() != JsonToken.START_OBJECT) {
                        throw new IllegalStateException("Hits' must be an object");
                    }
                    while (parser.nextToken() != JsonToken.END_OBJECT) {//loop first hits
                        if (parser.currentToken() != JsonToken.FIELD_NAME) continue;
                        String hitsField = parser.currentName();
                        if ("total".equals(hitsField)) {//loop total
                            parser.nextToken();
                            if (parser.currentToken() != JsonToken.START_OBJECT) {
                                throw new IllegalStateException("Total must be an object");
                            }
                            while (parser.nextToken() != JsonToken.END_OBJECT) { // Extract only "value"
                                if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                                    parser.nextToken();
                                    gen.writeNumberField("total", parser.getValueAsLong());
                                } else {
                                    parser.skipChildren();
                                }
                            }
                        } else if ("hits".equals(hitsField)) {//hits.hits
                            parser.nextToken(); // should be START_ARRAY
                            if (parser.currentToken() != JsonToken.START_ARRAY) {
                                throw new IllegalStateException("'hits.hits' must be an array");
                            }
                            gen.writeObjectFieldStart(title);//不管里面有没有，先写个
                            boolean first = true;
                            while (parser.nextToken() != JsonToken.END_ARRAY) {//loop hits array
                                if (parser.getCurrentToken() != JsonToken.START_OBJECT) {//not start blank start object
                                    parser.skipChildren();
                                    continue;
                                }
                                int left = 1, right = 1;
                                while (parser.nextToken() != JsonToken.END_OBJECT) {//loop { 开始在hits下面的{}中循环,包含_source,_index啥的
                                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_source".equals(parser.currentName())) {// enter _source
                                        parser.nextToken();
                                        if (parser.currentToken() != JsonToken.START_OBJECT) {
                                            parser.skipChildren();
                                            continue;
                                        }
                                        if (!first)//第一个,上面写了title:{,不写了,后面需要写｛
                                            gen.writeStartObject();
                                        while (parser.nextToken() != JsonToken.END_OBJECT) {//loop source
                                            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                                                String srcField = parser.currentName();
                                                if ("_meta".equals(srcField)) {
                                                    //parser.nextToken();
                                                    parser.skipChildren();
                                                } else {
                                                    gen.writeFieldName(srcField);
                                                    parser.nextToken();
                                                    switch (srcField) {//这个位置必须固定在这里
                                                        case "left" -> left = parser.getValueAsInt();
                                                        case "right" -> right = parser.getValueAsInt();
                                                    }
                                                    //System.out.println(parser.currentToken()+":"+parser.currentName()+":"+first);
                                                    //System.out.println(right + ":" + left + ":" + (right - left));
                                                    gen.copyCurrentStructure(parser);
                                                }
                                            }
                                        }//end loop source
                                    } else {
                                        parser.skipChildren(); // skip _id, _index, sort, _score, etc.
                                    }
                                }//end loop {
                                first = false;//第一 category 本体source结束了,下面直接写开始{
                                if (right - left == 1) {//叶子,闭合
                                    gen.writeEndObject();
                                }
                                if (right - left > 1) {//有儿子
                                    gen.writeArrayFieldStart("children");
                                    rightValueStack.push(right);
                                }
                                while (!rightValueStack.isEmpty() && rightValueStack.peek() - right == 1) {
                                    gen.writeEndArray();//end children array
                                    gen.writeEndObject();//每个end children array后面就结束父对象
                                    right = rightValueStack.pop();
                                }
                            }//end loop hits array
                        } else {//skip warp hits
                            parser.skipChildren(); // skip max_score, etc.
                        }
                    }
                } else {//skip not wrap hits
                    parser.skipChildren(); // skip max_score, etc.
                }
            }
        }
        while (!rightValueStack.isEmpty()) {
            gen.writeEndArray();
            gen.writeEndObject();
            rightValueStack.pop();
        }
        gen.writeEndObject();//end
        gen.close();
    }
}
