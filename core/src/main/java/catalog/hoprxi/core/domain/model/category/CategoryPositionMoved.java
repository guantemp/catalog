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

package catalog.hoprxi.core.domain.model.category;

import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/7
 */
public class CategoryPositionMoved implements DomainEvent {
    private final LocalDateTime occurredOn;
    private final String id;
    private final String movedId;
    private final int version;

    public CategoryPositionMoved(String id, String movedId) {
        this.id = id;
        this.movedId = movedId;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    @Override
    public LocalDateTime occurredOn() {
        return null;
    }

    @Override
    public int version() {
        return 0;
    }

    public String id() {
        return id;
    }

    public String movedId() {
        return movedId;
    }
}
