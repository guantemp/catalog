/*
 *  Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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
import event.foxtail.alpha.domain.model.DomainEvent;

import java.time.LocalDateTime;


/**
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.0.2 builder 2019-05-15
 * @since JDK8.0
 */
public class BrandRenamed implements DomainEvent {
    private Name newName;
    private LocalDateTime occurredOn;
    private String id;
    private int version;

    /**
     * @param id
     * @param newName
     */
    public BrandRenamed(String id, Name newName) {
        super();
        this.id = id;
        this.newName = newName;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrandRenamed that = (BrandRenamed) o;

        if (version != that.version) return false;
        if (newName != null ? !newName.equals(that.newName) : that.newName != null) return false;
        if (occurredOn != null ? !occurredOn.equals(that.occurredOn) : that.occurredOn != null) return false;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        int result = newName != null ? newName.hashCode() : 0;
        result = 31 * result + (occurredOn != null ? occurredOn.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + version;
        return result;
    }

    /**
     * @return the newName
     */
    public Name newName() {
        return newName;
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
