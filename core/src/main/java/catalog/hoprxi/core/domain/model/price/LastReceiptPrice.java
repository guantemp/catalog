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
 * @version 0.0.1 builder 2023-07-11
 */
public class LastReceiptPrice {
    public static final LastReceiptPrice RMB_ZERO = new LastReceiptPrice(Price.zero(Locale.CHINA));
    public static final LastReceiptPrice USD_ZERO = new LastReceiptPrice(Price.zero(Locale.US));
    private static final int NAME_MAX_LENGTH = 64;
    private Price price;
    private String name;

    public LastReceiptPrice(Price price) {
        this(Label.PRICE_LAST_RECEIPT, price);
    }

    public LastReceiptPrice(String name, Price price) {
        setName(name);
        setPrice(price);
    }

    public static LastReceiptPrice zero(Locale locale, Unit unit) {
        return new LastReceiptPrice(Price.zero(locale, unit));
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LastReceiptPrice)) return false;

        LastReceiptPrice that = (LastReceiptPrice) o;

        if (!Objects.equals(price, that.price)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = price != null ? price.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LastReceiptPrice.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("price=" + price)
                .toString();
    }
}
