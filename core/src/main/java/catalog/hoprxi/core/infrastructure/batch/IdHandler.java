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
import salt.hoprxi.id.LongId;

import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class IdHandler implements EventHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{12,19}$");

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        String id = itemImportEvent.map.get(Corresponding.ID);
        if (id == null || id.isEmpty() || !ID_PATTERN.matcher(id).matches()) {
            id = String.valueOf(LongId.generate());
        }
        itemImportEvent.map.put(Corresponding.ID, id);
    }
}
