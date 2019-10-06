/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.domain.model.role;


import catalog.hoprxi.core.infrastructure.i18n.Label;
import com.arangodb.entity.DocumentField;

import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-09-02
 */
public class Role {
    public static final Role ANONYMOUS = new Role("anonymous", "anonymous", Label.PRICE_RETAIL);
    private String name;
    private String priceName;
    @DocumentField(DocumentField.Type.KEY)
    private String id;

    public Role(String id, String name, String priceName) {
        setId(id);
        setName(name);
        setPriceName(priceName);
    }

    private void setPriceName(String priceName) {
        this.priceName = Objects.requireNonNull(priceName, "priceName required");
    }

    private void setName(String name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        if (id.isEmpty() || id.length() > 36)
            throw new IllegalArgumentException("id length is 1 to 36");
        this.id = id;
    }

    public void rename(String newName) {
        newName = Objects.requireNonNull(newName, "newName required");
        if (!newName.equals(name))
            this.name = newName;
    }

    public void renamePrice(String newPriceName) {
        newPriceName = Objects.requireNonNull(newPriceName, "newName required");
        if (!newPriceName.equals(name))
            this.name = newPriceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        return id != null ? id.equals(role.id) : role.id == null;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String priceName() {
        return priceName;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Role.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("name='" + name + "'")
                .toString();
    }
}
