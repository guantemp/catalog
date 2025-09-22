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

package catalog.hoprxi.core.domain.model.category;


import java.net.URL;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/22
 */

public class CategoryCreated {
    private final long parentId;
    private final long id;
    private final String name;
    private final String alias;
    private final URL logo;
    private final String description;

    public CategoryCreated(long parentId, long id, String name, String alias, URL logo, String description) {
        this.parentId = parentId;
        this.id = id;
        this.name = name;
        this.alias = alias;
        this.logo = logo;
        this.description = description;
    }
}
