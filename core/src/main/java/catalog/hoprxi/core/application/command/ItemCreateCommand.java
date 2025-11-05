/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.application.command;

import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.LastReceiptPrice;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;

import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-07-10
 */
public class ItemCreateCommand implements Command<Item> {
    private final Barcode barcode;
    private final GradeEnum grade;
    private final Name name;
    private final MadeIn madeIn;
    private final Specification spec;
    private final ShelfLife shelfLife;
    private final long brandId;
    private final long categoryId;

    private final LastReceiptPrice lastReceiptPrice;
    private final RetailPrice retailPrice;
    private final MemberPrice memberPrice;
    private final VipPrice vipPrice;

    public ItemCreateCommand(Barcode barcode, Name name, MadeIn madeIn, Specification spec, GradeEnum grade, ShelfLife shelfLife, LastReceiptPrice lastReceiptPrice, RetailPrice retailPrice, MemberPrice memberPrice, VipPrice vipPrice, long categoryId, long brandId) {
        this.barcode = barcode;
        this.grade = grade;
        this.name = name;
        this.madeIn = madeIn;
        this.spec = spec;
        this.shelfLife = shelfLife;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.lastReceiptPrice = lastReceiptPrice;
        this.retailPrice = retailPrice;
        this.memberPrice = memberPrice;
        this.vipPrice = vipPrice;
    }

    public Barcode barcode() {
        return barcode;
    }

    public GradeEnum grade() {
        return grade;
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

    public ShelfLife shelfLife() {
        return shelfLife;
    }

    public long brandId() {
        return brandId;
    }

    public long categoryId() {
        return categoryId;
    }

    public LastReceiptPrice lastReceiptPrice() {
        return lastReceiptPrice;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", ItemCreateCommand.class.getSimpleName() + "[", "]")
                .add("barcode=" + barcode)
                .add("grade=" + grade)
                .add("name=" + name)
                .add("madeIn=" + madeIn)
                .add("spec=" + spec)
                .add("shelfLife=" + shelfLife)
                .add("brandId=" + brandId)
                .add("categoryId=" + categoryId)
                .add("lastReceiptPrice=" + lastReceiptPrice)
                .add("retailPrice=" + retailPrice)
                .add("memberPrice=" + memberPrice)
                .add("vipPrice=" + vipPrice)
                .toString();
    }
}
