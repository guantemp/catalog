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

package catalog.hoprxi.core.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.*;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.id.LongId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-09-21
 */
public class PsqlCategoryRepository implements CategoryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlCategoryRepository.class);
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Not find Name class has such constructor", e);
        }
    }

    private final String databaseName;

    public PsqlCategoryRepository(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public Category find(String id) {
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

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String removeSql = "delete from brand where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Not save brand", e);
        }
    }

    @Override
    public Category[] root() {
        return new Category[0];
    }

    @Override
    public void save(Category category) {
        PGobject name = new PGobject();
        name.setType("jsonb");
        PGobject about = new PGobject();
        about.setType("jsonb");
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            name.setValue(toJson(category.name()));
            final String replaceInto = "insert into category (parentId,id,name,description,icon) values (?,?,?,?,?,?) on conflict(id) do update set name=?,about=?";
            PreparedStatement preparedStatement = connection.prepareStatement(replaceInto);
            preparedStatement.setLong(1, Long.parseLong(category.id()));
            preparedStatement.setObject(2, name);
            preparedStatement.setObject(3, about);
            preparedStatement.setObject(4, name);
            preparedStatement.setObject(5, about);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Not save brand", e);
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
}
