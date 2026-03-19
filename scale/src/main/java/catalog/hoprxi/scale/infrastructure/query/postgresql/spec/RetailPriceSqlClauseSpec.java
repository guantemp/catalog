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

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/3/18
 */

public record RetailPriceSqlClauseSpec(MonetaryAmount min, MonetaryAmount max) implements SqlClauseSpec {
    public RetailPriceSqlClauseSpec {
        if (min == null && max == null) {
            throw new IllegalArgumentException("At least one of 'min' or 'max' must be provided.");
        }

        if (min != null && max != null && min.isGreaterThan(max)) {
            throw new IllegalArgumentException(
                    String.format("Min price (%s) must be less than or equal to Max price (%s)", min, max)
            );
        }
    }

    public static RetailPriceSqlClauseSpec min(MonetaryAmount min) {
        return new RetailPriceSqlClauseSpec(min, null);
    }

    public static RetailPriceSqlClauseSpec max(MonetaryAmount max) {
        return new RetailPriceSqlClauseSpec(null, max);
    }

    @Override
    public SqlClause toClause() {
        if (!isSatisfied()) {
            return new SqlClause("", Collections.emptyList());
        }

        String priceColumnExpr = "(s.retail_price->>'number')::numeric";

        List<Object> params = new ArrayList<>(2);
        StringJoiner sqlJoiner = new StringJoiner(" ");

        boolean hasMin = (min != null);
        boolean hasMax = (max != null);

        if (hasMin && hasMax) {
            // 情况 1: 既有 min 又有 max -> 使用 BETWEEN 或 >= AND <=
            // 使用 BETWEEN 语义更清晰，性能通常也更好
            sqlJoiner.add(priceColumnExpr + " BETWEEN ? AND ?");
            params.add(min.getNumber().numberValueExact(BigDecimal.class));
            params.add(max.getNumber().numberValueExact(BigDecimal.class));
        } else if (hasMin) {
            // 情况 2: 只有 min -> 大于等于
            sqlJoiner.add(priceColumnExpr + " >= ?");
            params.add(min.getNumber().numberValueExact(BigDecimal.class));
        } else {
            // 情况 3: 只有 max -> 小于等于
            sqlJoiner.add(priceColumnExpr + " <= ?");
            params.add(max.getNumber().numberValueExact(BigDecimal.class));
        }

        return new SqlClause(sqlJoiner.toString(), params);
    }

    @Override
    public boolean isSatisfied() {
        return min != null || max != null;
    }
}
