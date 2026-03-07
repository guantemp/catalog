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

package catalog.hoprxi.scale.infrastructure.persistence.postgresql;

import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.LastReceiptPrice;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import catalog.hoprxi.scale.domain.model.Count;
import catalog.hoprxi.scale.domain.model.CountRepository;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.price.WeightLastReceiptPrice;
import catalog.hoprxi.scale.domain.model.price.WeightMemberPrice;
import catalog.hoprxi.scale.domain.model.price.WeightRetailPrice;
import catalog.hoprxi.scale.domain.model.price.WeightVipPrice;
import catalog.hoprxi.scale.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/6
 * @since JDK 21
 */

public class PsqlCountRepository implements CountRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlCountRepository.class);
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    /**
     * @param plu
     * @return
     */
    @Override
    public Count find(int plu) {
        final String findSql = """
                SELECT plu, name, category_id, brand_id, grade, made_in, spec, shelf_life,
                       retail_price, last_receipt_price, member_price, vip_price
                FROM scale
                WHERE plu = ?
                LIMIT 1
                """;
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(findSql)) {
            ps.setInt(1, plu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PsqlCountRepository.rebuild(rs);
                }
            }
            return null;// 如果没有返回数据，正常返回 null，不要抛异常
        } catch (SQLException e) {// 【关键修复 2】区分数据库异常和业务异常
            LOGGER.error("Database access failure while fetching weight for PLU={}", plu.id(), e);
            throw new PersistenceException("Database query execution failed for PLU " + plu.id() + ": " + e.getMessage(), e);
        } catch (IOException e) {// 重建对象时的错误属于系统内部错误，不是“未找到”
            LOGGER.error("Critical failure: Unable to map result set to weight object (id={})", plu.id(), e);
            throw new PersistenceException("Internal error: Failed to reconstruct weight entity from database result (ID: " + plu.id() + ")", e);
        }
    }

    private static Count rebuild(ResultSet rs) throws SQLException {
        Plu plu = new Plu(rs.getInt("plu"));
        Name name = PsqlCountRepository.buildName(rs.getString("name"));
        long categoryId = rs.getLong("category_id");
        long brandId = rs.getLong("brand_id");
        GradeEnum grade = GradeEnum.valueOf(rs.getString("grade"));
        MadeIn madeIn = PsqlCountRepository.buildMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.create(rs.getInt("shelf_life"));

        LastReceiptPrice lastReceiptPrice = PsqlCountRepository.buildLastReceiptPricePrice(rs.getString("last_receipt_price"));
        RetailPrice retailPrice = PsqlCountRepository.buildRetailPrice(rs.getString("retail_price"));
        MemberPrice memberPrice = PsqlCountRepository.buildMemberPricePrice(rs.getString("member_price"));
        VipPrice vipPrice = PsqlCountRepository.buildVipPrice(rs.getString("vip_price"));
        return new Count(plu, name, madeIn, spec, grade, shelfLife,
                lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    private static VipPrice buildVipPrice(String vipPrice) {
    }

    private static MemberPrice buildMemberPricePrice(String memberPrice) {
    }

    private static RetailPrice buildRetailPrice(String retailPrice) {
    }

    private static LastReceiptPrice buildLastReceiptPricePrice(String lastReceiptPrice) {
    }

    private static MadeIn buildMadeIn(String madeIn) {
    }

    private static Name buildName(String name) {
    }

    /**
     * @return
     */
    @Override
    public Plu nextPlu() {
        return null;
    }

    /**
     * @param plu
     */
    @Override
    public void delete(Plu plu) {

    }

    /**
     * @param count
     */
    @Override
    public void save(Count count) {

    }

    /**
     * @param plu
     * @return
     */
    @Override
    public boolean isPluExists(int plu) {
        return false;
    }
}
