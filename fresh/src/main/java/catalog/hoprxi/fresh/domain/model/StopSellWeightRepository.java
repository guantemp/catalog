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
package catalog.hoprxi.fresh.domain.model;

public interface StopSellWeightRepository {
    /**
     * @param brandId
     * @return
     */
    StopSellWeight[] belongingToBrand(long brandId);

    /**
     * @param categoryId
     * @return
     */
    StopSellWeight[] belongingToCategory(long categoryId);

    /**
     * @param id
     * @return
     */
    StopSellWeight[] find(int id);

    /**
     * @param low
     * @param high
     * @return
     */
    StopSellWeight[] findAll(int low, int high);

    /**
     * @param id
     */
    void remove(String id);

    /**
     * @param stopSellWeight
     */
    void save(StopSellWeight stopSellWeight);

    /**
     * @return
     */
    int size();

    /**
     * @param mnemonicCode
     * @return
     */
    StopSellWeight[] withMnemonicCode(String mnemonicCode);

    /**
     * @param name
     * @return
     */
    StopSellWeight[] withName(String name);
}
