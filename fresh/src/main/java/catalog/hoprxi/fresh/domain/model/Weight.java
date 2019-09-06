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
package catalog.hoprxi.fresh.domain.model;

import catalog.foxtail.core.domain.model.*;
import catalog.foxtail.core.domain.model.brand.Brand;
import catalog.foxtail.core.domain.model.category.Category;
import catalog.foxtail.core.domain.model.category.ValidatorCategoryId;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
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
    private PlaceOfProduction placeOfProduction;
    private Specification spec;
    private ShelfLife shelfLife;
    private WeightUnit unit;

    /**
     * @param id
     * @param plu               1-99999
     * @param name
     * @param spec
     * @param unit
     * @param grade
     * @param placeOfProduction
     * @param shelfLife         at least one day
     * @param brandId
     * @param categoryId
     */
    public Weight(String id, Plu plu, Name name, Specification spec, WeightUnit unit, Grade grade, PlaceOfProduction placeOfProduction, ShelfLife shelfLife, String brandId, String categoryId) {
        setId(id);
        setPlu(plu);
        setName(name);
        setSpecification(spec);
        setUnit(unit);
        setGrade(grade);
        setPlaceOfProduction(placeOfProduction);
        setShelfLife(shelfLife);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }

    public Weight(String id, Plu plu, Name name, WeightUnit unit, PlaceOfProduction placeOfProduction) {
        this(id, plu, name, Specification.UNDEFINED, unit, Grade.QUALIFIED, placeOfProduction, ShelfLife.SAME_DAY, Brand.UNDEFINED.id(), Category.UNDEFINED.id());
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

    public PlaceOfProduction placeOfProduction() {
        return placeOfProduction;
    }

    public void changePlaceOfProduction(PlaceOfProduction placeOfProduction) {

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

    private void setPlaceOfProduction(PlaceOfProduction placeOfProduction) {
        this.placeOfProduction = Objects.requireNonNull(placeOfProduction, "madeIn required");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Weight weight = (Weight) o;

        return id != null ? id.equals(weight.id) : weight.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Weight.class.getSimpleName() + "[", "]")
                .add("brandId='" + brandId + "'")
                .add("categoryId='" + categoryId + "'")
                .add("grade=" + grade)
                .add("id='" + id + "'")
                .add("plu=" + plu)
                .add("name=" + name)
                .add("placeOfProduction=" + placeOfProduction)
                .add("spec=" + spec)
                .add("shelfLife=" + shelfLife)
                .add("unit=" + unit)
                .toString();
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
