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
package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.BrandValidatorService;
import catalog.hoprxi.core.domain.CategoryValidatorService;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.util.DomainRegistry;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.0.2 builder 2019-10-23
 * @since JDK8.0
 */
public final class Item {
    private static final int ID_MAX_LENGTH = 48;
    @Expose(serialize = false, deserialize = false)
    private Barcode barcode;
    private String brandId;
    private String categoryId;
    private Grade grade;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
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
     * @param shelfLife
     * @param retailPrice
     * @param memberPrice
     * @param vipPrice
     * @param categoryId id of category
     * @param brandId     id of brand
     * @throws IllegalArgumentException if id is null or id length range not in [1-36]
     *                                  if name is null
     *                                  if madeIn is null
     */
    public Item(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade, ShelfLife shelfLife, RetailPrice retailPrice,
                MemberPrice memberPrice, VipPrice vipPrice, String categoryId, String brandId) {
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

    public Item(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, String categoryId, String brandId) {
        this(id, barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    public Item(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice) {
        this(id, barcode, name, madeIn, spec, grade, retailPrice, memberPrice, vipPrice, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
    }

    public Item(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade, RetailPrice retailPrice) {
        this(id, barcode, name, madeIn, spec, grade, retailPrice, MemberPrice.RMB_ZERO, VipPrice.RMB_ZERO, Category.UNDEFINED.id(), Brand.UNDEFINED.id());
    }

    private void setCategoryId(String categoryId) {
        categoryId = Objects.requireNonNull(categoryId, "categoryId required").trim();
        if (!categoryId.equals(Category.UNDEFINED.id()) && !CategoryValidatorService.isCategoryExist(categoryId))
            throw new IllegalArgumentException("categoryId isn't effective");
        this.categoryId = categoryId;
    }

    private void setBrandId(String brandId) {
        brandId = Objects.requireNonNull(brandId, "brandId required").trim();
        if (!brandId.equals(Brand.UNDEFINED.id()) && !BrandValidatorService.isBrandExist(brandId))
            throw new IllegalArgumentException("brandId isn't effective");
        this.brandId = brandId;
    }

    private void setSpecification(Specification spec) {
        if (spec == null) spec = Specification.UNDEFINED;
        this.spec = spec;
    }

    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        if (id.isEmpty() || id.length() > ID_MAX_LENGTH)
            throw new IllegalArgumentException("id length range is 1 to " + ID_MAX_LENGTH);
        this.id = id;
    }


    private void setVipPrice(VipPrice vipPrice) {
        Objects.requireNonNull(vipPrice, "vipPrice required");
        if (vipPrice.price().unit() != Unit.PCS && vipPrice.price().unit() != retailPrice.price().unit())
            throw new IllegalArgumentException("vipPrice unit must be consistent with retailPrice unit");
        this.vipPrice = vipPrice;
    }

    private void setMemberPrice(MemberPrice memberPrice) {
        Objects.requireNonNull(memberPrice, "memberPrice required");
        if (memberPrice.price().unit() != Unit.PCS && memberPrice.price().unit() != retailPrice.price().unit())
            throw new IllegalArgumentException("memberPrice unit must be consistent with retailPrice unit");
        this.memberPrice = memberPrice;
    }

    private void setRetailPrice(RetailPrice retailPrice) {
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = madeIn;
    }

    private void setBarcode(Barcode barcode) {
        this.barcode = Objects.requireNonNull(barcode, "barcode required");
    }

    private void setGrade(Grade grade) {
        if (null == grade) grade = Grade.QUALIFIED;
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
    public void changeGrade(Grade grade) {
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
     * @throws IllegalArgumentException if categoryId is <code>NULL</code>
     *                                  categoryId is not valid
     */
    public void moveToNewCategory(String categoryId) {
        categoryId = Objects.requireNonNull(categoryId, "categoryId required").trim();
        if (!this.categoryId.equals(categoryId)) {
            setCategoryId(categoryId);
            DomainRegistry.domainEventPublisher().publish(new ItemCategoryReallocated(id, categoryId));
        }
    }

    /**
     * @param brandId
     * @throws IllegalArgumentException if brandId is <code>NULL</code>
     *                                  brandId is not valid
     */
    public void moveToNewBrand(String brandId) {
        brandId = Objects.requireNonNull(brandId, "brandId required").trim();
        if (!this.brandId.equals(brandId)) {
            setBrandId(brandId);
            DomainRegistry.domainEventPublisher().publish(new ItemBrandReallocated(id, brandId));
        }
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

    public String brandId() {
        return brandId;
    }

    public String categoryId() {
        return categoryId;
    }

    public Grade grade() {
        return grade;
    }

    public String id() {
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
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public ProhibitSellItem prohibitSell() {
        return new ProhibitSellItem(id, barcode, name, madeIn, spec, grade, shelfLife, retailPrice, memberPrice, vipPrice, brandId, categoryId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Item.class.getSimpleName() + "[", "]")
                .add("barcode=" + barcode)
                .add("brandId='" + brandId + "'")
                .add("categoryId='" + categoryId + "'")
                .add("grade=" + grade)
                .add("id='" + id + "'")
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
