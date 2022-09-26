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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
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
            LOGGER.error("Name class no such constructor", e);
        }
    }

    private final String databaseName;

    public PsqlCategoryRepository(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public Category find(String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select id,parent_id,name,description,icon from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(findSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            ResultSet rs = preparedStatement.executeQuery();
            return rebuild(rs);
        } catch (SQLException | IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.error("Can't rebuild category with (id = {})", id, e);
        }
        return null;
    }

    private Category rebuild(ResultSet rs) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (rs.next()) {
            String id = rs.getString("id");
            if (id.equals(Category.UNDEFINED.id()))
                return Category.UNDEFINED;
            String parent_id = rs.getString("parent_id");
            Name name = toName(rs.getString("name"));
            String description = rs.getString("description");
            URI icon = URI.create(rs.getString("icon"));
            return new Category(parent_id, id, name, description, icon);
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
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String rootSql = "select id,parent_id,name,description,logo-uri from category where id = parent_id";
            PreparedStatement preparedStatement = connection.prepareStatement(rootSql);
            ResultSet resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            LOGGER.error("Not save brand", e);
        }
        return new Category[0];
    }

    @Override
    public void save(Category category) {
        PGobject name = new PGobject();
        name.setType("jsonb");
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String isExistsSql = "select id,name from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(isExistsSql);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {

            } else {
                if (category.isRoot()) {
                    final String insertRoot = "insert into category (id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values (?,?,?,?,?,?,1,2)";
                    PreparedStatement ps1 = connection.prepareStatement(insertRoot);
                    ps1.setLong(1, Long.parseLong(category.id()));
                    ps1.setLong(2, Long.parseLong(category.parentId()));
                    name.setValue(toJson(category.name()));
                    ps1.setObject(3, name);
                    ps1.setString(4, category.description());
                    ps1.setString(5, category.icon().toASCIIString());
                    ps1.setLong(6, Long.parseLong(category.id()));
                    ps1.executeUpdate();
                } else {
                    final String brotherOfTheSameNameSql = "select \"right\",root_id from category where id=?";
                    PreparedStatement ps2 = connection.prepareStatement(brotherOfTheSameNameSql);
                    ps2.setLong(1, Long.parseLong(category.parentId()));
                    ResultSet rs1 = ps2.executeQuery();
                    if (rs1.next()) {
                        int right = rs1.getInt("right");
                        long root_id = rs1.getLong("root_id");
                        Statement statement = connection.createStatement();
                        connection.setAutoCommit(false);
                        statement.addBatch("update catgory set right=right+2 where right>=" + right);
                        statement.addBatch("update catgory set left=left+2 where left>" + right);
                        StringBuilder insertSql = new StringBuilder("insert into category(id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values(")
                                .append(category.id()).append(category.parentId()).append(toJson(category.name())).append(category.description())
                                .append(category.icon().toASCIIString()).append(root_id).append(right).append(right + 1);
                        statement.addBatch(insertSql.toString());
                        statement.executeBatch();
                        connection.commit();
                        connection.setAutoCommit(true);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Can't save category{}", category, e);
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
