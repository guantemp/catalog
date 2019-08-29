/*
 * Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

package catalog.hoprxi.core.domain.model.category.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-08-28
 */
public class SpecificationFamily {
    private boolean multiSelect;
    private String name;
    private boolean required;
    private boolean inherit;
    private List<Specification> specs = new ArrayList<>(0);

    /**
     * @param name
     */
    public SpecificationFamily(String name) {
        this(name, false, false, false);
    }

    /**
     * @param name
     * @param required
     * @param multiSelect
     * @param inherit
     */
    public SpecificationFamily(String name, boolean required, boolean multiSelect, boolean inherit) {
        setName(name);
        this.required = required;
        this.multiSelect = multiSelect;
        this.inherit = inherit;
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

    public SpecificationFamily addSpecification(Specification spec) {
        specs.add(spec);
        return new SpecificationFamily(name, required, multiSelect, inherit);
    }

    public String name() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isInherit() {
        return inherit;
    }
}
