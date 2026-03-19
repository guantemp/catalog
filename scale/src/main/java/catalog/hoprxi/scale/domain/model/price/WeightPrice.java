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

package catalog.hoprxi.scale.domain.model.price;

import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 2026-03-01
 * */
public record WeightPrice(MonetaryAmount amount, WeightUnit weightUnit) {
    public static final WeightPrice ZERO_KILOGRAM_RMB = new WeightPrice(Money.zero(Monetary.getCurrency(Locale.CHINA)), WeightUnit.KILOGRAM);
    public static final WeightPrice ZERO_USD_KILOGRAM = new WeightPrice(Money.zero(Monetary.getCurrency(Locale.US)), WeightUnit.KILOGRAM);
    public static final WeightPrice ZERO_EUR_KILOGRAM = new WeightPrice(Money.of(0, "EUR"), WeightUnit.KILOGRAM);

    public WeightPrice {
        Objects.requireNonNull(amount, "amount required");
        if (amount.isNegative())
            throw new IllegalArgumentException("amount isn't negative");
        if (weightUnit == null)
            weightUnit = WeightUnit.KILOGRAM;
    }

    public static WeightPrice zero(Locale locale) {
        Objects.requireNonNull(locale, "locale required");
        if ("CN".equals(locale.getCountry()))
            return ZERO_KILOGRAM_RMB;
        if ("US".equals(locale.getCountry()))
            return ZERO_USD_KILOGRAM;
        return new WeightPrice(Money.zero(Monetary.getCurrency(locale)), WeightUnit.KILOGRAM);
    }

    public static WeightPrice zero() {
        return WeightPrice.zero(Locale.getDefault());
    }

    public MonetaryAmount amount() {
        return amount;
    }

    public WeightUnit unit() {
        return weightUnit;
    }

    public WeightPrice convert(WeightUnit weightUnit) {
        if (this.weightUnit == weightUnit)
            return this;
        BigDecimal magnification = this.weightUnit.convert(weightUnit);
        return new WeightPrice(amount.divide(magnification), weightUnit);
    }
}
