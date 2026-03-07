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

package catalog.hoprxi.core.domain.model.price;

import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.3 builder 2026-02-22
 */
public record LastReceiptPrice(String name, Price price) {
    public static final LastReceiptPrice ZERO_RMB_PCS = new LastReceiptPrice(Price.zero(Locale.CHINA));
    public static final LastReceiptPrice ZERO_USD_PCS = new LastReceiptPrice(Price.zero(Locale.US));
    private static final int NAME_MAX_LENGTH = 64;

    public LastReceiptPrice {
        // 名称规范化：空值/空白/超长则使用默认名称，否则trim去空格
        name = (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH)
                ? Label.PRICE_LAST_RECEIPT
                : name.trim();
        // 价格非空校验（保留原异常提示）
        Objects.requireNonNull(price, "price required");
    }

    public LastReceiptPrice(Price price) {
        this(Label.PRICE_LAST_RECEIPT, price);
    }

    public static LastReceiptPrice zero(Locale locale, UnitEnum unit) {
        Objects.requireNonNull(locale, "locale required");
        Objects.requireNonNull(unit, "unit required");
        if ("CN".equals(locale.getCountry()) && unit == UnitEnum.PCS)
            return ZERO_RMB_PCS;
        if ("US".equals(locale.getCountry()) && unit == UnitEnum.PCS)
            return ZERO_USD_PCS;
        return new LastReceiptPrice(Price.zero(locale, unit));
    }

    public static LastReceiptPrice zero(Locale locale) {
        return LastReceiptPrice.zero(locale, UnitEnum.PCS);
    }

    public static LastReceiptPrice zero() {
        return LastReceiptPrice.zero(Locale.getDefault(), UnitEnum.PCS);
    }
}
