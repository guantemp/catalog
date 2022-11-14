/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-14
 */
public class PsqlItemQueryService implements ItemQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlItemQueryService.class);
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not find Name class has such constructor", e);
        }
    }

    private final String databaseName;

    public PsqlItemQueryService(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public ItemView find(String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,barcode,category_id," +
                    "brand_id,grade,made_in,specs,shelf_life,retail_price::jsonb->>'number' as retail_price_number,retail_price::jsonb->>'currencyCode' as retail_price_currencyCode,retail_price::jsonb->>'unit' as retail_price_unit" +
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

    private ItemView rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        String id = rs.getString("id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(rs.getString("barcode"));
        String categoryId = rs.getString("category_id");
        String brandId = rs.getString("brand_id");
        Grade grade = Grade.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("specs"));
        ShelfLife shelfLife = ShelfLife.rebuild(rs.getInt("shelf_life"));
        ItemView itemView = new ItemView(id, barcode, name, madeIn, spec, grade, shelfLife);
        return itemView;
    }

    private MadeIn toMadeIn(String json) throws IOException {
        String _class = null, province = null, city = null, country = null;
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
                    case "province":
                        province = parser.getValueAsString();
                        break;
                    case "city":
                        city = parser.getValueAsString();
                        break;
                    case "country":
                        country = parser.getValueAsString();
                        break;
                }
            }
        }
        if (Domestic.class.getName().equals(_class)) {
            return new Domestic(province, city);
        } else if (Imported.class.getName().equals(_class)) {
            return new Imported(country);
        }
        return null;
    }

    @Override
    public ItemView[] belongToBrand(String brandId, int offset, int limit) {
        return new ItemView[0];
    }

    @Override
    public ItemView[] belongToCategory(String categoryId, long offset, int limit) {
        return new ItemView[0];
    }

    @Override
    public ItemView[] findAll(long offset, int limit) {
        return new ItemView[0];
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public ItemView[] fromBarcode(String barcode) {
        return new ItemView[0];
    }

    @Override
    public ItemView[] fromName(String name) {
        return new ItemView[0];
    }
}
