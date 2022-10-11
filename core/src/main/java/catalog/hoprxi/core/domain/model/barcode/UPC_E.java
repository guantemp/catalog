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
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 2019-04-22
 */
public class UPC_E extends Barcode {
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{6}$");

    public UPC_E(CharSequence barcode) {
        super(barcode);
    }

    @Override
    public boolean checkFeature(CharSequence barcode) {
        Matcher matcher = BARCODE_PATTERN.matcher(barcode);
        if (matcher.matches())
            return EanCheckService.checkChecksum(barcode);
        return true;
    }

    public UPC_A toUPC_A() {
        StringBuilder sb = new StringBuilder().append(barcode.charAt(0));
        switch (barcode.charAt(6)) {
            case '0':
            case '1':
            case '2':
                sb.append(barcode.charAt(1));
                sb.append(barcode.charAt(2));
                sb.append(barcode.charAt(6));
                sb.append("0000");
                sb.append(barcode.charAt(3));
                sb.append(barcode.charAt(4));
                sb.append(barcode.charAt(5));
                break;

            case '3':
                sb.append(barcode.charAt(1));
                sb.append(barcode.charAt(2));
                sb.append(barcode.charAt(3));
                sb.append("00000");
                sb.append(barcode.charAt(4));
                sb.append(barcode.charAt(5));
                break;

            case '4':
                sb.append(barcode.charAt(1));
                sb.append(barcode.charAt(2));
                sb.append(barcode.charAt(3));
                sb.append(barcode.charAt(4));
                sb.append("00000");
                sb.append(barcode.charAt(5));
                break;

            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                sb.append(barcode.charAt(1));
                sb.append(barcode.charAt(2));
                sb.append(barcode.charAt(3));
                sb.append(barcode.charAt(4));
                sb.append(barcode.charAt(5));
                sb.append("0000");
                sb.append(barcode.charAt(6));
                break;

            default:
        }
        if (barcode.length() == 8) {
            sb.append(barcode.charAt(7));
        }
        return new UPC_A(sb.toString());
    }
}
