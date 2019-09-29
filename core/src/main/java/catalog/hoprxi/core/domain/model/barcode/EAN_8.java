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

package catalog.hoprxi.core.domain.model.barcode;

import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 2019-04-22
 */
public class EAN_8 extends EANUPCBarcode {
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{8}$");

    public EAN_8(CharSequence barcode) {
        super(barcode);
    }

    public EAN_8 createChecksum(CharSequence barcode) {
        try {
            int checksuum = computeChecksum(barcode);
        } catch (InvalidBarcodeException e) {
            e.printStackTrace();
        }
        return new EAN_8("");
    }

    @Override
    public boolean checkFeature(CharSequence barcode) {
        Matcher matcher = BARCODE_PATTERN.matcher(barcode);
        if (matcher.matches())
            return checkChecksum(barcode);
        return false;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EAN_8.class.getSimpleName() + "[", "]")
                .add("barcode=" + barcode)
                .toString();
    }
}
