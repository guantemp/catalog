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

import catalog.hoprxi.core.domain.DomainRegistry;
import catalog.hoprxi.core.domain.Validator;
import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import com.arangodb.entity.DocumentField;
import com.arangodb.velocypack.annotations.Expose;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-06-19
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
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private MadeIn madeIn;
    private Specification spec;


    /**
     * @param id
     * @param barcode
     * @param name
     * @param madeIn
     * @param spec
     * @param grade
     * @param retailPrice
     * @param memberPrice
     * @param vipPrice
     * @param brandId
     * @param categoryId
     */
    protected ProhibitPurchaseSku(String id, EANUPCBarcode barcode, Name name, MadeIn madeIn, Specification spec,
                                  Grade grade, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, String brandId, String categoryId) {
        setId(id);
        setBarcode(barcode);
        setName(name);
        setMadeIn(madeIn);
        setSpecification(spec);
        setGrade(grade);
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
        this.retailPrice = retailPrice;
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

    private void setBarcode(EANUPCBarcode barcode) {
        this.barcode = barcode;
    }

    private void setGrade(Grade grade) {
        this.grade = grade;
    }

    private void setName(Name name) {
        this.name = name;
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = madeIn;
    }

    public void rename(Name name) {
        name = Objects.requireNonNull(name, "name required");
        if (!this.name.equals(name)) {
            this.name = name;
        }
    }

    /**
     * @param barcode
     */
    public void changeBarcode(EANUPCBarcode barcode) {
        Objects.requireNonNull(barcode, "barcode required");
        if (!barcode.equals(this.barcode)) {
            this.barcode = barcode;
            DomainRegistry.domainEventPublisher().publish(new SkuBarcodeChanged(id, barcode));
        }
    }

    /**
     * @param grade
     */
    public void changeGrade(Grade grade) {
        if (grade != null) {
            if (this.grade != grade)
                this.grade = grade;
        }
    }

    /**
     * @param newMadeIn
     * @throws IllegalArgumentException if newMadeIn is <CODE>NULL</CODE>
     */
    public void changeMadeIn(MadeIn newMadeIn) {
        Objects.requireNonNull(newMadeIn, "newMadeIn required");
        if (!newMadeIn.equals(this.madeIn)) {
            this.madeIn = newMadeIn;
            DomainRegistry.domainEventPublisher().publish(new SKuMadeInChanged(id, madeIn.code(), madeIn.madeIn()));
        }
    }

    /**
     * @param retailPrice
     */
    public void changeRetailPrice(RetailPrice retailPrice) {
        Objects.requireNonNull(retailPrice, "retailPrice required");
        if (this.retailPrice.equals(retailPrice)) {
            this.retailPrice = retailPrice;
            DomainRegistry.domainEventPublisher().publish(new SkuRetailPriceChanged(id, retailPrice.price().amount(), retailPrice.price().unit()));
        }
    }

    /**
     * @param spec
     * @throws IllegalArgumentException if spec is <CODE>NULL</CODE>
     */
    public void changeSpecification(Specification spec) {
        if (spec == null)
            spec = Specification.UNDEFINED;
        if (!this.spec.equals(spec)) {
            this.spec = spec;
            DomainRegistry.domainEventPublisher().publish(new SkuSpecificationChanged(id, spec));
        }
    }

    public void moveToCategory(String categoryId) {
        if (!this.categoryId.equals(categoryId) && Validator.isCategoryExist(categoryId)) {
            setCategoryId(categoryId);
        }
    }

    public void moveToNewBrand(String brandId) {
        if (!this.brandId.equals(brandId) && Validator.isBrandExist(brandId)) {
            setBrandId(brandId);
            DomainRegistry.domainEventPublisher().publish(new SkuBrandReallocated(id, brandId));
        }
    }

    public EANUPCBarcode barcode() {
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

    public MadeIn madeIn() {
        return madeIn;
    }

    public Specification spec() {
        return spec;
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
                "barcode=" + barcode +
                ", brandId='" + brandId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", grade=" + grade +
                ", id='" + id + '\'' +
                ", name=" + name +
                ", retailPrice=" + retailPrice +
                ", memberPrice=" + memberPrice +
                ", vipPrice=" + vipPrice +
                ", madeIn=" + madeIn +
                ", spec=" + spec +
                '}';
    }

    public Sku permitPurchase() {
        return new Sku(id, barcode, name, madeIn, spec, grade, retailPrice, memberPrice, vipPrice, brandId, categoryId);
    }

    public ProhibitPurchaseAndSellSku prohibitSales() {
        return new ProhibitPurchaseAndSellSku(id, barcode, name, madeIn, spec, grade, retailPrice, memberPrice, vipPrice, brandId, categoryId);
    }
}
