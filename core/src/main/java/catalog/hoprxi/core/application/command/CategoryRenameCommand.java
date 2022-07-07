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
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-07-04
 */
public class CategoryRenameCommand implements Command {
    private String id;
    private String name;
    private String alias;

    public CategoryRenameCommand(String id, String name, String alias) {
        setId(id);
        setName(name);
        this.alias = alias;
    }

    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "id required").trim();
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name required").trim();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryRenameCommand)) return false;

        CategoryRenameCommand that = (CategoryRenameCommand) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CategoryRenameCommand.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("name='" + name + "'")
                .add("alias='" + alias + "'")
                .toString();
    }
}
