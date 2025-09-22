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

package catalog.hoprxi.core.application.view;

import catalog.hoprxi.core.domain.model.Name;

import java.net.URL;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-03-20
 */
public class CategoryView {
    private static final String PLACEHOLDER = "placeholder";
    private final long id;
    private final Name name;
    private String description;
    private long parentId;
    private URL icon;
    private boolean isLeaf;

    public CategoryView(long parentId, long id, Name name, String description, URL icon, boolean isLeaf) {
        this.id = id;
        this.description = description;
        this.name = name;
        this.parentId = parentId;
        this.icon = icon;
        this.isLeaf = isLeaf;
    }

    public static CategoryView identifiableCategoryView(long id) {
        return new CategoryView(id, id, Name.valueOf(PLACEHOLDER), null, null, true);
    }

    public boolean isRoot() {
        return id == parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public URL getIcon() {
        return icon;
    }

    public void setIcon(URL icon) {
        this.icon = icon;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryView)) return false;

        CategoryView view = (CategoryView) o;

        return id == view.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CategoryView.class.getSimpleName() + "[", "]")
                .add("parentId='" + parentId + "'")
                .add("id='" + id + "'")
                .add("name=" + name)
                .add("description='" + description + "'")
                .add("icon=" + icon)
                .add("isLeaf=" + isLeaf)
                .toString();
    }
}
