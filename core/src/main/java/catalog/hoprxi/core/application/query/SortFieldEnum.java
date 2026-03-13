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
import java.util.Objects;

/**
 * 定义物品（Item）搜索时可用的排序字段枚举。
 * <p>
 * 该枚举不仅封装了前端或 API 传入的排序字段名称（如 "ID", "NAME"），
 * 还映射了底层存储（如 Elasticsearch 或数据库）中对应的实际字段路径。
 * </p>
 *
 * <h3>命名约定与排序方向</h3>
 * <p>本枚举利用命名前缀隐式控制排序方向：</p>
 * <ul>
 *     <li><strong>升序 (ASC)</strong>：枚举常量名不以 {@code _} 开头（例如 {@link #NAME}, {@link #LAST_RECEIPT_PRICE}）。
 *         调用 {@link #sort()} 方法将返回 {@code "asc"}。</li>
 *     <li><strong>降序 (DESC)</strong>：枚举常量名以 {@code _} 开头（例如 {@link #_NAME}, {@link #_LAST_RECEIPT_PRICE}）。
 *         调用 {@link #sort()} 方法将返回 {@code "desc"}。</li>
 * </ul>
 * <p>这种设计允许客户端仅通过传递字段名称字符串即可同时指定“排序字段”和“排序顺序”。</p>
 *
 * <h3>字段映射示例</h3>
 * <table border="1" cellpadding="5" cellspacing="0">
 *   <tr>
 *     <th>枚举常量</th>
 *     <th>映射的实际字段路径 (field)</th>
 *     <th>默认排序方向</th>
 *   </tr>
 *   <tr>
 *     <td>{@link #ID}</td>
 *     <td>{@code id}</td>
 *     <td>ASC</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #_ID}</td>
 *     <td>{@code id}</td>
 *     <td>DESC</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #NAME}</td>
 *     <td>{@code name.mnemonic.raw}</td>
 *     <td>ASC</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #LAST_RECEIPT_PRICE}</td>
 *     <td>{@code last_receipt_price.price.number}</td>
 *     <td>ASC</td>
 *   </tr>
 * </table>
 *
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @version 0.0.1 builder 2024-11-24
 * @since JDK8.0
 */
public enum SortFieldEnum {
    ID("id"), _ID("id"),
    /**
     * 物品助记码/名称 (升序)，映射到 {@code name.mnemonic.raw}
     */
    NAME("name.mnemonic.raw"), _NAME("name.mnemonic.raw"),
    BARCODE("barcode.raw"), _BARCODE("barcode.raw"),
    MADE_IN("madeIn.code"), _MADE_IN("madeIn.code"),
    GRADE("grade"), _GRADE("grade"),
    SPEC("spec"), _SPEC("spec"),
    CATEGORY("category.name"), _CATEGORY("category.name"),
    BRAND("brand.name"), _BRAND("brand.name"),
    LAST_RECEIPT_PRICE("last_receipt_price.price.number"), _LAST_RECEIPT_PRICE("last_receipt_price.price.number"),
    RETAIL_PRICE("retail_price.number"), _RETAIL_PRICE("retail_price.number"),
    MEMBER_PRICE("member_price.price.number"), _MEMBER_PRICE("member_price.price.number"),
    VIP_PRICE("vip_price.price.number"), _VIP_PRICE("vip_price.price.number");
    private final String esField;

    SortFieldEnum(String esField) {
        this.esField = Objects.requireNonNull(esField, "field is required").trim();
    }

    /**
     * 底层存储对应的实际字段名称
     */
    public String field() {
        return esField;
    }

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

        String target = s.trim(); // 去除可能的首尾空格
        // 直接查找，找不到则返回默认值
        return CACHE.getOrDefault(s.toUpperCase(), SortFieldEnum._ID);
    }

    public String sort() {
        return name().charAt(0) == '_' ? "desc" : "asc";
    }
}
