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


import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/29
 */
public class WeightRetailPrice {
    private WeightPrice weightPrice;

    public WeightRetailPrice(WeightPrice weightPrice) {
        setWeightPrice(weightPrice);
    }

    public static final WeightRetailPrice zero(Locale locale) {
        return new WeightRetailPrice(WeightPrice.zero(locale));
    }

    public String name() {
        return Label.PRICE_RETAIL;
    }

    public WeightPrice weightPrice() {
        return weightPrice;
    }

    private void setWeightPrice(WeightPrice weightPrice) {
        Objects.requireNonNull(weightPrice, "weightPrice required");
        this.weightPrice = weightPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeightRetailPrice that = (WeightRetailPrice) o;

        return weightPrice != null ? weightPrice.equals(that.weightPrice) : that.weightPrice == null;
    }

    @Override
    public int hashCode() {
        return weightPrice != null ? weightPrice.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "WeightRetailPrice{" +
                "weightPrice=" + weightPrice +
                '}';
    }
}
