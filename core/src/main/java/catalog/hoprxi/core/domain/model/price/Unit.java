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
package catalog.hoprxi.core.domain.model.price;


import catalog.hoprxi.core.infrastructure.i18n.Label;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2021-09-19
 */
public enum Unit {
    BEI {
        @Override
        public String toString() {
            return Label.UNIT_BEI;
        }
    }, BEN {
        @Override
        public String toString() {
            return Label.UNIT_BEN;
        }
    }, CHUAN {
        @Override
        public String toString() {
            return Label.UNIT_CHUAN;
        }
    }, DA {
        @Override
        public String toString() {
            return Label.UNIT_DA;
        }
    },
    DAI {
        @Override
        public String toString() {
            return Label.UNIT_DAI;
        }
    }, DUI {
        @Override
        public String toString() {
            return Label.UNIT_DUI;
        }
    }, HE {
        @Override
        public String toString() {
            return Label.UNIT_HE;
        }
    }, JIAN {
        @Override
        public String toString() {
            return Label.UNIT_JIAN;
        }
    }, PCS, PING {
        @Override
        public String toString() {
            return Label.UNIT_PING;
        }
    }, TAO {
        @Override
        public String toString() {
            return Label.UNIT_TAO;
        }
    }, ZHI_LIFE {
        @Override
        public String toString() {
            return Label.UNIT_ZHI_LIFE;
        }
    }, GE {
        @Override
        public String toString() {
            return Label.UNIT_GE;
        }
    }, TAI {
        @Override
        public String toString() {
            return Label.UNIT_TAI;
        }
    }, ZHANG {
        @Override
        public String toString() {
            return Label.UNIT_ZHANG;
        }
    }, ZHI_THING {
        @Override
        public String toString() {
            return Label.UNIT_ZHI_THING;
        }
    }, XIANG {
        @Override
        public String toString() {
            return Label.UNIT_XIANG;
        }
    }, BA {
        @Override
        public String toString() {
            return Label.UNIT_BA;
        }
    }, BAO {
        @Override
        public String toString() {
            return Label.UNIT_BAO;
        }
    }, SHUANG {
        @Override
        public String toString() {
            return Label.UNIT_SHUANG;
        }
    }, GENG {
        @Override
        public String toString() {
            return Label.UNIT_GENG;
        }
    }, KUAI {
        @Override
        public String toString() {
            return Label.UNIT_KUAI;
        }
    }, TING {
        @Override
        public String toString() {
            return Label.UNIT_TING;
        }
    }, MEI {
        @Override
        public String toString() {
            return Label.UNIT_MEI;
        }
    }, BANG {
        @Override
        public String toString() {
            return Label.UNIT_BANG;
        }
    }, TIAO {
        @Override
        public String toString() {
            return Label.UNIT_TIAO;
        }
    }, ZHAN {
        @Override
        public String toString() {
            return Label.UNIT_ZHAN;
        }
    }, CE {
        @Override
        public String toString() {
            return Label.UNIT_CE;
        }
    }, GUAN {
        @Override
        public String toString() {
            return Label.UNIT_GUAN;
        }
    }, TONG {
        @Override
        public String toString() {
            return Label.UNIT_TONG;
        }
    }, PIAN {
        @Override
        public String toString() {
            return Label.UNIT_PIAN;
        }
    }, JUAN {
        @Override
        public String toString() {
            return Label.UNIT_JUAN;
        }
    }, PI {
        @Override
        public String toString() {
            return Label.UNIT_PI;
        }
    }, KUN {
        @Override
        public String toString() {
            return Label.UNIT_KUN;
        }
    }, ZHU {
        @Override
        public String toString() {
            return Label.UNIT_ZHU;
        }
    }, TI {
        @Override
        public String toString() {
            return Label.UNIT_TI;
        }
    }, LI {
        @Override
        public String toString() {
            return Label.UNIT_LI;
        }
    }, FENG {
        @Override
        public String toString() {
            return Label.UNIT_FENG;
        }
    }, WANG {
        @Override
        public String toString() {
            return Label.UNIT_WANG;
        }
    }, PAN {
        @Override
        public String toString() {
            return Label.UNIT_PAN;
        }
    }, LIANG {
        @Override
        public String toString() {
            return Label.UNIT_LIANG;
        }
    };

    /**
     * @param s
     * @return
     */
    public static Unit of(String s) {
        for (Unit unit : values()) {
            if (unit.toString().equals(s))
                return unit;
        }
        return Unit.PCS;
    }
}
