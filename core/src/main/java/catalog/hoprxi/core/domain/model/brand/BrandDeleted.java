/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-01-07
 */
public class BrandDeleted implements DomainEvent {
    private final String id;
    private final LocalDateTime occurredOn;
    private final int version;

    public BrandDeleted(String id) {
        this.id = Objects.requireNonNull(id, "id is required").trim();
        this.version = 1;
        this.occurredOn = LocalDateTime.now();
    }

    public BrandDeleted(String id, LocalDateTime occurredOn, int version) {
        this.id = Objects.requireNonNull(id, "id is required").trim();
        this.occurredOn = Objects.requireNonNull(occurredOn, "occurredOn is required");
        this.version = Objects.requireNonNull(version, "version is required");
    }

    public String id() {
        return id;
    }

    @Override
    public int version() {
        return version();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
