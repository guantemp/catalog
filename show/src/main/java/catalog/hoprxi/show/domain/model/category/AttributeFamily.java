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

package catalog.hoprxi.show.domain.model.category;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-11-12
 */
public class AttributeFamily<E> implements Comparable<AttributeFamily<E>> {
    private boolean multiSelect;
    private String name;
    private boolean required;
    private Set<E> specs;
    private int ordinal;

    public AttributeFamily(String name, Set<E> specs, int ordinal, boolean required, boolean multiSelect) {
        setName(name);
        setSpecs(specs);
        this.required = required;
        this.multiSelect = multiSelect;
        setOrdinal(ordinal);
    }

    public static <E> AttributeFamily createBlankSpecFamily(String name) {
        return new AttributeFamily<E>(name, new HashSet<E>(), 0, false, false);
    }

    private void setOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal > 9)
            throw new IllegalArgumentException("ordinal rang is 0-9");
        this.ordinal = ordinal;
    }

    private void setName(String name) {
        name = Objects.requireNonNull(name, "name required").trim();
        if (name.isEmpty() || name.length() > 32)
            throw new IllegalArgumentException("name length rang is 1-32");
        this.name = name;
    }

    private void setSpecs(Set<E> specs) {
        if (specs == null)
            specs = new HashSet<>();
        this.specs = specs;
    }

    @Override
    public int compareTo(AttributeFamily o) {
        return ordinal == o.ordinal ? 0 : ordinal > o.ordinal ? 1 : -1;
    }

    public AttributeFamily addSpec(E spec) {
        specs.add(spec);
        return new AttributeFamily(name, specs, ordinal, required, multiSelect);
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public AttributeFamily addSpec(Set<E> specs) {
        this.specs.addAll(specs);
        return new AttributeFamily(name, this.specs, ordinal, required, multiSelect);
    }

    public Set<E> specs() {
        return specs;
    }

    public String name() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public int ordinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AttributeFamily.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("specs=" + specs)
                .add("ordinal=" + ordinal)
                .add("multiSelect=" + multiSelect)
                .add("required=" + required)
                .toString();
    }
}
