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

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-01-06
 */

public class PlaceOfProduction {
    private String locality;
    private Map<String, String> multipleLocality;

    /**
     * @param locality such as 合江县（泸州本地生产标识：县/区） or 乐山（省/市级） or 美国(进口）
     */
    public PlaceOfProduction(String locality) {
        setLocality(locality);
    }

    /**
     * @param multipleMadeIn
     */
    private PlaceOfProduction(Map<String, String> multipleMadeIn) {
        this.multipleLocality = Objects.requireNonNull(multipleMadeIn, "multipleMadeIn is required");
    }

    public static PlaceOfProduction multipleMadeIn(Map<String, String> multipleMadeIn) {
        return new PlaceOfProduction(multipleMadeIn);
    }

    /**
     * @return
     */
    public String locality() {
        return locality;
    }

    /**
     * @return
     */
    public boolean isMultiple() {
        return multipleLocality != null;
    }

    /**
     * @param code for multiple locality
     * @return
     */
    public String locality(String code) {
        if (multipleLocality == null)
            return "";
        else
            return multipleLocality.get(code);
    }

    private void setLocality(String locality) {
        this.locality = Objects.requireNonNull(locality, "locality required").trim();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PlaceOfProduction.class.getSimpleName() + "[", "]")
                .add("locality='" + locality + "'")
                .add("multipleLocality=" + multipleLocality)
                .toString();
    }
}
