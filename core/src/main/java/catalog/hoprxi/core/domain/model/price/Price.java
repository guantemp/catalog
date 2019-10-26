/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.domain.model.Unit;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/15
 */
public class Price {
    private static Money MONEY_ZERO = Money.zero(Monetary.getCurrency(Locale.getDefault()));
    public static Price ZERO = new Price(MONEY_ZERO, Unit.PCS);
    private MonetaryAmount amount;
    private Unit unit;

    public static Price zero(Locale locale) {
        return new Price(Money.zero(Monetary.getCurrency(locale)), Unit.PCS);
    }

    public Price(MonetaryAmount amount, Unit unit) {
        setAmount(amount);
        setUnit(unit);
    }

    private void setUnit(Unit unit) {
        Objects.requireNonNull(unit, "unit required");
        this.unit = unit;
    }

    private void setAmount(MonetaryAmount amount) {
        if (amount == null)
            amount = MONEY_ZERO;
        if (amount.isNegative())
            throw new IllegalArgumentException("amount isn't negative");
        this.amount = amount;
    }

    public MonetaryAmount amount() {
        return amount;
    }

    public Unit unit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Price price = (Price) o;

        if (amount != null ? !amount.equals(price.amount) : price.amount != null) return false;
        return unit == price.unit;
    }

    @Override
    public int hashCode() {
        int result = amount != null ? amount.hashCode() : 0;
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }
}
