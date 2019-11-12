/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.show.domain.model.category.spec;

import java.util.Objects;
import java.util.Set;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-11-12
 */
public class SpecificationFamily implements Comparable<SpecificationFamily> {
    private boolean multiSelect;
    private String name;
    private boolean required;
    private Set<Specification> specs;
    private int ordinal;

    public SpecificationFamily(String name, Set<Specification> specs, int ordinal, boolean required, boolean multiSelect) {
        setName(name);
        this.required = required;
        this.multiSelect = multiSelect;
        this.specs = specs;
        this.ordinal = ordinal;
    }

    public SpecificationFamily(String name, Set<Specification> specs) {
        this(name, specs, 0, false, false);
    }

    @Override
    public int compareTo(SpecificationFamily o) {
        return ordinal == o.ordinal ? 0 : ordinal > o.ordinal ? 1 : -1;
    }

    private void setName(String name) {
        name = Objects.requireNonNull(name, "name required").trim();
        if (name.length() > 32)
            throw new IllegalArgumentException("name length must less than 32");
        this.name = name;
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public SpecificationFamily addSpec(Specification spec) {
        specs.add(spec);
        return new SpecificationFamily(name, specs, ordinal, required, multiSelect);
    }

    public SpecificationFamily addSpec(Set<Specification> specs) {
        this.specs.addAll(specs);
        return new SpecificationFamily(name, specs, ordinal, required, multiSelect);
    }

    public String name() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

}
