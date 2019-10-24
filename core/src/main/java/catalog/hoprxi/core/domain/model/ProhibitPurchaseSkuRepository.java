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

import java.util.Collection;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-06-21
 */

public interface ProhibitPurchaseSkuRepository {
    /**
     * @param brandId
     * @return
     */
    ProhibitPurchaseSku[] belongingToBrand(long brandId);

    /**
     * @param categoryId
     * @return
     */
    ProhibitPurchaseSku[] belongingToCategory(long categoryId);

    /**
     * @param id
     * @return
     */
    ProhibitPurchaseSku find(long id);

    /**
     * @param low
     * @param high
     * @return
     */
    Collection<ProhibitPurchaseSku> findAll(int low, int high);

    /**
     * @param sku
     */
    void remove(ProhibitPurchaseSku sku);

    /**
     * @param sku
     */
    void save(ProhibitPurchaseSku sku);

    /**
     * @return
     */
    long size();

    /**
     * @param barcode
     * @return
     */
    Collection<ProhibitPurchaseSku> withBarcode(String barcode);

    /**
     * @param mnemonicCode
     * @return
     */
    Collection<ProhibitPurchaseSku> withMnemonicCode(String mnemonicCode);

    /**
     * @param name
     * @return
     */
    Collection<ProhibitPurchaseSku> withName(String name);
}
