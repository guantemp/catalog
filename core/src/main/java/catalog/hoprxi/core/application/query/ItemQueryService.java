/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.application.query;

import catalog.hoprxi.core.application.view.ItemView;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-10-15
 */
public interface ItemQueryService {
    /**
     * @param id
     * @return
     */
    ItemView query(String id);

    /**
     * @param brandId
     * @return
     */
    ItemView[] belongToBrand(String brandId, int offset, int limit);

    /**
     * @param categoryId
     * @return
     */
    ItemView[] belongToCategory(String categoryId, long offset, int limit);

    /**
     * @param categoryId
     * @return all item belong current category and descendants category
     */
    ItemView[] belongToCategoryAndDescendants(String categoryId);

    /**
     * @param offset
     * @param limit
     * @return
     */
    ItemView[] queryAll(long offset, int limit);

    /**
     * @return
     */
    long size();

    /**
     * @param barcode is support regular
     * @return
     */
    ItemView[] queryByBarcode(String barcode);

    ItemView[] accurateQueryByBarcode(String barcode);

    /**
     * @param regularExpression is support regular,will search Name(name,alias,mnemonic),barcode
     * @return matched result
     */
    ItemView[] serach(String regularExpression);

    ItemView[] serach(String regularExpression, long offset, int limit);
}
