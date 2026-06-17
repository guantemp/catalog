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

package catalog.hoprxi.core.application.handler;

import catalog.hoprxi.core.application.command.CategoryChangDescriptionCommand;
import catalog.hoprxi.core.domain.model.category.Category;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/6/17
 * @since JDK 21
 */

public class CategoryDescriptionHandler implements AggregateHandler<CategoryChangDescriptionCommand, Category>{
    private final UnitOfWork<Category> uow;

    public CategoryDescriptionHandler(UnitOfWork<Category> uow) {
        this.uow = uow;
    }

    /**
     * @param entity  内存中的领域对象
     * @param command 具体的命令
     * @return
     */
    @Override
    public Category execute(Category entity, CategoryChangDescriptionCommand command) {
        entity.changeDescription(command.description());
        //uow.addEvent(new CategoryNodeMoved(command.id(), command.movedId()));
        return entity;
    }
}
