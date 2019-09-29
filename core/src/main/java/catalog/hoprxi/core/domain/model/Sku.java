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
package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.DomainRegistry;
import catalog.hoprxi.core.domain.Validator;
import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcode;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import javax.money.MonetaryAmount;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.0.2 builder 2019-05-02
 * @since JDK8.0
 */
public class Sku {
    @Expose(serialize = false, deserialize = false)
    private EANUPCBarcode barcode;
    private String brandId;
    private String categoryId;
    private Grade grade;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    private Name name;
    private MadeIn madeIn;
    private Unit unit;
    private Specification spec;
    private ShelfLife shelfLife;

    /**
     * @param id
     * @param barcode
     * @param name
     * @param madeIn
     * @param spec
     * @param unit
     * @param grade
     * @param brandId
     * @param categoryId
     * @throws IllegalArgumentException if id is null or id length range not in [1-255]
     *                                  if name is null
     *                                  if madeIn is null
     *                                  if unit is null
     */
    public Sku(String id, EANUPCBarcode barcode, Name name, MadeIn madeIn, Unit unit, Specification spec,
               Grade grade, ShelfLife shelfLife, String brandId, String categoryId) {
        setId(id);
        setBarcode(barcode);
        setName(name);
        setMadeIn(madeIn);
        setUnit(unit);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = madeIn;
    }

    /**
     * @param barcode
     */
    public void changeBarcode(EANUPCBarcode barcode) {
        Objects.requireNonNull(barcode, "barcode required");
        if (!barcode.equals(this.barcode)) {
            this.barcode = barcode;
            DomainRegistry.domainEventPublisher().publish(new SkuRenamed(id, name));
        }
    }

    /**
     * @param grade
     */
    public void changeGrade(Grade grade) {
        if (grade != null) {
            if (this.grade != grade)
                this.grade = grade;
        }
    }

    /**
     * @param newMadeIn
     * @throws IllegalArgumentException if newMadeIn is <CODE>NULL</CODE>
     */
    public void changeMadeIn(MadeIn newMadeIn) {
        Objects.requireNonNull(newMadeIn, "newMadeIn required");
        if (!newMadeIn.equals(this.madeIn)) {
            this.madeIn = newMadeIn;
        }
    }

    /**
     * @param unit
     */
    public void changeUnit(Unit unit) {
        if (unit != null) {
            if (!this.unit.equals(unit))
                this.unit = unit;
        }
    }

    /**
     * @param spec
     * @throws IllegalArgumentException if spec is <CODE>NULL</CODE>
     */
    public void changeSpecification(Specification spec) {
        if (spec == null)
            spec = Specification.UNDEFINED;
        if (!this.spec.equals(spec)) {
            this.spec = spec;
            DomainRegistry.domainEventPublisher().publish(new SkuSpecificationChanged(id, spec));
        }
    }

    /**
     * @param shelfLife
     */
    public void changeShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.NO_SHELF_LIFE;
        if (!this.shelfLife.equals(shelfLife)) {
            this.shelfLife = shelfLife;
            DomainRegistry.domainEventPublisher().publish(new SkuShelfLifeChanged(id, shelfLife));
        }
    }

    private void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.NO_SHELF_LIFE;
        this.shelfLife = shelfLife;
    }

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    public Specification spec() {
        return spec;
    }

    private void setCategoryId(String categoryId) {
        if (categoryId != null) {
            categoryId = categoryId.trim();
            if (!categoryId.equals(Category.UNDEFINED.id()) && Validator.isCategoryExist(categoryId)) {
                this.categoryId = categoryId;
                return;
            }
        }
        this.categoryId = Category.UNDEFINED.id();
    }

    private void setBrandId(String brandId) {
        if (brandId != null) {
            brandId = brandId.trim();
            if (!brandId.equals(Brand.UNDEFINED.id()) && Validator.isBrandExist(brandId)) {
                this.brandId = brandId;
                return;
            }
        }
        this.brandId = Brand.UNDEFINED.id();
    }

    private void setSpecification(Specification spec) {
        if (spec == null)
            spec = Specification.UNDEFINED;
        this.spec = spec;
    }

    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        if (id.isEmpty() || id.length() > 36)
            throw new IllegalArgumentException("id length range is [1-36]");
        this.id = id;
    }

    /**
     * @param name
     * @throws IllegalArgumentException if name is <Code>NULL</Code>
     */
    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!this.name.equals(name)) {
            this.name = name;
            DomainRegistry.domainEventPublisher().publish(new SkuRenamed(id, name));
        }
    }

    /**
     * @param categoryId
     * @throws IllegalArgumentException if categoryId is <code>NULL</code>
     *                                  categoryId is not valid
     */
    public void moveToNewCategory(String categoryId) {
        if (!this.categoryId.equals(categoryId) && Validator.isCategoryExist(categoryId)) {
            setCategoryId(categoryId);
            DomainRegistry.domainEventPublisher().publish(new SkuCategoryReallocated(id, categoryId));
        }
    }

    /**
     * @param brandId
     * @throws IllegalArgumentException if brandId is <code>NULL</code>
     *                                  brandId is not valid
     */
    public void moveToNewBrand(String brandId) {
        if (!this.brandId.equals(brandId) && Validator.isBrandExist(brandId)) {
            setBrandId(brandId);
            DomainRegistry.domainEventPublisher().publish(new SkuBrandReallocated(id, brandId));
        }
    }

    public EANUPCBarcode barcode() {
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

    private void setBarcode(EANUPCBarcode barcode) {
        this.barcode = Objects.requireNonNull(barcode, "barcode required");
    }

    private void setGrade(Grade grade) {
        if (null == grade)
            grade = Grade.QUALIFIED;
        this.grade = grade;
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setUnit(Unit unit) {
        this.unit = Objects.requireNonNull(unit, "unit required");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sku sku = (Sku) o;

        return id != null ? id.equals(sku.id) : sku.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Unit unit() {
        return unit;
    }

    public ProhibitPurchaseSku prohibitPurchase() {
        return new ProhibitPurchaseSku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public ProhibitSellSku prohibitSell() {
        return new ProhibitSellSku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public ProhibitPurchaseAndSellSku prohibitPurchaseAndSell() {
        return new ProhibitPurchaseAndSellSku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public Price price(String priceId,
                       String customerRoleId,
                       MonetaryAmount amount) {
        Price price = new Price(priceId, customerRoleId, amount);
        return price;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Sku.class.getSimpleName() + "[", "]")
                .add("barcode=" + barcode)
                .add("brandId='" + brandId + "'")
                .add("categoryId='" + categoryId + "'")
                .add("grade=" + grade)
                .add("id='" + id + "'")
                .add("name=" + name)
                .add("madeIn=" + madeIn)
                .add("unit=" + unit)
                .add("spec=" + spec)
                .add("shelfLife=" + shelfLife)
                .toString();
    }
}
