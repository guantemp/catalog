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

import java.util.EnumMap;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class AssembleHandler implements EventHandler<ItemImportEvent> {
    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        if (itemImportEvent.verify != Verify.OK) {
            System.out.println(itemImportEvent.verify + ":" + itemImportEvent.map.get(Corresponding.BARCODE));
            //未通过验证处理
        } else {
            EnumMap<Corresponding, String> map = itemImportEvent.map;
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            joiner.add(map.get(Corresponding.ID)).add(map.get(Corresponding.NAME)).add(map.get(Corresponding.BARCODE)).add(map.get(Corresponding.CATEGORY))
                    .add(map.get(Corresponding.BRAND)).add(map.get(Corresponding.GRADE)).add(map.get(Corresponding.MADE_IN)).add(map.get(Corresponding.SPEC))
                    .add(map.get(Corresponding.SHELF_LIFE)).add(map.get(Corresponding.RETAIL_PRICE)).add(map.get(Corresponding.MEMBER_PRICE)).add(map.get(Corresponding.VIP_PRICE));
            System.out.println(joiner.toString());
        }
    }
}
