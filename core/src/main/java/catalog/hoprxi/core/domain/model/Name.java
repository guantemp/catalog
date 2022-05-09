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

package catalog.hoprxi.core.domain.model;

import salt.hoprxi.to.PinYin;

import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.3 builder 2022-05-09
 */
public class Name {
    private static final int MAX_LENGTH = 256;
    private String name;
    private String mnemonic;
    private String alias;

    /**
     * Reconstructing constructors for designs of name classes
     */
    private Name(String name, String mnemonic, String alias) {
        setName(name);
        this.mnemonic = mnemonic;
        setAlias(alias);
    }

    /**
     * @param name
     * @param alias equal name if null
     * @throws IllegalArgumentException if name is <code>NULL</code>
     */
    public Name(String name, String alias) {
        setName(name);
        this.mnemonic = PinYin.toShortPinYing(this.name);
        setAlias(alias);
    }

    /**
     * @param name
     * @throws IllegalArgumentException if name is <code>NULL</code>
     */
    public Name(String name) {
        this(name, name);
    }

    public static Name of(String name) {
        return new Name(name);
    }

    public String alias() {
        return alias;
    }

    private void setAlias(String alias) {
        alias = Objects.requireNonNull(alias, "alias required").trim();
        if (alias.length() > MAX_LENGTH)
            throw new IllegalArgumentException("alias length rang is 0-256");
        this.alias = alias;
    }

    public String name() {
        return name;
    }

    private void setName(String name) {
        name = Objects.requireNonNull(name, "name required").trim();
        if (name.length() > MAX_LENGTH)
            throw new IllegalArgumentException("name length rang is 0-256");
        this.name = name;
    }

    public String mnemonic() {
        return mnemonic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Name name1 = (Name) o;

        if (!Objects.equals(name, name1.name)) return false;
        if (!Objects.equals(mnemonic, name1.mnemonic)) return false;
        return Objects.equals(alias, name1.alias);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (mnemonic != null ? mnemonic.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Name{" +
                "name='" + name + '\'' +
                ", mnemonic='" + mnemonic + '\'' +
                ", alias='" + alias + '\'' +
                '}';
    }
}
