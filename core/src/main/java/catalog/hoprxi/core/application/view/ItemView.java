/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.application.view;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;


/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-10-13
 */
public class ItemView {
    private Barcode barcode;
    private BrandView brandView;
    private CategoryView categoryView;
    private Grade grade;
    private String id;
    private Name name;
    private MadeIn madeIn;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private Specification spec;

    public ItemView(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade) {
        this.barcode = barcode;
        this.grade = grade;
        this.id = id;
        this.name = name;
        this.madeIn = madeIn;
        this.spec = spec;
    }

    public Barcode barcode() {
        return barcode;
    }

    public void setBarcode(Barcode barcode) {
        this.barcode = barcode;
    }

    public BrandView brandView() {
        return brandView;
    }

    public void setBrandView(BrandView brandView) {
        this.brandView = brandView;
    }

    public CategoryView categoryView() {
        return categoryView;
    }

    public void setCategoryView(CategoryView categoryView) {
        this.categoryView = categoryView;
    }

    public Grade grade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Name name() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public MadeIn madeIn() {
        return madeIn;
    }

    public void setMadeIn(MadeIn madeIn) {
        this.madeIn = madeIn;
    }

    public RetailPrice retailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(RetailPrice retailPrice) {
        this.retailPrice = retailPrice;
    }

    public MemberPrice memberPrice() {
        return memberPrice;
    }

    public void setMemberPrice(MemberPrice memberPrice) {
        this.memberPrice = memberPrice;
    }

    public VipPrice vipPrice() {
        return vipPrice;
    }

    public void setVipPrice(VipPrice vipPrice) {
        this.vipPrice = vipPrice;
    }

    public Specification spec() {
        return spec;
    }

    public void setSpec(Specification spec) {
        this.spec = spec;
    }

    public static class BrandView {
        private String id;
        private String name;

        public BrandView(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }
    }

    public static class CategoryView {
        private String id;
        private String name;

        public CategoryView(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }
    }
}
