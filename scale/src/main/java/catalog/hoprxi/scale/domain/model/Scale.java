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

import catalog.hoprxi.core.domain.BrandValidatorService;
import catalog.hoprxi.core.domain.CategoryValidatorService;
import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.application.PluSegmentationService;
import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/4
 * @since JDK 21
 */

public abstract class Scale {
    protected long brandId;
    protected long categoryId;
    protected GradeEnum grade;
    protected Plu plu;
    protected Name name;
    protected Specification spec;
    protected ShelfLife shelfLife;
    protected MadeIn madeIn;

    protected Scale(Plu plu, Name name, Specification spec, GradeEnum grade, MadeIn madeIn,
                 ShelfLife shelfLife, long categoryId, long brandId) {
        setPlu(plu);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }

    protected void setMadeIn(MadeIn madeIn) {
        if (madeIn == null) {
            madeIn = MadeIn.UNKNOWN;
        }
        this.madeIn = madeIn;
    }

    protected void setPlu(Plu plu) {
        Objects.requireNonNull(plu, "plu required");
        if (!PluSegmentationService.isComplyWithSpec(plu))//not impl
            throw new IllegalArgumentException("plu is not comply with spec");
        this.plu = plu;
    }

    protected void setName(Name name) {
        if (name == null) {
            name = Name.EMPTY;
        }
        this.name = name;
    }

    protected void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.SAME_DAY;
        this.shelfLife = shelfLife;
    }

    protected void setSpecification(Specification spec) {
        if (spec == null)
            spec = Specification.UNDEFINED;
        this.spec = spec;
    }

    protected void setCategoryId(long categoryId) {
        if (Category.UNDEFINED.id() != categoryId && !CategoryValidatorService.isCategoryExist(categoryId))
            throw new IllegalArgumentException("categoryId isn't effective");
        this.categoryId = categoryId;
    }

    protected void setBrandId(long brandId) {
        if (Brand.UNDEFINED.id() != brandId && !BrandValidatorService.isBrandExist(brandId))
            throw new IllegalArgumentException("brandId isn't effective");
        this.brandId = brandId;
    }

    protected void setGrade(GradeEnum grade) {
        if (null == grade)
            grade = GradeEnum.QUALIFIED;
        this.grade = grade;
    }

    public long brandId() {
        return brandId;
    }

    public GradeEnum grade() {
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

    public long categoryId() {
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

    public void changGrade(GradeEnum grade) {

    }

    public void changeSpec(Specification spec) {
        Objects.requireNonNull(spec, "spec required");
        if (!this.spec.equals(spec)) {
            this.spec = spec;
            //DomainRegistry1.domainEventPublisher().publish(new WeightSpecificationChanged(id, spec));
        }
    }

    /**
     * @param categoryId of
     * @throws IllegalArgumentException if categoryId is <code>NULL</code>
     *                                  categoryId is not valid
     */
    public void moveToNewCategory(long categoryId) {
        if (categoryId != this.categoryId && CategoryValidatorService.isCategoryExist(categoryId)) {
            setCategoryId(categoryId);
            //catalog.hoprxi.core.util.DomainRegistry.domainEventPublisher().publish(new SkuCategoryReallocated(id, categoryId));
        }
    }

    /**
     * @throws IllegalArgumentException if brandId is <code>NULL</code>
     *                                  brandId is not valid
     */
    public void moveToNewBrand(long brandId) {
        if (this.brandId != brandId && BrandValidatorService.isBrandExist(brandId)) {
            setBrandId(brandId);
            //DomainRegistry.domainEventPublisher().publish(new SkuBrandReallocated(id, brandId));
        }
    }

    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!name.equals(this.name))
            this.name = name;
    }

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Scale scale)) return false;
        return Objects.equals(plu, scale.plu);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(plu);
    }

    @Override
    public String toString() {
        return "Scale{" +
                "brandId=" + brandId +
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
