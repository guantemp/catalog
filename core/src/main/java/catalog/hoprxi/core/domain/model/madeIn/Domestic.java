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

package catalog.hoprxi.core.domain.model.madeIn;

import catalog.hoprxi.core.infrastructure.i18n.Label;

import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-08-28
 */
public class Domestic implements MadeIn {
    public static final Domestic BEI_JING = new Domestic("北京市", "北京市");
    public static final Domestic TIAN_JIN = new Domestic("天津市", "天津市");
    public static final Domestic SHANG_HAI = new Domestic("上海市", "上海市");
    public static final Domestic CHONG_QING = new Domestic("重庆市", "重庆市");

    // 省/市级
    private final String province;//四川省
    private final String city;//乐山市
    private long code;

    public String province() {
        return province;
    }

    public String city() {
        return city;
    }

    public Domestic(String province, String city) {
        this.province = Objects.requireNonNull(province, "province required").trim();
        this.city = Objects.requireNonNull(city, "city required").trim();
    }

    @Override
    public String madeIn() {
        return province + Label.MADIN_SEPARATORS + city;
        //return new StringJoiner(Label.MADIN_SEPARATORS).add(province).add(city).toString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Domestic.class.getSimpleName() + "[", "]")
                .add("province='" + province + "'")
                .add("city='" + city + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Domestic domestic = (Domestic) o;

        if (province != null ? !province.equals(domestic.province) : domestic.province != null) return false;
        return city != null ? city.equals(domestic.city) : domestic.city == null;
    }

    @Override
    public int hashCode() {
        int result = province != null ? province.hashCode() : 0;
        result = 31 * result + (city != null ? city.hashCode() : 0);
        return result;
    }
}
