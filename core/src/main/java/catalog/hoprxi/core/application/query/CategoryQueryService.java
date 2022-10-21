/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.view.CategoryView;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-20
 */
public interface CategoryQueryService {
    CategoryView[] root();

    /**
     * @param id
     * @return
     */
    CategoryView find(String id);

    /**
     * @param id
     * @return
     */
    CategoryView[] children(String id);

    /**
     * @param id
     * @return descendants
     */
    CategoryView[] descendants(String id);

    CategoryView[] searchName(String regularExpression);

    CategoryView[] siblings(String id);

    CategoryView[] path(String id);

    int depth(String id);
}
