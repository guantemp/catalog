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

package catalog.hoprxi.core.domain.model.barcode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2021-09-19
 */
public class ISSN extends EAN_13 {
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^977\\d{10}$");

    public ISSN(CharSequence barcode) {
        super(barcode);
    }

    @Override
    public boolean checkFeature(CharSequence barcode) {
        Matcher matcher = BARCODE_PATTERN.matcher(barcode);
        if (!matcher.matches()) {
            throw new InvalidBarcodeException("error barcode format");
        }
        return super.checkFeature(barcode);
    }
}
