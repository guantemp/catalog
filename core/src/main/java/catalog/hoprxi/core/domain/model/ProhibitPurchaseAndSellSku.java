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

package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-07-13
 */
public class ProhibitPurchaseAndSellSku {
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

    protected ProhibitPurchaseAndSellSku(String id, EANUPCBarcode barcode, Name name, MadeIn madeIn, Unit unit, Specification spec,
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

    private void setShelfLife(ShelfLife shelfLife) {
        this.shelfLife = shelfLife;
    }

    public Specification spec() {
        return spec;
    }

    private void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    private void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    private void setSpecification(Specification spec) {
        this.spec = spec;
    }

    private void setId(String id) {
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

    private void setBarcode(EANUPCBarcode barcode) {
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

    public Unit unit() {
        return unit;
    }


    public ProhibitSellSku permitPurchase() {
        return new ProhibitSellSku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public ProhibitPurchaseSku permitSales() {
        return new ProhibitPurchaseSku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public Sku permitPurchaseAndSales() {
        return new Sku(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProhibitPurchaseAndSellSku that = (ProhibitPurchaseAndSellSku) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ProhibitPurchaseAndSalesSku{" +
                ", barcode=" + barcode +
                ", brandId='" + brandId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", grade=" + grade +
                ", id='" + id + '\'' +
                ", name=" + name +
                ", madeIn=" + madeIn +
                ", unit=" + unit +
                ", spec=" + spec +
                ", shelfLife=" + shelfLife +
                '}';
    }
}
