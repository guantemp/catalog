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

package catalog.hoprxi.core.application.query;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-07-28
 */
public interface ItemJsonQuery {
    /**
     * @param id of item
     * @return product value,EMPTY will return if not find
     */
    String query(long id);

    /**
     * @param barcode
     * @return
     */
    String queryByBarcode(String barcode);

    /**
     * @param key
     * @param filters
     * @param size
     * @param searchAfter
     * @param sortField
     * @return
     * @throws IllegalArgumentException if size<=0 or size >10000
     */
    String query(String key, ItemQueryFilter[] filters, int size, String searchAfter, SortField sortField);

    default String query(ItemQueryFilter[] filters, int size, String searchAfter, SortField sortField) {
        return query(null, filters, size, searchAfter, sortField);
    }

    default String query(int size, String searchAfter, SortField sortField) {
        return query(new ItemQueryFilter[0], size, searchAfter, sortField);
    }

    default String query(String key, int size, String searchAfter, SortField sortField) {
        return query(key, new ItemQueryFilter[0], size, searchAfter, sortField);
    }

    String query(String key, ItemQueryFilter[] filters, int from, int size, SortField sortField);

    default String query(ItemQueryFilter[] filters, int from, int size, SortField sortField) {
        return query(null, filters, from, size, sortField);
    }

    default String query(int from, int size, SortField sortField) {
        return query(null, new ItemQueryFilter[0], from, size, sortField);
    }

    default String query(int from, int size) {
        return query(null, new ItemQueryFilter[0], from, size, SortField._ID);
    }
}
