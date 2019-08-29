/*
 *  Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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


import event.foxtail.alpha.domain.model.DomainEvent;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-05-13
 */

public class BrandAboutChanged implements DomainEvent {
    private BufferedImage logo;
    private Year since;
    private String story;
    private URL homePage;
    private String id;
    private LocalDateTime occurredOn;
    private int version;

    public BrandAboutChanged(String id, BufferedImage logo, URL homePage, Year since, String story) {
        super();
        this.id = id;
        this.logo = logo;
        this.homePage = homePage;
        this.since = since;
        this.story = story;
        this.version = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrandAboutChanged that = (BrandAboutChanged) o;
        return version == that.version &&
                Objects.equals(id, that.id) &&
                Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, occurredOn, version);
    }

    public BufferedImage logo() {
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

    public String id() {
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
