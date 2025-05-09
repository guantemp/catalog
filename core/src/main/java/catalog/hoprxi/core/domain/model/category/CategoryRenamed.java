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


import catalog.hoprxi.core.domain.model.Name;
import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2025-03-18
 */
public class CategoryRenamed implements DomainEvent {
    private final Name name;
    private final LocalDateTime occurredOn;
    private final long id;
    private final int version;

    /**
     * @param id
     * @param name
     */
    public CategoryRenamed(long id, Name name) {
        this.id = id;
        this.name = name;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryRenamed)) return false;

        CategoryRenamed that = (CategoryRenamed) o;

        if (id != that.id) return false;
        if (version != that.version) return false;
        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (occurredOn != null ? occurredOn.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + version;
        return result;
    }

    /**
     * @return the name
     */
    public Name name() {
        return name;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    /**
     * @return the id
     */
    public long id() {
        return id;
    }

    @Override
    public int version() {
        return version;
    }
}