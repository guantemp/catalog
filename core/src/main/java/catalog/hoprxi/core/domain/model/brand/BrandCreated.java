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

package catalog.hoprxi.core.domain.model.brand;

import event.hoprxi.domain.model.DomainEvent;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025-09-17
 */
public class BrandCreated implements DomainEvent {
    private final URL logo;
    private final Year since;
    private final String story;
    private final URL homePage;
    private final String name;
    private final String alias;
    private final String mnemonic;
    private final long id;
    private final LocalDateTime occurredOn;
    private final int version;

    public BrandCreated(long id, String name, String mnemonic, String alias, URL homePage, URL logo, Year since, String story) {
        this.logo = logo;
        this.since = since;
        this.story = story;
        this.homePage = homePage;
        this.name = name;
        this.alias = alias;
        this.mnemonic = mnemonic;
        this.id = id;
        this.version = 1;
        this.occurredOn = LocalDateTime.now();
    }


    @Override
    public LocalDateTime occurredOn() {
        return null;
    }

    @Override
    public int version() {
        return DomainEvent.super.version();
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

    public URL homePage() {
        return homePage;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
    }

    public String mnemonic() {
        return mnemonic;
    }

    public long id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BrandCreated that = (BrandCreated) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BrandCreated.class.getSimpleName() + "[", "]")
                .add("logo=" + logo)
                .add("since=" + since)
                .add("story='" + story + "'")
                .add("homePage=" + homePage)
                .add("name='" + name + "'")
                .add("alias='" + alias + "'")
                .add("mnemonic='" + mnemonic + "'")
                .add("id=" + id)
                .add("occurredOn=" + occurredOn)
                .add("version=" + version)
                .toString();
    }
}
