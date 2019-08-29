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
package catalog.hoprxi.core.domain.model;


import catalog.hoprxi.core.infrastructure.i18n.Label;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-04-28
 */
public enum Grade {
    ONE_LEVEL {
        @Override
        public String toString() {
            return Label.GRADE_ONE_LEVEL;
        }
    }, QUALIFIED {
        @Override
        public String toString() {
            return Label.GRADE_QUALIFIED;
        }
    }, SECOND_LEVEL {
        @Override
        public String toString() {
            return Label.GRADE_SECOND_LEVEL;
        }
    }, UNQUALIFIED {
        @Override
        public String toString() {
            return Label.GRADE_UNQUALIFIED;
        }
    }, THREE_LEVEL {
        @Override
        public String toString() {
            return Label.GRADE_THREE_LEVEL;
        }
    };

    /**
     * @param s
     * @return
     */
    public static Grade of(String s) {
        for (Grade grade : values()) {
            if (grade.toString().equals(s))
                return grade;
        }
        return Grade.QUALIFIED;
    }
}
