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

package catalog.hoprxi.core.infrastructure.persistence.postgresql;

import catalog.hoprxi.core.domain.model.*;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import com.fasterxml.jackson.core.*;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-03-08
 */
public class PsqlProhibitSellItemRepository implements ProhibitSellItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlProhibitSellItemRepository.class);
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static Constructor<ProhibitSellItem> prohibitSellItemConstructor;

    static {
        try {
            prohibitSellItemConstructor = ProhibitSellItem.class.getDeclaredConstructor(long.class, Barcode.class, Name.class, MadeIn.class,  Specification.class,
                    GradeEnum.class, ShelfLife.class, LastReceiptPrice.class, RetailPrice.class, MemberPrice.class, VipPrice.class, long.class, long.class);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query has such constructor", e);
        }
    }

    @Override
    public ProhibitSellItem find(long id) {
        final String findSql = """
                SELECT id, name, barcode, category_id, brand_id, grade, made_in, spec, shelf_life,
                       retail_price, last_receipt_price, member_price, vip_price
                FROM item 
                WHERE id = ? 
                LIMIT 1
                """;
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(findSql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rebuild(rs);
                }
            }
            return null;// 如果没有返回数据，正常返回 null，不要抛异常
        } catch (SQLException e) {// 【关键修复 2】区分数据库异常和业务异常
            LOGGER.warn("Database error while fetching item id={}", id, e);
            throw new PersistenceException("Failed to query item from database: " + id, e);
        } catch (IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {// 重建对象时的错误属于系统内部错误，不是“未找到”
            LOGGER.error("Failed to rebuild item object from result set (id={})", id, e);
            throw new PersistenceException("Failed to reconstruct item object: " + id, e);
        }
    }

    private static ProhibitSellItem rebuild(ResultSet rs) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        long id = rs.getLong("id");
        Name name = PsqlProhibitSellItemRepository.buildName(rs.getString("name"));
        Barcode barcode = BarcodeGenerateServices.createBarcode(rs.getString("barcode"));
        long categoryId = rs.getLong("category_id");
        long brandId = rs.getLong("brand_id");
        GradeEnum grade = GradeEnum.valueOf(rs.getString("grade"));
        MadeIn madeIn = buildMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.create(rs.getInt("shelf_life"));

        LastReceiptPrice lastReceiptPrice = buildLastReceiptPricePrice(rs.getString("last_receipt_price"));
        RetailPrice retailPrice = buildRetailPrice(rs.getString("retail_price"));
        MemberPrice memberPrice = buildMemberPricePrice(rs.getString("member_price"));
        VipPrice vipPrice = buildVipPrice(rs.getString("vip_price"));
        return prohibitSellItemConstructor.newInstance(id, barcode, name, madeIn, spec, grade, shelfLife,
                lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
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

    private static MadeIn buildMadeIn(String json) throws IOException {
        if (json == null || json.isEmpty()) {
            return MadeIn.UNKNOWN;
        }
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
        if (_class != null && _class.endsWith("Domestic")) {
            return new Domestic(code, madeIn);
        } else if (_class != null && _class.endsWith("Imported")) {
            return new Imported(code, madeIn);
        }
        return MadeIn.UNKNOWN;
    }

    private static LastReceiptPrice buildLastReceiptPricePrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return LastReceiptPrice.RMB_PCS_ZERO;
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return LastReceiptPrice.RMB_PCS_ZERO;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (JsonToken.FIELD_NAME == parser.currentToken() && "name".equals(parser.currentName())) {
                    name = parser.nextTextValue();
                } else if (JsonToken.FIELD_NAME == parser.currentToken() && "price".equals(parser.currentName())) {
                    price = buildPrice(parser);
                }
            }
        }
        return new LastReceiptPrice(name, price);
    }

    private static RetailPrice buildRetailPrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return RetailPrice.RMB_PCS_ZERO; // 调用方需处理 null，或返回对应的 EMPTY 对象
        }
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            Price price = buildPrice(parser);
            return new RetailPrice(price);
        }
    }

    private static MemberPrice buildMemberPricePrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return MemberPrice.RMB_PCS_ZERO; // 调用方需处理 null，或返回对应的 EMPTY 对象
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return MemberPrice.RMB_PCS_ZERO;
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

    private static VipPrice buildVipPrice(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return VipPrice.RMB_PCS_ZERO; // 调用方需处理 null，或返回对应的 EMPTY 对象
        }
        String name = "";
        Price price = Price.zero(Locale.getDefault());
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return VipPrice.RMB_PCS_ZERO;
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

    private static Price buildPrice(JsonParser parser) throws IOException {
        if (parser.nextToken() != JsonToken.START_OBJECT) return Price.RMB_PCS_ZERO;
        BigDecimal number = BigDecimal.ZERO;
        String currency = "CNY";
        UnitEnum unit = UnitEnum.PCS;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String filedName = parser.currentName();
                parser.nextToken();
                switch (filedName) {
                    case "number" -> number = parser.getDecimalValue();
                    case "currencyCode" -> currency = parser.getValueAsString();
                    case "unit" -> unit = UnitEnum.valueOf(parser.getValueAsString());
                }
            }
        }
        return new Price(Money.of(number, currency), unit);
    }


    @Override
    public void delete(long id) {
        final String delSql = "delete from prohibitSellItem where id=?";
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(delSql)) {
            preparedStatement.setLong(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't remove from item(id={})", id, e);
            throw new PersistenceException(String.format("Can't delte from item(id=%s)", id), e);
        }
    }

    @Override
    public void save(ProhibitSellItem item) {
        final String insertOrReplaceSql = """
                INSERT INTO item (
                    id, name, barcode, category_id, brand_id, grade, made_in, spec, shelf_life,
                    last_receipt_price, retail_price, member_price, vip_price
                ) VALUES (
                    ?, ?::jsonb, ?, ?, ?, ?::grade, ?::jsonb, ?, ?,
                    ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    barcode = EXCLUDED.barcode,
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
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertOrReplaceSql)) {
            int idx = 1;
            ps.setLong(idx++, item.id());
            ps.setString(idx++, toJson(item.name()));
            ps.setString(idx++, String.valueOf(item.barcode().barcode()));
            ps.setLong(idx++, item.categoryId());
            ps.setLong(idx++, item.brandId());
            ps.setString(idx++, item.grade().name()); // 确保 enum 名称匹配 DB
            ps.setString(idx++, toJson(item.madeIn()));
            ps.setString(idx++, item.spec().value());
            ps.setInt(idx++, item.shelfLife().days());
            ps.setString(idx++, toJson(item.lastReceiptPrice()));
            ps.setString(idx++, toJson(item.retailPrice()));
            ps.setString(idx++, toJson(item.memberPrice()));
            ps.setString(idx++, toJson(item.vipPrice()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save item {}", item, e);
            throw new PersistenceException(String.format("Can't save Item(%s)", item), e);
        }
    }

    private static String toJson(MadeIn madeIn) {
        StringWriter writer = new StringWriter(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeStringField("_class", madeIn.getClass().getName());
            generator.writeStringField("code", madeIn.code());
            generator.writeStringField("madeIn", madeIn.madeIn());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write madeIn {} as json", madeIn, e);
            throw new RuntimeException("Failed to serialize MadeIn: " + madeIn, e);
        }
        return writer.toString();
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
}
