/*
 * Copyright (c) 2021. www.hoprxi.com All Rights Reserved.
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
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2020-03-15
 */
public class Agile {
    private Price price;

    public Agile(Price price) {
        setPrice(price);
    }

    public String name() {
        return Label.PRICE_RETAIL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Agile)) return false;

        Agile agile = (Agile) o;

        return price != null ? price.equals(agile.price) : agile.price == null;
    }

    @Override
    public int hashCode() {
        return price != null ? price.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Agile.class.getSimpleName() + "[", "]")
                .add("price=" + price)
                .toString();
    }

    private void setPrice(Price price) {
        this.price = Objects.requireNonNull(price, "price required");
    }
}
