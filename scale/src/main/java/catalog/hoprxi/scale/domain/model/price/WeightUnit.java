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

package catalog.hoprxi.scale.domain.model.price;


import catalog.hoprxi.scale.infrastructure.i18n.WeightLabel;

import java.math.BigDecimal;

/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @version 0.0.3 builder 2019-10-30
 * @since JDK21
 */
public enum WeightUnit {
    TON {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case FIVE_HUNDRED_GRAM:
                    return BigDecimal.valueOf(2000);
                case KILOGRAM:
                    return BigDecimal.valueOf(1000);
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_TON;
        }
    }, KILOGRAM {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case FIVE_HUNDRED_GRAM:
                    return BigDecimal.valueOf(2);
                case GRAM:
                    return BigDecimal.valueOf(1000);
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_KILOGRAM;
        }
    }, GRAM {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case FIVE_HUNDRED_GRAM:
                    return new BigDecimal("0.05");
                case KILOGRAM:
                    return new BigDecimal("0.001");
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_GRAM;
        }
    }, FIVE_HUNDRED_GRAM {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return BigDecimal.valueOf(500);
                case KILOGRAM:
                    return new BigDecimal("0.5");
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_FIVE_HUNDRED_GRAM;
        }
    }, MILLIGRAM {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("0.001");
                case KILOGRAM:
                    return new BigDecimal("0.000001");
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_MILLIGRAM;
        }
    }, OUNCE {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("28.5");
                case KILOGRAM:
                    return new BigDecimal("0.0285");
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_OUNCE;
        }

    }, POUND {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("453.59237");
                case KILOGRAM:
                    return new BigDecimal("0.45359237");
                default:
                    return BigDecimal.valueOf(1);
            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_POUND;
        }
    }, CARAT {
        @Override
        public BigDecimal convert(WeightUnit unit) {
            switch (unit) {
                case GRAM:
                    return new BigDecimal("0.2");
                default:
                    return BigDecimal.valueOf(1);

            }
        }

        @Override
        public String toString() {
            return WeightLabel.WEIGHT_UNIT_CARAT;
        }
    };


    public abstract BigDecimal convert(WeightUnit unit);

    public static WeightUnit of(String s) {
        if (s == null)
            return WeightUnit.KILOGRAM;
        for (WeightUnit unit : values()) {
            if (s.equalsIgnoreCase(unit.name()) || unit.toString().equals(s))
                return unit;
        }
        return WeightUnit.KILOGRAM;
    }

}

