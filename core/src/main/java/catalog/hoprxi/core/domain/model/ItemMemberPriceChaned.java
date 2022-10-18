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

package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.model.price.Unit;
import event.hoprxi.domain.model.DomainEvent;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-11-11
 */
public class ItemMemberPriceChaned implements DomainEvent {
    private String id;
    private MonetaryAmount amount;
    private Unit unit;
    private String name;
    private LocalDateTime occurredOn;
    private int version;

    public ItemMemberPriceChaned(String id, String name, MonetaryAmount amount, Unit unit) {
        this.id = id;
        this.amount = amount;
        this.unit = unit;
        this.name = name;
        occurredOn = LocalDateTime.now();
        version = 1;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
