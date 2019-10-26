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
package catalog.hoprxi.scale.domain.model;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.g builder 2019-05-04
 */

public interface WeightRepository {

    /**
     * @param brandId
     * @param offset
     * @param limit
     * @return
     */
    Weight[] belongingToBrand(String brandId, int offset, int limit);

    /**
     * @param categoryId
     * @param offset
     * @param limit
     * @return
     */
    Weight[] belongingToCategory(String categoryId, int offset, int limit);

    /**
     * @param id
     * @return
     */
    Weight find(String id);

    /**
     * @param offset
     * @param limit
     * @return
     */
    Weight[] findAll(int offset, int limit);

    /**
     * @return
     */
    String nextIdentity();

    /**
     * @return
     */
    int nextPlu();

    /**
     * @param id
     */
    void remove(String id);

    /**
     * @param weight
     */
    void save(Weight weight);

    /**
     * @param plu
     * @return
     */
    boolean isPluExists(int plu);

    /**
     * @return
     */
    int size();

    /**
     * @param mnemonic
     * @return
     */
    Weight[] fromMnemonic(String mnemonic);

    /**
     * @param name
     * @return
     */
    Weight[] fromName(String name);

    /**
     * @param plu
     * @return
     */
    Weight fromPlu(int plu);
}
