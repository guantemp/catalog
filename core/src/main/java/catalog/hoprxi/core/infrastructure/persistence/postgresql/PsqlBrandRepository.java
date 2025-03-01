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

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.DataSourceUtil;
import com.fasterxml.jackson.core.*;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.Cache;
import salt.hoprxi.cache.CacheFactory;
import salt.hoprxi.id.LongId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-21
 */
public class PsqlBrandRepository implements BrandRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlBrandRepository.class);
    private static Constructor<Name> nameConstructor;

    private static Cache<String, Brand> cache = CacheFactory.build("brand");

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Name class no such constructor", e);
        }
    }


    @Override
    public Brand find(String id) {
        Brand brand = cache.get(id);
        if (brand == null) {
            try (Connection connection = DataSourceUtil.getConnection()) {
                final String findSql = "select id,name,about from brand where id=? limit 1";
                PreparedStatement preparedStatement = connection.prepareStatement(findSql);
                preparedStatement.setLong(1, Long.parseLong(id));
                ResultSet rs = preparedStatement.executeQuery();
                brand = rebuild(rs);
                cache.put(id, brand);
            } catch (SQLException e) {
                LOGGER.error("Can't rebuild brand with (id = {})", id, e);
            } catch (IOException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {

            }
        }
        return brand;
    }

    private Brand rebuild(ResultSet resultSet) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (resultSet.next()) {
            String id = resultSet.getString("id");
            if (Brand.UNDEFINED.id().equals(id))
                return Brand.UNDEFINED;
            Name name = toName(resultSet.getString("name"));
            AboutBrand about = toAboutBrand(resultSet.getString("about"));
            return new Brand(id, name, about);
        }
        return null;
    }

    private Name toName(String json) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String name = null, alias = null, mnemonic = null;
        JsonFactory jasonFactory = new JsonFactory();
        JsonParser parser = jasonFactory.createParser(json.getBytes(StandardCharsets.UTF_8));
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name":
                        name = parser.getValueAsString();
                        break;
                    case "alias":
                        alias = parser.getValueAsString();
                        break;
                    case "mnemonic":
                        mnemonic = parser.getValueAsString();
                        break;
                }
            }
        }
        return nameConstructor.newInstance(name, mnemonic, alias);
    }

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
                        since = Year.of(parser.getIntValue());
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

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        try (Connection connection = DataSourceUtil.getConnection()) {
            final String removeSql = "remove from brand where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't remove brand(id={})", id, e);
        }
    }

    @Override
    public void save(Brand brand) {
        PGobject name = new PGobject();
        name.setType("jsonb");
        PGobject about = new PGobject();
        about.setType("jsonb");
        try (Connection connection = DataSourceUtil.getConnection()) {
            name.setValue(toJson(brand.name()));
            about.setValue(toJson(brand.about()));
            //insert into brand (id,name,about) values (?,?::jsonb,?::jsonb) 没有用PGobject修饰的sql
            final String replaceInto = "insert into brand (id,name,about) values (?,?,?) on conflict(id) do update set name=?,about=?";
            PreparedStatement ps = connection.prepareStatement(replaceInto);
            ps.setLong(1, Long.parseLong(brand.id()));
            ps.setObject(2, name);
            ps.setObject(3, about);
            ps.setObject(4, name);
            ps.setObject(5, about);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save brand{}", brand, e);
        }
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

    private String toJson(AboutBrand about) {
        if (about == null) return null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonFactory jasonFactory = new JsonFactory();
        try (JsonGenerator generator = jasonFactory.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeNumberField("since", about.since().getValue());
            if (about.story() != null)
                generator.writeStringField("story", about.story());
            if (about.logo() != null)
                generator.writeStringField("logo", about.logo().toExternalForm());
            if (about.homepage() != null)
                generator.writeStringField("homepage", about.homepage().toExternalForm());
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Not write about as json", e);
        }
        return output.toString();
    }
}
