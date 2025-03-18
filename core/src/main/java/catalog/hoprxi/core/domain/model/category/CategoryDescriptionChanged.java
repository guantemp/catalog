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

import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2020-05-05
 */
public class CategoryDescriptionChanged implements DomainEvent {
    private final String description;
    private final LocalDateTime occurredOn;
    private final long id;
    private final int version;

    public CategoryDescriptionChanged(long id, String description) {
        this.description = description;
        this.id = id;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    public String description() {
        return description;
    }

    public long id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryDescriptionChanged)) return false;

        CategoryDescriptionChanged that = (CategoryDescriptionChanged) o;

        if (id != that.id) return false;
        if (version != that.version) return false;
        if (!Objects.equals(description, that.description)) return false;
        return Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (occurredOn != null ? occurredOn.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + version;
        return result;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public int version() {
        return version;
    }
}
