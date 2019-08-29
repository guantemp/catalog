/*
 * Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 2019-04-22
 */
public class EANUPCBarcodeGenerateServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(EANUPCBarcodeGenerateServices.class);
    private static final Pattern IN_STORE_PREFIX = Pattern.compile("^2[0-4]$");
    private static final DecimalFormat EAN_8_DECIMAL_FORMAT = new DecimalFormat("00000");
    private static final DecimalFormat EAN_13_DECIMAL_FORMAT = new DecimalFormat("0000000000");

    public static EANUPCBarcode createMatchingBarcode(CharSequence barcode) {
        if (barcode.length() == 8)
            return new EAN_8(barcode);
        if (barcode.length() == 13)
            return new EAN_13(barcode);
        return null;
    }

    /**
     * @param start
     * @param amount
     * @param prefix
     * @return
     */
    public static EANUPCBarcode[] inStoreEAN_8BarcodeGenerate(int start, int amount, String prefix) {
        if (start < 0)
            throw new IllegalArgumentException("initiaVlaue is positive number");
        if (amount <= 0)
            throw new IllegalArgumentException("amount required larger zero");
        if (start + amount > 99999)
            throw new IllegalArgumentException("sum(start,amount) must less than or equal to 99999");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        EAN_8[] ean_8s = new EAN_8[amount];
        try {
            for (int i = 0; i < amount; i++) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_8_DECIMAL_FORMAT.format(start));
                int checkSum = EANUPCBarcode.computeChecksum(sb);
                ean_8s[i] = new EAN_8(sb.append(checkSum));
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN8 barcode");
        }
        return ean_8s;
    }

    /**
     * @param start  rang is 0-999999999999
     * @param amount
     * @param prefix rang is 20-24
     * @return
     */
    public static EANUPCBarcode[] inStoreEAN_13BarcodeGenerate(long start, int amount, String prefix) {
        if (start < 0l)
            throw new IllegalArgumentException("initiaVlaue is positive number");
        if (amount <= 0)
            throw new IllegalArgumentException("amount required larger zero");
        if (start + amount > 9999999999l)
            throw new IllegalArgumentException("sum(start,amount) must less than or equal to  9999999999");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        EAN_13[] ean_13s = new EAN_13[amount];
        try {
            for (int i = 0; i < amount; i++) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_13_DECIMAL_FORMAT.format(start));
                int checkSum = EANUPCBarcode.computeChecksum(sb);
                ean_13s[i] = new EAN_13(sb.append(checkSum));
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN13 barcode");
        }
        return ean_13s;
    }
}
