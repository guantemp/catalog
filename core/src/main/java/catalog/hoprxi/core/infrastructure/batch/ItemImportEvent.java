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

import java.util.EnumSet;
import java.util.Map;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class ItemImportEvent {
    Map<ItemMapping, String> map;
    EnumSet<Verify> wrong = EnumSet.noneOf(Verify.class);// 初始无错误
    public volatile long generatedId;      // 由 IdHandler 写入
    public volatile String barcode;      // 由 BarcodeHandler 写入
    public volatile String madeInJson;       // 由 MadeInHandler 写入
    public volatile long categoryId;     // 由 CategoryHandler 写入
    public volatile long brandId;        // 由 BrandHandler 写入
    public volatile String show;        // 由 UploadHandler 等写入

    public void addWrong(Verify error) {
        wrong.add(error);
    }

    public boolean hasWrong() {
        return !wrong.isEmpty();
    }

    public void setMap(Map<ItemMapping, String> map) {
        this.map = map;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ItemImportEvent.class.getSimpleName() + "[", "]")
                .add("map=" + map)
                .add("wrong=" + wrong)
                .toString();
    }
}
