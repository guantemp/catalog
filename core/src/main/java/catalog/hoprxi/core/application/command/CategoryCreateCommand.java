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

package catalog.hoprxi.core.application.command;

import catalog.hoprxi.core.domain.model.category.Category;

import java.net.URI;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-06-24
 */
public class CategoryCreateCommand implements Command<Category> {
    private long parentId;
    private final String name;
    private final String alias;
    private final URI logo;
    private final String description;

    public CategoryCreateCommand(long parentId, String name, String alias, String description, URI logo) {
        setParentId(parentId);
        this.name = Objects.requireNonNull(name, "name required").trim();
        this.alias = alias;
        this.logo = logo;
        this.description = description;
    }

    public long parentId() {
        return parentId;
    }

    private void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
    }

    public URI logo() {
        return logo;
    }

    public String description() {
        return description;
    }
}
