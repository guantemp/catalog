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
package catalog.hoprxi.core.domain.model.barcode;

import java.util.BitSet;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 2021-09-19
 */
public abstract class Barcode {
    protected CharSequence barcode;
    protected BitSet bitSet;

    public Barcode(CharSequence barcode) {
        Objects.requireNonNull(barcode, "barcode is required");
        if (!checkFeature(barcode))
            throw new IllegalArgumentException("Invalid barcode");
        this.barcode = barcode;
    }

    public CharSequence barcode() {
        return barcode;
    }

    public abstract boolean checkFeature(CharSequence barcode);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Barcode)) return false;

        Barcode barcode1 = (Barcode) o;

        return barcode != null ? barcode.equals(barcode1.barcode) : barcode1.barcode == null;
    }

    @Override
    public int hashCode() {
        return barcode != null ? barcode.hashCode() : 0;
    }
}
