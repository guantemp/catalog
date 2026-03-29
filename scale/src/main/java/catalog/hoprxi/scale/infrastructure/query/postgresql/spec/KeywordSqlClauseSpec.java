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


import catalog.hoprxi.scale.application.query.SqlClauseSpec;
import catalog.hoprxi.scale.application.query.SqlClause;
import salt.hoprxi.to.PinYin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/3/8
 */

public record KeywordSqlClauseSpec(String keyword) implements SqlClauseSpec {

    public KeywordSqlClauseSpec {
       Objects.requireNonNull(keyword,"keyword is null");
    }

    @Override
    public boolean isSatisfied() {
        return  !keyword.isEmpty();
    }

    @Override
    public SqlClause toClause() {
        if (!isSatisfied()) {
            return new SqlClause("", Collections.emptyList());
        }
        String sql = """
            s.search_vector @@ to_tsquery('simple', ?)
            """;
        return new SqlClause(sql, Collections.singletonList(keyword));
    }
}
