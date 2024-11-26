/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.infrastructure.query.elasticsearch.SortField;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2024-11-17
 */
public interface BrandJsonQuery {

    String query(String id);

    /**
     * @param name      required(not NULL)
     * @param offset    specified start.
     * @param limit     specified size
     * @param sortField Null or empty will be replaced with {@link SortField}
     * @return
     */
    String queryByName(String name, int offset, int limit, SortField sortField);

    default String queryByName(String name, int offset, int limit) {
        return queryByName(name, offset, limit, SortField.ID_ASC);
    }


    default String queryAll(int size, String[] searchAfter, SortField sortField) {
        return "{\"total\":0}";
    }

    default String queryAll(int size) {
        return queryAll(size, new String[0], SortField.ID_ASC);
    }

    default String queryAll(int offset, int limit, SortField sortField) {
        return "{\"total\":0}";
    }
}
