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

package catalog.hoprxi.core.application.processor;

import catalog.hoprxi.core.application.command.CategoryCreateCommand;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBCategoryRepository;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-06-24
 */
public class CategoryCreateProcessor implements Processor<CategoryCreateCommand> {

    @Override
    public void processor(CategoryCreateCommand command) {
        Objects.requireNonNull(command, "command required");
        final CategoryRepository repository = new ArangoDBCategoryRepository("catalog");
        Category category = new Category(command.getParentId(), repository.nextIdentity(), new Name(command.getName(), command.getAlias()), command.getDescription(), command.getLogo());
        repository.save(category);
    }

    @Override
    public void undo() {
        //final CategoryRepository repository = new ArangoDBCategoryRepository("catalog");
    }
}
