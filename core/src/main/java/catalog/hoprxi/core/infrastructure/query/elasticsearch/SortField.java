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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-11-24
 */
public enum SortField {
    ID_ASC("id"), ID_DESC("id", false), NAME_ASC("name.mnemonic.raw"), NAME_DESC("name.mnemonic.raw", false),
    BARCODE_ASC("barcode.raw"), BARCODE_DESC("barcode.raw", false), MADE_IN_ASC("madeIn.code"), MADE_IN_DESC("madeIn.code", false),
    LAST_RECEIPT_PRICE_ASC("last_receipt_price.price.number"), LAST_RECEIPT_PRICE_DESC("last_receipt_price.price.number", false),
    RETAIL_PRICE_ASC("retail_price.price.number"), RETAIL_PRICE_DESC("retail_price.price.number", false);
    private String field;
    private boolean sort;

    SortField(String field) {
        this(field, true);
    }

    SortField(String field, boolean asc) {
        this.field = Objects.requireNonNull(field, "field is required").trim();
        this.sort = asc;
    }

    public String field() {
        return field;
    }

    public String sort() {
        return sort ? "asc" : "desc";
    }

    public static SortField of(String s) {
        for (SortField sortField : values()) {
            if (sortField.toString().equals(s))
                return sortField;
        }
        return SortField.ID_DESC;
    }
}
