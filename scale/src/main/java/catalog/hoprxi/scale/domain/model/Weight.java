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
package catalog.hoprxi.scale.domain.model;

import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.domain.model.price.WeightLastReceiptPrice;
import catalog.hoprxi.scale.domain.model.price.WeightMemberPrice;
import catalog.hoprxi.scale.domain.model.price.WeightRetailPrice;
import catalog.hoprxi.scale.domain.model.price.WeightVipPrice;

import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.2 2026/3/4
 * @since JDK 21
 */
public class Weight extends Scale {
    private WeightLastReceiptPrice lastReceiptPrice;
    private WeightRetailPrice retailPrice;
    private WeightMemberPrice memberPrice;
    private WeightVipPrice vipPrice;

    public Weight(Plu plu, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, ShelfLife shelfLife, WeightLastReceiptPrice lastReceiptPrice,
                  WeightRetailPrice retailPrice, WeightMemberPrice memberPrice, WeightVipPrice vipPrice, long categoryId, long brandId) {
        super(plu, name, spec, grade, madeIn, shelfLife, categoryId, brandId);
        this.lastReceiptPrice = Objects.requireNonNull(lastReceiptPrice, "lastReceiptPrice is null");
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
        this.memberPrice = Objects.requireNonNull(memberPrice, "memberPrice is null");
        this.vipPrice = Objects.requireNonNull(vipPrice, "vipPrice is null");
    }

    public WeightMemberPrice memberPrice() {
        return memberPrice;
    }

    public WeightVipPrice vipPrice() {
        return vipPrice;
    }

    public WeightRetailPrice retailPrice() {
        return retailPrice;
    }

    public void adjustRetailPrice(WeightRetailPrice retailPrice) {
        Objects.requireNonNull(retailPrice, "retailPrice required");
        if (!this.retailPrice.equals(retailPrice))
            this.retailPrice = retailPrice;
    }

    public void adjustLastReceiptPrice(WeightLastReceiptPrice lastReceiptPrice) {
        Objects.requireNonNull(lastReceiptPrice, "lastReceiptPrice required");
        if (!this.lastReceiptPrice.equals(lastReceiptPrice))
            this.lastReceiptPrice = lastReceiptPrice;
    }

    public WeightLastReceiptPrice lastReceiptPrice() {
        return lastReceiptPrice;
    }

    @Override
    public String toString() {
        return "Weight{" +
                "lastReceiptPrice=" + lastReceiptPrice +
                ", retailPrice=" + retailPrice +
                ", memberPrice=" + memberPrice +
                ", vipPrice=" + vipPrice +
                ", brandId=" + brandId +
                ", categoryId=" + categoryId +
                ", grade=" + grade +
                ", plu=" + plu +
                ", name=" + name +
                ", spec=" + spec +
                ", shelfLife=" + shelfLife +
                ", madeIn=" + madeIn +
                '}';
    }
}
