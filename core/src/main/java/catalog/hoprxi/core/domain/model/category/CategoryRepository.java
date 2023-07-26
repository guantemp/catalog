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
package catalog.hoprxi.core.domain.model.category;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2022-04-27
 */
public interface CategoryRepository {

    /**
     * @param id
     * @return
     */
    Category find(String id);

    /**
     * @return
     */
    String nextIdentity();

    /**
     * Delete the current category exclude if it's has not subclasses
     *
     * @param id
     */
    void remove(String id);

    /**
     * @return category root
     */
    Category[] root();

    /**
     * @param category
     */
    void save(Category category);
}
