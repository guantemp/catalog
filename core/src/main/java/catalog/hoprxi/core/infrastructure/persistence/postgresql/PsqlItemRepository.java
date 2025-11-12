/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.query.SearchException;
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
import salt.hoprxi.id.LongId;

import javax.money.MonetaryAmount;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2023-03-01
 */
public class PsqlItemRepository implements ItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlItemRepository.class);
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Name class no such constructor", e);
            throw new RuntimeException("Name class no such constructor", e);
        }
    }

    @Override
    public Item find(long id) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,barcode,category_id," +
                                   "brand_id,grade,made_in,spec,shelf_life,retail_price::jsonb->>'number' as retail_price_number,retail_price::jsonb->>'currencyCode' as retail_price_currencyCode,retail_price::jsonb->>'unit' as retail_price_unit," +
                                   "last_receipt_price::jsonb ->> 'name' last_receipt_price_name,last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit," +
                                   "member_price::jsonb ->> 'name' as member_price_name, member_price::jsonb -> 'price' ->> 'number' as member_price_number, member_price::jsonb -> 'price' ->> 'currencyCode' as member_price_currencyCode, member_price::jsonb -> 'price' ->> 'unit' as member_price_unit," +
                                   "vip_price::jsonb ->> 'name' as vip_price_name, vip_price::jsonb -> 'price' ->> 'number' as vip_price_number, vip_price::jsonb -> 'price' ->> 'currencyCode' as vip_price_currencyCode, vip_price::jsonb -> 'price' ->> 'unit' as vip_price_unit " +
                                   "from item where id=? limit 1";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rebuild(rs);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IOException |
                 SQLException e) {
            LOGGER.error("The item(id={}) not found", id, e);
            throw new SearchException(String.format("The item(id=%s) not found", id), e);
        }
        return null;
    }

    private Item rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        long id = rs.getLong("id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createBarcode(rs.getString("barcode"));
        long categoryId = rs.getLong("category_id");
        long brandId = rs.getLong("brand_id");
        GradeEnum grade = GradeEnum.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.create(rs.getInt("shelf_life"));

        MonetaryAmount amount = Money.of(rs.getBigDecimal("last_receipt_price_number"), rs.getString("last_receipt_price_currencyCode"));
        UnitEnum unit = UnitEnum.valueOf(rs.getString("last_receipt_price_unit"));
        String priceName = rs.getString("last_receipt_price_name");
        LastReceiptPrice lastReceiptPrice = new LastReceiptPrice(priceName, new Price(amount, unit));
        amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));

        return new Item(id, barcode, name, madeIn, spec, grade, shelfLife, lastReceiptPrice, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    private MadeIn toMadeIn(String json) throws IOException {
        String _class = null, madeIn = null, code = "156";
        try (JsonParser parser = JSON_FACTORY.createParser(json.getBytes(StandardCharsets.UTF_8))) {
            while (parser.nextToken() != null) {
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
        if (Domestic.class.getName().equals(_class)) {
            return new Domestic(code, madeIn);
        } else if (Imported.class.getName().equals(_class)) {
            return new Imported(code, madeIn);
        }
        return MadeIn.UNKNOWN;
    }

    @Override
    public long nextIdentity() {
        return LongId.generate();
    }

    @Override
    public void delete(long id) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String removeSql = "delete from item where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeSql);
            preparedStatement.setLong(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't remove from item(id={})", id, e);
            throw new PersistenceException(String.format("Can't remove from item(id=%s)", id), e);
        }
    }

    @Override
    public void save(Item item) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String insertOrReplaceSql = "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,last_receipt_price,retail_price,member_price,vip_price) " +
                                              "values (?,?::jsonb,?,?,?,?::grade,?::jsonb,?,?,?::jsonb,?::jsonb,?::jsonb,?::jsonb) " +
                                              "on conflict(id) do update set name=?::jsonb,barcode=?,category_id=?,brand_id=?,grade=?::grade,made_in=?::jsonb,spec=?,shelf_life=?,last_receipt_price=?::jsonb,retail_price=?::jsonb,member_price=?::jsonb,vip_price=EXCLUDED.vip_price";
            PreparedStatement ps = connection.prepareStatement(insertOrReplaceSql);
            ps.setLong(1, item.id());
            ps.setString(2, toJson(item.name()));
            ps.setString(3, String.valueOf(item.barcode().barcode()));
            ps.setLong(4, item.categoryId());
            ps.setLong(5, item.brandId());
            ps.setString(6, item.grade().name());
            ps.setString(7, toJson(item.madeIn()));
            ps.setString(8, item.spec().value());
            ps.setInt(9, item.shelfLife().days());
            ps.setString(10, toJson(item.lastReceiptPrice()));
            ps.setString(11, toJson(item.retailPrice()));
            ps.setString(12, toJson(item.memberPrice()));
            ps.setString(13, toJson(item.vipPrice()));
            ps.setString(14, toJson(item.name()));
            ps.setString(15, String.valueOf(item.barcode().barcode()));
            ps.setLong(16, item.categoryId());
            ps.setLong(17, item.brandId());
            ps.setString(18, item.grade().name());
            ps.setString(19, toJson(item.madeIn()));
            ps.setString(20, item.spec().value());
            ps.setInt(21, item.shelfLife().days());
            ps.setString(22, toJson(item.lastReceiptPrice()));
            ps.setString(23, toJson(item.retailPrice()));
            ps.setString(24, toJson(item.memberPrice()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save item {}", item, e);
            throw new PersistenceException(String.format("Can't save Item(%s)", item), e);
        }
    }

    private String toJson(MadeIn madeIn) {
        String _class = madeIn.getClass().getName();
        ByteArrayOutputStream output = new ByteArrayOutputStream(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("_class", _class);
            generator.writeStringField("code", madeIn.code());
            generator.writeStringField("madeIn", madeIn.madeIn());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write madeIn {} as json", madeIn, e);
        }
        //System.out.println(output.size());
        return output.toString();
    }

    private String toJson(Name name) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(128);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("name", name.name());
            generator.writeStringField("mnemonic", name.mnemonic());
            generator.writeStringField("alias", name.alias());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write name as json", e);
        }
        //System.out.println(output.size());
        return output.toString();
    }

    private String toJson(LastReceiptPrice lastReceiptPrice) {
        return priceToJsonWithName(lastReceiptPrice.name(), lastReceiptPrice.price());
    }

    private String toJson(VipPrice vipPrice) {
        return priceToJsonWithName(vipPrice.name(), vipPrice.price());
    }

    private String toJson(MemberPrice memberPrice) {
        return priceToJsonWithName(memberPrice.name(), memberPrice.price());
    }

    private String priceToJsonWithName(String name, Price price) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(96);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
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
        }
        return output.toString();
    }

    private String toJson(RetailPrice retailPrice) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(64);
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeNumberField("number", retailPrice.price().amount().getNumber().numberValue(BigDecimal.class));
            generator.writeStringField("currencyCode", retailPrice.price().amount().getCurrency().getCurrencyCode());
            //generator.writeNumberField("precision", retailPrice.price().amount().getNumber().getPrecision());
            //generator.writeStringField("roundingMode", retailPrice.price().amount().getContext().get("java.math.RoundingMode", RoundingMode.class).name());
            generator.writeStringField("unit", retailPrice.price().unit().name());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write RetailPrice as json", e);
        }
        return output.toString();
    }
}
