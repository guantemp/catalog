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

package catalog.hoprxi.core.application.command;


import catalog.hoprxi.core.infrastructure.i18n.Label;

import javax.money.MonetaryAmount;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/12/8
 */

public class ItemMemberPriceAdjustedCommand implements Command{
    private final long id;
    private final MonetaryAmount amount;
    private final String unit;
    private final String name;

    public ItemMemberPriceAdjustedCommand(long id,  String name,MonetaryAmount amount, String unit) {
        this.id = id;
        this.amount = amount;
        this.unit = unit;
        this.name = name;
    }
    public ItemMemberPriceAdjustedCommand(long id,  MonetaryAmount amount, String unit) {
        this.id = id;
        this.amount = amount;
        this.unit = unit;
        this.name = Label.PRICE_MEMBER;
    }


    public long id() {
        return id;
    }

    public MonetaryAmount amount() {
        return amount;
    }

    public String unit() {
        return unit;
    }

    public String name() {
        return name;
    }
}
