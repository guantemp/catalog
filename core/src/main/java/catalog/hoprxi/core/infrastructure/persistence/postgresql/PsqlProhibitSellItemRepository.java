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
 * @version 0.0.1 builder 2024-03-08
 */
public class PsqlProhibitSellItemRepository implements ProhibitSellItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlProhibitSellItemRepository.class);
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static Constructor<Name> nameConstructor;
    private static Constructor<ProhibitSellItem> prohibitSellItemConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class);
            prohibitSellItemConstructor = ProhibitSellItem.class.getDeclaredConstructor(Long.class, Barcode.class, Name.class, MadeIn.class, UnitEnum.class, Specification.class,
                    GradeEnum.class, ShelfLife.class, LastReceiptPrice.class, RetailPrice.class, MemberPrice.class, VipPrice.class, Long.class, Long.class);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query has such constructor", e);
        }
    }

    @Override
    public ProhibitSellItem find(long id) {
        return null;
    }

    private ProhibitSellItem rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
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
        return prohibitSellItemConstructor.newInstance(id, barcode, name, madeIn, unit, spec, grade, shelfLife,
                lastReceiptPrice, retailPrice, memberPrice, vipPrice, brandId, categoryId);
    }

    private MadeIn toMadeIn(String json) throws IOException {
        String _class = null;
        String city = null;
        String country = null;
        String code = "156";
        JsonFactory jasonFactory = new JsonFactory();
        JsonParser parser = jasonFactory.createParser(json.getBytes(StandardCharsets.UTF_8));
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "_class":
                        _class = parser.getValueAsString();
                        break;
                    case "city":
                        city = parser.getValueAsString();
                        break;
                    case "country":
                        country = parser.getValueAsString();
                        break;
                    case "code":
                        code = parser.getValueAsString();
                        break;
                }
            }
        }
        if ("156".equals(code))
            return MadeIn.UNKNOWN;
        else if (Domestic.class.getName().equals(_class)) {
            return new Domestic(code, city);
        } else if (Imported.class.getName().equals(_class)) {
            return new Imported(code, country);
        }
        return MadeIn.UNKNOWN;
    }

    @Override
    public void delete(long id) {
        final String removeSql = "delete from item where id=?";
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(removeSql)) {
            preparedStatement.setLong(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't remove from item(id={})", id, e);
            throw new PersistenceException(String.format("Can't remove from item(id=%s)", id), e);
        }
    }

    @Override
    public void save(ProhibitSellItem item) {
        final String insertOrReplaceSql = "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,last_receipt_price,retail_price,member_price,vip_price) " +
                "values (?,?::jsonb,?,?,?,?::grade,?::jsonb,?,?,?::jsonb,?::jsonb,?::jsonb,?::jsonb) " +
                "on conflict(id) do update set name=?::jsonb,barcode=?,category_id=?,brand_id=?,grade=?::grade,made_in=?::jsonb,spec=?,shelf_life=?,last_receipt_price=?::jsonb,retail_price=?::jsonb,member_price=?::jsonb,vip_price=EXCLUDED.vip_price";
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertOrReplaceSql)) {
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
