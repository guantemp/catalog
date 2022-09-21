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
package catalog.hoprxi.core.domain.model.brand;

import java.net.URL;
import java.time.Year;
import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.3 builder 2019-05-16
 */
public class AboutBrand {
    private URL logo;
    private Year since;
    private String story;
    private URL homepage;

    public AboutBrand(URL homepage, URL logo, Year since, String story) {
        this.logo = logo;
        this.homepage = homepage;
        this.since = since;
        this.story = story;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", AboutBrand.class.getSimpleName() + "[", "]")
                .add("logo=" + logo)
                .add("since=" + since)
                .add("story='" + story + "'")
                .add("homepage=" + homepage)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AboutBrand that = (AboutBrand) o;

        if (!Objects.equals(logo, that.logo)) return false;
        if (!Objects.equals(since, that.since)) return false;
        if (!Objects.equals(story, that.story)) return false;
        return Objects.equals(homepage, that.homepage);
    }

    @Override
    public int hashCode() {
        int result = logo != null ? logo.hashCode() : 0;
        result = 31 * result + (since != null ? since.hashCode() : 0);
        result = 31 * result + (story != null ? story.hashCode() : 0);
        result = 31 * result + (homepage != null ? homepage.hashCode() : 0);
        return result;
    }

    /**
     * @return the homepage
     */
    public URL homepage() {
        return homepage;
    }
}
