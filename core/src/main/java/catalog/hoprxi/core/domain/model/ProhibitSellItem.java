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
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-06-19
 */
public class ProhibitSellItem {
    private Barcode barcode;
    private long brandId;
    private long categoryId;
    private GradeEnum grade;
    private long id;
    private Name name;
    private MadeIn madeIn;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private Specification spec;
    private ShelfLife shelfLife;

    /**
     * @param id
     * @param barcode
     * @param name
     * @param madeIn
     * @param spec
     * @param grade
     * @param retailPrice
     * @param memberPrice
     * @param vipPrice
     * @param brandId
     * @param categoryId
     */
    protected ProhibitSellItem(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec,
                               GradeEnum grade, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        setId(id);
        setBarcode(barcode);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
        setRetailPrice(retailPrice);
        setMemberPrice(memberPrice);
        setVipPrice(vipPrice);
        setCategoryId(categoryId);
        setBrandId(brandId);
    }

    protected ProhibitSellItem(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec,
                               GradeEnum grade, ShelfLife shelfLife, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        setId(id);
        setBarcode(barcode);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        setRetailPrice(retailPrice);
        setMemberPrice(memberPrice);
        setVipPrice(vipPrice);
        setCategoryId(categoryId);
        setBrandId(brandId);
    }

    private void setBarcode(Barcode barcode) {
        this.barcode = barcode;
    }

    private void setGrade(GradeEnum grade) {
        this.grade = grade;
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = Objects.requireNonNull(madeIn, "madeIn required");
    }

    private void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    private void setBrandId(long brandId) {
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

    private void setRetailPrice(RetailPrice retailPrice) {
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
    }

    private void setSpecification(Specification spec) {
        this.spec = spec;
    }

    private void setId(long id) {
        this.id = id;
    }

    private void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.SAME_DAY;
        this.shelfLife = shelfLife;
    }

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    /**
     * @param name
     */
    public void rename(Name name) {
        name = Objects.requireNonNull(name, "name required");
        if (!this.name.equals(name)) {
            this.name = name;
        }
    }

    /**
     * @param categoryId
     */
    public void moveToCategory(long categoryId) {
        if (this.categoryId != categoryId)
            setCategoryId(categoryId);
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
        return new Item(id, barcode, name, madeIn, spec, grade, LastReceiptPrice.RMB_ZERO, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProhibitSellItem)) return false;

        ProhibitSellItem that = (ProhibitSellItem) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProhibitSellItem.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("barcode=" + barcode)
                .add("brandId='" + brandId + "'")
                .add("categoryId='" + categoryId + "'")
                .add("grade=" + grade)
                .add("name=" + name)
                .add("madeIn=" + madeIn)
                .add("retailPrice=" + retailPrice)
                .add("memberPrice=" + memberPrice)
                .add("vipPrice=" + vipPrice)
                .add("spec=" + spec)
                .add("shelfLife=" + shelfLife)
                .toString();
    }
}
