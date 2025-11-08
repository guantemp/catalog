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

import catalog.hoprxi.core.domain.BrandValidatorService;
import catalog.hoprxi.core.domain.CategoryValidatorService;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.util.DomainRegistry;

import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @version 0.0.4 builder 2025-11-08
 * @since JDK21
 */
public class Item {
    private Barcode barcode;
    private long brandId;
    private long categoryId;
    private GradeEnum grade;
    private final long id;
    private Name name;
    private MadeIn madeIn;
    private LastReceiptPrice lastReceiptPrice;
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
     * @param shelfLife
     * @param lastReceiptPrice
     * @param retailPrice
     * @param memberPrice
     * @param vipPrice
     * @param categoryId       id valueOf category
     * @param brandId          id valueOf brand
     * @throws IllegalArgumentException if id is null or id length range not in [1-36]
     *                                  if name is null
     *                                  if madeIn is null
     */
    public Item(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, ShelfLife shelfLife, LastReceiptPrice lastReceiptPrice, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        this.id = id;
        setBarcode(barcode);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        setLastReceiptPrice(lastReceiptPrice);
        setRetailPrice(retailPrice);
        setMemberPrice(memberPrice);
        setVipPrice(vipPrice);
        setCategoryId(categoryId);
        setBrandId(brandId);
    }

