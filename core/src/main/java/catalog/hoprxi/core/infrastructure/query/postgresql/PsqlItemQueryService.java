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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.brand.Brand;
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

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-28
 */
public class PsqlItemQueryService implements ItemQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlItemQueryService.class);
    private static final Cache<String, ItemView> CACHE = CacheFactory.build("itemView");
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Not find Name class has such constructor", e);
        }
    }

    private final String databaseName;

    public PsqlItemQueryService(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public ItemView find(String id) {
        ItemView itemView = CACHE.get(id);
        if (itemView != null) return itemView;
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,\n" + "i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" +
                    "from item  i,category  c,brand  b\n" + "where i.id= ? and i.category_id = c.id and i.brand_id = b.id limit 1";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setLong(1, Long.parseLong(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                itemView = rebuild(rs);
                CACHE.put(id, itemView);
                return itemView;
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item with (id = {})", id, e);
        }
        return null;
    }

    @Override
    public ItemView[] belongToBrand(String brandId, int offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String sql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic'  mnemonic,i.name::jsonb ->> 'alias'  alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name'  category_name,i.brand_id,b.name::jsonb ->> 'name'  brand_name,\n" + "i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" + "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" + "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit\n" + "from brand b left join item i on i.brand_id = b.id left join category c on c.id = i.category_id\n" + "where b.id = ? offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(brandId));
            ps.setLong(2, offset);
            ps.setLong(3, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] belongToCategory(String categoryId, long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String sql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic'  mnemonic,i.name::jsonb ->> 'alias'  alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name'  category_name,i.brand_id,b.name::jsonb ->> 'name'  brand_name,\n" + "i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" + "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" + "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit\n" + "from category  c left join item i on i.category_id = c.id left join brand b on b.id = i.brand_id\n" + "where c.id = ? offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(categoryId));
            ps.setLong(2, offset);
            ps.setLong(3, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] belongToCategoryAndDescendants(String categoryId) {
        CategoryQueryService categoryQuery = new PsqlCategoryQueryService(databaseName);
        PsqlBrandQueryService brandQueryService = new PsqlBrandQueryService(databaseName);
        List<ItemView> itemViews = new ArrayList<>();
        CategoryView[] categoryViews = categoryQuery.descendants(categoryId);
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            for (CategoryView categoryView : categoryViews) {
                ItemView.CategoryView categoryViewTemp = new ItemView.CategoryView(categoryView.getId(), categoryView.getName().name());
                String itemViewQuerySql = "select id,name::jsonb ->> 'name' name, name::jsonb ->> 'mnemonic' mnemonic,name::jsonb ->> 'alias' alias,barcode,\n" + "brand_id,grade,made_in,specs,shelf_life,\n" + "retail_price::jsonb ->> 'number' retail_price_number,retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "member_price::jsonb ->> 'name' member_price_name,member_price::jsonb -> 'price' ->> 'number' member_price_number,member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "vip_price::jsonb ->> 'name' vip_price_name,vip_price::jsonb -> 'price' ->> 'number' vip_price_number,vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item where category_id = ?";
                PreparedStatement ps = connection.prepareStatement(itemViewQuerySql);
                ps.setLong(1, Long.parseLong(categoryView.getId()));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Brand brand = brandQueryService.find(rs.getString("brand_id"));
                    ItemView itemView = rebuildWith(rs, categoryViewTemp, new ItemView.BrandView(brand.id(), brand.name().name()));
                    itemViews.add(itemView);
                }
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return itemViews.toArray(new ItemView[0]);
    }

    private ItemView rebuildWith(ResultSet rs, ItemView.CategoryView categoryView, ItemView.BrandView brandView) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        String id = rs.getString("id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(rs.getString("barcode"));
        Grade grade = Grade.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("specs"));
        ShelfLife shelfLife = ShelfLife.rebuild(rs.getInt("shelf_life"));
        ItemView itemView = new ItemView(id, barcode, name, madeIn, spec, grade, shelfLife);
        itemView.setCategoryView(categoryView);
        itemView.setBrandView(brandView);
        MonetaryAmount amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        Unit unit = Unit.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        itemView.setRetailPrice(retailPrice);
        String priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        itemView.setMemberPrice(memberPrice);
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        itemView.setVipPrice(vipPrice);
        return itemView;
    }

    public ItemView[] belongToCategoryTest(String categoryId) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String sql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic'  mnemonic,i.name::jsonb ->> 'alias'  alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name'  category_name,i.brand_id,b.name::jsonb ->> 'name'  brand_name,\n" + "i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" + "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" + "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit\n" + "from item i, category c,brand b\n" +
                    "where c.id in (select id from category where root_id = (select root_id from category where id = ?)\n" +
                    "and \"left\" >= (select \"left\" from category where id = ?)\n" +
                    "and \"right\" <= (select \"right\" from category where id = ?))\n" +
                    "and i.category_id = c.id and i.brand_id = b.id";
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

    @Override
    public ItemView[] findAll(long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,\n" + "i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setLong(1, offset);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    @Override
    public long size() {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String sizeSql = "select count(*) from item";
            PreparedStatement ps = connection.prepareStatement(sizeSql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("count");
            }
        } catch (SQLException e) {
            LOGGER.error("Can't count item size", e);
        }
        return 0;
    }

    @Override
    public ItemView[] findByBarcode(String barcode) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setString(1, barcode);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    @Override
    public ItemView[] serach(String regularExpression) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.name::jsonb ->> 'name' ~ ?\n" + "union\n" + "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.name::jsonb ->> 'alias' ~ ?\n" + "union\n" + "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.name::jsonb ->> 'mnemonic' ~ ?\n" + "union\n" + "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" + "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.specs,i.shelf_life,\n" + "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" + "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" + "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit\n" + "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?";
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
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(rs.getString("barcode"));
        Grade grade = Grade.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("specs"));
        ShelfLife shelfLife = ShelfLife.rebuild(rs.getInt("shelf_life"));
        ItemView itemView = new ItemView(id, barcode, name, madeIn, spec, grade, shelfLife);

        ItemView.CategoryView categoryView = new ItemView.CategoryView(rs.getString("category_id"), rs.getString("category_name"));
        itemView.setCategoryView(categoryView);
        ItemView.BrandView brandView = new ItemView.BrandView(rs.getString("brand_id"), rs.getString("brand_name"));
        itemView.setBrandView(brandView);

        MonetaryAmount amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        Unit unit = Unit.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        itemView.setRetailPrice(retailPrice);
        String priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        itemView.setMemberPrice(memberPrice);
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = Unit.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        itemView.setVipPrice(vipPrice);
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
}
