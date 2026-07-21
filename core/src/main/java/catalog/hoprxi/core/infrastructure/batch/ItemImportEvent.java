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

import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.2 builder 2026-07-20
 */
public final class ItemImportEvent {
    record BasicInfo(String nameJson, GradeEnum grade, String spec, long shelfLife,
                     String lastReceiptPriceJson, String retailPriceJson, String memberPriceJson,
                     String vipPriceJson) {
    }
    Map<ItemMapping, String> map;
    // 改为 Map，存储错误类型及触发时的原始值（作为快照）
    final Map<Verify, String> wrong = new EnumMap<>(Verify.class);
    volatile long generatedId;      // 由 IdHandler 写入
    volatile String barcode;      // 由 BarcodeHandler 写入
    volatile String madeInJson;       // 由 MadeInHandler 写入
    volatile long categoryId;     // 由 CategoryHandler 写入
    volatile long brandId;        // 由 BrandHandler 写入
    volatile String show;// 由 UploadHandler 等写入
    volatile BasicInfo basicInfo;

    public void addWrong(Verify error, String snapshotValue) {
        wrong.put(error, snapshotValue);
    }

    public boolean hasWrong() {
        return !wrong.isEmpty();
    }

    public void setMap(Map<ItemMapping, String> map) {
        this.map = map;
    }
}
