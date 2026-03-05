/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.domain.model;

import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/4
 * @since JDK 21
 */

public abstract class Scale {
    protected long brandId;
    protected long categoryId;
    protected GradeEnum grade;
    protected Plu plu;
    protected Name name;
    protected Specification spec;
    protected ShelfLife shelfLife;
    protected MadeIn madeIn;

    public Scale(Plu plu, Name name, Specification spec, GradeEnum grade, MadeIn madeIn, ShelfLife shelfLife, long categoryId, long brandId) {
        this.plu = plu;
        this.name = name;
        this.spec = spec;
        this.shelfLife = shelfLife;
        this.grade = grade;
        this.madeIn = madeIn;
        this.categoryId = categoryId;
        this.brandId = brandId;
    }
}
