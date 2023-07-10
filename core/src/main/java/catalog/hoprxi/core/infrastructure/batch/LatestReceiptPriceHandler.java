/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.batch.ItemCorrespondence;
import catalog.hoprxi.core.domain.model.price.Unit;
import com.lmax.disruptor.EventHandler;

import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-13
 */
public class LatestReceiptPriceHandler implements EventHandler<ItemImportEvent> {
    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        Unit systemUnit = Unit.of(itemImportEvent.map.get(ItemCorrespondence.UNIT));
        StringJoiner joiner = new StringJoiner(",", "'{", "}'");
        joiner.add("\"number\":" + itemImportEvent.map.get(ItemCorrespondence.LATEST_RECEIPT_PRICE));
        joiner.add("\"currencyCode\":\"CNY\"");
        joiner.add("\"unit\":\"" + systemUnit.name() + "\"");
        itemImportEvent.map.put(ItemCorrespondence.LATEST_RECEIPT_PRICE, joiner.toString());
    }
}
