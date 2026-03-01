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

package catalog.hoprxi.scale.domain.model.price;


import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Locale;
import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/29
 */
public class WeightMemberPrice {
    private final String name;
    private WeightPrice weightPrice;

    public static final WeightMemberPrice ZERO_KILOGRAM_RMB = new WeightMemberPrice(WeightPrice.ZERO_KILOGRAM_RMB);

    public WeightMemberPrice(String name, WeightPrice weightPrice) {
        this.name = Objects.requireNonNull(name, "name is null");
        setWeightPrice(weightPrice);
    }

    public WeightMemberPrice(WeightPrice weightPrice) {
        this(Label.PRICE_MEMBER, weightPrice);
    }

    public static WeightMemberPrice zero(String name, Locale locale) {
        return new WeightMemberPrice(name, WeightPrice.zero(locale));
    }

    public String name() {
        return name;
    }


    private void setWeightPrice(WeightPrice weightPrice) {
        if (weightPrice == null)
            weightPrice = WeightPrice.zero(Locale.getDefault());
        this.weightPrice = weightPrice;
    }

    public WeightPrice weightPrice() {
        return weightPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeightMemberPrice)) return false;
        WeightMemberPrice that = (WeightMemberPrice) o;
        return Objects.equals(name, that.name) && Objects.equals(weightPrice, that.weightPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, weightPrice);
    }

    @Override
    public String toString() {
        return "WeightMemberPrice{" +
                "name='" + name + '\'' +
                ", weightPrice=" + weightPrice +
                '}';
    }
}
