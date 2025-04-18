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
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-10-07
 */

public class BrandAboutChanged implements DomainEvent {
    private final URL logo;
    private final Year since;
    private final String story;
    private final URL homePage;
    private final long id;
    private final LocalDateTime occurredOn;
    private final int version;

    public BrandAboutChanged(long id, URL logo, URL homePage, Year since, String story) {
        super();
        this.id = id;
        this.logo = logo;
        this.homePage = homePage;
        this.since = since;
        this.story = story;
        this.version = 1;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrandAboutChanged)) return false;

        BrandAboutChanged that = (BrandAboutChanged) o;

        if (id != that.id) return false;
        if (version != that.version) return false;
        if (!Objects.equals(logo, that.logo)) return false;
        if (!Objects.equals(since, that.since)) return false;
        if (!Objects.equals(story, that.story)) return false;
        if (!Objects.equals(homePage, that.homePage)) return false;
        return Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        int result = logo != null ? logo.hashCode() : 0;
        result = 31 * result + (since != null ? since.hashCode() : 0);
        result = 31 * result + (story != null ? story.hashCode() : 0);
        result = 31 * result + (homePage != null ? homePage.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (occurredOn != null ? occurredOn.hashCode() : 0);
        result = 31 * result + version;
        return result;
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

    public URL url() {
        return homePage;
    }

    public long id() {
        return id;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public int version() {
        return version;
    }
}
