/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

import java.util.Objects;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2023-03-01
 */
public record Domestic(String code, String city) implements MadeIn {
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{3,}$");

    public static final Domestic BEI_JING = new Domestic("110100", "北京市");
    public static final Domestic TIAN_JIN = new Domestic("120100", "天津市");
    public static final Domestic SHANG_HAI = new Domestic("310100", "上海市");
    public static final Domestic CHONG_QING = new Domestic("500100", "重庆市");
    
    public Domestic {
        Objects.requireNonNull(city, "city required");
        Objects.requireNonNull(code, "code required");

        city = city.trim();
        code = code.trim();

        if (code.isBlank()) {
            throw new IllegalArgumentException("code cannot be empty");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            // 优化错误信息：带上实际值，方便排查
            throw new IllegalArgumentException("Domestic code must be 3 or more digits, but got: '" + code + "'");
        }
    }

    @Override
    public String madeIn() {
        return city;
    }
}
