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

package catalog.hoprxi.core.domain.model.shelfLife;

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-04-28
 */
public class ShelfLife {
    public final static ShelfLife SAME_DAY = new ShelfLife(0) {
        @Override
        public ShelfLife extend(int days) {
            return this;
        }

        @Override
        public ShelfLife reduce(int days) {
            return this;
        }
    };
    public final static ShelfLife THREE_DAY = new ShelfLife(3) {
        @Override
        public ShelfLife extend(int days) {
            return this;
        }

        @Override
        public ShelfLife reduce(int days) {
            return this;
        }
    };
    public final static ShelfLife PERMANENTLY = new ShelfLife(35640) {
        @Override
        public ShelfLife extend(int days) {
            return this;
        }

        @Override
        public ShelfLife reduce(int days) {
            return this;
        }
    };
    private int days;

    public ShelfLife(int days) {
        setDays(days);
    }

    public static ShelfLife rebuild(int days) {
        if (days == 0)
            return SAME_DAY;
        if (days == 3)
            return THREE_DAY;
        if (days == 35640)
            return PERMANENTLY;
        return new ShelfLife(days);
    }

    public static ShelfLife createShelfLifeWithMonth(int month) {
        return new ShelfLife(month * 30);
    }

    public static ShelfLife createShelfLifeWithYear(int year) {
        return new ShelfLife(year * 360);
    }

    private void setDays(int days) {
        if (days < 0 || days > 35640)
            throw new IllegalArgumentException("Shelf life days 1-1095 days");
        this.days = days;
    }

    public TimeUnit unit() {
        return TimeUnit.DAYS;
    }

    public int days() {
        return days;
    }

    public ShelfLife extend(int days) {
        return rebuild(days);
    }

    public ShelfLife reduce(int days) {
        return rebuild(days);
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
