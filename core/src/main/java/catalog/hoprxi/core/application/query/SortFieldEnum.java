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

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-11-24
 */
public enum SortFieldEnum {
    ID("id"), _ID("id"), NAME("name.mnemonic.raw"), _NAME("name.mnemonic.raw"), BARCODE("barcode.raw"), _BARCODE("barcode.raw"), MADE_IN("madeIn.code"), _MADE_IN("madeIn.code"),
    GRADE("grade"), _GRADE("grade"), SPEC("spec"), _SPEC("spec"), CATEGORY("category.name"), _CATEGORY("category.name"), BRAND("brand.name"), _BRAND("brand.name"),
    LAST_RECEIPT_PRICE("last_receipt_price.price.number"), _LAST_RECEIPT_PRICE("last_receipt_price.price.number"),
    RETAIL_PRICE("retail_price.number"), _RETAIL_PRICE("retail_price.number"),
    MEMBER_PRICE("member_price.price.number"), _MEMBER_PRICE("member_price.price.number"),
    VIP_PRICE("vip_price.price.number"), _VIP_PRICE("vip_price.price.number");
    private final String field;

    SortFieldEnum(String field) {
        this.field = Objects.requireNonNull(field, "field is required").trim();
    }

    public String field() {
        return field;
    }

    /**
     *
     * @return SortField if name
     */
    public static SortFieldEnum of(String s) {
        for (SortFieldEnum sortField : values()) {
            if (sortField.name().equalsIgnoreCase(s))
                return sortField;
        }
        return SortFieldEnum._ID;
    }

    public String sort() {
        return name().charAt(0) == '_' ? "desc" : "asc";
    }
}
