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

package catalog.hoprxi.core.domain.model.madeIn;

import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-08-28
 */
public class Imported implements MadeIn {
    // 进口（国家或地区,如：美国）
    private String country;

    public Imported(String country) {
        this.country = country;
    }

    @Override
    public String madeIn() {
        return country;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Imported.class.getSimpleName() + "[", "]")
                .add("country='" + country + "'")
                .toString();
    }

    @Override
    public long code() {
        return 0l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Imported imported = (Imported) o;

        return country != null ? country.equals(imported.country) : imported.country == null;
    }

    @Override
    public int hashCode() {
        return country != null ? country.hashCode() : 0;
    }
}
