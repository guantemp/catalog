/*
 * Copyright (c) 2020. www.hoprxi.com All Rights Reserved.
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
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-06-04
 */
public interface ItemRepository {
    /**
     * @param brandId
     * @return
     */
    Item[] belongToBrand(String brandId, int offset, int limit);

    /**
     * @param categoryId
     * @return
     */
    Item[] belongToCategory(String categoryId, long offset, int limit);

    /**
     * @param id
     * @return
     */
    Item find(String id);

    /**
     * @param offset
     * @param limit
     * @return
     */
    Item[] findAll(long offset, int limit);

    /**
     * @return
     */
    String nextIdentity();

    /**
     * @param id
     */
    void remove(String id);

    /**
     * @param item
     */
    void save(Item item);

    /**
     * @return
     */
    long size();

    /**
     * @param barcode is support regular
     * @return
     */
    Item[] fromBarcode(String barcode);

    /**
     * @param mnemonic is support regular
     * @return
     */
    Item[] fromMnemonic(String mnemonic);

    /**
     * @param name is support regular
     * @return
     */
    Item[] fromName(String name);
}
