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
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.DataSourceUtil;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
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
 * @version 0.0.2 builder 2025-03-18
 */
public class PsqlCategoryRepository implements CategoryRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Category");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Name class no such constructor", e);
        }
    }

    @Override
    public Category find(long id) {
        try (Connection connection = DataSourceUtil.getConnection()) {
            final String findSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(findSql);
            preparedStatement.setLong(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            return rebuild(rs);
        } catch (SQLException e) {
            LOGGER.error("Database error", e);
            throw new SearchException("Database error", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild name", e);
            return null;
        }
    }

    private Category rebuild(ResultSet rs) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (rs.next()) {
            long id = rs.getLong("id");
            if (id == Category.UNDEFINED.id()) return Category.UNDEFINED;
            long parent_id = rs.getLong("parent_id");
            Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
            String description = rs.getString("description");
            URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
            return new Category(parent_id, id, name, description, icon);
        }
        return null;
    }

    @Override
    public long nextIdentity() {
        return LongId.generate();
    }

    @Override
    public void remove(long id) throws PersistenceException {
        try (Connection connection = DataSourceUtil.getConnection()) {
            final String removeSql = "select \"left\",\"right\",root_id from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(removeSql);
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int right = resultSet.getInt("right");
                int left = resultSet.getInt("left");
                int offset = right - left + 1;
                long rootId = resultSet.getLong("root_id");
                connection.setAutoCommit(false);
                Statement statement = connection.createStatement();
                //移除自己及所有后代
                statement.addBatch("delete from category where \"left\">=" + left + " and \"right\"<=" + right + " and root_id=" + rootId);
                //所有大于left的其它类前移
                statement.addBatch("update category set \"left\"= \"left\"-" + offset + " where \"left\">" + left + " and root_id=" + rootId);
                statement.addBatch("update category set \"right\"= \"right\"-" + offset + " where \"right\">" + right + " and root_id=" + rootId);
                statement.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("Can't remove category(id = {}) and children", id, e);
            throw new PersistenceException(String.format("Can't remove category(id = %s) and children", id), e);
        }
    }

    @Override
    public Category[] root() {
        try (Connection connection = DataSourceUtil.getConnection()) {
            List<Category> categoryList = new ArrayList<>();
            final String rootSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri from category where id = parent_id";
            PreparedStatement preparedStatement = connection.prepareStatement(rootSql);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                if (id == Category.UNDEFINED.id()) {
                    categoryList.add(Category.UNDEFINED);
                    continue;
                }
                long parent_id = rs.getLong("parent_id");
                Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
                String description = rs.getString("description");
                URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
                categoryList.add(new Category(parent_id, id, name, description, icon));
            }
            return categoryList.toArray(new Category[0]);
        } catch (SQLException e) {
            LOGGER.error("Database error", e);
            throw new SearchException("Database error", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild name", e);
            return new Category[0];
        }
    }

    @Override
    public void save(Category category) {
        Objects.requireNonNull(category, "category required");
        PGobject name = new PGobject();
        name.setType("jsonb");
        try (Connection connection = DataSourceUtil.getConnection()) {
            final String isExistsSql = "select id,parent_id,\"left\",\"right\",root_id from category where id=?";
            PreparedStatement preparedStatement = connection.prepareStatement(isExistsSql);
            preparedStatement.setLong(1, category.id());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) { //exists->update
                if (category.parentId() == resultSet.getLong("parent_id")) {  //not move from old tree
                    final String updateSql = "update category set name=?,description=?,logo_uri=? where id=?";
                    PreparedStatement ps1 = connection.prepareStatement(updateSql);
                    name.setValue(toJson(category.name()));
                    ps1.setObject(1, name);
                    ps1.setString(2, category.description());
                    ps1.setString(3, category.icon() == null ? null : category.icon().toASCIIString());
                    ps1.setLong(4, category.id());
                    ps1.executeUpdate();
                } else {//move to new tree node
                    moveCategory(connection, category, resultSet.getInt("left"), resultSet.getInt("right"), resultSet.getLong("root_id"));
                }
            } else {//new
                if (category.isRoot()) {
                    final String insertRoot = "insert into category (id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values (?,?,?,?,?,?,1,2)";
                    PreparedStatement ps1 = connection.prepareStatement(insertRoot);
                    ps1.setLong(1, category.id());
                    ps1.setLong(2, category.parentId());
                    name.setValue(toJson(category.name()));
                    ps1.setObject(3, name);
                    ps1.setString(4, category.description());
                    ps1.setString(5, category.icon() == null ? null : category.icon().toASCIIString());
                    ps1.setLong(6, category.id());
                    ps1.executeUpdate();
                } else {
                    insertNewCategory(category, connection);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Can't save category{}", category, e);
            throw new PersistenceException(String.format("Can't save category(%s)", category), e);
        }
    }

    private void moveCategory(Connection connection, Category category, int left, int right, long originalRootId) throws SQLException {
        int offset = right - left + 1;
        int targetRight = 0;
        long targetRootId = -1L;
        //获取数据库中父节点
        final String targetSql = "select \"right\",root_id from category where id=?";
        PreparedStatement ps = connection.prepareStatement(targetSql);
        ps.setLong(1, category.parentId());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            targetRootId = rs.getLong("root_id");
            targetRight = rs.getInt("right");
            //目标节点位于被移动节点后面
            if (targetRootId == originalRootId && targetRight > right)
                targetRight = targetRight - offset;
        }
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        //需要移动的节点及其子节点的 left, right 值置为负,归属到新的树形（root_id),移动节点的顶点parent_id设置为新的父节点的id值
        statement.addBatch("update category set \"left\"=0-\"left\",\"right\"=0-\"right\",root_id=" + targetRootId + " where \"left\">=" + left + " and \"right\"<=" + right + " and root_id=" + originalRootId);
        StringBuilder updateSql = new StringBuilder("update category set parent_id=").append(category.parentId())
                .append(",name='").append(toJson(category.name())).append("'")
                .append(",description='").append(category.description()).append("'")
                .append(",logo_uri='").append(category.icon() == null ? category.icon() : category.icon().toASCIIString()).append("'")
                .append(" where id=").append(category.id());
        statement.addBatch(updateSql.toString());
        //原始树中被移动节点（含子节点）后面的节点往前移, 填充空缺位置
        statement.addBatch("update category set \"left\"= \"left\"-" + offset + " where \"left\">" + left + " and root_id=" + originalRootId);
        statement.addBatch("update category set \"right\"= \"right\"-" + offset + " where \"right\">" + right + " and root_id=" + originalRootId);
        //目标树被移动到父节点的队尾留出移动项目的空间
        statement.addBatch("update category set \"left\"= \"left\"+" + offset + " where \"left\">" + targetRight + " and root_id=" + targetRootId);
        statement.addBatch("update category set \"right\"= \"right\"+" + offset + " where \"right\">=" + targetRight + " and root_id=" + targetRootId);
        //将负值记录填充到正确位置,队尾位置
        statement.addBatch("update category set \"left\"=0-\"left\"-(" + (left - targetRight) + "),\"right\"=0-\"right\"-(" + (left - targetRight) + ") where \"left\"<0 and root_id=" + targetRootId);
        //System.out.println("handle category set \"left\"=0-\"left\"-(" + (left - targetRight) + "),\"right\"=0-\"right\"-(" + (left - targetRight) + ") where \"left\"<0");
        statement.executeBatch();
        connection.commit();
        connection.setAutoCommit(true);
    }


    private void insertNewCategory(Category category, Connection connection) throws SQLException {
        final String parentSql = "select \"right\",root_id from category where id=?";
        PreparedStatement ps = connection.prepareStatement(parentSql);
        ps.setLong(1, category.parentId());
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
            //System.out.println(insertSql.toString());
            statement.addBatch(insertSql.toString());
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        }
    }


    private String toJson(Name name) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("name", name.name());
            generator.writeStringField("mnemonic", name.mnemonic());
            generator.writeStringField("alias", name.alias());
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Not write name as json", e);
        }
        return output.toString();
    }
}
