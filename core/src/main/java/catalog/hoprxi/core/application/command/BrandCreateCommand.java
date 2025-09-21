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

package catalog.hoprxi.core.application.command;

import catalog.hoprxi.core.domain.model.brand.Brand;

import java.net.URL;
import java.time.Year;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 2019-05-14
 */
public class BrandCreateCommand implements Command<Brand> {
    private final String name;
    private final String alias;
    private final URL logo;
    private final Year since;
    private final String story;
    private final URL homepage;

    public BrandCreateCommand(String name, String alias, URL homepage, URL logo, Year since, String story) {
        this.name = name;
        this.alias = alias;
        this.logo = logo;
        this.since = since;
        this.story = story;
        this.homepage = homepage;
    }

    public String name() {
        return name;
    }

    public URL logo() {
        return logo;
    }

    public Year since() {
        return since;
    }

    public String story() {
        return story;
    }

    public URL homepage() {
        return homepage;
    }

    public String alias() {
        return alias;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BrandCreateCommand.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("alias='" + alias + "'")
                .add("logo=" + logo)
                .add("since=" + since)
                .add("story='" + story + "'")
                .add("homepage=" + homepage)
                .toString();
    }
}
