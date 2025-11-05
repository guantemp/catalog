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
import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-10-26
 */

public class Count {
    //@Expose(serialize = false, deserialize = false)
    private String brandId;
    // @Expose(serialize = false, deserialize = false)
    private String categoryId;
    private GradeEnum grade;
    @Expose(serialize = false, deserialize = false)
    private Plu plu;
    private Name name;
    private MadeIn madeIn;
    private Specification spec;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private ShelfLife shelfLife;

    public Count(Plu plu, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, ShelfLife shelfLife,
                 RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, String categoryId, String brandId) {
        setPlu(plu);
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

    public void changeRetailPrice(RetailPrice retailPrice) {
        if (retailPrice == null)
            retailPrice = RetailPrice.RMB_ZERO;
        if (!this.retailPrice.equals(retailPrice))
            this.retailPrice = retailPrice;
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

    private void setGrade(GradeEnum grade) {
        if (null == grade)
            grade = GradeEnum.QUALIFIED;
        this.grade = grade;
    }

    private void setPlu(Plu plu) {
        this.plu = Objects.requireNonNull(plu, "plu required");
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = madeIn;
    }

    public Plu plu() {
        return plu;
    }

    public ShelfLife shelflife() {
        return shelfLife;
    }

    public String brandId() {
        return brandId;
    }

    public String categoryId() {
        return categoryId;
    }

    public GradeEnum grade() {
        return grade;
    }

    public Name name() {
        return name;
    }

    public MadeIn madeIn() {
        return madeIn;
    }

    public Specification spec() {
        return spec;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Count count = (Count) o;

        return plu != null ? plu.equals(count.plu) : count.plu == null;
    }

    @Override
    public int hashCode() {
        return plu != null ? plu.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Count.class.getSimpleName() + "[", "]")
                .add("brandId='" + brandId + "'")
                .add("categoryId='" + categoryId + "'")
                .add("grade=" + grade)
                .add("plu=" + plu)
                .add("name=" + name)
                .add("madeIn=" + madeIn)
                .add("spec=" + spec)
                .add("retailPrice=" + retailPrice)
                .add("memberPrice=" + memberPrice)
                .add("vipPrice=" + vipPrice)
                .add("shelfLife=" + shelfLife)
                .toString();
    }
}
