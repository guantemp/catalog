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

package catalog.hoprxi.core.domain.model.price;

import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/15
 */
public class MemberPrice {
    public static final MemberPrice ZERO = new MemberPrice(Price.ZERO);
    private static final int NAME_LENGTH = 64;
    private Price price;
    private String name;

    public MemberPrice(String name, Price price) {
        setName(name);
        setPrice(price);
    }

    public MemberPrice(Price price) {
        this(Label.PRICE_MEMBER, price);
    }

    private void setPrice(Price price) {
        this.price = Objects.requireNonNull(price, "price required");
    }

    private void setName(String name) {
        name = Objects.requireNonNull(name, "name required").trim();
        if (name.isEmpty() || name.length() > NAME_LENGTH)
            throw new IllegalArgumentException("name length rang is 1-" + NAME_LENGTH);
        this.name = name;
    }

    public Price price() {
        return price;
    }

    public String name() {
        return name;
    }
}
