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

import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.application.query.SortField;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.DataSourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.Cache;
import salt.hoprxi.cache.CacheFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-18
 */
public class PsqlBrandQuery implements BrandQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlBrandQuery.class);
    private static final Cache<String, Brand> CACHE = CacheFactory.build("brand");
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



    public Brand search(String id) {
        Brand brand = CACHE.get(id);
        if (brand != null)
            return brand;
        try (Connection connection = DataSourceUtil.getConnection()) {
            final String findSql = "select id,name::jsonb->>'name' name,name::jsonb->>'mnemonic' mnemonic,name::jsonb->>'alias' alias,about::jsonb->>'story' story, about::jsonb->>'since' since,about::jsonb->>'homepage' homepage,about::jsonb->>'logo' logo from brand where id=? limit 1";
            PreparedStatement preparedStatement = connection.prepareStatement(findSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                brand = rebuild(rs);
                CACHE.put(id, brand);
                return brand;
            }
        } catch (SQLException | IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.error("Can't rebuild brand with (id = {})", id, e);
        }
        return null;
    }


    public Brand[] queryAll(int offset, int limit) {
        final String query = "select id,name::jsonb->>'name' name,name::jsonb->>'mnemonic' mnemonic,name::jsonb->>'alias' alias,about::jsonb->>'story' story, about::jsonb->>'since' since,about::jsonb->>'homepage' homepage,about::jsonb->>'logo' logo " +
                "from brand a INNER JOIN (SELECT id FROM brand order by id desc offset ? LIMIT ?) b USING (id)";
        try (Connection connection = DataSourceUtil.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, offset);
            preparedStatement.setInt(2, limit);
            ResultSet rs = preparedStatement.executeQuery();
            return transform(rs);
        } catch (SQLException | IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.error("Can't rebuild brand", e);
        }
        return new Brand[0];
    }


    public Brand[] queryByName(String name) {
        final String query = "select id,name::jsonb->>'name' name,name::jsonb->>'mnemonic' mnemonic,name::jsonb->>'alias' alias,about::jsonb->>'story' story, about::jsonb->>'since' since,about::jsonb->>'homepage' homepage,about::jsonb->>'logo' logo from brand " +
                "where name::jsonb->>'name' ~ ? union select id,name::jsonb->>'name' name,name::jsonb->>'mnemonic' mnemonic,name::jsonb->>'alias' alias,about::jsonb->>'story' story, about::jsonb->>'since' since,about::jsonb->>'homepage' homepage,about::jsonb->>'logo' logo from brand " +
                "where name::jsonb->>'mnemonic' ~ ? union select id,name::jsonb->>'name' name,name::jsonb->>'mnemonic' mnemonic,name::jsonb->>'alias' alias,about::jsonb->>'story' story, about::jsonb->>'since' since,about::jsonb->>'homepage' homepage,about::jsonb->>'logo' logo from brand " +
                "where name::jsonb->>'alias' ~ ?";
        try (Connection connection = DataSourceUtil.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, name);
            ResultSet rs = preparedStatement.executeQuery();
            return transform(rs);
        } catch (SQLException | IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.error("Can't rebuild brand with (name = {})", name, e);
        }
        return new Brand[0];
    }

    private Brand[] transform(ResultSet rs) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Brand> brandList = new ArrayList<>();
        while (rs.next()) {
            brandList.add(rebuild(rs));
        }
        return brandList.toArray(new Brand[0]);
    }

    private Brand rebuild(ResultSet rs) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        long id = rs.getLong("id");
        if (Brand.UNDEFINED.id() == id)
            return Brand.UNDEFINED;
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        AboutBrand about = null;
        URL homepage = rs.getString("homepage") == null ? null : new URL(rs.getString("homepage"));
        Year since = rs.getString("since") == null ? null : Year.of(rs.getInt("since"));
        String story = rs.getString("story") == null ? null : rs.getString("story");
        URL logo = rs.getString("logo") == null ? null : new URL(rs.getString("logo"));
        if (homepage != null || since != null || story != null || logo != null)
            about = new AboutBrand(homepage, logo, since, story);
        return new Brand(id, name, about);
    }

    /*
        private AboutBrand toAboutBrand(String json) throws IOException {
            if (json == null)
                return null;
            String story = null;
            Year since = null;
            URL homepage = null, logo = null;
            JsonFactory jasonFactory = new JsonFactory();
            JsonParser parser = jasonFactory.createParser(json.getBytes(StandardCharsets.UTF_8));
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (JsonToken.FIELD_NAME == jsonToken) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "story":
                            story = parser.getValueAsString();
                            break;
                        case "since":
                            since = Year.valueOf(parser.getIntValue());
                            break;
                        case "homepage":
                            homepage = new URL(parser.getValueAsString());
                            break;
                        case "logo":
                            logo = new URL(parser.getValueAsString());
                            break;
                    }
                }
            }
            return new AboutBrand(homepage, logo, since, story);
        }
     */

    public int size() {
        final String query = "select count(*) from brand";
        try (Connection connection = DataSourceUtil.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next())
                return rs.getInt("count");
        } catch (SQLException e) {
            LOGGER.error("Can't get brand count", e);
        }
        return 0;
    }

    @Override
    public InputStream find(long id) {
        return BrandQuery.super.find(id);
    }

    @Override
    public InputStream search(String name, int offset, int size, SortField sortField) {
        return null;
    }

    @Override
    public InputStream search(String name, int size, String searchAfter, SortField sortField) {
        return null;
    }
}
