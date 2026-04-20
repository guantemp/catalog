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
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import com.fasterxml.jackson.core.*;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.id.LongId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-21
 */
public class PsqlBrandRepository implements BrandRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Override
    public Brand find(long id) {
        final String findSql = "select id,name,about from brand where id=?";
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement ps = connection.prepareStatement(findSql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery();) {
                return rebuild(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Database query failed for brand id={}", id, e);
            throw new SearchException("Database error when querying brand", e);
        } catch (IOException  e) {
            LOGGER.error("Failed to rebuild brand with id={}", id, e);
            // 4. 反序列化失败不返回 null，直接抛异常，避免上游空指针
            throw new SearchException("Failed to parse brand data from JSON", e);
        }
    }

    private Brand rebuild(ResultSet rs) throws SQLException, IOException {
        if (rs.next()) {
            long id = rs.getLong("id");
            if (Brand.UNDEFINED.id() == id)
                return Brand.UNDEFINED;
            Name name = PsqlBrandRepository.toName(rs.getString("name"));
            AboutBrand about = PsqlBrandRepository.toAboutBrand(rs.getString("about"));
            return new Brand(id, name, about);
        }
        return null;
    }

    private static Name toName(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return Name.EMPTY;
        }
        String name = null, shortName = null;
        try (JsonParser parser = JSON_FACTORY.createParser(json.getBytes(StandardCharsets.UTF_8))) {
            while (parser.nextToken() != null) {
                if (JsonToken.FIELD_NAME.equals(parser.currentToken())) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "name" -> name = parser.getValueAsString();
                        case "shortName" -> shortName = parser.getValueAsString();
                    }
                }
            }
            return new Name(name, shortName);
        }
    }

    private static AboutBrand toAboutBrand(String json) throws IOException {
        if (json == null)
            return null;
        String story = null;
        Year since = null;
        URL homepage = null, logo = null;
        try (JsonParser parser = JSON_FACTORY.createParser(json.getBytes(StandardCharsets.UTF_8))) {
            while (parser.nextToken() != null) {
                if (JsonToken.FIELD_NAME == parser.currentToken()) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "story" -> story = parser.getValueAsString();
                        case "since" -> since = Year.of(parser.getIntValue());
                        case "homepage" -> homepage = URI.create(parser.getValueAsString()).toURL();
                        case "logo" -> logo = URI.create(parser.getValueAsString()).toURL();
                    }
                }
            }
            return new AboutBrand(homepage, logo, since, story);
        }
    }

    @Override
    public long nextIdentity() {
        return LongId.generate();
    }

    @Override
    public void remove(long id) {
        final String DELETE_SQL = """
                DELETE FROM brand 
                WHERE id = ?
                """;
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_SQL)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't remove brand(id={})", id, e);
            throw new PersistenceException(String.format("Can't remove brand(id={%d})", id), e);
        }
    }

    @Override
    public void save(Brand brand) {
        Objects.requireNonNull(brand, "brand required");
        final String INSERT_OR_UPDATE_SQL = """
                INSERT INTO brand (id, name, about) 
                VALUES (?, ?, ?) 
                ON CONFLICT(id) DO UPDATE 
                SET name = EXCLUDED.name, about = EXCLUDED.about
                """;
        PGobject name = new PGobject();
        name.setType("jsonb");
        PGobject about = new PGobject();
        about.setType("jsonb");
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement ps = connection.prepareStatement(INSERT_OR_UPDATE_SQL)) {
            name.setValue(PsqlBrandRepository.toJson(brand.name()));
            about.setValue(PsqlBrandRepository.toJson(brand.about()));
            ps.setLong(1, brand.id());
            ps.setObject(2, name);
            ps.setObject(3, about);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Can't save brand{}", brand, e);
            throw new PersistenceException(String.format("Can't save brand(%s)", brand), e);
        }
    }

    private static String toJson(Name name) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("name", name.name());
            generator.writeStringField("shortName", name.shortName());
            generator.writeEndObject();
            generator.close();
            return output.toString();
        } catch (IOException e) {
            LOGGER.error("Not write name as json", e);
            throw new IllegalStateException("Failed to serialize Name object", e);
        }
    }

    private static String toJson(AboutBrand about) {
        if (about == null) return null;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeNumberField("since", about.since().getValue());
            if (about.story() != null)
                generator.writeStringField("story", about.story());
            if (about.logo() != null)
                generator.writeStringField("logo", about.logo().toExternalForm());
            if (about.homepage() != null)
                generator.writeStringField("homepage", about.homepage().toExternalForm());
            generator.writeEndObject();
            generator.close();
            return output.toString();
        } catch (IOException e) {
            LOGGER.error("Not write about as json", e);
            throw new IllegalStateException("Failed to serialize AboutBrand object", e);
        }
    }
}
