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
package catalog.hoprxi.scale.domain.model;

import catalog.hoprxi.core.domain.BrandValidatorService;
import catalog.hoprxi.core.domain.CategoryValidatorService;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.domain.model.weight_price.WeightMemberPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightRetailPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightVipPrice;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Locale;
import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.0.2 builder 2019-10-29
 * @since JDK8.0
 */
public class Weight {
    //@Expose(serialize = false, deserialize = false)
    private String brandId;
    // @Expose(serialize = false, deserialize = false)
    private String categoryId;
    private Grade grade;
    @Expose(serialize = false, deserialize = false)
    private Plu plu;
    private Name name;
    private Specification spec;
    private ShelfLife shelfLife;
    private WeightRetailPrice retailPrice;
    private WeightMemberPrice memberPrice;
    private WeightVipPrice vipPrice;
    private MadeIn madeIn;

    public Weight(Plu plu, Name name, MadeIn madeIn, Specification spec, Grade grade, ShelfLife shelfLife,
                  WeightRetailPrice retailPrice, WeightMemberPrice memberPrice, WeightVipPrice vipPrice, String categoryId, String brandId) {
        setPlu(plu);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        setRetailPrice(retailPrice);
        setMemberPrice(memberPrice);
        setVipPrice(vipPrice);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }

    private void setVipPrice(WeightVipPrice vipPrice) {
        this.vipPrice = vipPrice;
    }

    private void setMemberPrice(WeightMemberPrice memberPrice) {
        this.memberPrice = memberPrice;
    }

    private void setRetailPrice(WeightRetailPrice retailPrice) {
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = madeIn;
    }

    private void setPlu(Plu plu) {
        this.plu = Objects.requireNonNull(plu, "plu required");
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.SAME_DAY;
        this.shelfLife = shelfLife;
    }

    private void setSpecification(Specification spec) {
        if (spec == null)
            spec = Specification.UNDEFINED;
        this.spec = spec;
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

    private void setGrade(Grade grade) {
        if (null == grade)
            grade = Grade.QUALIFIED;
        this.grade = grade;
    }

    public WeightRetailPrice retailPrice() {
        return retailPrice;
    }

    public void changeRetailPrice(WeightRetailPrice retailPrice) {
        if (retailPrice == null)
            retailPrice = WeightRetailPrice.zero(Locale.getDefault());
        if (!this.retailPrice.equals(retailPrice))
            this.retailPrice = retailPrice;
    }

    public WeightMemberPrice memberPrice() {
        return memberPrice;
    }

    public WeightVipPrice vipPrice() {
        return vipPrice;
    }

    public ShelfLife shelLife() {
        return shelfLife;
    }

    public String brandId() {
        return brandId;
    }

    public Grade grade() {
        return grade;
    }

    public Name name() {
        return name;
    }

    public Specification spec() {
        return spec;
    }

    public Plu plu() {
        return plu;
    }

    public String categoryId() {
        return categoryId;
    }

    public MadeIn madeIn() {
        return madeIn;
    }

    public void changeShelLife(ShelfLife shelfLife) {
        Objects.requireNonNull(shelfLife, "shelLife required");
        if (!this.shelfLife.equals(shelfLife)) {
            this.shelfLife = shelfLife;
        }
    }

    public void changGrade(Grade grade) {

    }

    public void changeSpec(Specification spec) {
        Objects.requireNonNull(spec, "spec required");
        if (!this.spec.equals(spec)) {
            this.spec = spec;
            //DomainRegistry1.domainEventPublisher().publish(new WeightSpecificationChanged(id, spec));
        }
    }

    /**
     * @param categoryId
     * @throws IllegalArgumentException if categoryId is <code>NULL</code>
     *                                  categoryId is not valid
     */
    public void moveToNewCategory(String categoryId) {
        if (!this.categoryId.equals(categoryId) && CategoryValidatorService.isCategoryExist(categoryId)) {
            setCategoryId(categoryId);
            //catalog.hoprxi.core.util.DomainRegistry.domainEventPublisher().publish(new SkuCategoryReallocated(id, categoryId));
        }
    }

    /**
     * @param brandId
     * @throws IllegalArgumentException if brandId is <code>NULL</code>
     *                                  brandId is not valid
     */
    public void moveToNewBrand(String brandId) {
        if (!this.brandId.equals(brandId) && BrandValidatorService.isBrandExist(brandId)) {
            setBrandId(brandId);
            //DomainRegistry.domainEventPublisher().publish(new SkuBrandReallocated(id, brandId));
        }
    }

    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!name.equals(this.name))
            this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Weight weight = (Weight) o;

        return plu != null ? plu.equals(weight.plu) : weight.plu == null;
    }

    @Override
    public int hashCode() {
        return plu != null ? plu.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Weight{" +
                "brandId='" + brandId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", grade=" + grade +
                ", plu=" + plu +
                ", name=" + name +
                ", spec=" + spec +
                ", shelfLife=" + shelfLife +
                ", retailPrice=" + retailPrice +
                ", memberPrice=" + memberPrice +
                ", vipPrice=" + vipPrice +
                ", madeIn=" + madeIn +
                '}';
    }
}
