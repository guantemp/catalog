/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.persistence.postgresql;

import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.WeightRepository;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/1
 * @since JDK 21
 */

public class PsqlWeightRepository implements WeightRepository {
    /**
     * @param plu
     * @return
     */
    @Override
    public Weight find(Plu plu) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Plu nextPlu() {
        return null;
    }

    /**
     * @param plu
     */
    @Override
    public void delete(Plu plu) {

    }

    /**
     * @param weight
     */
    @Override
    public void save(Weight weight) {

    }

    /**
     * @param plu
     * @return
     */
    @Override
    public boolean isPluExists(Plu plu) {
        return false;
    }
}
