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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-09-19
 */
public class EanUcc {
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{7}$||^\\d{11}$||^\\d{12}$");

    /**
     * @param barcode
     * @return
     */
    public static boolean checkChecksum(CharSequence barcode) {
        try {
            int checksum = computeChecksum(barcode.subSequence(0, barcode.length() - 1));
            return checksum == barcode.charAt(barcode.length() - 1) - '0';
        } catch (InvalidBarcodeException e) {
            return false;
        }
    }

    /**
     * @param barcode
     * @return
     * @throws InvalidBarcodeException
     */
    public static int computeChecksum(CharSequence barcode) throws InvalidBarcodeException {
        Matcher matcher = BARCODE_PATTERN.matcher(barcode);
        if (!matcher.matches()) {
            throw new InvalidBarcodeException("error barcode format");
        }
        int sum = 0;
        for (int i = 0, j = barcode.length() - 1, k = j; i <= j; i++, k--) {
            if (i % 2 == 0)
                sum += 3 * (barcode.charAt(k) - '0');// i=0,2,4,6....
            else
                sum += (barcode.charAt(k) - '0');// i=1,3,5,7...
        }
        return (10 - sum % 10) % 10;
    }
}
