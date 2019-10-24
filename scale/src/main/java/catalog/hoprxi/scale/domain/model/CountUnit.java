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

package catalog.hoprxi.scale.domain.model;

import catalog.foxtail.fresh.infrastructure.i18n.Label;

/**
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuan</a>
 * @version 0.0.2 builder 2018-06-03
 * @since JDK8.0
 */
public enum CountUnit {
    PCS, PAN {
        @Override
        public String toString() {
            return Label.COUNT_UNIT_PAN;
        }
    }, FENG {
        @Override
        public String toString() {
            return Label.COUNT_UNIT_FENG;
        }
    }, BA {
        @Override
        public String toString() {
            return Label.COUNT_UNIT_BA;
        }
    }
}
