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

package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.model.price.UnitEnum;
import event.hoprxi.domain.model.DomainEvent;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 2022-10-18
 */
public class ItemVipPriceAdjusted implements DomainEvent {
    private long id;
    private String name;
    private MonetaryAmount amount;
    private UnitEnum unit;
    private LocalDateTime occurredOn;
    private int version;

    public ItemVipPriceAdjusted(long id, String name, MonetaryAmount amount, UnitEnum unit) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        occurredOn = LocalDateTime.now();
        version = 1;
    }

    public long id() {
        return id;
    }

    public MonetaryAmount amount() {
        return amount;
    }

    public UnitEnum unit() {
        return unit;
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
