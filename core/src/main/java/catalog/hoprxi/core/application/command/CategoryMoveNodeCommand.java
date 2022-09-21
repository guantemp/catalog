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

package catalog.hoprxi.core.application.command;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-07-04
 */
public class CategoryMoveNodeCommand implements Command {
    private String id;
    private String parentId;

    public CategoryMoveNodeCommand(String id, String parentId) {
        setId(id);
        setParentId(parentId);
    }

    private void setId(String id) {
        this.id = Objects.requireNonNull(id, "id required").trim();
    }

    private void setParentId(String parentId) {
        this.parentId = Objects.requireNonNull(parentId, "parentId required").trim();
    }

    public String id() {
        return id;
    }

    public String parentId() {
        return parentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryMoveNodeCommand)) return false;

        CategoryMoveNodeCommand that = (CategoryMoveNodeCommand) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
