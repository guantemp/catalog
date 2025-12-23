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

package catalog.hoprxi.core.application.handler;


import catalog.hoprxi.core.application.command.ItemMadeInChangCommand;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/12/9
 */

public class ItemMadinChangeAggHandler implements AggregateHandler<ItemMadeInChangCommand, Item> {
    @Override
    public Item execute(Item item, ItemMadeInChangCommand command) {
        String code = command.code();
        MadeIn madeIn;
        if (code.length() == 3 && !"156".equals(code))
            madeIn = new Imported(code, command.madeIn());
        else {
            madeIn = new Domestic(code, command.madeIn());
        }
        item.changeMadeIn(madeIn);
        return item;
    }
}
