/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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

import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-02-09
 */
public class BarcodeValidServices {
    private static final Pattern BARCODE = Pattern.compile("^\\d{8}$|^\\d{12,14}$");

    public static boolean valid(String barcode) {
        return barcode == null ? false : BARCODE.matcher(barcode).matches() ? EanCheckService.isChecksum(barcode) : false;
    }

    public static boolean valid(long barcode) {
        return valid(String.valueOf((barcode)));
    }
}
