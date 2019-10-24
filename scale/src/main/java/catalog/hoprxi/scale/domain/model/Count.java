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
import catalog.foxtail.core.domain.model.brand.Brand;
import catalog.foxtail.core.domain.model.category.Category;
import catalog.foxtail.core.domain.model.category.ValidatorCategoryId;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-05-04
 */

public class Count {
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
    private CountUnit unit;

    /**
     * @param id
     * @param plu
     * @param name
     * @param spec
     * @param unit
     * @param grade
     * @param placeOfProduction
     * @param shelfLife
     * @param categoryId
     * @param brandId
     */
    public Count(String id, Plu plu, Name name, Specification spec, CountUnit unit, Grade grade, PlaceOfProduction placeOfProduction, ShelfLife shelfLife, String categoryId, String brandId) {
        setId(id);
        setPlu(plu);
        setName(name);
        setSpecification(spec);
        setCountUnit(unit);
        setGrade(grade);
        setPlaceOfProduction(placeOfProduction);
        setShelfLife(shelfLife);
        setCategoryId(categoryId);
        setBrandId(brandId);
    }

    public Count(String id, Plu plu, Name name, CountUnit unit, PlaceOfProduction placeOfProduction) {
        this(id, plu, name, Specification.UNDEFINED, unit, Grade.QUALIFIED, placeOfProduction, ShelfLife.SAME_DAY, Brand.UNDEFINED.id(), Category.UNDEFINED.id());
    }

    public ShelfLife shelflife() {
        return shelfLife;
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

    public PlaceOfProduction placeOfProduction() {
        return placeOfProduction;
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

    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        this.id = id;
    }

    /**
     * @param name the name to set
     */
    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    /**
     * @param placeOfProduction
     */
    private void setPlaceOfProduction(PlaceOfProduction placeOfProduction) {
        this.placeOfProduction = Objects.requireNonNull(placeOfProduction, "madeIn required");
    }

    /**
     * @param unit the unit to set
     */
    private void setCountUnit(CountUnit unit) {
        if (unit == null)
            unit = CountUnit.PCS;
        this.unit = unit;
    }

    public Specification spec() {
        return spec;
    }

    public CountUnit unit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Count count = (Count) o;

        return id != null ? id.equals(count.id) : count.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Count.class.getSimpleName() + "[", "]")
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
