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

import java.io.InputStream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/10/13
 */
public interface ItemQuery {
    InputStream find(long id);

    InputStream findByBarcode(String barcode);

    /**
     * @param filters
     * @param size
     * @param searchAfter
     * @param sortField
     * @return
     */
    InputStream search(ItemQueryFilter[] filters, int size, String searchAfter, SortField sortField);

    default InputStream search(ItemQueryFilter[] filters, int size) {
        return search(filters, size,"", SortField._ID);
    }

    default InputStream search(int size, String searchAfter, SortField sortField) {
        return search(new ItemQueryFilter[0], size, searchAfter, sortField);
    }

    default InputStream search(int size, SortField sortField) {
        return search(new ItemQueryFilter[0], size, "", sortField);
    }

    /**
     * @param filters
     * @param offset
     * @param size
     * @param sortField
     * @return
     */
    InputStream search(ItemQueryFilter[] filters, int offset, int size, SortField sortField);

    default InputStream search(int offset, int size, SortField sortField) {
        return search(new ItemQueryFilter[0], offset, size, sortField);
    }

    default InputStream search(int offset, int size) {
        return search(new ItemQueryFilter[0], offset, size, SortField._ID);
    }
}
