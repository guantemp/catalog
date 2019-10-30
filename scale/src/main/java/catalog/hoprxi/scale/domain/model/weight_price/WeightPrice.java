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

package catalog.hoprxi.scale.domain.model.weight_price;

import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/25
 */
public class WeightPrice {
    private static Money MONEY_ZERO = Money.zero(Monetary.getCurrency(Locale.getDefault()));
    private MonetaryAmount amount;
    private WeightUnit weightUnit;

    public WeightPrice(MonetaryAmount amount, WeightUnit weightUnit) {
        setAmount(amount);
        setWeightUnit(weightUnit);
    }

    private void setWeightUnit(WeightUnit weightUnit) {
        Objects.requireNonNull(weightUnit, "unit required");
        this.weightUnit = weightUnit;
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

    public WeightUnit weightUnit() {
        return weightUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeightPrice that = (WeightPrice) o;

        if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
        return weightUnit == that.weightUnit;
    }

    @Override
    public int hashCode() {
        int result = amount != null ? amount.hashCode() : 0;
        result = 31 * result + (weightUnit != null ? weightUnit.hashCode() : 0);
        return result;
    }

    public WeightPrice conversion(WeightUnit weightUnit) {
        if (this.weightUnit == weightUnit)
            return this;
        Number number = this.weightUnit.toKillogram(1);
        amount = amount.multiply(number);
        return new WeightPrice(amount, weightUnit);
    }
}
