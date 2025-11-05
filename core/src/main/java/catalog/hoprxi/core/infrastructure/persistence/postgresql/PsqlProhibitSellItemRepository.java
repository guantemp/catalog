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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-03-08
 */
public class PsqlProhibitSellItemRepository implements ProhibitSellItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlProhibitSellItemRepository.class);

    private final String databaseName;
    private static Constructor<Name> nameConstructor;
    private static Constructor<ProhibitSellItem> prohibitSellItemConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class);
            prohibitSellItemConstructor = ProhibitSellItem.class.getDeclaredConstructor(String.class, Barcode.class, Name.class, MadeIn.class, Unit.class, Specification.class,
                    GradeEnum.class, ShelfLife.class, String.class, String.class);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query has such constructor", e);
        }
    }

    public PsqlProhibitSellItemRepository(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public ProhibitSellItem find(String id) {
        return null;
    }

    private ProhibitSellItem rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        String id = rs.getString("id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createBarcode(rs.getString("barcode"));
        String categoryId = rs.getString("category_id");
        String brandId = rs.getString("brand_id");
        GradeEnum grade = GradeEnum.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.create(rs.getInt("shelf_life"));

        MonetaryAmount amount = Money.of(rs.getBigDecimal("last_receipt_price_number"), rs.getString("last_receipt_price_currencyCode"));
        Unit unit = Unit.valueOf(rs.getString("last_receipt_price_unit"));
        String priceName = rs.getString("last_receipt_price_name");
        LastReceiptPrice lastReceiptPrice = new LastReceiptPrice(priceName, new Price(amount, unit));
        amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        return prohibitSellItemConstructor.newInstance(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    private MadeIn toMadeIn(String json) throws IOException {
        String _class = null, city = null, country = null, code = "156";
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
    public void remove(String id) {

    }

    @Override
    public void save(ProhibitSellItem item) {

    }
}