    public Item(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, LastReceiptPrice lastReceiptPrice, RetailPrice retailPrice,
                MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        this(id, barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    public Item(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, LastReceiptPrice lastReceiptPrice, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice) {
        this(id, barcode, name, madeIn, spec, grade, lastReceiptPrice, retailPrice, memberPrice, vipPrice, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
    }

    public Item(long id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, RetailPrice retailPrice) {
        this(id, barcode, name, madeIn, spec, grade, LastReceiptPrice.RMB_ZERO, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
    }

    private void setCategoryId(long categoryId) {
        if (categoryId != Category.UNDEFINED.id() && CategoryValidatorService.isCategoryExist(categoryId))
            throw new IllegalArgumentException("categoryId isn't effective");
        this.categoryId = categoryId;
    }

    private void setBrandId(long brandId) {
        if (!BrandValidatorService.isBrandExist(brandId))
            throw new IllegalArgumentException("brandId isn't effective");
        this.brandId = brandId;
    }

    private void setSpecification(Specification spec) {
        if (spec == null) spec = Specification.UNDEFINED;
        this.spec = spec;
    }

    private void setLastReceiptPrice(LastReceiptPrice lastReceiptPrice) {
        if (lastReceiptPrice == null)
            lastReceiptPrice = LastReceiptPrice.RMB_ZERO;
        this.lastReceiptPrice = lastReceiptPrice;
    }

    private void setRetailPrice(RetailPrice retailPrice) {
        if (retailPrice == null) retailPrice = RetailPrice.zero(Locale.getDefault(), UnitEnum.PCS);
        this.retailPrice = retailPrice;
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

    private void setMadeIn(MadeIn madeIn) {
        if (madeIn == null)
            madeIn = MadeIn.UNKNOWN;
        this.madeIn = madeIn;
    }

    private void setBarcode(Barcode barcode) {
        this.barcode = Objects.requireNonNull(barcode, "barcode required");
    }

    private void setGrade(GradeEnum grade) {
        if (null == grade) grade = GradeEnum.QUALIFIED;
        this.grade = grade;
    }

    private void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null) shelfLife = ShelfLife.SAME_DAY;
        this.shelfLife = shelfLife;
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    /**
     * @param barcode
     */
    public void changeBarcode(Barcode barcode) {
        if (!this.barcode.equals(barcode)) {
            setBarcode(barcode);
            DomainRegistry.domainEventPublisher().publish(new ItemBarcodeChanged(id, barcode));
        }
    }

    /**
     * @param grade
     */
    public void changeGrade(GradeEnum grade) {
        if (this.grade != grade) {
            setGrade(grade);
            DomainRegistry.domainEventPublisher().publish(new ItemGradeChanged(id, grade));
        }
    }

    /**
     * @param madeIn
     * @throws IllegalArgumentException if newMadeIn is <CODE>NULL</CODE>
     */
    public void changeMadeIn(MadeIn madeIn) {
        Objects.requireNonNull(madeIn, "newMadeIn required");
        if (!this.madeIn.equals(madeIn)) {
            this.madeIn = madeIn;
            DomainRegistry.domainEventPublisher().publish(new ItemMadeInChanged(id, madeIn.code(), madeIn.madeIn()));
        }
    }

    /**
     * @param retailPrice
     */
    public void changeRetailPrice(RetailPrice retailPrice) {
        Objects.requireNonNull(retailPrice, "retailPrice required");
        if (!this.retailPrice.equals(retailPrice)) {
            this.retailPrice = retailPrice;
            DomainRegistry.domainEventPublisher().publish(new ItemRetailPriceChanged(id, retailPrice.price().amount(), retailPrice.price().unit()));
        }
    }

    public void changeMemberPrice(MemberPrice memberPrice) {
        Objects.requireNonNull(memberPrice, "memberPrice required");
        if (!this.memberPrice.equals(memberPrice)) {
            setMemberPrice(memberPrice);
            DomainRegistry.domainEventPublisher().publish(new ItemMemberPriceChaned(id, memberPrice.name(), memberPrice.price().amount(), memberPrice.price().unit()));
        }
    }

    public void changeVipPrice(VipPrice vipPrice) {
        Objects.requireNonNull(vipPrice, "vipPrice required");
        if (!this.vipPrice.equals(vipPrice)) {
            setVipPrice(vipPrice);
            DomainRegistry.domainEventPublisher().publish(new ItemVipPriceChaned(id, vipPrice.name(), vipPrice.price().amount(), vipPrice.price().unit()));
        }
    }

    /**
     * @param spec
     * @throws IllegalArgumentException if spec is <CODE>NULL</CODE>
     */
    public void changeSpecification(Specification spec) {
        if (!this.spec.equals(spec)) {
            setSpecification(spec);
            DomainRegistry.domainEventPublisher().publish(new ItemSpecificationChanged(id, spec));
        }
    }

    /**
     * @param name
     * @throws IllegalArgumentException if name is <Code>NULL</Code>
     */
    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!this.name.equals(name)) {
            this.name = name;
            DomainRegistry.domainEventPublisher().publish(new ItemRenamed(id, name.name(), name.mnemonic(), name.alias()));
        }
    }

    /**
     * @param categoryId
     * @throws IllegalArgumentException   if categoryId is <code>NULL</code>
     * @throws InvalidCategoryIdException categoryId is not valid
     */
    public void moveToNewCategory(long categoryId) {
        if (this.categoryId != categoryId) {
            setCategoryId(categoryId);
            //DomainRegistry.domainEventPublisher().publish(new ItemCategoryReallocated(id, categoryId));
        }
    }

    /**
     * @param brandId
     * @throws IllegalArgumentException if brandId is <code>NULL</code>
     *                                  brandId is not valid
     */
    public void moveToNewBrand(long brandId) {
        if (this.brandId != brandId) {
            setBrandId(brandId);
            DomainRegistry.domainEventPublisher().publish(new ItemBrandReallocated(id, brandId));
        }
    }

    public LastReceiptPrice lastReceiptPrice() {
        return lastReceiptPrice;
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

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    public MadeIn madeIn() {
        return madeIn;
    }

    public Specification spec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        return id == item.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    public ProhibitSellItem toProhibitSell() {
        return new ProhibitSellItem(id, barcode, name, madeIn, spec, grade, shelfLife, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Item.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("barcode=" + barcode)
                .add("brandId='" + brandId + "'")
                .add("categoryId='" + categoryId + "'")
                .add("grade=" + grade)
                .add("name=" + name)
                .add("madeIn=" + madeIn)
                .add("lastReceiptPrice=" + lastReceiptPrice)
                .add("retailPrice=" + retailPrice)
                .add("memberPrice=" + memberPrice)
                .add("vipPrice=" + vipPrice)
                .add("spec=" + spec)
                .add("shelfLife=" + shelfLife)
                .toString();
    }
}
