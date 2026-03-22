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
 * @since JDK21
 * @version 0.0.2 2026-03-22
 */
public record Imported(String code, String country) implements MadeIn {
    // 进口（国家或地区,如：美国）
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{2,3}$");

    public Imported {
        Objects.requireNonNull(country, "country required");
        Objects.requireNonNull(code, "code required");
        code = code.trim();

        if (code.isEmpty()) {
            throw new IllegalArgumentException("code cannot be empty after trim");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            // 建议：将错误信息动态化，告诉用户实际传了什么，方便调试
            throw new IllegalArgumentException("code must be two or three digits, but got: '" + code + "'");
        }
    }

    @Override
    public String madeIn() {
        return country;
    }
}
