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

package catalog.hoprxi.core.domain.model.madeIn;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-08-28
 */
public class Imported implements MadeIn {
    // 进口（国家或地区,如：美国）
    private final String country;
    private final String code;
    private final Pattern CODE_PATTERBN = Pattern.compile("^\\d{3}$");

    public Imported(String code, String country) {
        this.country = Objects.requireNonNull(country, "country required");
        code = Objects.requireNonNull(code, "city required").trim();
        if (!CODE_PATTERBN.matcher(code).matches())
            throw new IllegalArgumentException("code is three digit");
        this.code = code;
    }

    public String country() {
        return country;
    }

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String madeIn() {
        return country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Imported)) return false;

        Imported imported = (Imported) o;

        return Objects.equals(code, imported.code);
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Imported.class.getSimpleName() + "[", "]")
                .add("country='" + country + "'")
                .add("code='" + code + "'")
                .toString();
    }
}
