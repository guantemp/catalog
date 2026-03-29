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

package catalog.hoprxi.scale.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.scale.application.query.ScaleQuery;
import catalog.hoprxi.scale.application.query.SqlClause;
import catalog.hoprxi.scale.application.query.SqlClauseSpec;
import catalog.hoprxi.scale.domain.model.Plu;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/7
 * @since JDK 21
 */

public class PsqlScaleQuery implements ScaleQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlScaleQuery.class);

    @Override
    public Flux<ByteBuf> findAsync(Plu plu) {
        final String sql = """
                SELECT
                    s.plu, s.name, s.grade, s.made_in, s.spec, s.shelf_life,
                    s.last_receipt_price, s.retail_price, s.member_price, s.vip_price,
                    json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                    json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                FROM scale s
                LEFT JOIN category c ON i.category_id = c.id
                LEFT JOIN brand b ON b.id = i.brand_id
                WHERE plu = ?
                """;
/*
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sb.toString())) {
            ps.setInt(1,plu.id());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    //return PsqlWeightRepository.rebuild(rs);
                }
            }
            return null;// 如果没有返回数据，正常返回 null，不要抛异常
        } catch (SQLException e) {// 【关键修复 2】区分数据库异常和业务异常
            LOGGER.error("Database access failure while fetching weight for PLU={}", plu.id(), e);
            throw new PersistenceException("Database query execution failed for PLU " + plu.id() + ": " + e.getMessage(), e);
        } /*catch (IOException e) {// 重建对象时的错误属于系统内部错误，不是“未找到”
            LOGGER.error("Critical failure: Unable to map result set to weight object (id={})", plu.id(), e);
            throw new PersistenceException("Internal error: Failed to reconstruct weight entity from database result (ID: " + plu.id() + ")", e);
        }
        */

        return null;
    }


    @Override
    public Flux<ByteBuf> searchAsync(SqlClauseSpec[] specs, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }

        // 过滤出满足条件的规格，然后生成 SQL 片段
        List<SqlClause> clauses = Stream.of(specs)
                .filter(SqlClauseSpec::isSatisfied) // 只保留满足的规格
                .map(SqlClauseSpec::toClause)      // 转换为 SQL 片段
                .toList();

        StringBuilder whereClause = new StringBuilder();

        List<Object> allParams = new ArrayList<>();
        for (SqlClause clause : clauses) {
            if (!clause.sql().isEmpty()) {
                if (!whereClause.isEmpty()) {
                    whereClause.append(" AND ");
                }
                whereClause.append(clause.sql().trim());
                allParams.addAll(clause.params());
            }
        }

        final StringBuilder sb = new StringBuilder("""
                SELECT
                    s.plu, s.name, s.grade, s.made_in, s.spec, s.shelf_life,
                    s.last_receipt_price, s.retail_price, s.member_price, s.vip_price,
                    json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                    json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                FROM scale s
                LEFT JOIN category c ON s.category_id = c.id
                LEFT JOIN brand b ON s.brand_id = b.id
                """);

        if (!whereClause.isEmpty()) {
            sb.append("WHERE ").append(whereClause);
        }

        String orderClause = buildOrderClause(sortField);
        sb.append(orderClause);

        sb.append(" LIMIT ? OFFSET ?");
        allParams.add(size);
        allParams.add(offset);
        System.out.println(sb);
        System.out.println(Arrays.toString(allParams.toArray()));
        return null;
    }

    private static String buildOrderClause(SortFieldEnum sortBy) {
        if (sortBy == null) {
            sortBy = SortFieldEnum._ID;
        }
        return " ORDER BY " + mapEsFieldToDbField(sortBy) + " " + sortBy.sort().toUpperCase();
    }

    private static String mapEsFieldToDbField(SortFieldEnum sortField) {
        return switch (sortField) {
            case ID,_ID -> "s.id";
            case NAME,_NAME -> "s.name ->> 'mnemonic'";
            case BARCODE,_BARCODE -> "s.barcode";
            case MADE_IN,_MADE_IN -> "s.made_in";
            case GRADE,_GRADE -> "s.grade";
            case SPEC,_SPEC -> "s.spec";
            case CATEGORY,_CATEGORY -> "c.id";
            case BRAND,_BRAND -> "b.id";
            case LAST_RECEIPT_PRICE,_LAST_RECEIPT_PRICE -> "(s.last_receipt_price->>'number')::numeric";
            case RETAIL_PRICE,_RETAIL_PRICE -> "(s.retail_price->>'number')::numeric";
            case MEMBER_PRICE,_MEMBER_PRICE -> "(s.member_price->>'number')::numeric";
            case VIP_PRICE,_VIP_PRICE -> "(s.vip_price->>'number')::numeric";
            //case STOCK -> "i.stock";
        };
    }
}
