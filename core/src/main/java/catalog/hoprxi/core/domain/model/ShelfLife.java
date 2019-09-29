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

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-04-28
 */
public class ShelfLife {
    public final static ShelfLife SAME_DAY = new ShelfLife(1);
    public final static ShelfLife NO_SHELF_LIFE = new ShelfLife(0);
    private int days;

    public static ShelfLife rebuild(int days) {
        if (days == 0)
            return NO_SHELF_LIFE;
        if (days == 1)
            return SAME_DAY;
        return new ShelfLife(days);
    }

    public ShelfLife(int days) {
        setDays(days);
    }

    public static ShelfLife createShelfLifeWithMonth(int month) {
        return new ShelfLife(month * 30);
    }

    public static ShelfLife createShelfLifeWithYear(int year) {
        return new ShelfLife(year * 365);
    }

    private void setDays(int days) {
        if (days < 0)
            throw new IllegalArgumentException("days larger zero");
        this.days = days;
    }

    public TimeUnit unit() {
        return TimeUnit.DAYS;
    }

    public int days() {
        return days;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ShelfLife.class.getSimpleName() + "[", "]")
                .add("days=" + days + " " + TimeUnit.DAYS)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShelfLife shelfLife = (ShelfLife) o;

        return days == shelfLife.days;
    }

    @Override
    public int hashCode() {
        return days;
    }
}
