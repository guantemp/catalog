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
package catalog.hoprxi.core.domain.model;


import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.HashMap;
import java.util.Map;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.2 builder 2025-11-08
 */
public enum GradeEnum {
    ONE_LEVEL {
        @Override
        public String toString() {
            return Label.ONE_LEVEL_GRADE;
        }
    }, QUALIFIED {
        @Override
        public String toString() {
            return Label.QUALIFIED_GRADE;
        }
    }, UNQUALIFIED {
        @Override
        public String toString() {
            return Label.UNQUALIFIED_GRADE;
        }
    }, PREMIUM {
        @Override
        public String toString() {
            return Label.PREMIUM_GRADE;
        }
    };

    // 2. 利用静态代码块初始化一个查找 Map (只加载一次)
    private static final Map<String, GradeEnum> LOOKUP = new HashMap<>();

    static {
        // 这里用了一次 values() 仅用于初始化，类加载后就不再遍历了
        for (GradeEnum grade : values()) {
            LOOKUP.put(grade.name(), grade);       // 放入枚举名
            LOOKUP.put(grade.toString(), grade);        // 放入 i18n 字符
        }
    }

    public static GradeEnum of(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Grade string cannot be null or empty");
        }
        String cleanS = s.replace("\u3000", "").replace(" ", "").trim();
        return LOOKUP.getOrDefault(cleanS, GradeEnum.QUALIFIED);
    }
}
