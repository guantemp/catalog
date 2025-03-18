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

import java.net.URI;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-06-24
 */
public class CategoryCreateCommand implements Command {
    private long parentId;
    private String name;
    private String alias;
    private URI logo;
    private String description;

    public CategoryCreateCommand(long parentId, String name, String alias, String description, URI logo) {
        this.parentId = parentId;
        this.name = Objects.requireNonNull(name, "name required").trim();
        this.alias = alias;
        this.logo = logo;
        this.description = description;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public URI getLogo() {
        return logo;
    }

    public void setLogo(URI logo) {
        this.logo = logo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryCreateCommand)) return false;

        CategoryCreateCommand that = (CategoryCreateCommand) o;

        if (parentId != that.parentId) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(alias, that.alias)) return false;
        if (!Objects.equals(logo, that.logo)) return false;
        return Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        int result = (int) (parentId ^ (parentId >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (logo != null ? logo.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
