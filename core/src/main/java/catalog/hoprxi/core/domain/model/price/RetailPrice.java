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
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2025-11-08
 */
public record RetailPrice(Price price) {
    public static final RetailPrice RMB_PCS_ZERO = new RetailPrice(Price.zero(Locale.CHINA));
    public static final RetailPrice USD_PCS_ZERO = new RetailPrice(Price.zero(Locale.US));

    public RetailPrice(Price price) {
        this.price = Objects.requireNonNull(price, "price required");
    }

    public static RetailPrice zero(Locale locale, UnitEnum unit) {
        Objects.requireNonNull(locale, "locale required");
        Objects.requireNonNull(unit, "unit required");
        if ("CN".equals(locale.getCountry()) && unit == UnitEnum.PCS)
            return RMB_PCS_ZERO;
        if ("US".equals(locale.getCountry()) && unit == UnitEnum.PCS)
            return USD_PCS_ZERO;
        return new RetailPrice(Price.zero(locale, unit));
    }

    public String name() {
        return Label.PRICE_RETAIL;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof RetailPrice that)) return false;

        return price.equals(that.price);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetailPrice.class.getSimpleName() + "[", "]")
                .add("name=" + Label.PRICE_RETAIL).add("price=" + price)
                .toString();
    }
}
