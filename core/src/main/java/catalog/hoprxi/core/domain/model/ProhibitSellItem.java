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

import catalog.hoprxi.core.domain.model.barcode.Barcode;
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
public class ProhibitSellItem {
    @Expose(serialize = false, deserialize = false)
    private Barcode barcode;
    private String brandId;
    private String categoryId;
    private Grade grade;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    private Name name;
    private MadeIn madeIn;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
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
    protected ProhibitSellItem(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec,
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
        setCategoryId(categoryId);
        setBrandId(brandId);
    }

    private void setBarcode(Barcode barcode) {
        this.barcode = barcode;
    }

    private void setGrade(Grade grade) {
        this.grade = grade;
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setMadeIn(MadeIn madeIn) {
        this.madeIn = Objects.requireNonNull(madeIn, "madeIn required");
    }

    private void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    private void setBrandId(String brandId) {
        this.brandId = Objects.requireNonNull(brandId, "brand id required").trim();
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

    public Barcode barcode() {
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

    public Specification spec() {
        return spec;
    }

    public MadeIn madeIn() {
        return madeIn;
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

    public Item permitSell() {
        return new Item(id, barcode, name, madeIn, spec, grade, retailPrice, memberPrice, vipPrice, brandId, categoryId);
    }

    public ProhibitPurchaseAndSellItem prohibitPurchase() {
        return new ProhibitPurchaseAndSellItem(id, barcode, name, madeIn, spec, grade, retailPrice, memberPrice, vipPrice, brandId, categoryId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProhibitSellItem that = (ProhibitSellItem) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ProhibitSellSku{" +
                "barcode=" + barcode +
                ", brandId='" + brandId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", grade=" + grade +
                ", id='" + id + '\'' +
                ", name=" + name +
                ", madeIn=" + madeIn +
                ", retailPrice=" + retailPrice +
                ", memberPrice=" + memberPrice +
                ", vipPrice=" + vipPrice +
                ", spec=" + spec +
                '}';
    }
}
