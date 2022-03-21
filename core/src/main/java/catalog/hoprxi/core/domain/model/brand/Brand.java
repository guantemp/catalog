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
package catalog.hoprxi.core.domain.model.brand;


import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.util.DomainRegistry;
import com.arangodb.entity.DocumentField;

import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.3 builder 2019-10-07
 */
public class Brand {
    public static final Brand UNDEFINED = new Brand("undefined", new Name(Label.BRAND_UNDEFINED, "undefined")) {
        @Override
        public void rename(Name newName) {
        }

        @Override
        public void changeAbout(AboutBrand newAbout) {
        }
    };
    private static final int ID_MAX_LENGTH = 36;
    private AboutBrand about;
    @DocumentField(DocumentField.Type.KEY)
    private String id;
    private Name name;

    /**
     * @param id
     * @param name
     */
    public Brand(String id, Name name) {
        this(id, name, null);
    }

    public Brand(String name) {
        this(name, new Name(name), null);
    }

    public Brand(String id, String name) {
        this(id, new Name(name), null);
    }

    /**
     * @param id
     * @param name
     * @param about
     */
    public Brand(String id, Name name, AboutBrand about) {
        setId(id);
        setName(name);
        this.about = about;
    }

    public void changeAbout(AboutBrand newAbout) {
        if ((about == null && newAbout != null) || (about != null && !about.equals(newAbout))) {
            this.about = newAbout;
            DomainRegistry.domainEventPublisher().publish(new BrandAboutChanged(id, about.logo(), about.homepage(), about.since(), about.story()));
        }
    }

    private void setId(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        if (id.isEmpty() || id.length() > ID_MAX_LENGTH)
            throw new IllegalArgumentException("id length is 1-" + ID_MAX_LENGTH);
        this.id = id;
    }

    public AboutBrand about() {
        return about;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Brand brand = (Brand) o;

        return Objects.equals(id, brand.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String id() {
        return id;
    }

    public Name name() {
        return name;
    }

    public void rename(Name newName) {
        Objects.requireNonNull(newName, "newName required");
        if (!newName.equals(name)) {
            this.name = newName;
            DomainRegistry.domainEventPublisher().publish(new BrandRenamed(id, newName.name(), newName.mnemonic(), newName.alias()));
        }
    }

    private void setName(Name name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Brand.class.getSimpleName() + "[", "]")
                .add("about=" + about)
                .add("id='" + id + "'")
                .add("name=" + name)
                .toString();
    }
}
