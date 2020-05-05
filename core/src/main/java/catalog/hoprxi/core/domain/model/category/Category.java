/*
 * Copyright (c) 2020. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.domain.DomainRegistry;
import catalog.hoprxi.core.domain.Validator;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import com.arangodb.entity.DocumentField;

import java.awt.image.BufferedImage;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2020-05-05
 */
public class Category {
    public static final Category UNDEFINED = new Category("undefined", "undefined", new Name(Label.CATEGORY_UNDEFINED, "undefined")) {
        @Override
        public void rename(Name newName) {
        }

        @Override
        public void changeDescription(String description) {
        }

        @Override
        public void changeMark(BufferedImage mark) {
        }

        @Override
        public void moveTo(String movedId) {
        }
    };
    private String description;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    private Name name;
    private String parentId;
    private static final int ID_MAX_LENGTH = 36;
    private static final int DESCRIPTION_MAX_LENGTH = 512;

    public Category(String parentId, String id, Name name) {
        this(parentId, id, name, null);
    }

    public Category(String parentId, String id, String name) {
        this(parentId, id, new Name(name), null);
    }


    /**
     * @param parentId
     * @param id
     * @param name
     * @param description
     * @throws IllegalArgumentException if parentId is null or length rang not in 1-36
     *                                  if id is null or length range not in [1-36]
     *                                  if name length range not in [1-256]
     *                                  if description not null and length range not in [0-512]
     */
    public Category(String parentId, String id, Name name, String description) {
        setIdAndParentId(parentId, id);
        setName(name);
        setDescription(description);
    }

    public Category(String parentId, String id, String name, String description) {
        this(parentId, id, new Name(name), null);
    }

    private void setDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH)
            throw new IllegalArgumentException("description length rang is 1-" + DESCRIPTION_MAX_LENGTH);
        this.description = description;
    }

    public static Category createCategoryRoot(String id, Name name) {
        return new Category(id, id, name);
    }

    public static Category createCategoryRoot(String id, String name) {
        return new Category(id, id, name);
    }

    public static Category createCategoryRoot(String id, Name name, String description) {
        return new Category(id, id, name, description);
    }

    public static Category createCategoryRoot(String id, String name, String description) {
        return new Category(id, id, name, description);
    }


    public String parentId() {
        return parentId;
    }

    private void setIdAndParentId(String parentId, String id) {
        setId(id);
        setParentId(parentId);
    }

    private void setParentId(String parentId) {
        parentId = Objects.requireNonNull(parentId, "parentId required").trim();
        if (parentId.isEmpty() || parentId.length() > ID_MAX_LENGTH)
            throw new IllegalArgumentException("parentId length rang is 1-" + ID_MAX_LENGTH + "char");
        if (!id.equals(parentId) && !Validator.isCategoryExist(parentId))
            throw new InvalidCategoryIdException("parent id not exist");
        this.parentId = parentId;
    }

    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        if (id.isEmpty() || id.length() > ID_MAX_LENGTH)
            throw new IllegalArgumentException("id length range is 1-" + ID_MAX_LENGTH);
        this.id = id;
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
    public String id() {
        return id;
    }

    /**
     * @return the value
     */
    public Name name() {
        return name;
    }

    public boolean isRoot() {
        return parentId.equals(id);
    }

    /**
     * @param newName
     */
    public void rename(Name newName) {
        if (!newName.equals(name)) {
            this.name = newName;
            DomainRegistry.domainEventPublisher().publish(new CategoryRenamed(id, newName));
        }
    }

    public void changeDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH)
            throw new IllegalArgumentException("description length rang is 1-" + DESCRIPTION_MAX_LENGTH);
        if ((this.description == null && description != null) || (this.description != null && !this.description.equals(description))) {
            this.description = description;
            DomainRegistry.domainEventPublisher().publish(new CategoryDescriptionChanged(id, description));
        }
    }

    public void changeMark(BufferedImage mark) {

    }

    public void moveTo(String movedId) {
        movedId = Objects.requireNonNull(movedId, "parentId required").trim();
        if (movedId.isEmpty() || movedId.length() > ID_MAX_LENGTH)
            throw new IllegalArgumentException("parentId length rang is 1-" + ID_MAX_LENGTH);
        if (!Validator.isCategoryExist(movedId))
            throw new InvalidCategoryIdException("parent id not exist");
        if (!id.equals(parentId) && !parentId.equals(movedId)) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;

        return id != null ? id.equals(category.id) : category.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Category{" +
                "description='" + description + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", parentId='" + parentId + '\'' +
                '}';
    }
}
