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

package catalog.hoprxi.scale.domain.model;

import com.arangodb.entity.DocumentField;

import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-11-04
 */
public class Plu {
    @DocumentField(DocumentField.Type.KEY)
    private String plu;

    public Plu(int plu) {
        setPlu(plu);
    }

    public int plu() {
        return Integer.parseInt(plu);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plu plu1 = (Plu) o;

        return plu != null ? plu.equals(plu1.plu) : plu1.plu == null;
    }

    @Override
    public int hashCode() {
        return plu != null ? plu.hashCode() : 0;
    }

    private void setPlu(int plu) {
        if (plu <= 0 || plu > 99999)
            throw new IllegalArgumentException("plu rang is [1-99999]");
        this.plu = String.valueOf(plu);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Plu.class.getSimpleName() + "[", "]")
                .add("plu=" + plu)
                .toString();
    }
}
