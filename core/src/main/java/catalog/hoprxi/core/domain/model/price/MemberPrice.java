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
 * @since JDK8.0
 * @version 0.0.1 2019-10-15
 */
public class MemberPrice {
    public static final MemberPrice RMB_ZERO = new MemberPrice(Price.zero(Locale.CHINA));
    public static final MemberPrice USD_ZERO = new MemberPrice(Price.zero(Locale.US));
    private static final int NAME_MAX_LENGTH = 64;
    private Price price;
    private String name;

    public MemberPrice(String name, Price price) {
        setName(name);
        setPrice(price);
    }

    public MemberPrice(Price price) {
        this(Label.PRICE_MEMBER, price);
    }

    public static MemberPrice zero(Locale locale, Unit unit) {
        return new MemberPrice(Price.zero(locale, unit));
    }

    private void setPrice(Price price) {
        this.price = Objects.requireNonNull(price, "price required");
    }

    private void setName(String name) {
        name = Objects.requireNonNull(name, "name required").trim();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH)
            throw new IllegalArgumentException("name length rang is 1-" + NAME_MAX_LENGTH);
        this.name = name;
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
}
