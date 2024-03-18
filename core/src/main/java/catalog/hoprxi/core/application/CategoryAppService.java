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

import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.category.InvalidCategoryIdException;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.List;
import java.util.Objects;


/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2022-04-27
 */
public class CategoryAppService {
    private CategoryRepository repository;

    public CategoryAppService() {
        Config conf = ConfigFactory.load("database");
        String provider = conf.hasPath("provider") ? conf.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
                repository = new PsqlCategoryRepository("catalog");
                break;
        }
    }

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

    public CategoryView update(String id, List<Command> commands) {
        Category category = repository.find(id);
        if (category == null)
            throw new InvalidCategoryIdException("Id not exists");
        for (Command command : commands) {
            switch (command.getClass().getSimpleName()) {
                case "CategoryRenameCommand":
                    CategoryRenameCommand rename = (CategoryRenameCommand) command;
                    category.rename(rename.name(), rename.alias());
                    break;
                case "CategoryChangeDescriptionCommand":
                    CategoryChangeDescriptionCommand changeDescription = (CategoryChangeDescriptionCommand) command;
                    category.changeDescription(changeDescription.description());
                    break;
                case "CategoryChangeIconCommand":
                    CategoryChangeIconCommand changeIcon = (CategoryChangeIconCommand) command;
                    category.changeIcon(changeIcon.icon());
                    break;
            }
            repository.save(category);
        }
        return category.toView();
    }

}
