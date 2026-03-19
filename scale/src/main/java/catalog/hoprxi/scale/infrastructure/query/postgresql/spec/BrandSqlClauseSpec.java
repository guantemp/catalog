/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.query.postgresql.spec;


import catalog.hoprxi.scale.application.query.SqlClause;
import catalog.hoprxi.scale.application.query.SqlClauseSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/3/18
 */

public record BrandSqlClauseSpec(long[] ids) implements SqlClauseSpec {

    public BrandSqlClauseSpec(long id) {
        this(new long[]{id});
    }

    @Override
    public SqlClause toClause() {
        if (!isSatisfied()) {
            return new SqlClause("", Collections.emptyList());
        }
        String placeholders = Stream.of(ids)
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        String sql = "b.id IN (" + placeholders + ")";
        List<Object> longList = Arrays.stream(ids) // 生成 LongStream
                .boxed()           // 将 long 转换为 Long (装箱)
                .collect(Collectors.toCollection(ArrayList::new)); // 收集到 ArrayList
        return new SqlClause(sql, longList);
    }

    @Override
    public boolean isSatisfied() {
        return ids != null && ids.length != 0;
    }
}
