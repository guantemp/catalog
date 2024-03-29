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

package catalog.hoprxi.show.domain.model;

import salt.hoprxi.to.PinYin;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2020-01-11
 */
public class Name {
    private String name;
    private String mnemonic;
    private String keywords;

    private Name(String name, String mnemonic, String keywords) {
        setName(name);
        this.mnemonic = mnemonic;
        this.keywords = keywords;
    }

    public Name(String name, String keywords) {
        setName(name);
        this.mnemonic = PinYin.toShortPinYing(this.name);
        setKeywords(keywords);
    }

    public Name(String name) {
        this(name, name);
    }

    private void setName(String name) {
        name = Objects.requireNonNull(name, "name required").trim();
        if (name.isEmpty() || name.length() > 256)
            throw new IllegalArgumentException("name length rang is 1-256");
        this.name = name;
    }

    private void setKeywords(String keywords) {
        if (keywords == null)
            keywords = name;
        this.keywords = keywords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Name name1 = (Name) o;

        if (name != null ? !name.equals(name1.name) : name1.name != null) return false;
        if (mnemonic != null ? !mnemonic.equals(name1.mnemonic) : name1.mnemonic != null) return false;
        return keywords != null ? keywords.equals(name1.keywords) : name1.keywords == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (mnemonic != null ? mnemonic.hashCode() : 0);
        result = 31 * result + (keywords != null ? keywords.hashCode() : 0);
        return result;
    }

    public String name() {
        return name;
    }

    public String mnemonic() {
        return mnemonic;
    }

    public String keywords() {
        return keywords;
    }
}
