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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.ItemQuery2;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.Cache;
import salt.hoprxi.cache.CacheFactory;
import salt.hoprxi.utils.NumberHelper;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2023-08-13
 */
public class PsqlItemQuery implements ItemQuery2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlItemQuery.class);
    private static final Cache<String, ItemView> CACHE = CacheFactory.build("itemView");
    private final JsonFactory jasonFactory = JsonFactory.builder().build();
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Not query Name class has such constructor", e);
        }
    }


    @Override
    public ItemView query(String id) {
        ItemView itemView = CACHE.get(id);
        if (itemView != null) return itemView;
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id, i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item  i,category  c,brand  b where i.id= ? and i.category_id = c.id and i.brand_id = b.id limit 1";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                itemView = rebuild(rs);
                CACHE.put(id, itemView);
                return itemView;
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView with (id = {})", id, e);
        }
        return null;
    }

    @Override
    public ItemView[] belongToBrand(String brandId, long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from item i left join brand b on i.brand_id = b.id left join category c on c.id = i.category_id where b.id = ? offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(brandId));
            ps.setLong(2, offset);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] belongToCategory(String categoryId, long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where c.id = ? offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, NumberHelper.longOf(categoryId, 0l));
            ps.setLong(2, offset);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] belongToCategoryAndDescendants(String categoryId, long offset, int limit) {
        categoryId = categoryId.trim();
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic'  mnemonic,i.name::jsonb ->> 'alias'  alias,i.barcode,\n" +
                    "i.category_id,c.name::jsonb ->> 'name'  category_name,i.brand_id,b.name::jsonb ->> 'name'  brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from category c left join item i on i.category_id = c.id left join brand b on b.id = i.brand_id\n" +
                    "where c.id in (select id from category where root_id = (select root_id from category where id = ?) and \"left\" >= (select \"left\" from category where id = ?)\n" +
                    "and \"right\" <= (select \"right\" from category where id =?)) offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            long cid = NumberHelper.longOf(categoryId, 0l);
            ps.setLong(1, cid);
            ps.setLong(2, cid);
            ps.setLong(3, cid);
            ps.setLong(4, offset);
            ps.setInt(5, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }
/*
    public ItemView[] belongToCategoryTest(String categoryId) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String sql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic'  mnemonic,i.name::jsonb ->> 'alias'  alias,i.barcode,\n" +
                    "i.category_id,c.name::jsonb ->> 'name'  category_name,i.brand_id,b.name::jsonb ->> 'name'  brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id\n" +
                    "where c.id in (select id from category where root_id = (select root_id from category where id = 14986369380716560) and \"left\" >= (select \"left\" from category where id = 14986369380716560)\n" +
                    "and \"right\" <= (select \"right\" from category where id = 14986369380716560))";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(categoryId));
            ps.setLong(2, Long.parseLong(categoryId));
            ps.setLong(3, Long.parseLong(categoryId));
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }
 */

    @Override
    public ItemView[] queryAll(long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" +
                    "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id limit ? offset ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setLong(2, offset);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    @Override
    public long size() {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select count(*) from item";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("count");
            }
        } catch (SQLException e) {
            LOGGER.error("Can't count itemView size", e);
        }
        return 0l;
    }

    @Override
    public ItemView[] queryByBarcode(String barcode) {
        barcode = barcode.trim();
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,\n" +
                    "i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setString(1, barcode);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] accurateQueryByBarcode(String barcode) {
        barcode = barcode.trim();
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,\n" +
                    "i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode = ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setString(1, barcode);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] queryByRegular(String regularExpression) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?" +
                    "union\n" +
                    "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'name')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                    "union\n" +
                    "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'alias')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                    "union\n" +
                    "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.\"name\" ->> 'mnemonic' ~ ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setString(1, regularExpression);
            ps.setString(2, regularExpression);
            ps.setString(3, regularExpression);
            ps.setString(4, regularExpression);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] queryByName(String expression, long offset, int limit) {
        final String sql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?" +
                "union\n" +
                "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'name')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                "union\n" +
                "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'alias')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                "union\n" +
                "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.\"name\" ->> 'mnemonic' ~ ? limit ? offset ?";
        try (Connection connection = PsqlUtil.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, expression);
            ps.setString(2, expression);
            ps.setString(3, expression);
            ps.setString(4, expression);
            ps.setInt(5, limit);
            ps.setLong(6, offset);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    private ItemView[] transform(ResultSet rs) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<ItemView> itemViews = new ArrayList<>();
        while (rs.next()) {
            itemViews.add(rebuild(rs));
        }
        return itemViews.toArray(new ItemView[0]);
    }

    private ItemView rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        String id = rs.getString("id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createBarcode(rs.getString("barcode"));
        GradeEnum grade = GradeEnum.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.create(rs.getInt("shelf_life"));
        ItemView itemView = new ItemView(id, barcode, name, madeIn, spec, grade, shelfLife);

        ItemView.CategoryView categoryView = new ItemView.CategoryView(rs.getString("category_id"), rs.getString("category_name"));
        itemView.setCategoryView(categoryView);
        ItemView.BrandView brandView = new ItemView.BrandView(rs.getString("brand_id"), rs.getString("brand_name"));
        itemView.setBrandView(brandView);

        String priceName = rs.getString("last_receipt_price_name");
        MonetaryAmount amount = Money.of(rs.getBigDecimal("last_receipt_price_number"), rs.getString("last_receipt_price_currencyCode"));
        UnitEnum unit = UnitEnum.valueOf(rs.getString("last_receipt_price_unit"));
        LastReceiptPrice lastReceiptPrice = new LastReceiptPrice(priceName, new Price(amount, unit));
        itemView.setLastReceiptPrice(lastReceiptPrice);
        amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        itemView.setRetailPrice(retailPrice);
        priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        itemView.setMemberPrice(memberPrice);
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        itemView.setVipPrice(vipPrice);
        itemView.setImages(toImages(rs.getBinaryStream("show")));
        //for (URI uri : toImages(rs.getBinaryStream("show")))
        //System.out.println(barcode + ":" + uri);
        return itemView;
    }

    private MadeIn toMadeIn(String json) throws IOException {
        String _class = null;
        String madeIn = null;
        String code = MadeIn.UNKNOWN.code();
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
                    case "madeIn":
                        madeIn = parser.getValueAsString();
                        break;
                    case "code":
                        code = parser.getValueAsString();
                        break;
                }
            }
        }
        if (MadeIn.UNKNOWN.code().equals(code)) {
            return MadeIn.UNKNOWN;
        } else if (Domestic.class.getName().equals(_class)) {
            return new Domestic(code, madeIn);
        } else if (Imported.class.getName().equals(_class)) {
            return new Imported(code, madeIn);
        }
        return MadeIn.UNKNOWN;
    }

    private URI[] toImages(InputStream is) throws IOException {
        URI[] result = new URI[0];
        JsonParser parser = jasonFactory.createParser(is);
        while (!parser.isClosed()) {
            if (JsonToken.FIELD_NAME == parser.nextToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "images":
                        result = readImages(parser);
                        break;
                }
            }
        }
        return result;
    }

    private URI[] readImages(JsonParser parser) throws IOException {
        List<URI> uriList = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            uriList.add(URI.create(parser.getValueAsString()));
        }
        return uriList.toArray(new URI[0]);
    }
}
