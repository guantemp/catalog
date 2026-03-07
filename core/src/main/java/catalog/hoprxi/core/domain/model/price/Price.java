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

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2026-02-22
 */
public record Price(MonetaryAmount amount, UnitEnum unit) {
    private static final CurrencyUnit CNY = Monetary.getCurrency("CNY");
    private static final CurrencyUnit USD = Monetary.getCurrency("USD");
    public static final Price ZERO_RMB_PCS = new Price(Money.zero(CNY), UnitEnum.PCS);
    public static final Price ZERO_USD_PCS = new Price(Money.zero(USD), UnitEnum.PCS);

    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());


    public Price {
        Objects.requireNonNull(amount, "amount required");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        Objects.requireNonNull(unit, "unit required");
    }

    /**
     * Creates a zero-value Price instance using system default locale and {@link UnitEnum#PCS} unit.
     *
     * <p>Default locale is obtained via {@link Locale#getDefault()}, and default currency is determined by this locale.
     * For {@link UnitEnum#PCS} unit, pre-defined zero-price constants (CNY/USD) are returned if applicable;
     * otherwise, a new zero-value Price instance is created with the locale's currency.
     *
     * @return a zero-value Price instance (locale: system default, unit: PCS)
     */
    public static Price zero() {
        return Price.zero(Locale.getDefault(), UnitEnum.PCS);
    }

    /**
     * Creates a zero-value Price instance using the specified locale and {@link UnitEnum#PCS} unit.
     *
     * <p>Currency is determined by the given locale. For {@link UnitEnum#PCS} unit, pre-defined zero-price constants
     * (CNY/USD) are returned if the locale's currency matches; otherwise, a new zero-value Price instance is created.
     *
     * @param locale the locale to determine the currency (must not be null)
     * @return a zero-value Price instance (locale: specified, unit: PCS)
     * @throws NullPointerException if the given locale is null
     */
    public static Price zero(Locale locale) {
        return Price.zero(locale, UnitEnum.PCS);
    }

    /**
     * Creates a zero-value Price instance with the specified locale and unit.
     *
     * <p>Key logic:
     * <ul>
     *   <li>Validates that locale and unit are not null</li>
     *   <li>Determines the target currency based on the locale</li>
     *   <li>For {@link UnitEnum#PCS} unit: returns pre-defined CNY/USD zero-price constants if currency matches</li>
     *   <li>For other units/currencies: creates a new zero-value Price instance with the target currency and unit</li>
     * </ul>
     *
     * @param locale the locale to determine the currency (must not be null)
     * @param unit   the price unit (must not be null)
     * @return a zero-value Price instance with the specified locale and unit
     * @throws NullPointerException if locale or unit is null
     */
    public static Price zero(Locale locale, UnitEnum unit) {
        Objects.requireNonNull(locale, "locale required");
        Objects.requireNonNull(unit, "unit required");
        CurrencyUnit targetCurrency = Monetary.getCurrency(locale);
        if (unit == UnitEnum.PCS) {
            if (CNY == targetCurrency) {
                return ZERO_RMB_PCS;
            }
            if (USD == targetCurrency) {
                return ZERO_USD_PCS;
            }
        }
        return new Price(Money.zero(targetCurrency), unit);
    }
}
