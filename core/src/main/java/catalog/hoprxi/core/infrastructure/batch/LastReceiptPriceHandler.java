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

package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.domain.model.price.UnitEnum;
import com.lmax.disruptor.EventHandler;

import java.math.BigDecimal;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-13
 */
public class LastReceiptPriceHandler implements EventHandler<ItemImportEvent> {
    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) throws Exception {
        String retailPriceStr = event.map.get(ItemMapping.LAST_RECEIPT_PRICE);

        if (retailPriceStr == null || retailPriceStr.trim().isEmpty()) {
            event.addWrong(Verify.LAST_RECEIPT_PRICE_ZERO,retailPriceStr);
            return;
        }

        try {
            // 使用 BigDecimal 解析，彻底杜绝精度丢失
            BigDecimal price = new BigDecimal(retailPriceStr.trim());

            // 判断是否为 0（compareTo 返回 0 表示相等）
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                event.addWrong(Verify.LAST_RECEIPT_PRICE_ZERO,event.map.get(ItemMapping.BARCODE)+":"+retailPriceStr);
                return;
            }

            // 解析成功且不为 0，继续后续逻辑...

        } catch (NumberFormatException e) {
            event.addWrong(Verify.LAST_RECEIPT_PRICE_ZERO,event.map.get(ItemMapping.BARCODE)+":"+retailPriceStr);
            return;
        }
        String units = event.map.get(ItemMapping.UNIT);
        if (units == null) units = "";
        String cleanS = units.replace("\u3000", "").replace(" ", "").trim();

        // 2. 【新增】如果清洗后为空，给予默认值（例如 "个" 或 "PCS"）
        // 如果你们系统有默认单位，请替换下面的 "个"
        if (cleanS.isEmpty()) {
            cleanS = UnitEnum.PCS.name();
        }
        UnitEnum unit = UnitEnum.of(cleanS);
        StringJoiner joiner = new StringJoiner(",", "'{\"name\":\"最近入库价\",\"price\": ", "}'");
        StringJoiner subJoiner = new StringJoiner(",", "{", "}");
        subJoiner.add("\"number\":" + event.map.get(ItemMapping.LAST_RECEIPT_PRICE));
        subJoiner.add("\"currencyCode\":\"CNY\"");
        subJoiner.add("\"unit\":\"" + unit.name() + "\"");
        joiner.add(subJoiner.toString());
        //System.out.println(joiner.toString());
        event.lastReceiptPriceJson= joiner.toString();
    }
}
