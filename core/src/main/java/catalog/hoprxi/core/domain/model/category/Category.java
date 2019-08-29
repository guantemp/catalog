/*
 * Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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
import catalog.hoprxi.core.domain.model.category.spec.SpecificationFamily;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import com.arangodb.entity.DocumentField;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2018-05-23
 */
public class Category {
    public static final Category UNDEFINED = new Category("undefined", "undefined", Label.CATEGORY_UNDEFINED) {
        @Override
        public void rename(String newName) {
        }

        @Override
        public void changeDescription(String description) {
        }

        @Override
        public void changeMark(BufferedImage mark) {
        }

        @Override
        public void moveTo(String parentId) {
        }
    };
    private String description;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    private BufferedImage mark;
    private String name;
    private String parentId;
    private List<SpecificationFamily> specificationFamily;
    private static final int NAME_MAX_LENGTH = 256;
    private static final int ID_MAX_LENGTH = 36;
    private static final int DESCRIPTION_MAX_LENGTH = 512;

    public Category(String parentId, String id, String name) {
        this(parentId, id, name, null, null);
    }

    /**
     * @see catalog.hoprxi.core.domain.model.category
     */
    public Category(String parentId, String id, String name, String description) {
        this(parentId, id, name, description, null);
    }

    /**
     * @param parentId
     * @param id
     * @param name
     * @param description
     * @param mark
     * @throws IllegalArgumentException if parentId is null or rang not in 1-36
     *                                  if id is null or range not in [1-36]
     *                                  if name length range not in [1-256]
     *                                  if description not null and length range not in [0-512]
     */
    public Category(String parentId, String id, String name, String description, BufferedImage mark) {
        setIdAndParentId(parentId, id);
        setName(name);
        setDescription(description);
        this.mark = mark;
    }

    private void setDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH)
            throw new IllegalArgumentException("description length rang is 1-" + DESCRIPTION_MAX_LENGTH);
        this.description = description;
    }

    public static Category createRootCategory(String id, String name) {
        return new Category(id, id, name);
    }

    public static Category createRootCategory(String id, String name, String description, BufferedImage mark) {
        return new Category(id, id, name, description, mark);
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
        if (!id.equals(parentId) && !Validator.isCategoryIdExist(parentId))
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
     * @return the mark
     */
    public BufferedImage mark() {
        return mark;
    }

    /**
     * @return the value
     */
    public String name() {
        return name;
    }

    public boolean isRoot() {
        return parentId.equals(id);
    }

    /**
     * @param newName
     */
    public void rename(String newName) {
        newName = Objects.requireNonNull(newName, "newName required").trim();
        if (newName.length() > NAME_MAX_LENGTH)
            throw new IllegalArgumentException(" newName length is 1-" + NAME_MAX_LENGTH);
        if (!newName.equals(name)) {
            this.name = newName;
            DomainRegistry.domainEventPublisher().publish(new CategoryRenamed(id, newName));
        }
    }

    public void changeDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH)
            throw new IllegalArgumentException("description length rang is 1-" + NAME_MAX_LENGTH);
        if ((this.description == null && description != null) || (this.description != null && !this.description.equals(description))) {
            this.description = description;
            DomainRegistry.domainEventPublisher().publish(new CategoryRenamed(id, description));
        }
    }

    public void changeMark(BufferedImage mark) {

    }

    public void moveTo(String parentId) {
        parentId = Objects.requireNonNull(parentId, "parentId required").trim();
        if (parentId.isEmpty() || parentId.length() > ID_MAX_LENGTH)
            throw new IllegalArgumentException("parentId length rang is 1-" + ID_MAX_LENGTH);
        if (!Validator.isCategoryIdExist(parentId))
            throw new InvalidCategoryIdException("parent id not exist");
        if (!this.parentId.equals(parentId)) {
            this.parentId = parentId;
            //DomainRegistry.domainEventPublisher().publish(new CategoryRenamed(id, description));
        }
    }

    /**
     * @param name
     */
    protected void setName(String name) {
        name = Objects.requireNonNull(name, "name required.").trim();
        if (name.length() > NAME_MAX_LENGTH)
            throw new IllegalArgumentException("name length is 1-256");
        this.name = name;
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
                ", mark=" + mark +
                ", name='" + name + '\'' +
                ", parentId='" + parentId + '\'' +
                '}';
    }
}
