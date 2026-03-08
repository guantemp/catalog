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
import catalog.hoprxi.core.domain.model.price.LastReceiptPrice;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;

import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.2 2026/3/6
 * @since JDK 21
 */

public class Count extends Scale {
    private LastReceiptPrice lastReceiptPrice;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;

    public Count(Plu plu, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, ShelfLife shelfLife, LastReceiptPrice lastReceiptPrice,
                 RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        super(plu, name, spec, grade, madeIn, shelfLife, categoryId, brandId);
        this.lastReceiptPrice = Objects.requireNonNull(lastReceiptPrice, "lastReceiptPrice is null");
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
        this.memberPrice = Objects.requireNonNull(memberPrice, "memberPrice required");
        this.vipPrice = Objects.requireNonNull(vipPrice, "vipPrice is null");
    }

    public LastReceiptPrice lastReceiptPrice() {
        return lastReceiptPrice;
    }

    public void adjustLastReceiptPrice(LastReceiptPrice lastReceiptPrice) {
        Objects.requireNonNull(lastReceiptPrice, "lastReceiptPrice required");
        if (!this.lastReceiptPrice.equals(lastReceiptPrice))
            this.lastReceiptPrice = lastReceiptPrice;
    }

    public RetailPrice retailPrice() {
        return retailPrice;
    }

    public void adjustRetailPrice(RetailPrice retailPrice) {
        Objects.requireNonNull(retailPrice, "retailPrice required");
        if (!this.retailPrice.equals(retailPrice))
            this.retailPrice = retailPrice;
    }

    public MemberPrice memberPrice() {
        return memberPrice;
    }

    public VipPrice vipPrice() {
        return vipPrice;
    }

    @Override
    public String toString() {
        return "Count{" +
                "plu=" + plu +
                ", name=" + name +
                ", grade=" + grade +
                ", spec=" + spec +
                ", madeIn=" + madeIn +
                ", shelfLife=" + shelfLife +
                ", lastReceiptPrice=" + lastReceiptPrice +
                ", retailPrice=" + retailPrice +
                ", memberPrice=" + memberPrice +
                ", vipPrice=" + vipPrice +
                ", categoryId=" + categoryId +
                ", brandId=" + brandId +
                '}';
    }
}
