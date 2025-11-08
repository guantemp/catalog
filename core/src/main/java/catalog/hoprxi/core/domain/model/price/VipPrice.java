/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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
 * @version 0.0.2 builder 2025-11-08
 */
public class VipPrice {
    public static final VipPrice RMB_ZERO = new VipPrice(Price.zero(Locale.CHINA));
    public static final VipPrice USD_ZERO = new VipPrice(Price.zero(Locale.US));
    private static final int NAME_MAX_LENGTH = 64;
    private Price price;
    private String name;

    public VipPrice(String name, Price price) {
        setName(name);
        setPrice(price);
    }

    public VipPrice(Price price) {
        this(Label.PRICE_VIP, price);
    }

    public static VipPrice zero(Locale locale, Unit unit) {
        if (locale == Locale.CHINA || locale == Locale.CHINESE)
            return RMB_ZERO;
        if (locale == Locale.US)
            return USD_ZERO;
        return new VipPrice(Price.zero(locale, unit));
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
        return "VipPrice{" +
               "price=" + price +
               ", name='" + name + '\'' +
               '}';
    }
}
