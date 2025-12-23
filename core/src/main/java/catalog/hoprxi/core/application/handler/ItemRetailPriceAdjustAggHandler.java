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


import catalog.hoprxi.core.application.command.ItemRetailPriceAdjustCommand;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.UnitEnum;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/11/24
 */

public class ItemRetailPriceAdjustAggHandler implements AggregateHandler<ItemRetailPriceAdjustCommand, Item> {
    @Override
    public Item execute(Item item, ItemRetailPriceAdjustCommand command) {
        RetailPrice price = new RetailPrice(new Price(command.amount(), UnitEnum.valueOf(command.unit())));
        item.adjustRetailPrice(price);
        return item;
    }
}
