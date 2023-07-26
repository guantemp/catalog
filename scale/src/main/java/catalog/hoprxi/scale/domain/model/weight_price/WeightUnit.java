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

package catalog.hoprxi.scale.domain.model.weight_price;


import catalog.hoprxi.scale.infrastructure.i18n.Label;

import java.math.BigDecimal;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @version 0.0.3 builder 2019-10-30
 * @since JDK8.0
 */
public enum WeightUnit {
    TON {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case FIVE_HUNDRED_GRAM:
                    return 2000;
                case KILOGRAM:
                    return 1000;
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_TON;
        }
    }, KILOGRAM {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case FIVE_HUNDRED_GRAM:
                    return 2;
                case GRAM:
                    return 1000;
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_KILOGRAM;
        }
    }, GRAM {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case FIVE_HUNDRED_GRAM:
                    return 0.05;
                case KILOGRAM:
                    return 0.001;
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_GRAM;
        }
    }, FIVE_HUNDRED_GRAM {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return 500;
                case KILOGRAM:
                    return 0.5;
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_FIVE_HUNDRED_GRAM;
        }
    }, MILLIGRAM {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return 0.001;
                case KILOGRAM:
                    return 0.000001;
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_MILLIGRAM;
        }
    }, OUNCE {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("28.5");
                case KILOGRAM:
                    return new BigDecimal("0.0285");
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_OUNCE;
        }

    }, POUND {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("453.59237");
                case KILOGRAM:
                    return new BigDecimal("0.45359237");
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_POUND;
        }
    }, CARAT {
        @Override
        public Number convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("0.2");
                default:
                    return 1;

            }
        }

        @Override
        public String toString() {
            return Label.WEIGHT_UNIT_CARAT;
        }
    };


    public abstract Number convert(WeightUnit unit);
/*
    public static WeightUnit valueOf(String s) {
        for (WeightUnit unit : values()) {
            if (unit.toString().equals(s))
                return unit;
        }
        return WeightUnit.KILOGRAM;
    }
 */
}

