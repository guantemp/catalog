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
import catalog.hoprxi.core.domain.model.GradeEnum;
import com.lmax.disruptor.EventHandler;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class GrandHandler implements EventHandler<ItemImportEvent> {
    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) throws Exception {
        String temp = event.map.get(ItemMapping.GRADE);
        if (temp == null) temp = "";
        String cleanS = temp.replace("\u3000", "").replace(" ", "").trim();

        // 2. 【新增】如果清洗后为空，给予默认值（例如 "合格"）
        if (cleanS.isEmpty()) {
            cleanS = GradeEnum.QUALIFIED.name();
        }
        GradeEnum g = GradeEnum.of(cleanS);
        event.map.put(ItemMapping.GRADE, "'" + g.name() + "'");
    }
}
