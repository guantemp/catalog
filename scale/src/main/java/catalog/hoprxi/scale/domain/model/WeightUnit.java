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


import catalog.hoprxi.scale.infrastructure.i18n.Label;

import java.math.BigDecimal;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @version 0.0.2 builder 2019-10-03
 * @since JDK8.0
 */
public enum WeightUnit {
    TON {
        @Override
        public Number toFiveHundredGram(Number number) {
            return super.toFiveHundredGram(number);
        }

        @Override
        public Number toKillogram(Number number) {
            return 1000;
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_TON;
        }
    }, KILOGRAM {
        @Override
        public Number toGram(Number number) {
            return 1000;
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_KILOGRAM;
        }
    }, GRAM {
        @Override
        public Number toKillogram(Number number) {
            return new BigDecimal("0.001");
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_GRAM;
        }
    }, FIVE_HUNDRED_GRAM {
        @Override
        public Number toGram(Number number) {
            return 500;
        }

        @Override
        public Number toKillogram(Number number) {
            return new BigDecimal("0.5");
        }
    }, MILLIGRAM {
        @Override
        public Number toGram(Number number) {
            return new BigDecimal("0.001");
        }

        @Override
        public Number toKillogram(Number number) {
            return new BigDecimal("0.000001");
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_MILLIGRAM;
        }
    }, OUNCE {
        @Override
        public Number toGram(Number number) {
            return new BigDecimal("28.5");
        }

        @Override
        public Number toKillogram(Number number) {
            return new BigDecimal("0.0285");
        }
    }, POUND {
        @Override
        public Number toKillogram(Number number) {
            return new BigDecimal("0.45359237");
        }

        @Override
        public Number toGram(Number number) {
            return new BigDecimal("453.59237");
        }
    };

    public Number toKillogram(Number number) {
        return 1;
    }

    public Number toGram(Number number) {
        return 1;
    }

    public Number toFiveHundredGram(Number number) {
        return 1;
    }
}

