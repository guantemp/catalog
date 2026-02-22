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
 * @version 0.0.3 builder 2026-02-22
 */
public class MemberPrice {
    public static final MemberPrice RMB_PCS_ZERO = new MemberPrice(Price.zero(Locale.CHINA));
    public static final MemberPrice USD_PCS_ZERO = new MemberPrice(Price.zero(Locale.US));
    private static final int NAME_MAX_LENGTH = 64;
    private final Price price;
    private final String name;

    public MemberPrice(String name, Price price) {
        this.name = (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) ? Label.PRICE_MEMBER : name.trim();
        this.price = Objects.requireNonNull(price, "price required");
    }

    public MemberPrice(Price price) {
        this(Label.PRICE_MEMBER, price);
    }

    public static MemberPrice zero(Locale locale, UnitEnum unit) {
        Objects.requireNonNull(locale, "locale required");
        Objects.requireNonNull(unit, "unit required");
        if ("CN".equals(locale.getCountry()) && unit == UnitEnum.PCS)
            return RMB_PCS_ZERO;
        if ("US".equals(locale.getCountry()) && unit == UnitEnum.PCS)
            return USD_PCS_ZERO;
        return new MemberPrice(Price.zero(locale, unit));
    }

    public Price price() {
        return price;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MemberPrice.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'").add("price=" + price)
                .toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MemberPrice that)) return false;

        return Objects.equals(price, that.price) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(price);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }
}
