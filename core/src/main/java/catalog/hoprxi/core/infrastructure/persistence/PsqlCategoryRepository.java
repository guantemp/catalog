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
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.id.LongId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
            final String findSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri from category where id=?";
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
            if (id.equals(Category.UNDEFINED.id())) return Category.UNDEFINED;
            String parent_id = rs.getString("parent_id");
            Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
            String description = rs.getString("description");
            URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
            return new Category(parent_id, id, name, description, icon);
        }
        return null;
    }

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String removeSql = "select \"left\",\"right\",root_id from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int right = resultSet.getInt("right");
                int left = resultSet.getInt("left");
                int offset = right - left + 1;
                long rootId = resultSet.getLong("root_id");
                connection.setAutoCommit(false);
                Statement statement = connection.createStatement();
                statement.addBatch("delete from category where \"left\">=" + left + " and \"right\"<=" + right + " and root_id=" + rootId);
                statement.addBatch("update category set \"left\"= \"left\"-" + offset + " where \"left\">" + left + " and root_id=" + rootId);
                statement.addBatch("update category set \"right\"= \"right\"-" + offset + "where \"right\">" + right + " and root_id=" + rootId);
                statement.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("Can't remove category(id = {}) and children", id, e);
        }
    }

    @Override
    public Category[] root() {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            List<Category> categoryList = new ArrayList<>();
            final String rootSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri from category where id = parent_id";
            PreparedStatement preparedStatement = connection.prepareStatement(rootSql);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                if (id.equals(Category.UNDEFINED.id())) {
                    categoryList.add(Category.UNDEFINED);
                    continue;
                }
                String parent_id = rs.getString("parent_id");
                Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
                String description = rs.getString("description");
                URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
                categoryList.add(new Category(parent_id, id, name, description, icon));
            }
            return categoryList.toArray(new Category[0]);
        } catch (SQLException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.error("Can't rebuild category", e);
        }
        return new Category[0];
    }

    @Override
    public void save(Category category) {
        PGobject name = new PGobject();
        name.setType("jsonb");
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String isExistsSql = "select id,parent_id,\"left\",\"right\",root_id from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(isExistsSql);
            preparedStatement.setLong(1, Long.parseLong(category.id()));
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                if (category.parentId().equals(resultSet.getString("parent_id"))) {
                    String updateSql = "update category set name=?,description=?,logo_uri=? where id=?";
                    PreparedStatement ps1 = connection.prepareStatement(updateSql);
                    name.setValue(toJson(category.name()));
                    ps1.setObject(1, name);
                    ps1.setString(2, category.description());
                    ps1.setString(3, category.icon() == null ? null : category.icon().toASCIIString());
                    ps1.setLong(4, Long.parseLong(category.id()));
                    ps1.executeUpdate();
                } else {
                    moveCategory(connection, category, resultSet.getInt("left"), resultSet.getInt("right"), resultSet.getLong("root_id"));
                }
            } else {
                if (category.isRoot()) {
                    final String insertRoot = "insert into category (id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values (?,?,?,?,?,?,1,2)";
                    PreparedStatement ps1 = connection.prepareStatement(insertRoot);
                    ps1.setLong(1, Long.parseLong(category.id()));
                    ps1.setLong(2, Long.parseLong(category.parentId()));
                    name.setValue(toJson(category.name()));
                    ps1.setObject(3, name);
                    ps1.setString(4, category.description());
                    ps1.setString(5, category.icon() == null ? null : category.icon().toASCIIString());
                    ps1.setLong(6, Long.parseLong(category.id()));
                    ps1.executeUpdate();
                } else {
                    insertNewCategory(category, connection);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Can't save category{}", category, e);
        }
    }

    private void moveCategory(Connection connection, Category category, int left, int right, long rootId) throws SQLException {
        int offset = right - left + 1;
        int targetRight = 0;
        long targetRootId = -1L;
        final String targetSql = "select \"right\",root_id from category where id=?";
        PreparedStatement ps = connection.prepareStatement(targetSql);
        ps.setLong(1, Long.parseLong(category.parentId()));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            targetRootId = rs.getLong("root_id");
            targetRight = rs.getInt("right");
            //目标节点位于被移动节点后面
            if (targetRootId == rootId && targetRight > right)
                targetRight = targetRight - offset;
        }

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        //需要移动的节点及子节点 left, right 值置为负值,归属到新的树形（root_id),移动节点的顶点parent_id设置为新的父节点的id值
        statement.addBatch("update category set \"left\"=0-\"left\",\"right\"=0-\"right\",root_id=" + targetRootId + " where \"left\">=" + left + " and \"right\"<=" + right);
        StringBuilder updateSql = new StringBuilder("update category set parent_id=").append(category.parentId()).append(",name='").append(toJson(category.name())).append("'");
        if (category.description() != null)
            updateSql.append(",description='").append(category.description()).append("'");
        if (category.icon() != null)
            updateSql.append(",logo_uri='").append(category.icon().toASCIIString()).append("'");
        updateSql.append(" where id=").append(category.id());
        statement.addBatch(updateSql.toString());
        //被移动节点及子节点后面的节点往前移, 填充空缺位置
        statement.addBatch("update category set \"left\"= \"left\"-" + offset + " where \"left\">" + left + " and root_id=" + rootId);
        statement.addBatch("update category set \"right\"= \"right\"-" + offset + " where \"right\">" + right + " and root_id=" + rootId);
        //被移动到父节点的队尾留出移动项目的空间
        statement.addBatch("update category set \"left\"= \"left\"+" + offset + " where \"left\">" + targetRight + " and root_id=" + targetRootId);
        statement.addBatch("update category set \"right\"= \"right\"+" + offset + " where \"right\">=" + targetRight + " and root_id=" + targetRootId);
        //将负值记录填充到正确位置,队尾位置
        statement.addBatch("update category set \"left\"=0-\"left\"-(" + (left - targetRight) + "),\"right\"=0-\"right\"-(" + (right - targetRight - 1) + ") where \"left\"<0");
        statement.executeBatch();
        connection.commit();
        connection.setAutoCommit(true);
    }


    private void insertNewCategory(Category category, Connection connection) throws SQLException {
        final String parentSql = "select \"right\",root_id from category where id=?";
        PreparedStatement ps = connection.prepareStatement(parentSql);
        ps.setLong(1, Long.parseLong(category.parentId()));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int right = rs.getInt("right");
            long rootId = rs.getLong("root_id");
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.addBatch("update category set \"right\"=\"right\"+2 where \"right\">=" + right + " and root_id=" + rootId);
            statement.addBatch("update category set \"left\"= \"left\"+2 where \"left\">" + right + " and root_id=" + rootId);
            StringBuilder insertSql = new StringBuilder("insert into category(id,parent_id,name,root_id,\"left\",\"right\",description,logo_uri) values(").append(category.id()).append(",").append(category.parentId()).append(",'").append(toJson(category.name())).append("',").append(rootId).append(",").append(right).append(",").append(right + 1).append(",");
            if (category.description() != null)
                insertSql.append("'").append(category.description()).append("'");
            else insertSql.append((String) null);
            insertSql.append(",");
            if (category.icon() != null)
                insertSql.append("'").append(category.icon().toASCIIString()).append("'");
            else insertSql.append((String) null);
            insertSql.append(")");
            statement.addBatch(insertSql.toString());
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
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
