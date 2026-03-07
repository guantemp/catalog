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
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import catalog.hoprxi.scale.domain.model.Count;
import catalog.hoprxi.scale.domain.model.CountRepository;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.price.*;
import catalog.hoprxi.scale.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

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
    public Count find(Plu plu) {
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
                    return PsqlCountRepository.rebuild(rs);
                }
            }
            return null;// 如果没有返回数据，正常返回 null，不要抛异常
        } catch (SQLException e) {// 【关键修复 2】区分数据库异常和业务异常
            LOGGER.error("Database access failure while fetching count for PLU={}", plu.id(), e);
            throw new PersistenceException("Database query execution failed for PLU " + plu.id() + ": " + e.getMessage(), e);
        } catch (IOException e) {// 重建对象时的错误属于系统内部错误，不是“未找到”
            LOGGER.error("Critical failure: Unable to map result set to count object (id={})", plu.id(), e);
            throw new PersistenceException("Internal error: Failed to reconstruct count entity from database result (ID: " + plu.id() + ")", e);
        }
    }

    private static Count rebuild(ResultSet rs) throws SQLException, IOException {
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

    private static VipPrice buildVipPrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return VipPrice.zero(Locale.getDefault());
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return VipPrice.zero(Locale.getDefault());
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (JsonToken.FIELD_NAME == parser.currentToken() && "name".equals(parser.currentName())) {
                    name = parser.nextTextValue();
                } else if (JsonToken.FIELD_NAME == parser.currentToken() && "price".equals(parser.currentName())) {
                    price = buildPrice(parser);
                }
            }
        }
        return new VipPrice(name, price);
    }

    private static MemberPrice buildMemberPricePrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return MemberPrice.zero(Locale.getDefault());
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return MemberPrice.zero(Locale.getDefault());
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (JsonToken.FIELD_NAME == parser.currentToken() && "name".equals(parser.currentName())) {
                    name = parser.nextTextValue();
                } else if (JsonToken.FIELD_NAME == parser.currentToken() && "price".equals(parser.currentName())) {
                    price = buildPrice(parser);
                }
            }
        }
        return new MemberPrice(name, price);
    }

    private static RetailPrice buildRetailPrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return RetailPrice.zero(Locale.getDefault());
        }
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            Price price = PsqlCountRepository.buildPrice(parser);
            return new RetailPrice(price);
        }
    }

    private static LastReceiptPrice buildLastReceiptPricePrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return LastReceiptPrice.zero(Locale.getDefault());
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return LastReceiptPrice.zero(Locale.getDefault());
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (JsonToken.FIELD_NAME == parser.currentToken() && "name".equals(parser.currentName())) {
                    name = parser.nextTextValue();
                } else if (JsonToken.FIELD_NAME == parser.currentToken() && "price".equals(parser.currentName())) {
                    price = PsqlCountRepository.buildPrice(parser);
                }
            }
        }
        return new LastReceiptPrice(name, price);
    }

    private static Price buildPrice(JsonParser parser) throws IOException {
        if (parser.nextToken() != JsonToken.START_OBJECT) return Price.zero(Locale.getDefault());
        BigDecimal number = BigDecimal.ZERO;
        String currency = Locale.getDefault().getCountry();
        UnitEnum unit = UnitEnum.PCS;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String filedName = parser.currentName();
                parser.nextToken();
                switch (filedName) {
                    case "number" -> number = parser.getDecimalValue();
                    case "currencyCode" -> currency = parser.getValueAsString();
                    case "unit" -> unit = UnitEnum.of(parser.getValueAsString());
                }
            }
        }
        return new Price(Money.of(number, currency), unit);
    }

    private static MadeIn buildMadeIn(String json) throws IOException {
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

    private static Name buildName(String json) throws IOException {
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
            int idx = 1;
            ps.setLong(idx++, count.plu().id());
            ps.setString(idx++, toJson(count.name()));
            ps.setLong(idx++, count.categoryId());
            ps.setLong(idx++, count.brandId());
            ps.setString(idx++, count.grade().name()); // 确保 enum 名称匹配 DB
            ps.setString(idx++, toJson(count.madeIn()));
            ps.setString(idx++, count.spec().value());
            ps.setInt(idx++, count.shelfLife().days());
            ps.setString(idx++, toJson(count.lastReceiptPrice()));
            ps.setString(idx++, toJson(count.retailPrice()));
            ps.setString(idx++, toJson(count.memberPrice()));
            ps.setString(idx++, toJson(count.vipPrice()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save count {}", count, e);
            throw new PersistenceException(String.format("Can't save Weight(%s)", count), e);
        }
    }

    private static String toJson(Name name) {
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

    private static String toJson(MadeIn madeIn) {
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

    private static String toJson(LastReceiptPrice lastReceiptPrice) {
        return priceToJsonWithName(lastReceiptPrice.name(), lastReceiptPrice.price());
    }

    private static String toJson(VipPrice vipPrice) {
        return priceToJsonWithName(vipPrice.name(), vipPrice.price());
    }

    private static String toJson(MemberPrice memberPrice) {
        return priceToJsonWithName(memberPrice.name(), memberPrice.price());
    }

    private static String priceToJsonWithName(String name, Price price) {
        StringWriter writer = new StringWriter(96);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeStringField("name", name);
            generator.writeObjectFieldStart("price");
            generator.writeNumberField("number", price.amount().getNumber().numberValue(BigDecimal.class));
            generator.writeStringField("currencyCode", price.amount().getCurrency().getCurrencyCode());
            //generator.writeNumberField("precision", price.amount().getNumber().getPrecision());
            //generator.writeStringField("roundingMode", price.amount().getContext().get("java.math.RoundingMode", RoundingMode.class).name());
            generator.writeStringField("unit", price.unit().name());
            generator.writeEndObject();
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write Price as json", e);
            throw new RuntimeException("Failed to serialize Price (with name)", e);
        }
        return writer.toString();
    }

    private static String toJson(RetailPrice retailPrice) {
        StringWriter writer = new StringWriter(96);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("number", retailPrice.price().amount().getNumber().numberValue(BigDecimal.class));
            generator.writeStringField("currencyCode", retailPrice.price().amount().getCurrency().getCurrencyCode());
            generator.writeStringField("unit", retailPrice.price().unit().name());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write RetailPrice as json", e);
        }
        return writer.toString();
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
