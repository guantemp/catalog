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

import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import catalog.hoprxi.scale.application.query.ScaleQuery;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.infrastructure.PsqlUtil;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/7
 * @since JDK 21
 */

public class PsqlScaleQuery implements ScaleQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlScaleQuery.class);

    @Override
    public Flux<ByteBuf> findAsync(Plu plu) {
        final String findSql = """
                SELECT plu, name, category_id, brand_id, grade, made_in, spec, shelf_life,
                       retail_price, last_receipt_price, member_price, vip_price
                FROM scale
                WHERE plu = ?
                LIMIT 1
                """;
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(findSql)) {
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

    }

    /**
     * @param filters
     * @param offset
     * @param size
     * @param sortField
     * @return
     */
    @Override
    public Flux<ByteBuf> searchAsync(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        return null;
    }
}
