/*
 * Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

import catalog.hoprxi.core.domain.Validator;
import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcode;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-06-19
 */
public class ProhibitPurchaseSku {
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
    protected ProhibitPurchaseSku(String id, EANUPCBarcode barcode, Name name, MadeIn madeIn, Unit unit, Specification spec,
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

    protected void setShelfLife(ShelfLife shelfLife) {
        if (null == shelfLife)
            shelfLife = ShelfLife.NO_SHELF_LIFE;
        this.shelfLife = shelfLife;
    }

    public Specification spec() {
        return spec;
    }

    protected void setCategoryId(String categoryId) {
        if (categoryId != null) {
            categoryId = categoryId.trim();
            if (Validator.isCategoryIdExist(categoryId)) {
                this.categoryId = categoryId;
                return;
            }
        }
        this.categoryId = Category.UNDEFINED.id();
    }

    protected void setBrandId(String brandId) {
        this.brandId = Objects.requireNonNull(brandId, "brand id required").trim();
    }

    protected void setSpecification(Specification spec) {
        this.spec = spec;
    }

    protected void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        if (id.isEmpty() || id.length() > 255)
            throw new IllegalArgumentException("id length range is [1-255]");
        this.id = id;
    }

    /**
     * @param name
     */
    public void rename(Name name) {
        name = Objects.requireNonNull(name, "name required");
        if (!this.name.equals(name)) {
            this.name = name;
        }
    }

    /**
     * @param categoryId
     */
    public void moveToCategory(String categoryId) {
        if (!this.categoryId.equals(categoryId))
            setCategoryId(categoryId);
    }

    public EANUPCBarcode barcodeBook() {
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

    MadeIn origin() {
        return madeIn;
    }

    protected void setBarcode(EANUPCBarcode barcode) {
        this.barcode = Objects.requireNonNull(barcode, "barcode required");
    }

    protected void setGrade(Grade grade) {
        if (null == grade)
            grade = Grade.QUALIFIED;
        this.grade = grade;
    }

    protected void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    protected void setMadeIn(MadeIn madeIn) {
        this.madeIn = Objects.requireNonNull(madeIn, "madeIn required");
    }

    protected void setUnit(Unit unit) {
        if (unit == null)
            unit = Unit.PCS;
        this.unit = unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProhibitPurchaseSku that = (ProhibitPurchaseSku) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ProhibitPurchaseSku{" +
                ", barcode=" + barcode +
                ", brandId='" + brandId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", grade=" + grade +
                ", id='" + id + '\'' +
                ", name=" + name +
                ", madeIn=" + madeIn +
                ", unit=" + unit +
                ", spec=" + spec +
                '}';
    }

    public Unit unit() {
        return unit;
    }

    public Sku permitPurchase() {
        return new Sku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public ProhibitPurchaseAndSellSku prohibitSales() {
        return new ProhibitPurchaseAndSellSku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }
}
