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

import com.lmax.disruptor.EventHandler;

import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-10
 */
public class FailedValidationHandler implements EventHandler<ItemImportEvent> {
    private static AtomicInteger number = new AtomicInteger(0);

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        switch (itemImportEvent.verify) {
            case BARCODE_EXIST:
            case BARCODE_REPEAT:
            case BARCODE_CHECK_SUM_ERROR:
                number.incrementAndGet();
                System.out.println(itemImportEvent.verify + ":" + itemImportEvent.map.get(Corresponding.BARCODE));
                break;
        }
        if (itemImportEvent.map.get(Corresponding.LAST_ROW) != null) {
            System.out.println("Fail:" + number);
        }
    }
}
