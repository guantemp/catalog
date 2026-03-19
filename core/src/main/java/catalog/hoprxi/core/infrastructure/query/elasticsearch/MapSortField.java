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


import catalog.hoprxi.core.application.query.SortFieldEnum;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/3/19
 */

public final class MapSortField {
    public static String mapSortToField(SortFieldEnum sortField) {
        return switch (sortField) {
            case ID, _ID -> "id";
            case NAME, _NAME -> "name.mnemonic.raw";
            case BARCODE, _BARCODE -> "barcode.raw";
            case MADE_IN, _MADE_IN -> "made_in";
            case GRADE, _GRADE -> "grade";
            case SPEC, _SPEC -> "spec";
            case CATEGORY, _CATEGORY -> "category.name";
            case BRAND, _BRAND -> "brand.name";
            case LAST_RECEIPT_PRICE, _LAST_RECEIPT_PRICE -> "last_receipt_price.price.number";
            case RETAIL_PRICE, _RETAIL_PRICE -> "retail_price.number";
            case MEMBER_PRICE, _MEMBER_PRICE -> "member_price.number";
            case VIP_PRICE, _VIP_PRICE -> "vip_price.number";
        };
    }
}
