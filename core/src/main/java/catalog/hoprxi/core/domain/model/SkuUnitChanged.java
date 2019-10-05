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

package catalog.hoprxi.core.domain.model;

import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-05-02
 */
public class SkuUnitChanged implements DomainEvent {
    private String id;
    private Unit unit;
    private LocalDateTime occurredOn;
    private int version;

    public SkuUnitChanged(String id, Unit unit) {
        this.id = id;
        this.unit = unit;
        occurredOn = LocalDateTime.now();
        version = 1;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public int version() {
        return version;
    }

    public String id() {
        return id;
    }

    public Unit unit() {
        return unit;
    }
}
