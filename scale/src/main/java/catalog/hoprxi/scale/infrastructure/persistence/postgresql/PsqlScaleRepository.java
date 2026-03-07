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

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Scale;
import catalog.hoprxi.scale.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/6
 * @since JDK 21
 */

public abstract class PsqlScaleRepository<T extends Scale> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlScaleRepository.class);
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    // 通用查询方法（子类只需实现buildEntity方法）
    public T find(Plu plu) {
        final String findSql = """
                SELECT plu, name, category_id, brand_id, grade, made_in, spec, shelf_life,
                       retail_price, last_receipt_price, member_price, vip_price
                FROM scale
                WHERE plu = ?
                LIMIT 1
                """;
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(findSql)) {
            ps.setInt(1, plu.id());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 子类实现具体实体的构建逻辑
                    return buildEntity(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            LOGGER.error("Database access failure while fetching scale for PLU={}", plu.id(), e);
            throw new PersistenceException("Database query execution failed for PLU " + plu.id() + ": " + e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error("Critical failure: Unable to map result set to scale object (plu={})", plu.id(), e);
            throw new PersistenceException("Internal error: Failed to reconstruct scale entity from database result (PLU: " + plu.id() + ")", e);
        }
    }

    private T buildEntity(ResultSet rs) throws SQLException, IOException {
        return  null;
    }

    // 通用Name解析（抽取共性）
    protected Name buildName(String json) throws IOException {
        if (json == null)
            return Name.EMPTY;
        String name = "", alias = "";
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return Name.EMPTY;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String field = parser.currentName();
                    String val = parser.nextTextValue();
                    if (val != null) {
                        switch (field) {
                            case "name" -> name = val;
                            case "alias" -> alias = val;
                        }
                    }
                }
            }
        }
        return new Name(name, alias);
    }

    // 通用MadeIn解析（抽取共性）
    protected MadeIn buildMadeIn(String json) throws IOException {
        if (json == null || json.isEmpty()) return MadeIn.UNKNOWN;
        String _class = null;
        String madeIn = null;
        String code = "156";
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return MadeIn.UNKNOWN;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (JsonToken.FIELD_NAME == parser.currentToken()) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "_class" -> _class = parser.getValueAsString();
                        case "code" -> code = parser.getValueAsString();
                        case "madeIn" -> madeIn = parser.getValueAsString();
                    }
                }
            }
        }
        if (Objects.equals(MadeIn.UNKNOWN.code(), code))
            return MadeIn.UNKNOWN;
        if (_class != null && _class.endsWith("Domestic")) {
            return new Domestic(code, madeIn);
        } else if (_class != null && _class.endsWith("Imported")) {
            return new Imported(code, madeIn);
        }
        return MadeIn.UNKNOWN;
    }

    public void delete(Plu plu) {
        final String removeSql = "delete from scale where plu=?";
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(removeSql)) {
            preparedStatement.setLong(1, plu.id());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't remove from Weight(plu={})", plu.id(), e);
            throw new PersistenceException(String.format("Can't remove from Weight(plu=%s)", plu.id()), e);
        }
    }

    // 通用保存方法（子类实现具体的参数设置逻辑）
    public void save(T scale) {
        final String insertOrReplaceSql = """
                INSERT INTO scale (
                    plu, name,  category_id, brand_id, grade, made_in, spec, shelf_life,
                    last_receipt_price, retail_price, member_price, vip_price
                ) VALUES (
                    ?, ?::jsonb,  ?, ?, ?::grade, ?::jsonb, ?, ?,
                    ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb
                )
                ON CONFLICT (plu) DO UPDATE SET
                    name = EXCLUDED.name,
                    category_id = EXCLUDED.category_id,
                    brand_id = EXCLUDED.brand_id,
                    grade = EXCLUDED.grade,
                    made_in = EXCLUDED.made_in,
                    spec = EXCLUDED.spec,
                    shelf_life = EXCLUDED.shelf_life,
                    last_receipt_price = EXCLUDED.last_receipt_price,
                    retail_price = EXCLUDED.retail_price,
                    member_price = EXCLUDED.member_price,
                    vip_price = EXCLUDED.vip_price;
                """;
        try (Connection conn = PsqlUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertOrReplaceSql)) {
            // 子类实现具体的参数设置
            setSaveParameters(ps, scale);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save scale {}", scale, e);
            throw new PersistenceException(String.format("Can't save Scale(%s)", scale), e);
        }
    }

    private void setSaveParameters(PreparedStatement ps, T scale) {
    }
    // 通用Name序列化
    protected String toJson(Name name) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeStringField("name", name.name());
            generator.writeStringField("mnemonic", name.mnemonic());
            generator.writeStringField("alias", name.alias());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write name as json", e);
            throw new RuntimeException("Failed to serialize Name: " + name, e);
        }
        return writer.toString();
    }

    // 通用MadeIn序列化
    protected String toJson(MadeIn madeIn) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeStringField("_class", madeIn.getClass().getSimpleName());
            generator.writeStringField("code", madeIn.code());
            generator.writeStringField("madeIn", madeIn.madeIn());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write madeIn {} as json", madeIn, e);
            throw new RuntimeException("Failed to serialize MadeIn: " + madeIn, e);
        }
        return writer.toString();
    }

}
