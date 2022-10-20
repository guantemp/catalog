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

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.application.Tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
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
 * @version 0.0.1 builder 2022-10-20
 */
public class PsqlCategoryQueryService implements CategoryQueryService {
    private static final String PLACEHOLDER = "placeholder";
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlCategoryQueryService.class);
    private static Tree<Category>[] trees;
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

    public PsqlCategoryQueryService(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
    }

    @Override
    public Category[] root() {
        if (trees != null) {
            Category[] categories = new Category[trees.length];
            for (int i = 0, j = trees.length; i < j; i++) {
                categories[i] = trees[i].root();
            }
            return categories;
        } else {
            List<Category> categoryList = new ArrayList<>();
            try (Connection connection = PsqlUtil.getConnection(databaseName)) {
                final String rootSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri from category where id = parent_id";
                PreparedStatement preparedStatement = connection.prepareStatement(rootSql);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    String id = rs.getString("id");
                    String parentId = rs.getString("parent_id");
                    Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
                    String description = rs.getString("description");
                    URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
                    categoryList.add(new Category(parentId, id, name, description, icon));
                }
                categoryList.toArray(new Category[0]);
            } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LOGGER.error("Can't rebuild category", e);
            }
            trees = new Tree[categoryList.size()];
            for (int i = 0, j = categoryList.size(); i < j; i++) {
                trees[i] = Tree.root(categoryList.get(i));
            }
            return categoryList.toArray(new Category[0]);
        }
    }

    @Override
    public Category find(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        Category identifiable = Category.root(id, PLACEHOLDER);
        for (Tree<Category> t : trees) {
            if (t.contain(identifiable)) return t.value(identifiable);
        }
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri from category where id=? limit 1";
            PreparedStatement preparedStatement = connection.prepareStatement(findSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) return rebuild(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild category with (id = {})", id, e);
        }
        return null;
    }

    private Category rebuild(ResultSet rs) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String id = rs.getString("id");
        if (id.equals(Category.UNDEFINED.id())) return Category.UNDEFINED;
        String parentId = rs.getString("parent_id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        String description = rs.getString("description");
        URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
        return new Category(parentId, id, name, description, icon);
    }


    @Override
    public Category[] children(String id) {
        return new Category[0];
    }

    @Override
    public Category[] descendants(String id) {
        return new Category[0];
    }

    @Override
    public Category[] searchName(String regularExpression) {
        return new Category[0];
    }

    @Override
    public Category[] siblings(String id) {
        return new Category[0];
    }

    @Override
    public Category[] path(String id) {
        return new Category[0];
    }

    @Override
    public int depth(String id) {
        return 0;
    }
}
