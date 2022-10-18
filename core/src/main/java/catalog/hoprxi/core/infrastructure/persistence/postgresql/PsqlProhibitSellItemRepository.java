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

package catalog.hoprxi.core.infrastructure.persistence.postgresql;

import catalog.hoprxi.core.domain.model.*;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-18
 */
public class PsqlProhibitSellItemRepository implements ProhibitSellItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlProhibitSellItemRepository.class);
    private static Constructor<Name> nameConstructor;
    private static Constructor<ProhibitSellItem> prohibitSellItemConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class);
            prohibitSellItemConstructor = ProhibitSellItem.class.getDeclaredConstructor(String.class, Barcode.class, Name.class, MadeIn.class, Unit.class, Specification.class,
                    Grade.class, ShelfLife.class, String.class, String.class);

        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not find has such constructor", e);
        }
    }

    @Override
    public ProhibitSellItem find(String id) {
        return null;
    }

    @Override
    public void remove(String id) {

    }

    @Override
    public void save(ProhibitSellItem item) {

    }
}
