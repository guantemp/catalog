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

package catalog.hoprxi.core.application.command;


import catalog.hoprxi.core.domain.model.brand.Brand;

import java.net.URL;
import java.time.Year;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/11
 */

public class BrandUpdateCommand implements Command<Brand> {
    private long id;
    private String name;
    private String alias;
    private URL logo;
    private Year since;
    private String story;
    private URL homepage;

    public BrandUpdateCommand(long id) {
        setId(id);
    }

    public BrandUpdateCommand(long id, String name, String alias, URL logo, Year since, String story, URL homepage) {
        setId(id);
        this.name = name;
        this.alias = alias;
        this.logo = logo;
        this.since = since;
        this.story = story;
        this.homepage = homepage;
    }

    private void setId(long id) {
        if (id < 0) throw new IllegalArgumentException("id is ");
        this.id = id;
    }

    public void setName(String name, String alias) {
        this.name = name;
    }


    public void setAbout(URL logo, URL homepage, Year since, String story) {
        this.logo = logo;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
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
}
