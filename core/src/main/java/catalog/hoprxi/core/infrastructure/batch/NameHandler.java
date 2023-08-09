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
import com.lmax.disruptor.EventHandler;
import salt.hoprxi.to.PinYin;

import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class NameHandler implements EventHandler<ItemImportEvent> {
    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        String name = itemImportEvent.map.get(ItemCorrespondence.NAME);
        name = name.replaceAll("'", "''").replaceAll("\\\\", "\\\\\\\\").trim();
        String alias = itemImportEvent.map.get(ItemCorrespondence.ALIAS);
        alias = alias == null ? name : alias.replaceAll("'", "''").replaceAll("\\\\", "\\\\\\\\").trim();
        StringJoiner joiner = new StringJoiner(",", "'{", "}'");
        joiner.add("\"name\":\"" + name + "\"");
        joiner.add("\"mnemonic\":\"" + PinYin.toShortPinYing(name) + "\"");
        joiner.add("\"alias\":\"" + (alias == null ? name : alias.replaceAll("'", "''")).replaceAll("\\\\", "\\\\\\\\") + "\"");
        //StringJoiner joiner = new StringJoiner(",", "", "");
        //joiner.add("'" + name + "'").add("'" + PinYin.toShortPinYing(name) + "'").add("'" + alias + "'");
        //System.out.println(joiner);
        itemImportEvent.map.put(ItemCorrespondence.NAME, joiner.toString());
    }
}
