/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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
import salt.hoprxi.utils.DigitPreferenceFilter;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条形码生成服务类，提供创建单个条形码、补全校验位以及生成店内码（如EAN-8、EAN-13）序列的功能。
 *
 * <p>该类主要处理以下业务逻辑：
 * <ul>
 *   <li>根据输入字符长度自动识别并创建 EAN-8, UPC-A 或 EAN-13 对象。</li>
 *   <li>自动计算并补全条形码的校验位。</li>
 *   <li>批量生成具有特定前缀（如20-24代表店内码）和序列号的条形码。</li>
 *   <li>支持通过过滤器排除特定数字组合的条形码。</li>
 * </ul>
 *
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.0.3 2023-03-21
 * @since JDK8.0
 */
public class BarcodeGenerateServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(BarcodeGenerateServices.class);
    private static final Pattern IN_STORE_PREFIX = Pattern.compile("^2[0-4]$");
    private static final DecimalFormat EAN_8_DECIMAL_FORMAT = new DecimalFormat("00000");
    private static final DecimalFormat EAN_13_DECIMAL_FORMAT = new DecimalFormat("0000000000");


    /**
     * 根据条形码字符串创建对应的条形码对象。
     *
     * <p>该方法根据输入字符串的长度自动判断类型：
     * <ul>
     *   <li>长度为 8：创建 EAN-8 对象</li>
     *   <li>长度为 12：创建 UPC-A 对象</li>
     *   <li>长度为 13：创建 EAN-13 对象</li>
     * </ul>
     *
     * @param barcode 条形码字符序列，不可为 null
     * @return 返回对应的 Barcode 实现类实例
     * @throws InvalidBarcodeException 如果条形码长度不符合上述规范（非8、12、13位）则抛出异常
     */
    public static Barcode createBarcode(String barcode) {
        Objects.requireNonNull(barcode, "barcode must not be null");
        if (barcode.length() == 8)
            return new EAN_8(barcode);
        if (barcode.length() == 12)
            return new UPC_A(barcode);
        if (barcode.length() == 13)
            return new EAN_13(barcode);
        throw new InvalidBarcodeException("Invalid barcode");
    }

    /**
     * 创建条形码对象并自动补全校验位。
     *
     * <p>该方法会计算输入字符串的校验位，并将其追加到原字符串末尾后创建对象。
     * 适用于输入只有主体部分，缺少最后一位校验码的情况。
     *
     * @param barcode 不包含校验位的条形码主体，不可为 null
     * @return 返回补全校验位后的 Barcode 实例
     */
    public static Barcode createBarcodeCompleteChecksum(CharSequence barcode) {
        Objects.requireNonNull(barcode, "barcode must not be null");
        int checksum = EanCheckService.computeChecksum(barcode);
        String temp = barcode.toString() + checksum;
        return createBarcode(temp);
    }

    /**
     * 批量生成店内码 EAN-8 条形码。
     *
     * <p>用于生成指定数量、指定起始序列、指定前缀（20-24）的 EAN-8 条形码数组。
     * 序列号部分会自动格式化为5位数字（不足补零）。
     *
     * @param start  起始序列号，必须为非负数
     * @param amount 生成数量，必须大于0
     * @param prefix 前缀，必须匹配正则 "^2[0-4]$" (即20-24)
     * @return 包含指定数量 EAN-8 条形码的数组
     * @throws IllegalArgumentException 如果 start 为负数，amount 小于等于0，或前缀不符合规范
     * @throws IllegalArgumentException 如果 start + amount 超过最大限制 (99999)
     */
    public static Barcode[] inStoreEAN_8BarcodeGenerate(int start, int amount, String prefix) {
        if (start < 0)
            throw new IllegalArgumentException("start is positive number");
        if (amount < 0)
            throw new IllegalArgumentException("Amount required larger zero");
        if (start + amount > 99999)
            throw new IllegalArgumentException("Sum(start,amount) must less than or equal to 99999");
        Objects.requireNonNull(prefix, "prefix must not be null");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        EAN_8[] ean_8s = new EAN_8[amount];
        try {
            for (int i = 0; i < amount; i++) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_8_DECIMAL_FORMAT.format(start));
                int checkSum = EanCheckService.computeChecksum(sb);
                ean_8s[i] = new EAN_8(sb.append(checkSum).toString());
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN8 barcode");
        }
        return ean_8s;
    }

    /**
     * 生成单个店内码 EAN-8 条形码。
     *
     * @param start  序列号，必须为非负数
     * @param prefix 前缀，必须匹配正则 "^2[0-4]$" (即20-24)
     * @return 返回生成的 EAN-8 条形码实例
     * @throws IllegalArgumentException 如果 start 为负数，或前缀不符合规范
     */
    public static Barcode inStoreEAN_8BarcodeGenerate(int start, String prefix) {
        if (start < 0)
            throw new IllegalArgumentException("start is positive number");
        Objects.requireNonNull(prefix, "prefix must not be null");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        StringBuilder sb = new StringBuilder(prefix).append(EAN_8_DECIMAL_FORMAT.format(start));
        int checkSum = EanCheckService.computeChecksum(sb);
        return new EAN_8(sb.append(checkSum).toString());
    }

    /**
     * 批量生成店内码 EAN-8 条形码，并应用数字偏好过滤器。
     *
     * <p>该方法会在生成过程中调用 {@link DigitPreferenceFilter#mantissaPreferenceFilter} 进行过滤，
     * 只有通过过滤器的条形码才会被计入最终结果。
     *
     * @param start  起始序列号
     * @param amount 需要生成的有效数量
     * @param prefix 前缀 (20-24)
     * @param filter 过滤规则数组，用于排除特定数字组合
     * @return 包含指定数量且通过过滤的 EAN-8 条形码数组
     */
    public static Barcode[] inStoreEAN_8BarcodeGenerateWithFilter(int start, int amount, String prefix, int[] filter) {
        if (start < 0)
            throw new IllegalArgumentException("start is positive number");
        if (amount < 0)
            throw new IllegalArgumentException("Amount required larger zero");
        if (start + amount > 99999)
            throw new IllegalArgumentException("Sum(start,amount) must less than or equal to 99999");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        EAN_8[] ean_8s = new EAN_8[amount];
        try {
            for (int i = 0; i < amount; ) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_8_DECIMAL_FORMAT.format(start));
                int checkSum = EanCheckService.computeChecksum(sb);
                EAN_8 barcode = new EAN_8(sb.append(checkSum).toString());
                if (DigitPreferenceFilter.mantissaPreferenceFilter(barcode.barcode(), filter)) {
                    ean_8s[i] = barcode;
                    i++;
                }
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN8 barcode");
        }
        return ean_8s;
    }

    /**
     * 批量生成店内码 EAN-13 条形码。
     *
     * <p>用于生成指定数量、指定起始序列、指定前缀（20-24）的 EAN-13 条形码数组。
     * 序列号部分会自动格式化为10位数字（不足补零）。
     *
     * @param start  起始序列号 (0-999999999999)
     * @param amount 生成数量
     * @param prefix 前缀 (20-24)
     * @return 包含指定数量 EAN-13 条形码的数组
     */
    public static Barcode[] inStoreEAN_13BarcodeGenerate(long start, int amount, String prefix) {
        if (start < 0L)
            throw new IllegalArgumentException("start is positive number");
        if (amount < 0)
            throw new IllegalArgumentException("amount required larger zero");
        if (start + amount > 9999999999L)
            throw new IllegalArgumentException("Sum(start,amount) must less than or equal to 9999999999");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        EAN_13[] ean_13s = new EAN_13[amount];
        try {
            for (int i = 0; i < amount; i++) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_13_DECIMAL_FORMAT.format(start));
                int checkSum = EanCheckService.computeChecksum(sb);
                ean_13s[i] = new EAN_13(sb.append(checkSum).toString());
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN13 barcode");
        }
        return ean_13s;
    }

    /**
     * 生成单个店内码 EAN-13 条形码。
     *
     * @param start  序列号
     * @param prefix 前缀 (20-24)
     * @return 返回生成的 EAN-13 条形码实例
     */
    public static Barcode inStoreEAN_13BarcodeGenerate(long start, String prefix) {
        if (start < 0L)
            throw new IllegalArgumentException("start is positive number");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        StringBuilder sb = new StringBuilder(prefix).append(EAN_13_DECIMAL_FORMAT.format(start));
        int checkSum = EanCheckService.computeChecksum(sb);
        return new EAN_13(sb.append(checkSum).toString());
    }

    /**
     * 批量生成店内码 EAN-13 条形码，并应用数字偏好过滤器。
     *
     * @param start  起始序列号
     * @param amount 生成数量
     * @param prefix 前缀 (20-24)
     * @param filter 过滤规则数组
     * @return 包含指定数量且通过过滤的 EAN-13 条形码数组
     */
    public static Barcode[] inStoreEAN_13BarcodeGenerate(long start, int amount, String prefix, int[] filter) {
        if (start < 0L)
            throw new IllegalArgumentException("start is positive number");
        if (amount < 0)
            throw new IllegalArgumentException("amount required larger zero");
        if ((start + amount) > 9999999999L)
            throw new IllegalArgumentException("Sum(start,amount) must less than or equal to 9999999999");
        Matcher matcher = IN_STORE_PREFIX.matcher(prefix);
        if (!matcher.matches())
            throw new IllegalArgumentException("prefix required 20-24");
        EAN_13[] ean_13s = new EAN_13[amount];
        try {
            for (int i = 0; i < amount; ) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_13_DECIMAL_FORMAT.format(start));
                int checkSum = EanCheckService.computeChecksum(sb);
                EAN_13 barcode = new EAN_13(sb.append(checkSum).toString());
                if (DigitPreferenceFilter.mantissaPreferenceFilter(barcode.barcode(), filter)) {
                    ean_13s[i] = barcode;
                    i++;
                }
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN13 barcode");
        }
        return ean_13s;
    }

    /**
     * 批量生成优惠券条形码 (前缀为 "99") 并应用过滤器。
     *
     * <p>注意：原文档代码中此处逻辑与无过滤器版本一致，均为 "99" 前缀。
     * 这里根据方法名 "coupon" 保留了原有 "99" 逻辑，但建议核实业务需求。
     *
     * @param start  起始序列号
     * @param amount 生成数量
     * @param filter 过滤规则数组
     * @return 包含指定数量且通过过滤的 EAN-13 条形码数组
     */
    public static Barcode[] couponBarcodeGenerate(long start, int amount, int[] filter) {
        if (start < 0L)
            throw new IllegalArgumentException("start is positive number");
        if (amount <= 0)
            return new EAN_13[0];
        if (start + amount > 9999999999L)
            throw new IllegalArgumentException("Sum(start,amount) must less than or equal to  9999999999");

        EAN_13[] ean_13s = new EAN_13[amount];
        final String prefix = "99";
        try {
            for (int i = 0; i < amount; ) {
                StringBuilder sb = new StringBuilder(prefix).append(EAN_13_DECIMAL_FORMAT.format(start));
                int checkSum = EanCheckService.computeChecksum(sb);
                EAN_13 barcode = new EAN_13(sb.append(checkSum).toString());
                // 如果 filter 为 null，直接放入数组；如果 filter 不为 null，则进行过滤判断
                if (filter == null || DigitPreferenceFilter.mantissaPreferenceFilter(barcode.barcode(), filter)) {
                    ean_13s[i] = barcode;
                    i++;
                }
                start += 1;
            }
        } catch (InvalidBarcodeException e) {
            LOGGER.error("Invalid EAN13 barcode");
        }
        return ean_13s;
    }

    /**
     * 批量生成优惠券条形码 (前缀为 "99")。
     *
     * <p>专门用于生成以 "99" 开头的 EAN-13 优惠券条形码。
     *
     * @param start  起始序列号
     * @param amount 生成数量
     * @return 包含指定数量 EAN-13 条形码的数组
     */
    public static Barcode[] couponBarcodeGenerate(long start, int amount) {
        return BarcodeGenerateServices.couponBarcodeGenerate(start, amount, null);
    }
}
