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

package catalog.hoprxi.core.application.view;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.LastReceiptPrice;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;


/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-10-13
 */
public class ItemView {
    private final Barcode barcode;
    private final Grade grade;
    private final String id;
    private final Name name;
    private final MadeIn madeIn;
    private final Specification spec;
    private final ShelfLife shelfLife;
    private BrandView brandView;
    private CategoryView categoryView;


    private LastReceiptPrice lastReceiptPrice;
    private RetailPrice retailPrice;
    private MemberPrice memberPrice;
    private VipPrice vipPrice;
    private URI video;

    public void setImages(URI[] images) {
        this.images = images;
    }

    private URI[] images;


    public ItemView(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade) {
        this(id, barcode, name, madeIn, spec, grade, ShelfLife.SAME_DAY);
    }

    public ItemView(String id, Barcode barcode, Name name, MadeIn madeIn, Specification spec, Grade grade, ShelfLife shelfLife) {
        this.barcode = barcode;
        this.grade = grade;
        this.id = id;
        this.name = name;
        this.madeIn = madeIn;
        this.spec = spec;
        this.shelfLife = shelfLife;
    }

    public ItemView(Barcode barcode, Grade grade, String id, Name name, MadeIn madeIn, Specification spec, ShelfLife shelfLife, BrandView brandView, CategoryView categoryView, LastReceiptPrice lastReceiptPrice, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, URI video, URI[] images) {
        this.barcode = barcode;
        this.grade = grade;
        this.id = id;
        this.name = name;
        this.madeIn = madeIn;
        this.spec = spec;
        this.shelfLife = shelfLife;
        this.brandView = brandView;
        this.categoryView = categoryView;
        this.lastReceiptPrice = lastReceiptPrice;
        this.retailPrice = retailPrice;
        this.memberPrice = memberPrice;
        this.vipPrice = vipPrice;
        this.video = video;
        this.images = images;
    }

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    public Barcode barcode() {
        return barcode;
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

    public String id() {
        return id;
    }

    public Name name() {
        return name;
    }

    public MadeIn madeIn() {
        return madeIn;
    }

    public LastReceiptPrice lastReceiptPrice() {
        return lastReceiptPrice;
    }

    public void setLastReceiptPrice(LastReceiptPrice lastReceiptPrice) {
        this.lastReceiptPrice = lastReceiptPrice;
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


    public URI video() {
        return video;
    }

    public URI[] images() {
        return images;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemView)) return false;

        ItemView itemView = (ItemView) o;

        return Objects.equals(id, itemView.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ItemView.class.getSimpleName() + "[", "]")
                .add("barcode=" + barcode)
                .add("grade=" + grade)
                .add("id='" + id + "'")
                .add("name=" + name)
                .add("madeIn=" + madeIn)
                .add("spec=" + spec)
                .add("shelfLife=" + shelfLife)
                .add("brandView=" + brandView)
                .add("categoryView=" + categoryView)
                .add("lastReceiptPrice=" + lastReceiptPrice)
                .add("retailPrice=" + retailPrice)
                .add("memberPrice=" + memberPrice)
                .add("vipPrice=" + vipPrice)
                .add("video=" + video)
                .add("images=" + Arrays.toString(images))
                .toString();
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrandView)) return false;

            BrandView brandView = (BrandView) o;

            return Objects.equals(id, brandView.id);
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BrandView.class.getSimpleName() + "[", "]")
                    .add("id='" + id + "'")
                    .add("name='" + name + "'")
                    .toString();
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CategoryView)) return false;

            CategoryView that = (CategoryView) o;

            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CategoryView.class.getSimpleName() + "[", "]")
                    .add("id='" + id + "'")
                    .add("name='" + name + "'")
                    .toString();
        }
    }
}
