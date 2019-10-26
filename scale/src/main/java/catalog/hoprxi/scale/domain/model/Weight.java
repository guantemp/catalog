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

import catalog.foxtail.core.domain.model.*;
import catalog.foxtail.core.domain.model.category.ValidatorCategoryId;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.ShelfLife;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.0.1 builder 2019-05-02
 * @since JDK8.0
 */
public class Weight {
    //@Expose(serialize = false, deserialize = false)
    private String brandId;
    // @Expose(serialize = false, deserialize = false)
    private String categoryId;
    private Grade grade;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    @Expose(serialize = false, deserialize = false)
    private Plu plu;
    private Name name;
    private Specification spec;
    private ShelfLife shelfLife;
    private WeightUnit unit;
    private MadeIn madeIn;

    /**
     * @param id
     * @param plu
     * @param name
     * @param spec
     * @param unit
     * @param grade
     * @param shelfLife
     * @param brandId
     * @param categoryId
     */
    public Weight(String id, Plu plu, Name name, Specification spec, WeightUnit unit, Grade grade, ShelfLife shelfLife, String brandId, String categoryId) {
        setId(id);
        setPlu(plu);
        setName(name);
        setSpecification(spec);
        setUnit(unit);
        setGrade(grade);

        setShelfLife(shelfLife);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }

    public Weight(String id, Plu plu, Name name, WeightUnit unit) {
        this(id, plu, name, Specification.UNDEFINED, unit, Grade.QUALIFIED, ShelfLife.SAME_DAY, Brand.UNDEFINED.id(), Category.UNDEFINED.id());
    }

    public ShelfLife shelLife() {
        return shelfLife;
    }

    public void changeShelLife(ShelfLife shelfLife) {
        Objects.requireNonNull(shelfLife, "shelLife required");
        if (!this.shelfLife.equals(shelfLife)) {
            this.shelfLife = shelfLife;
        }
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

    public void changeSpec(Specification spec) {
        Objects.requireNonNull(spec, "spec required");
        if (!this.spec.equals(spec)) {
            this.spec = spec;
            DomainRegistry1.domainEventPublisher().publish(new WeightSpecificationChanged(id, spec));
        }
    }

    public String brandId() {
        return brandId;
    }

    public void reallocateBrand(String brandId) {

    }

    public String categoryId() {
        return categoryId;
    }

    public void reallocateCategory(String categoryId) {

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

    private void setBrandId(String brandId) {
        this.brandId = Objects.requireNonNull(brandId, "brand id required").trim();
    }

    private void setCategoryId(String categoryId) {
        if (categoryId != null) {
            categoryId = categoryId.trim();
            ValidatorCategoryId vcis = new ValidatorCategoryId();
            if (vcis.isIdExist(categoryId)) {
                this.categoryId = categoryId;
                return;
            }
        }
        this.categoryId = Category.UNDEFINED.id();
    }

    /**
     * @param grade the grade to set
     */
    private void setGrade(Grade grade) {
        if (null == grade)
            grade = Grade.QUALIFIED;
        this.grade = grade;
    }

    public void changGrade(Grade grade) {

    }

    /**
     * @param id
     */
    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        this.id = id;
    }

    public void rename(Name name) {
        Objects.requireNonNull(name, "name required");
        if (!name.equals(this.name))
            this.name = name;
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setUnit(WeightUnit unit) {
        if (unit == null)
            unit = WeightUnit.KILOGRAM;
        this.unit = unit;
    }

    public void changeUnit(WeightUnit unit) {

    }

    public Specification spec() {
        return spec;
    }

    public WeightUnit unit() {
        return unit;
    }


    public Plu plu() {
        return plu;
    }

    public void changePlu(Plu plu) {
        Objects.requireNonNull(plu, "plu is required");
        if (!this.plu.equals(plu))
            this.plu = plu;
    }

    private void setPlu(Plu plu) {
        this.plu = Objects.requireNonNull(plu, "plu required");
    }
}
