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
package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-06-19
 */
public class ProhibitSellItem {
    private final Barcode barcode;
    private final long brandId;
    private long categoryId;
    private final GradeEnum grade;
    private final long id;
    private Name name;
    private final MadeIn madeIn;
    private LastReceiptPrice lastReceiptPrice;
    private final RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private final Specification spec;
    private ShelfLife shelfLife;

    protected ProhibitSellItem(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec,
                               GradeEnum grade, ShelfLife shelfLife, LastReceiptPrice lastReceiptPrice,RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        this.id = id;
        this.barcode = barcode;
        this.name = Objects.requireNonNull(name, "name required");
        this.madeIn = Objects.requireNonNull(madeIn, "madeIn required");
        this.spec = spec;
        this.grade = grade;
        setShelfLife(shelfLife);
        this.lastReceiptPrice=lastReceiptPrice;
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
        setMemberPrice(memberPrice);
        setVipPrice(vipPrice);
        this.categoryId = categoryId;
        this.brandId = brandId;
    }

    private void setVipPrice(VipPrice vipPrice) {
        Objects.requireNonNull(vipPrice, "vipPrice required");
        if (vipPrice.price().unit() != UnitEnum.PCS && vipPrice.price().unit() != retailPrice.price().unit())
            throw new IllegalArgumentException("vipPrice unit must be consistent with retailPrice unit");
        this.vipPrice = vipPrice;
    }

    private void setMemberPrice(MemberPrice memberPrice) {
        Objects.requireNonNull(memberPrice, "memberPrice required");
        if (memberPrice.price().unit() != UnitEnum.PCS && memberPrice.price().unit() != retailPrice.price().unit())
            throw new IllegalArgumentException("memberPrice unit must be consistent with retailPrice unit");
        this.memberPrice = memberPrice;
    }


    private void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.SAME_DAY;
        this.shelfLife = shelfLife;
    }

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    public LastReceiptPrice lastReceiptPrice() {
        return lastReceiptPrice;
    }

    /**
     * @param name
     */
    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!this.name.equals(name)) {
            this.name = name;
        }
    }

    /**
     * @param categoryId
     */
    public void moveToCategory(long categoryId) {
        if (this.categoryId != categoryId)
            this.categoryId = categoryId;
    }

    public Barcode barcode() {
        return barcode;
    }

    public long brandId() {
        return brandId;
    }

    public long categoryId() {
        return categoryId;
    }

    public GradeEnum grade() {
        return grade;
    }

    public long id() {
        return id;
    }

    public Name name() {
        return name;
    }

    public Specification spec() {
        return spec;
    }

    public MadeIn madeIn() {
        return madeIn;
    }

    public RetailPrice retailPrice() {
        return retailPrice;
    }

    public MemberPrice memberPrice() {
        return memberPrice;
    }

    public VipPrice vipPrice() {
        return vipPrice;
    }

    public Item permitSell() {
        return new Item(id, barcode, name, madeIn, spec, grade, shelfLife, LastReceiptPrice.RMB_PCS_ZERO, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ProhibitSellItem that)) return false;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "ProhibitSellItem{" +
                "barcode=" + barcode +
                ", brandId=" + brandId +
                ", categoryId=" + categoryId +
                ", grade=" + grade +
                ", id=" + id +
                ", name=" + name +
                ", madeIn=" + madeIn +
                ", lastReceiptPrice=" + lastReceiptPrice +
                ", retailPrice=" + retailPrice +
                ", memberPrice=" + memberPrice +
                ", vipPrice=" + vipPrice +
                ", spec=" + spec +
                ", shelfLife=" + shelfLife +
                '}';
    }
}
