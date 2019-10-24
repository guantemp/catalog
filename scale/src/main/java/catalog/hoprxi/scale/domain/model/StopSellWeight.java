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
import catalog.foxtail.core.domain.model.category.Category;
import catalog.foxtail.core.domain.model.category.ValidatorCategoryId;
import com.arangodb.entity.DocumentField;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @version 0.0.1 builder 2018-06-20
 * @since JDK8.0
 */
public class StopSellWeight {
    private static final Pattern ID_PATTERN = Pattern.compile("[1-9]{1}\\d{0,4}");
    private String alias;
    private String brandId;
    private String categoryId;
    private Grade grade;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    private Name name;
    private PlaceOfProduction placeOfProduction;
    private Specification spec;
    private ShelfLife shelfLife;
    private WeightUnit unit;

    public StopSellWeight(String id, Name name, String alias, Specification spec, WeightUnit unit, Grade grade, PlaceOfProduction placeOfProduction,
                          ShelfLife shelfLife, String brandId, String categoryId) {
        super();
        setId(id);
        setName(name);
        setAlias(alias);
        setSpecification(spec);
        setWeightUnit(unit);
        setGrade(grade);
        setPlaceOfProduction(placeOfProduction);
        setShelfLife(shelfLife);
        setBrandId(brandId);
        setCategoryId(categoryId);
    }

    public ShelfLife shelflife() {
        return shelfLife;
    }

    protected void setShelfLife(ShelfLife shelfLife) {
        if (shelfLife == null)
            shelfLife = ShelfLife.SAME_DAY;
        this.shelfLife = shelfLife;
    }

    protected void setSpecification(Specification spec) {
        this.spec = Objects.requireNonNull(spec, "spec required");
    }

    public String alias() {
        return alias;
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

    public PlaceOfProduction origin() {
        return placeOfProduction;
    }

    protected void setAlias(String alias) {
        if (alias == null)
            alias = name.name();
        this.alias = alias;
    }

    protected void setBrandId(String brandId) {
        this.brandId = Objects.requireNonNull(brandId, "brand id required").trim();
    }

    protected void setCategoryId(String categoryId) {
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
    protected void setGrade(Grade grade) {
        if (null == grade)
            grade = Grade.QUALIFIED;
        this.grade = grade;
    }

    /**
     * @param id range is [1-99999]
     */
    protected void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        Matcher matcher = ID_PATTERN.matcher(id);
        if (!matcher.matches())
            throw new IllegalArgumentException("id range is [1-99999]");
        this.id = id;
    }

    /**
     * @param name the name to set
     */
    protected void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    /**
     * @param placeOfProduction
     */
    protected void setPlaceOfProduction(PlaceOfProduction placeOfProduction) {
        this.placeOfProduction = Objects.requireNonNull(placeOfProduction, "madeIn required");
    }

    /**
     * @param unit the unit to set
     */
    protected void setWeightUnit(WeightUnit unit) {
        if (unit == null)
            unit = WeightUnit.KILOGRAM;
        this.unit = unit;
    }

    /**
     * @return the specification
     */
    public Specification spec() {
        return spec;
    }

    public WeightUnit unit() {
        return unit;
    }
}
