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

package catalog.hoprxi.core.application.handler;

import catalog.hoprxi.core.application.command.Command;
import catalog.hoprxi.core.application.command.ItemCreateCommand;
import catalog.hoprxi.core.domain.model.Item;
import catalog.hoprxi.core.domain.model.ItemRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlItemRepository;
import com.lmax.disruptor.EventHandler;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-07-26
 */
public class ItemCreateHandler implements EventHandler<Command> {
    private static ItemRepository repository = new PsqlItemRepository("catalog");

    @Override
    public void onEvent(Command command, long l, boolean b) throws Exception {
        if (command instanceof ItemCreateCommand) {
            ItemCreateCommand icc = (ItemCreateCommand) command;
            Item item = new Item(repository.nextIdentity(), icc.getBarcode(), icc.getName(), icc.getMadeIn(), icc.getSpec(), icc.getGrade(), icc.getShelfLife(),
                    icc.getLastReceiptPrice(), icc.getRetailPrice(), icc.getMemberPrice(), icc.getVipPrice(), icc.getCategoryId(), icc.getBrandId());
            repository.save(item);
        }
    }
}
