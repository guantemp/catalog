/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.domain.model;


import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 2026-03-01
 */
public final class Plu {
    private int plu;

    public static Plu valueOf(int i) {
        return new Plu(i);
    }

    public Plu(int plu) {
        setPlu(plu);
    }

    public int id() {
        return plu;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Plu)) return false;
        Plu plu1 = (Plu) o;
        return plu == plu1.plu;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(plu);
    }

    private void setPlu(int plu) {
        if (plu <= 0 || plu > 99999)
            throw new IllegalArgumentException("plu rang is [1-99999]");
        this.plu = plu;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Plu.class.getSimpleName() + "[", "]")
                .add("plu=" + plu)
                .toString();
    }
}
