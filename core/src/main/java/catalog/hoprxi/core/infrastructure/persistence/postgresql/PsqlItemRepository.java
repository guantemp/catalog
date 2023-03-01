/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-11
 */
public class PsqlItemRepository implements ItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlItemRepository.class);
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query Name class has such constructor", e);
        }
    }

    private final String databaseName;

    public PsqlItemRepository(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public Item find(String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,barcode,category_id," +
                    "brand_id,grade,made_in,spec,shelf_life,retail_price::jsonb->>'number' as retail_price_number,retail_price::jsonb->>'currencyCode' as retail_price_currencyCode,retail_price::jsonb->>'unit' as retail_price_unit" +
                    ",member_price::jsonb ->> 'name' as member_price_name, member_price::jsonb -> 'price' ->> 'number' as member_price_number, member_price::jsonb -> 'price' ->> 'currencyCode' as member_price_currencyCode, member_price::jsonb -> 'price' ->> 'unit' as member_price_unit" +
                    ", vip_price::jsonb ->> 'name' as vip_price_name, vip_price::jsonb -> 'price' ->> 'number' as vip_price_number, vip_price::jsonb -> 'price' ->> 'currencyCode' as vip_price_currencyCode, vip_price::jsonb -> 'price' ->> 'unit' as vip_price_unit " +
                    "from item where id=? limit 1";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setLong(1, Long.parseLong(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rebuild(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item with (id = {})", id, e);
        }
        return null;
    }

    private Item rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        String id = rs.getString("id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(rs.getString("barcode"));
        String categoryId = rs.getString("category_id");
        String brandId = rs.getString("brand_id");
        Grade grade = Grade.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.rebuild(rs.getInt("shelf_life"));
        MonetaryAmount amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        Unit unit = Unit.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        String priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        return new Item(id, barcode, name, madeIn, spec, grade, shelfLife, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    private MadeIn toMadeIn(String json) throws IOException {
        String _class = null, city = null, country = null;
        int code = -1;
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
                        code = parser.getIntValue();
                        break;
                }
            }
        }
        if (code == 0)
            return Domestic.BLACK;
        if (Domestic.class.getName().equals(_class)) {
            return new Domestic(code, city);
        } else if (Imported.class.getName().equals(_class)) {
            return new Imported(code, country);
        }
        return Domestic.BLACK;
    }

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String removeSql = "delete from item where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't delete from item(id={})", id, e);
        }
    }

    @Override
    public void save(Item item) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String insertOrReplaceSql = "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) " +
                    "values (?,?::jsonb,?,?,?,?::grade,?::jsonb,?,?,?::jsonb,?::jsonb,?::jsonb) " +
                    "on conflict(id) do update set name=?::jsonb,barcode=?,category_id=?,brand_id=?,grade=?::grade,made_in=?::jsonb,spec=?,shelf_life=?,retail_price=?::jsonb,member_price=?::jsonb,vip_price=?::jsonb";
            PreparedStatement ps = connection.prepareStatement(insertOrReplaceSql);
            ps.setLong(1, Long.parseLong(item.id()));
            ps.setString(2, toJson(item.name()));
            ps.setString(3, String.valueOf(item.barcode().barcode()));
            ps.setLong(4, Long.parseLong(item.categoryId()));
            ps.setLong(5, Long.parseLong(item.brandId()));
            ps.setString(6, item.grade().name());
            ps.setString(7, toJson(item.madeIn()));
            ps.setString(8, item.spec().value());
            ps.setInt(9, item.shelfLife().days());
            ps.setString(10, toJson(item.retailPrice()));
            ps.setString(11, toJson(item.memberPrice()));
            ps.setString(12, toJson(item.vipPrice()));
            ps.setString(13, toJson(item.name()));
            ps.setString(14, String.valueOf(item.barcode().barcode()));
            ps.setLong(15, Long.parseLong(item.categoryId()));
            ps.setLong(16, Long.parseLong(item.brandId()));
            ps.setString(17, item.grade().name());
            ps.setString(18, toJson(item.madeIn()));
            ps.setString(19, item.spec().value());
            ps.setInt(20, item.shelfLife().days());
            ps.setString(21, toJson(item.retailPrice()));
            ps.setString(22, toJson(item.memberPrice()));
            ps.setString(23, toJson(item.vipPrice()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save item{}", item, e);
        }
    }

    private String toJson(MadeIn madeIn) {
        String _class = madeIn.getClass().getName();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonFactory jasonFactory = new JsonFactory();
        try (JsonGenerator generator = jasonFactory.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("_class", _class);
            generator.writeNumberField("code", madeIn.code());
            if (Domestic.class.getName().equals(_class)) {
                generator.writeStringField("city", ((Domestic) madeIn).city());
            } else if (Imported.class.getName().equals(_class)) {
                generator.writeStringField("country", ((Imported) madeIn).country());
            }
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Not write madeIn as json", e);
        }
        return output.toString();
    }

    private String toJson(Name name) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonFactory jasonFactory = new JsonFactory();
        try (JsonGenerator generator = jasonFactory.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("name", name.name());
            generator.writeStringField("mnemonic", name.mnemonic());
            generator.writeStringField("alias", name.alias());
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Not write name as json", e);
        }
        return output.toString();
    }

    private String toJson(VipPrice vipPrice) {
        return priceToJson(vipPrice.name(), vipPrice.price());
    }

    private String toJson(MemberPrice memberPrice) {
        return priceToJson(memberPrice.name(), memberPrice.price());
    }

    private String priceToJson(String name, Price price) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonFactory jasonFactory = new JsonFactory();
        try (JsonGenerator generator = jasonFactory.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("name", name);
            generator.writeObjectFieldStart("price");
            generator.writeNumberField("number", price.amount().getNumber().numberValue(BigDecimal.class));
            generator.writeStringField("currencyCode", price.amount().getCurrency().getCurrencyCode());
            generator.writeNumberField("precision", price.amount().getNumber().getPrecision());
            generator.writeStringField("roundingMode", price.amount().getContext().get("java.math.RoundingMode", RoundingMode.class).name());
            generator.writeStringField("unit", price.unit().name());
            generator.writeEndObject();
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Not write RetailPrice as json", e);
        }
        return output.toString();
    }

    private String toJson(RetailPrice retailPrice) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonFactory jasonFactory = new JsonFactory();
        try (JsonGenerator generator = jasonFactory.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeNumberField("number", retailPrice.price().amount().getNumber().numberValue(BigDecimal.class));
            generator.writeStringField("currencyCode", retailPrice.price().amount().getCurrency().getCurrencyCode());
            generator.writeNumberField("precision", retailPrice.price().amount().getNumber().getPrecision());
            generator.writeStringField("roundingMode", retailPrice.price().amount().getContext().get("java.math.RoundingMode", RoundingMode.class).name());
            generator.writeStringField("unit", retailPrice.price().unit().name());
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Not write RetailPrice as json", e);
        }
        return output.toString();
    }
}
