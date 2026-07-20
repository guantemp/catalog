/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.infrastructure.batch;


import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.price.UnitEnum;
import com.lmax.disruptor.EventHandler;

import java.math.BigDecimal;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/7/16
 */

public class BasicInfoHandler implements EventHandler<ItemImportEvent> {
    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) throws Exception {
        //long t1 = System.nanoTime();
        //name
        String name = event.map.get(ItemMapping.NAME);
        name = name.replaceAll("'", "''").replaceAll("\\\\", "\\\\\\\\").trim();
        //没有简称置空
        String shortName = event.map.get(ItemMapping.SHORT_NAME);
        shortName = shortName == null ? null : shortName.replaceAll("'", "''").replaceAll("\\\\", "\\\\\\\\").trim();
        StringJoiner nameJson = new StringJoiner(",", "'{", "}'");
        nameJson.add("\"name\":\"" + name + "\"");
        nameJson.add("\"shortName\":\"" + shortName + "\"");
//grade
        String gradeStr = event.map.get(ItemMapping.GRADE);
        if (gradeStr == null) gradeStr = "";
        String cleanGrade = gradeStr.replace("\u3000", "").replace(" ", "").trim();
        if (cleanGrade.isEmpty()) {  // 2. 【新增】如果清洗后为空，给予默认值（例如 "合格"）
            cleanGrade = GradeEnum.QUALIFIED.name();
        }
        GradeEnum grade = GradeEnum.of(cleanGrade);
//spec
        String spec = event.map.get(ItemMapping.SPEC);
        if (spec != null && spec.isBlank())
            spec = null;
        if (spec != null) {
            spec = "'" + spec.replaceAll("[*×Xx]\\d+.*", "") + "'";
        }
//保质期
        String shelfLife = event.map.get(ItemMapping.SHELF_LIFE);
        int days = BasicInfoHandler.parseShelfLife(shelfLife);

        //公共unit
        String units = event.map.get(ItemMapping.UNIT);
        if (units == null || units.isBlank()) units = "";
        String cleanS = units.replace("\u3000", "").replace(" ", "").trim();
        // 2. 【新增】如果清洗后为空，给予默认值（例如 "个" 或 "PCS"）
        // 如果你们系统有默认单位，请替换下面的 "个"
        if (cleanS.isEmpty()) {
            cleanS = UnitEnum.PCS.name();
        }
        UnitEnum unit = UnitEnum.of(cleanS);
//lastReceipt
        String lastReceiptPrice = event.map.get(ItemMapping.LAST_RECEIPT_PRICE);
        BigDecimal price = BasicInfoHandler.parsePriceOrDefault(lastReceiptPrice);
        StringJoiner lastReceiptPriceJoiner = new StringJoiner(",", "'{\"name\":\"最近入库价\",\"price\": ", "}'");
        StringJoiner lastReceiptPriceSubJoiner = new StringJoiner(",", "{", "}");
        lastReceiptPriceSubJoiner.add("\"number\":" + price);
        lastReceiptPriceSubJoiner.add("\"currencyCode\":\"CNY\"");
        lastReceiptPriceSubJoiner.add("\"unit\":\"" + unit.name() + "\"");
        lastReceiptPriceJoiner.add(lastReceiptPriceSubJoiner.toString());
// retailPrice
        String retailPrice = event.map.get(ItemMapping.RETAIL_PRICE);
        if (retailPrice == null || retailPrice.isBlank()) {
            event.addWrong(Verify.RETAIL_PRICE_ZERO, event.map.get(ItemMapping.BARCODE) + ":" + retailPrice);
            return;
        }
        try {
            // 使用 BigDecimal 解析，彻底杜绝精度丢失
            price = new BigDecimal(retailPrice.trim());
            // 判断是否为 0（compareTo 返回 0 表示相等）
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                event.addWrong(Verify.RETAIL_PRICE_ZERO, event.map.get(ItemMapping.BARCODE) + ":" + retailPrice);
                return;
            }
            // 解析成功且不为 0，继续后续逻辑...
        } catch (NumberFormatException e) {
            event.addWrong(Verify.RETAIL_PRICE_FORMAT_ERROR, event.map.get(ItemMapping.BARCODE) + ":" + retailPrice);
            return;
        }
        StringJoiner retailPriceJoiner = new StringJoiner(",", "'{", "}'");
        retailPriceJoiner.add("\"number\":" + event.map.get(ItemMapping.RETAIL_PRICE));
        retailPriceJoiner.add("\"currencyCode\":\"CNY\"");
        retailPriceJoiner.add("\"unit\":\"" + unit.name() + "\"");
