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

import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.ItemCreated;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.domain.model.category.CategoryCreated;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlItemRepository;
import catalog.hoprxi.core.util.DomainRegistry;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.2 builder 2025-11-05
 */
public class ItemCreateHandler implements Handler<ItemCreateCommand, Item> {
    private final ItemRepository repository = new PsqlItemRepository();


    @Override
    public Item execute(ItemCreateCommand command) {
        System.out.println(command.name());
        Item item = new Item(repository.nextIdentity(), command.barcode(), command.name(), command.madeIn(), command.spec(), command.grade(), command.shelfLife(),
                command.lastReceiptPrice(), command.retailPrice(), command.memberPrice(), command.vipPrice(), command.categoryId(), command.brandId());
        repository.save(item);
        //领域事件
        ItemCreated event = new ItemCreated ();
        DomainRegistry.domainEventPublisher().publish(event);
        return item;
    }

    @Override
    public void undo() {
        Handler.super.undo();
    }

    @Override
    public void redo() {
        Handler.super.redo();
    }
}
