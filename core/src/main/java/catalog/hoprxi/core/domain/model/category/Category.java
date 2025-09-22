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
package catalog.hoprxi.core.domain.model.category;

import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.CategoryValidatorService;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.util.DomainRegistry;

import java.net.URL;
import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.3 builder 2022-08-09
 */
public class Category {
    public static final Category UNDEFINED = new Category(-1L, -1L, new Name(Label.CATEGORY_UNDEFINED, "undefined"), "undefined category") {
        @Override
        public void rename(String newName, String newAlias) {
            throw new UnsupportedOperationException("");
        }

        @Override
        public void changeDescription(String description) {
            throw new UnsupportedOperationException("");
        }

        @Override
        public void changeIcon(URL icon) {
            throw new UnsupportedOperationException("");
        }

        @Override
        public void moveTo(long movedId) {
            throw new UnsupportedOperationException("");
        }
    };
    private static final int ID_MAX_LENGTH = 48;
    private static final int DESCRIPTION_MAX_LENGTH = 512;
    private String description;
    private long id;
    private Name name;
    private long parentId;
    private URL icon;

    public Category(long parentId, long id, Name name) {
        this(parentId, id, name, null);
    }

    public Category(long parentId, long id, String name) {
        this(parentId, id, Name.valueOf(name), null);
    }

    public Category(long parentId, long id, Name name, String description) {
        this(parentId, id, name, description, null);
    }

    public Category(long parentId, long id, String name, String description) {
        this(parentId, id, new Name(name), description);
    }

    public Category(long parentId, long id, String name, String description, URL icon) {
        this(parentId, id, Name.valueOf(name), description, icon);
    }

    /**
     * @param parentId
     * @param id
     * @param name
     * @param description
     * @param icon
     * @throws IllegalArgumentException   if length rang not in 1-36
     *                                    if name length range not in [1-256]
     *                                    if description not null and length range not in [0-512]
     * @throws InvalidCategoryIdException if parentId  not exists
     */
    public Category(long parentId, long id, Name name, String description, URL icon) {
        setIdAndParentId(parentId, id);
        setName(name);
        setDescription(description);
        this.icon = icon;
    }


    public static Category root(long id, Name name) {
        return new Category(id, id, name);
    }

    public static Category root(long id, String name) {
        return new Category(id, id, name);
    }

    public static Category root(long id, Name name, String description) {
        return new Category(id, id, name, description);
    }

    public static Category root(long id, String name, String description) {
        return new Category(id, id, name, description);
    }

    public static Category root(long id, Name name, String description, URL icon) {
        return new Category(id, id, name, description, icon);
    }

    private void setDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH)
            throw new IllegalArgumentException("description length rang is 0-" + DESCRIPTION_MAX_LENGTH);
        this.description = description;
    }

    private void setIdAndParentId(long parentId, long id) {
        if (id != parentId && CategoryValidatorService.isCategoryExist(parentId))
            throw new InvalidCategoryIdException("parent id not exist");
        this.id = id;
        this.parentId = parentId;
    }

    public long parentId() {
        return parentId;
    }

    /**
     * @return the description
     */
    public String description() {
        return description;
    }

    /**
     * @return the id
     */
    public long id() {
        return id;
    }

    /**
     * @return the value
     */
    public Name name() {
        return name;
    }

    public URL icon() {
        return icon;
    }

    public boolean isRoot() {
        return parentId == id;
    }

    public void rename(String newName, String newAlias) {
        Name temp = name.rename(newName, newAlias);
        if (temp != name) {
            name = temp;
            DomainRegistry.domainEventPublisher().publish(new CategoryRenamed(id, name));
        }
    }

    public void changeDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH)
            throw new IllegalArgumentException("description length rang is 1-" + DESCRIPTION_MAX_LENGTH);
        if ((description == null && this.description != null) || (description != null && !description.equals(this.description))) {
            this.description = description;
            DomainRegistry.domainEventPublisher().publish(new CategoryDescriptionChanged(id, description));
        }
    }

    /**
     * @param icon
     */
    public void changeIcon(URL icon) {
        if ((icon == null && this.icon != null) || (icon != null && !icon.equals(this.icon))) {
            this.icon = icon;
            DomainRegistry.domainEventPublisher().publish(new CategoryIconChanged(id, icon));
        }
    }

    public void moveTo(long movedId) {
        if (movedId == UNDEFINED.id)
            throw new IllegalArgumentException("Undefined classes do not allow subcategories");
        if (CategoryValidatorService.isCategoryExist(movedId))
            throw new InvalidCategoryIdException("move to id not exist");
        if (CategoryValidatorService.isCurrentCategoryDescendant(id, movedId))
            throw new InvalidCategoryIdException("Can’t move to its descendant node");
        if (movedId != UNDEFINED.id && id != parentId && parentId != movedId) {
            this.parentId = movedId;
            //DomainRegistry.domainEventPublisher().publish(new CategoryRenamed(id, description));
        }
    }

    /**
     * @param name
     */
    protected void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required.");
    }


    public CategoryView toView() {
        return new CategoryView(parentId, id, name, description, icon, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;

        Category category = (Category) o;

        return id == category.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Category.class.getSimpleName() + "[", "]")
                .add("parentId=" + parentId)
                .add("id=" + id)
                .add("name=" + name)
                .add("icon=" + icon)
                .add("description='" + description + "'")
                .toString();
    }
}
