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

package catalog.hoprxi.core.domain.model.category;


import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-05-23
 */
public class CategoryRenamed implements DomainEvent {
    private String name;
    private LocalDateTime occurredOn;
    private String id;
    private int version;

    /**
     * @param id
     * @param name
     */
    public CategoryRenamed(String id, String name) {
        super();
        this.id = id;
        this.name = name;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CategoryRenamed that = (CategoryRenamed) o;

        if (version != that.version) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (occurredOn != null ? !occurredOn.equals(that.occurredOn) : that.occurredOn != null) return false;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (occurredOn != null ? occurredOn.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + version;
        return result;
    }

    /**
     * @return the name
     */
    public String name() {
        return name;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    /**
     * @return the id
     */
    public String id() {
        return id;
    }

    @Override
    public int version() {
        return version;
    }
}