//memberprice
        String memberPrice = event.map.get(ItemMapping.MEMBER_PRICE);
        price = BasicInfoHandler.parsePriceOrDefault(memberPrice);
        StringJoiner memberJoiner = new StringJoiner(",", "'{\"name\":\"会员价\",\"price\": ", "}'");
        StringJoiner memberSubJoiner = new StringJoiner(",", "{", "}");
        memberSubJoiner.add("\"number\":" + price);
        memberSubJoiner.add("\"currencyCode\":\"CNY\"");
        memberSubJoiner.add("\"unit\":\"" + unit.name() + "\"");
        memberJoiner.add(memberSubJoiner.toString());
        //vipprice
        String VipPrice = event.map.get(ItemMapping.VIP_PRICE);
        price = BasicInfoHandler.parsePriceOrDefault(VipPrice);
        StringJoiner vipPriceJoiner = new StringJoiner(",", "'{\"name\":\"VIP\",\"price\": ", "}'");
        StringJoiner vipPriceSubJoiner = new StringJoiner(",", "{", "}");
        vipPriceSubJoiner.add("\"number\":" + price);
        vipPriceSubJoiner.add("\"currencyCode\":\"CNY\"");
        vipPriceSubJoiner.add("\"unit\":\"" + unit.name() + "\"");
        vipPriceJoiner.add(vipPriceSubJoiner.toString());

        event.basicInfo = new ItemImportEvent.BasicInfo(nameJson.toString(), grade, spec, days,
                lastReceiptPriceJoiner.toString(), retailPriceJoiner.toString(),
                memberJoiner.toString(), vipPriceJoiner.toString());

        //long t2 = System.nanoTime();
        //System.out.println("基本信息: " + (t2 - t1) / 1_000_000 + " ms");
    }

    private static BigDecimal parsePriceOrDefault(String priceStr) {
        if (priceStr == null || priceStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(priceStr.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析保质期字符串，返回对应的天数。
     * <p>支持的格式：
     * <ul>
     *   <li>纯数字（如 "25"）→ 视为 25 天</li>
     *   <li>数字 + 单位（如 "25天", "3个月", "3月", "1年", "3年"）</li>
     *   <li>特殊词（"半年" → 180天, "季度" → 90天）</li>
     * </ul>
     * </p>
     *
     * @param input 输入的保质期字符串，允许前后有空格
     * @return 转换后的天数，若输入无效或无法解析则返回 0（可根据业务调整）
     */
    public static int parseShelfLife(String input) {
        if (input == null || input.isBlank()) {
            return 0;
        }
        String trimmed = input.trim();

        // 1. 匹配特殊词
        if ("半年".equals(trimmed)) {
            return 180;
        }
        if ("季度".equals(trimmed)) {
            return 90;
        }

        // 2. 匹配纯数字（视为天）
        Pattern digitPattern = Pattern.compile("^\\d+$");
        Matcher digitMatcher = digitPattern.matcher(trimmed);
        if (digitMatcher.matches()) {
            return Integer.parseInt(trimmed);
        }

        // 3. 匹配数字 + 单位（允许中间有空格）
        // 单位支持：天/日、月/个月、年
        Pattern unitPattern = Pattern.compile("^(\\d+)\\s*(天|日|个月|月|年)$");
        Matcher unitMatcher = unitPattern.matcher(trimmed);
        if (unitMatcher.matches()) {
            int number = Integer.parseInt(unitMatcher.group(1));
            String unit = unitMatcher.group(2);
            return switch (unit) {
                case "天", "日" -> number;
                case "月", "个月" -> number * 30;
                case "年" -> number * 360;
                default -> 0; // 理论上不会走到这里
            };
        }
        // 4. 无法识别
        return 0;
    }
}
