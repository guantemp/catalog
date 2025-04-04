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
 * @version 0.0.2 builder 2024-11-17
 */
public interface BrandJsonQuery {

    /**
     * @param id
     * @return empty if not find
     */
    default String query(long id) {
        return "";
    }

    /**
     * @param name      required(not NULL)
     * @param offset    specified start.
     * @param limit     specified size
     * @param sortField Null or empty will be replaced with {@link SortField#_ID}
     * @return
     * @throw IllegalArgumentException name is null,10000< offset >0,0< limit >10000 10000,offset+limit>10000
     */
    String query(String name, int offset, int limit, SortField sortField);

    /**
     * @param name
     * @param offset
     * @param limit
     * @return
     * @see #query(String, int, int, SortField)
     */
    default String query(String name, int offset, int limit) {
        return query(name, offset, limit, SortField.ID);
    }


    /**
     * @param size
     * @param searchAfter
     * @param sortField
     * @return
     */
    String query(int size, String searchAfter, SortField sortField);

    /**
     * @param offset
     * @param limit
     * @param sortField
     * @return
     */
    String query(int offset, int limit, SortField sortField);
}
