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
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/15
 */
public class Price {
    private MonetaryAmount amount;
    private Unit unit;

    public Price(MonetaryAmount amount, Unit unit) {
        setAmount(amount);
        setUnit(unit);
    }

    private void setUnit(Unit unit) {
        Objects.requireNonNull(unit, "unit is required");
        this.unit = unit;
    }

    private void setAmount(MonetaryAmount amount) {
        if (amount == null)
            amount = Money.zero(Monetary.getCurrency(Locale.getDefault()));
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
}
