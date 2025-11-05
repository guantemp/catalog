/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.application;

import catalog.hoprxi.core.application.command.Command;
import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlItemRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 2022-11-29
 */
public class ItemAppService {
    private static ItemRepository itemRepository;
    private static CategoryRepository categoryRepository;
    private static BrandRepository brandRepository;


    static {
        Config config = ConfigFactory.load("database");
        String provider = config.hasPath("provider") ? config.getString("provider").toLowerCase() : "postgresql";
        String database = config.hasPath("database") ? config.getString("database").toLowerCase() : "catalog";
        switch ((provider)) {
            case "psql":
            case "postgres":
            case "postgresql":
                itemRepository = new PsqlItemRepository();
                categoryRepository = new PsqlCategoryRepository();
                brandRepository = new PsqlBrandRepository();
                break;
        }
    }

    public void createItem(Command itemCreateCommand) {
        ItemCreateCommand createCommand = (ItemCreateCommand) itemCreateCommand;
        Item item = new Item(itemRepository.nextIdentity(), createCommand.barcode(), createCommand.name(), createCommand.madeIn(), createCommand.spec(),
                createCommand.grade(), createCommand.shelfLife(), createCommand.lastReceiptPrice(), createCommand.retailPrice(), createCommand.memberPrice(),
                createCommand.vipPrice(), createCommand.categoryId(), createCommand.brandId());
        itemRepository.save(item);
    }

    public void removeItem(Command ItemRemoveCommand) {

    }

    public void deleteItem(Command ItemDeleteCommand) {

    }
}
