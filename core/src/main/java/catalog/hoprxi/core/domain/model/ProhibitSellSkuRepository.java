/*
 *  Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-06-21
 */

public interface ProhibitSellSkuRepository {
    /**
     * @param brandId
     * @return
     */
    ProhibitSellSku[] belongToBrand(String brandId);

    /**
     * @param categoryId
     * @return
     */
    ProhibitSellSku[] belongToCategory(String categoryId);

    /**
     * @param id
     * @return
     */
    ProhibitSellSku find(String id);

    /**
     * @param offset
     * @param limit
     * @return
     */
    ProhibitSellSku[] findAll(long offset, int limit);

    /**
     * @param id
     */
    void remove(String id);

    /**
     * @param sku
     */
    void save(ProhibitSellSku sku);

    /**
     * @return
     */
    long size();

    /**
     * @param barcode
     * @return
     */
    ProhibitSellSku[] fromBarcode(String barcode);

    /**
     * @param mnemonic
     * @return
     */
    ProhibitSellSku[] fromMnemonic(String mnemonic);

    /**
     * @param name
     * @return
     */
    ProhibitSellSku[] fromName(String name);
}
