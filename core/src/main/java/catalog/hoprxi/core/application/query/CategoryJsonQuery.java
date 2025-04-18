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
 * @version 0.0.1 builder 2024-07-14
 */
public interface CategoryJsonQuery {
    /**
     * @param id of category
     * @return
     */
    String query(long id);

    /**
     * @return all root node or "" if no root node
     */
    String root();

    /**
     * @param id of category
     * @return child node or "" if there are no child nodes
     */
    String queryChildren(long id);

    /**
     * @param id of category
     * @return descendant node or "" if there are no descendant nodes
     */
    String queryDescendant(long id);

    /**
     * @param name
     * @return
     */
    String queryByName(String name);

    /**
     * @param id of category
     * @return
     */
    String path(long id);
}
