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
import catalog.hoprxi.core.domain.model.price.LastReceiptPrice;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.application.PluSegmentationService;
import catalog.hoprxi.scale.domain.model.price.WeightLastReceiptPrice;
import catalog.hoprxi.scale.domain.model.price.WeightMemberPrice;
import catalog.hoprxi.scale.domain.model.price.WeightRetailPrice;
import catalog.hoprxi.scale.domain.model.price.WeightVipPrice;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-10-26
 */

public class Count {
    private long brandId;
    private long categoryId;
    private GradeEnum grade;
    private Plu plu;
    private Name name;
    private Specification spec;
    public ShelfLife shelfLife;
    private LastReceiptPrice lastReceiptPrice;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private MadeIn madeIn;

    public Count(Plu plu, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, ShelfLife shelfLife, LastReceiptPrice lastReceiptPrice,
                  RetailPrice retailPrice, MemberPrice memberPrice,VipPrice vipPrice, long categoryId, long brandId) {
        setPlu(plu);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
        setShelfLife(shelfLife);
        this.lastReceiptPrice = Objects.requireNonNull(lastReceiptPrice,"lastReceiptPrice is null");
        setRetailPrice(retailPrice);
        setMemberPrice(memberPrice);
        setVipPrice(vipPrice);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }
    private void setVipPrice(VipPrice vipPrice) {
        this.vipPrice = vipPrice;
    }

    private void setMemberPrice(MemberPrice memberPrice) {
        this.memberPrice = memberPrice;
    }

    private void setRetailPrice(RetailPrice retailPrice) {
        this.retailPrice = Objects.requireNonNull(retailPrice, "retailPrice required");
    }

    private void setMadeIn(MadeIn madeIn) {
        if (madeIn == null) {
            madeIn = MadeIn.UNKNOWN;
        }
        this.madeIn = madeIn;
    }

    private void setPlu(Plu plu) {
        Objects.requireNonNull(plu, "plu required");
        if (!PluSegmentationService.isComplyWithSpec(plu))
            throw new IllegalArgumentException("plu is not comply with spec");
        this.plu = plu;
    }

    private void setName(Name name) {
        if (name == null) {
            name = Name.EMPTY;
        }
        this.name = name;
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

    private void setCategoryId(long categoryId) {
        if (Category.UNDEFINED.id() != categoryId && !CategoryValidatorService.isCategoryExist(categoryId))
            throw new IllegalArgumentException("categoryId isn't effective");
        this.categoryId = categoryId;
    }

    private void setBrandId(long brandId) {
        if (Brand.UNDEFINED.id() != brandId && !BrandValidatorService.isBrandExist(brandId))
            throw new IllegalArgumentException("brandId isn't effective");
        this.brandId = brandId;
    }

    private void setGrade(GradeEnum grade) {
        if (null == grade)
            grade = GradeEnum.QUALIFIED;
        this.grade = grade;
    }

    public MemberPrice memberPrice() {
        return memberPrice;
    }

    public VipPrice vipPrice() {
        return vipPrice;
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

}
