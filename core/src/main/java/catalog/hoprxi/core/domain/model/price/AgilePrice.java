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

package catalog.hoprxi.core.domain.model.price;

import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 2026-03-01
 */
public record AgilePrice(Price price) {

    public AgilePrice {
        Objects.requireNonNull(price, "price required");
    }

    /**
     * Creates an AgilePrice instance with zero price value.
     *
     * @param locale the locale (affects price formatting)
     * @param unit   the price unit
     * @return an AgilePrice instance with zero price value
     */
    public static AgilePrice zero(Locale locale, UnitEnum unit) {
        return new AgilePrice(Price.zero(locale, unit));
    }

    /**
     * Creates an AgilePrice instance with zero price value using default locale and unit.
     *
     * <p>Default locale is obtained via {@link Locale#getDefault()}, and default unit is {@link UnitEnum#PCS}.
     *
     * @return an AgilePrice instance with zero price value (default locale: system default, default unit: PCS)
     */
    public static AgilePrice zero() {
        return zero(Locale.getDefault(), UnitEnum.PCS);
    }

    public String name() {
        return Label.PRICE_AGILE;
    }
}
