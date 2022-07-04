/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.command.CategoryCreateCommand;
import catalog.hoprxi.core.application.command.CategoryDeleteCommand;
import catalog.hoprxi.core.application.command.Command;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;

import java.util.Objects;
import java.util.Set;


/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2022-04-27
 */
public class CategoryAppService {
    private final CategoryRepository repository = new ArangoDBCategoryRepository("catalog");

    public CategoryView create(CategoryCreateCommand command) {
        Objects.requireNonNull(command, "command required");
        String parentId = command.getParentId();
        Category category = (parentId == null || parentId.isEmpty()) ?
                Category.root(repository.nextIdentity(), new Name(command.getName(), command.getAlias()), command.getDescription(), command.getLogo())
                : new Category(command.getParentId(), repository.nextIdentity(), new Name(command.getName(), command.getAlias()), command.getDescription(), command.getLogo());
        repository.save(category);
        return category.toView();
    }

    public void delete(CategoryDeleteCommand delete) {
        Objects.requireNonNull(delete, "delete command required");
        repository.remove(delete.id());
    }

    public CategoryView update(Set<Command> commands) {
        return null;
    }

}
