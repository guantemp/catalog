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

import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Locale;
import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/1
 * @since JDK 21
 */

public class WeightLastReceiptPrice {
    private final String name;
    private WeightPrice weightPrice;

    public static final WeightLastReceiptPrice ZERO_KILOGRAM_RMB = new WeightLastReceiptPrice(Label.PRICE_LAST_RECEIPT, WeightPrice.ZERO_KILOGRAM_RMB);

    public WeightLastReceiptPrice(String name, WeightPrice weightPrice) {
        this.name = Objects.requireNonNull(name, "name is null").trim();
        setWeightPrice(weightPrice);
    }

    public WeightLastReceiptPrice(WeightPrice weightPrice) {
        this(Label.PRICE_LAST_RECEIPT, weightPrice);
    }

    public String name() {
        return name;
    }

    public WeightPrice price() {
        return weightPrice;
    }

    private void setWeightPrice(WeightPrice weightPrice) {
        if (weightPrice == null)
            weightPrice = WeightPrice.zero(Locale.getDefault());
        this.weightPrice = weightPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeightLastReceiptPrice)) return false;
        WeightLastReceiptPrice that = (WeightLastReceiptPrice) o;
        return Objects.equals(name, that.name) && Objects.equals(weightPrice, that.weightPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, weightPrice);
    }

    @Override
    public String toString() {
        return "WeightLastReceiptPrice{" +
                "name='" + name + '\'' +
                ", weightPrice=" + weightPrice +
                '}';
    }
}
