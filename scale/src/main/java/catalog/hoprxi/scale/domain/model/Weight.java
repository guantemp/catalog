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
package catalog.hoprxi.scale.domain.model;

import catalog.hoprxi.core.domain.Validator;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.ShelfLife;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.scale.domain.model.weight_price.WeightMemberPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightRetailPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightVipPrice;
import com.arangodb.velocypack.annotations.Expose;

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
                  WeightPrice retailPrice, WeightPrice memberPrice, WeightPrice vipPrice, String categoryId, String brandId) {
        setPlu(plu);
        setName(name);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        setBrandId(brandId);
        setCategoryId(categoryId);
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
        if (!categoryId.equals(Category.UNDEFINED.id()) && !Validator.isCategoryExist(categoryId))
            throw new IllegalArgumentException("categoryId isn't effective");
        this.categoryId = categoryId;
    }

    private void setBrandId(String brandId) {
        brandId = Objects.requireNonNull(brandId, "brandId required").trim();
        if (!brandId.equals(Brand.UNDEFINED.id()) && !Validator.isBrandExist(brandId))
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
        if (!this.categoryId.equals(categoryId) && Validator.isCategoryExist(categoryId)) {
            setCategoryId(categoryId);
            //catalog.hoprxi.core.domain.DomainRegistry.domainEventPublisher().publish(new SkuCategoryReallocated(id, categoryId));
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
            //DomainRegistry.domainEventPublisher().publish(new SkuBrandReallocated(id, brandId));
        }
    }


    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!name.equals(this.name))
            this.name = name;
    }

    public void changePlu(Plu plu) {
        Objects.requireNonNull(plu, "plu is required");
        if (!this.plu.equals(plu))
            this.plu = plu;
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
}
