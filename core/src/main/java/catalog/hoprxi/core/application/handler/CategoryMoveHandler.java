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

import catalog.hoprxi.core.application.command.CategoryMoveCommand;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryNodeMoved;

import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/6/16
 * @since JDK 21
 */

public class CategoryMoveHandler implements AggregateHandler<CategoryMoveCommand, Category>{
    private final UnitOfWork<Category> uow;

    public CategoryMoveHandler(UnitOfWork<Category> uow) {
        this.uow = Objects.requireNonNull(uow);
    }

    /**
     * @param category  内存中的领域对象
     * @param command 具体的命令
     * @return
     */
    @Override
    public Category execute(Category category, CategoryMoveCommand command) {
        category.moveTo(command.movedId());
        // 直接使用注入的 uow 注册事件
        uow.addEvent(new CategoryNodeMoved(command.id(), command.movedId()));
        return category;
    }
}
