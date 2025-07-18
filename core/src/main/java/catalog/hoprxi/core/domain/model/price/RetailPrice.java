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
public class RetailPrice {
    public static final RetailPrice RMB_ZERO = new RetailPrice(Price.zero(Locale.CHINA));
    public static final RetailPrice USD_ZERO = new RetailPrice(Price.zero(Locale.US));
    private Price price;

    public RetailPrice(Price price) {
        setPrice(price);
    }

    public static RetailPrice zero(Locale locale, Unit unit) {
        return new RetailPrice(Price.zero(locale, unit));
    }

    private void setPrice(Price price) {
        this.price = Objects.requireNonNull(price, "price required");
    }

    public Price price() {
        return price;
    }

    public String name() {
        return Label.PRICE_RETAIL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RetailPrice that = (RetailPrice) o;

        return price != null ? price.equals(that.price) : that.price == null;
    }

    @Override
    public int hashCode() {
        return price != null ? price.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetailPrice.class.getSimpleName() + "[", "]")
                .add("name=" + Label.PRICE_RETAIL).add("price=" + price)
                .toString();
    }
}
