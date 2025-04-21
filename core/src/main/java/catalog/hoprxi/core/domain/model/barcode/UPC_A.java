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

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 2021-09-19
 */
public class UPC_A extends Barcode {

    public UPC_A(CharSequence barcode) {
        super(barcode);
    }

    @Override
    public boolean isCorrectChecksum(CharSequence barcode) {
        String tempBarcode = "0" + barcode;
        return EanCheckService.isChecksum(tempBarcode);
    }

    public UPC_E toUPC_E() {
        StringBuilder result = new StringBuilder().append(barcode.charAt(0));
        if (barcode.charAt(0) != '0' && barcode.charAt(0) != '1') {
            throw new InvalidBarcodeException("[UPCA] Invalid Number System,  only 0 & 1 are valid (" + barcode.charAt(0) + ").");
        } else if ("000".contentEquals(barcode.subSequence(3, 6)) || "100".contentEquals(barcode.subSequence(3, 6)) || "200".contentEquals(barcode.subSequence(3, 6))) {
            result.append(barcode.subSequence(1, 3));
            result.append(barcode.subSequence(8, 11));
            result.append(barcode.charAt(3));
        } else if ("00".contentEquals(barcode.subSequence(4, 6))) {
            result.append(barcode.subSequence(1, 4));
            result.append(barcode.subSequence(9, 11));
            result.append('3');
        } else if (barcode.charAt(5) == '0') {
            result.append(barcode.subSequence(1, 5));
            result.append(barcode.charAt(10));
            result.append('4');
        } else if (barcode.charAt(10) >= '5' && barcode.charAt(10) <= '9') {
            result.append(barcode.subSequence(1, 6));
            result.append(barcode.charAt(10));
        } else {
            throw new InvalidBarcodeException("[UPCA] Invalid code.");
        }
        if (barcode.length() == 12) {
            result.append(barcode.charAt(11));
        }
        return new UPC_E(result.toString());
    }
}
