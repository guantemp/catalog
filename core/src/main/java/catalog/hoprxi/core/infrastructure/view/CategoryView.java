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

package catalog.hoprxi.core.infrastructure.view;

import catalog.hoprxi.core.domain.model.Name;

import java.net.URI;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-03-20
 */
public class CategoryView {
    private String description;
    private String id;
    private Name name;
    private String parentId;
    private URI icon;
    private boolean isLeaf;
    private boolean hasSibling;

    public CategoryView(String parentId, String id, Name name, String description, boolean isLeaf, boolean hasSibling, URI icon) {
        this.description = description;
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.icon = icon;
        this.isLeaf = isLeaf;
        this.hasSibling = hasSibling;
    }

    public CategoryView(String parentId, String id, Name name) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public CategoryView(String description, String id, Name name, String parentId, boolean isLeaf, boolean hasSibling) {
        this.description = description;
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.isLeaf = isLeaf;
        this.hasSibling = hasSibling;
    }

    public boolean isRoot() {
        return parentId.equals(id);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public URI getIcon() {
        return icon;
    }

    public void setIcon(URI icon) {
        this.icon = icon;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public boolean isHasSibling() {
        return hasSibling;
    }

    public void setHasSibling(boolean hasSibling) {
        this.hasSibling = hasSibling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryView)) return false;

        CategoryView that = (CategoryView) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
