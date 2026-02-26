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

import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2026-02-22
 */
public class Price {
    public static final Price RMB_PCS_ZERO = new Price(Money.zero(Monetary.getCurrency(Locale.CHINA)), UnitEnum.PCS);
    public static final Price USD_PCS_ZERO = new Price(Money.zero(Monetary.getCurrency(Locale.US)), UnitEnum.PCS);

    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());
    private MonetaryAmount amount;
    private UnitEnum unit;

    public Price(MonetaryAmount amount, UnitEnum unit) {
        setAmount(amount);
        setUnit(unit);
    }

    public static Price zero(Locale locale) {
        Objects.requireNonNull(locale, "locale required");
        if ("CN".equals(locale.getCountry()))
            return RMB_PCS_ZERO;
        if ("US".equals(locale.getCountry()))
            return USD_PCS_ZERO;
        return new Price(Money.zero(Monetary.getCurrency(locale)), UnitEnum.PCS);
    }

    public static Price zero(Locale locale, UnitEnum unit) {
        return new Price(Money.zero(Monetary.getCurrency(locale)), unit);
    }

    private void setUnit(UnitEnum unit) {
        Objects.requireNonNull(unit, "unit required");
        this.unit = unit;
    }

    private void setAmount(MonetaryAmount amount) {
        Objects.requireNonNull(amount, "amount required");
        if (amount.isNegative())
            throw new IllegalArgumentException("amount isn't negative");
        this.amount = amount;
    }

    public MonetaryAmount amount() {
        return amount;
    }

    public UnitEnum unit() {
        return unit;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Price price)) return false;

        return Objects.equals(amount, price.amount) && unit == price.unit;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(amount);
        result = 31 * result + Objects.hashCode(unit);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Price.class.getSimpleName() + "[", "]")
                .add("amount=" + amount)
                .add("unit=" + unit)
                .toString();
    }
}
