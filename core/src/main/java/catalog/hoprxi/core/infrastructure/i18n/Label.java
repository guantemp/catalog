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
package catalog.hoprxi.core.infrastructure.i18n;


import salt.hoprxi.utils.NLS;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2021-06-02
 */
public class Label extends NLS {
    private static final String BUNDLE_NAME = "catalog.hoprxi.core.infrastructure.i18n.label"; //$NON-NLS-1$
    public static String BRAND_UNDEFINED;
    public static String CATEGORY_UNDEFINED;
    public static String SPEC_NAME;

    public static String UNIT_BEI;
    public static String UNIT_BEN;
    public static String UNIT_CHUAN;
    public static String UNIT_DA;
    public static String UNIT_DAI;
    public static String UNIT_DUI;
    public static String UNIT_HE;
    public static String UNIT_JIAN;
    public static String UNIT_PAN;
    public static String UNIT_PING;
    public static String UNIT_TAO;
    public static String UNIT_ZHI_LIFE;
    public static String UNIT_GE;
    public static String UNIT_TAI;
    public static String UNIT_ZHANG;
    public static String UNIT_ZHI_THING;
    public static String UNIT_XIANG;
    public static String UNIT_BA;
    public static String UNIT_BAO;
    public static String UNIT_SHUANG;
    public static String UNIT_GENG;
    public static String UNIT_KUAI;
    public static String UNIT_TING;
    public static String UNIT_MEI;
    public static String UNIT_BANG;
    public static String UNIT_TIAO;
    public static String UNIT_ZHAN;
    public static String UNIT_CE;
    public static String UNIT_GUAN;
    public static String UNIT_TONG;
    public static String UNIT_PIAN;
    public static String UNIT_JUAN;
    public static String UNIT_PI;
    public static String UNIT_KUN;
    public static String UNIT_ZHU;
    public static String UNIT_TI;
    public static String UNIT_LI;
    public static String UNIT_FENG;
    public static String UNIT_WANG;
    public static String UNIT_LIANG;

    public static String ONE_LEVEL_GRADE;
    public static String QUALIFIED_GRADE;
    public static String UNQUALIFIED_GRADE;
    public static String PREMIUM_GRADE;

    public static String PRICE_RETAIL;
    public static String PRICE_MEMBER;

    public static String PRICE_LAST_RECEIPT;
    public static String PRICE_VIP;

    public static String MADIN_SEPARATORS;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Label.class);
    }
}
