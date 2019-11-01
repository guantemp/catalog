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

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/29
 */
public class WeightMemberPrice {
    private String name;
    private WeightPrice weightPrice;

    public WeightMemberPrice(String name, WeightPrice weightPrice) {
        setName(name);
        setWeightPrice(weightPrice);
    }

    public WeightMemberPrice(WeightPrice weightPrice) {
        this(Label.PRICE_MEMBER, weightPrice);
    }

    public static final WeightMemberPrice zero(String name, Locale locale) {
        return new WeightMemberPrice(name, WeightPrice.zero(locale));
    }

    public String name() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setWeightPrice(WeightPrice weightPrice) {
        this.weightPrice = weightPrice;
    }

    public WeightPrice weightPrice() {
        return weightPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeightMemberPrice that = (WeightMemberPrice) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return weightPrice != null ? weightPrice.equals(that.weightPrice) : that.weightPrice == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (weightPrice != null ? weightPrice.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WeightMemberPrice{" +
                "name='" + name + '\'' +
                ", weightPrice=" + weightPrice +
                '}';
    }
}
