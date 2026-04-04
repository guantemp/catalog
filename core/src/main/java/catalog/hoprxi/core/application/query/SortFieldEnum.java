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

import java.util.HashMap;
import java.util.Map;

/**
 *  * 商品排序字段枚举。
 *  * <p>
 *  * 该枚举定义了商品列表支持的所有排序字段，并通过命名约定隐式控制排序方向：
 *  * <ul>
 *  *   <li><b>升序 (ASC)</b>: 枚举名不带下划线前缀 (例如: {@link #NAME}, {@link #BARCODE})。</li>
 *  *   <li><b>降序 (DESC)</b>: 枚举名带下划线前缀 (例如: {@link #_NAME}, {@link #_ID})。</li>
 *  * </ul>
 *  * <p>
 *  * <b>设计特点：</b>
 *  * <ol>
 *  *   <li><b>高性能查找</b>: 内部维护了一个静态 {@code HashMap} 缓存，将输入字符串（不区分大小写）映射到枚举实例，避免每次调用 {@code valueOf()} 时的异常捕获开销。</li>
 *  *   <li><b>容错处理</b>: 当输入为空、空白或未知字段时，默认降级为 {@link #_ID} (按 ID 降序)，通常用于展示最新录入的商品。</li>
 *  *   <li><b>类型安全</b>: 强制调用者只能选择预定义的排序字段，防止 SQL 注入风险。</li>
 *  * </ol>
 *  *
 *  * <h3>使用示例</h3>
 *  * <pre>{@code
 *  * // 1. 解析用户输入 (支持 "name", "NAME", "_name" 等)
 *  * SortFieldEnum sortField = SortFieldEnum.of("name");
 *  *
 *  * // 2. 获取排序方向 ("asc" 或 "desc")
 *  * String direction = sortField.sort();
 *  * // 若输入为 "name" -> 返回 "asc"
 *  * // 若输入为 "_name" -> 返回 "desc"
 *  *
 *  * }</pre>
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @version 0.0.2 builder 2026-03-19
 * @since JDK21
 */
public enum SortFieldEnum {
    ID, _ID,
    NAME, _NAME,
    BARCODE, _BARCODE,
    MADE_IN, _MADE_IN,
    GRADE, _GRADE,
    SPEC, _SPEC,
    CATEGORY, _CATEGORY,
    BRAND, _BRAND,
    LAST_RECEIPT_PRICE, _LAST_RECEIPT_PRICE,
    RETAIL_PRICE, _RETAIL_PRICE,
    MEMBER_PRICE, _MEMBER_PRICE,
    VIP_PRICE, _VIP_PRICE;


    private static final Map<String, SortFieldEnum> CACHE = new HashMap<>();

    static {
        for (SortFieldEnum field : values()) {
            CACHE.put(field.name().toUpperCase(), field); // 统一转大写存储
        }
    }

    /**
     * @return SortField if name
     */
    public static SortFieldEnum of(String s) {
        // 1. 空值或空白字符处理
        if (s == null || s.trim().isEmpty()) {
            return SortFieldEnum._ID;
        }
        // 直接查找，找不到则返回默认值
        return CACHE.getOrDefault(s.toUpperCase(), SortFieldEnum._ID);
    }

    public String sort() {
        return name().charAt(0) == '_' ? "desc" : "asc";
    }
}
