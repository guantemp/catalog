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
package catalog.hoprxi.fresh.infrastructure.i18n;


import mi.foxtail.util.NLS;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-05-03
 */
public class Label extends NLS {
    private static final String BUNDLE_NAME = "catalog.foxtail.fresh.infrastructure.i18n.label"; //$NON-NLS-1$

    public static String COUNT_UNIT_PAN;
    public static String COUNT_UNIT_FENG;
    public static String COUNT_UNIT_PCS;
    public static String COUNT_UNIT_BA;

    public static String WEIGHT_UNIT_GRAM;
    public static String WEIGHT_UNIT_KILOGRAM;
    public static String WEIGHT_UNIT_MILLIGRAM;
    public static String WEIGHT_UNIT_TON;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Label.class);
    }
}
