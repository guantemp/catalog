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

public interface CategoryQuery {
    /**
     * @return category root node
     */
    InputStream root();

    /**
     * @param id category id
     * @return category where id is correct
     */
    InputStream find(long id) throws SearchException;

    InputStream children(long id);

    InputStream descendants(long id);

    InputStream search(String key, int offset, int limit);

    default InputStream search(String key) {
        return this.search(key, 0, 64);
    }


    InputStream searchSiblings(long id);

    InputStream path(long id);
}
