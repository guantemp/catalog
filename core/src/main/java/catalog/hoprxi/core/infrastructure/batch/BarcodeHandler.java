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

package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.barcode.InvalidBarcodeException;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import com.lmax.disruptor.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class BarcodeHandler implements EventHandler<ItemImportEvent> {
    private static final Map<String, Barcode> BARCODE_MAP = new HashMap<>();
    private static final ItemQueryService ITEM_QUERY = new PsqlItemQueryService("catalog");

    private static final AtomicInteger start = new AtomicInteger(1);
    private String prefix = "21";

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        String barcode = itemImportEvent.map.get(ItemMapping.BARCODE);
        itemImportEvent.verify = Verify.OK;
        if (barcode == null || barcode.isEmpty()) {
            //根据设置规则生成店内码
            itemImportEvent.map.put(ItemMapping.BARCODE, BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(start.getAndIncrement(), 1, prefix)[0].toPlanString());
            return;
        }
        Barcode bar = null;
        try {
            bar = BarcodeGenerateServices.createBarcode(barcode);
        } catch (InvalidBarcodeException e) {
            try {
                bar = BarcodeGenerateServices.createBarcodeCompleteChecksum(barcode);
            } catch (InvalidBarcodeException f) {
                itemImportEvent.verify = Verify.BARCODE_CHECK_SUM_ERROR;
                return;
            }
        }
        if (BARCODE_MAP.containsValue(bar)) {
            itemImportEvent.verify = Verify.BARCODE_REPEAT;
            return;
        }
        BARCODE_MAP.put(barcode, bar);

        ItemView[] itemViews = ITEM_QUERY.queryByBarcode("^" + bar.toPlanString() + "$");
        if (itemViews.length != 0) {
            itemImportEvent.verify = Verify.BARCODE_EXIST;
            return;
        }
        itemImportEvent.map.put(ItemMapping.BARCODE, "'" + bar.toPlanString() + "'");
        //System.out.println("barcode:"+itemImportEvent.map.get(Corresponding.BARCODE));
    }
}
