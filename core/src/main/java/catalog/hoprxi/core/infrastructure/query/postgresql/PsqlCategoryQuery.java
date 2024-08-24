/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.query.CategoryQuery;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import event.hoprxi.domain.model.DomainEvent;
import event.hoprxi.domain.model.DomainEventSubscriber;
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
public class PsqlCategoryQuery implements CategoryQuery, DomainEventSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlCategoryQuery.class);
    private static Tree<CategoryView>[] trees;
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Not query Name class has such constructor", e);
        }
    }

    private final String databaseName;

    public PsqlCategoryQuery(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "The databaseName parameter is required");
        root();
    }

    @Override
    public CategoryView[] root() {
        if (trees != null) {
            CategoryView[] categoryViews = new CategoryView[trees.length];
            for (int i = 0, j = trees.length; i < j; i++) {
                categoryViews[i] = trees[i].root();
            }
            return categoryViews;
        } else {
            List<CategoryView> categoryViewList = new ArrayList<>();
            try (Connection connection = PsqlUtil.getConnection(databaseName)) {
                final String rootSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri,\"right\" - \"left\" as distance from category where id = parent_id";
                PreparedStatement preparedStatement = connection.prepareStatement(rootSql);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    categoryViewList.add(rebuild(rs));
                }
            } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LOGGER.error("Can't rebuild category", e);
            }
            trees = new Tree[categoryViewList.size()];
            for (int i = 0, j = categoryViewList.size(); i < j; i++) {
                trees[i] = Tree.root(categoryViewList.get(i));
            }
            return categoryViewList.toArray(new CategoryView[0]);
        }
    }

    @Override
    public CategoryView query(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView identifiable = CategoryView.identifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.contain(identifiable)) {
                return t.value(identifiable);
            }
        }
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String findSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri,\"right\" - \"left\" as distance from category where id=? limit 1";
            PreparedStatement preparedStatement = connection.prepareStatement(findSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                CategoryView view = rebuild(rs);
                CategoryView parentIdentifiable = CategoryView.identifiableCategoryView(view.getParentId());
                for (Tree<CategoryView> t : trees) {
                    if (t.contain(parentIdentifiable)) {
                        t.append(parentIdentifiable, view);
                        break;
                    }
                }
                return view;
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild category with (id = {})", id, e);
        }
        return null;
    }

    private CategoryView rebuild(ResultSet rs) throws SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String id = rs.getString("id");
        String parentId = rs.getString("parent_id");
        Name name = nameConstructor.newInstance(rs.getString("name"), rs.getString("mnemonic"), rs.getString("alias"));
        String description = rs.getString("description");
        URI icon = rs.getString("logo_uri") == null ? null : URI.create(rs.getString("logo_uri"));
        boolean isLeaf = rs.getInt("distance") == 1 ? true : false;
        return new CategoryView(parentId, id, name, description, icon, isLeaf);
    }


    @Override
    public CategoryView[] children(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] children = new CategoryView[0];
        CategoryView identifiable = CategoryView.identifiableCategoryView(id);
        for (Tree<CategoryView> tree : trees) {
            if (tree.contain(identifiable)) {
                if (tree.isLeaf(identifiable) && !tree.value(identifiable).isLeaf()) {
                    queryAndFillChildren(tree, identifiable, id);
                }
                children = tree.children(identifiable);
                break;
            }
        }
        return children;
    }

    private void queryAndFillChildren(Tree<CategoryView> tree, CategoryView parent, String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String childrenSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri,\"right\" - \"left\" as distance from category where parent_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(childrenSql);
            preparedStatement.setLong(1, Long.parseLong(id));
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                CategoryView view = rebuild(rs);
                tree.append(parent, view);
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild category view", e);
        }
    }

    @Override
    public CategoryView[] descendants(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] descendants = new CategoryView[0];
        CategoryView identifiable = CategoryView.identifiableCategoryView(id);
        for (Tree<CategoryView> tree : trees) {
            if (tree.contain(identifiable)) {
                if (tree.isLeaf(identifiable) && !tree.value(identifiable).isLeaf()) {
                    queryAndFillDescendants(tree, id);
                }
                descendants = tree.descendants(identifiable);
                break;
            }
        }
        return descendants;
    }

    public void queryAndFillDescendants(Tree<CategoryView> tree, String id) {
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String descendantsSql = "select id,parent_id,name::jsonb->>'name' as name,name::jsonb->>'mnemonic' as mnemonic,name::jsonb->>'alias' as alias,description,logo_uri,\"right\" - \"left\" as distance from category \n" +
                    "where root_id = (select root_id from category where id = ?)\n" +
                    "  and \"left\" >= (select \"left\" from category where id = ?)\n" +
                    "  and \"right\" <= (select \"right\" from category where id = ?)\n" +
                    "order by \"left\"";
            PreparedStatement preparedStatement = connection.prepareStatement(descendantsSql);
            long sqlId = Long.parseLong(id);
            preparedStatement.setLong(1, sqlId);
            preparedStatement.setLong(2, sqlId);
            preparedStatement.setLong(3, sqlId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                CategoryView view = rebuild(rs);
                tree.append(CategoryView.identifiableCategoryView(view.getParentId()), view);
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild category view", e);
        }
    }

    @Override
    public CategoryView[] queryByName(String regularExpression) {
        List<CategoryView> categoryViewList = new ArrayList<>();
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            final String searchSql = "select id,parent_id, name::jsonb ->> 'name' as name, name::jsonb ->> 'mnemonic' as mnemonic, name::jsonb ->> 'alias' as alias, description,logo_uri,\"right\" - \"left\" as distance from category where name::jsonb ->> 'name' ~ ?\n" +
                    "union\n" +
                    "select id,parent_id, name::jsonb ->> 'name' as name, name::jsonb ->> 'mnemonic' as mnemonic, name::jsonb ->> 'alias' as alias, description,logo_uri,\"right\" - \"left\" as distance from category where name::jsonb ->> 'alias' ~ ?\n" +
                    "union\n" +
                    "select id,parent_id, name::jsonb ->> 'name' as name, name::jsonb ->> 'mnemonic' as mnemonic, name::jsonb ->> 'alias' as alias, description,logo_uri,\"right\" - \"left\" as distance from category where name::jsonb ->> 'mnemonic' ~ ?";
            PreparedStatement ps = connection.prepareStatement(searchSql);
            ps.setString(1, regularExpression);
            ps.setString(2, regularExpression);
            ps.setString(3, regularExpression);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categoryViewList.add(rebuild(rs));
            }
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't rebuild category view", e);
        }
        return categoryViewList.toArray(new CategoryView[0]);
    }

    @Override
    public CategoryView[] siblings(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] siblings = new CategoryView[0];
        CategoryView identifiable = CategoryView.identifiableCategoryView(id);
        for (Tree<CategoryView> tree : trees) {
            if (tree.contain(identifiable)) {
                siblings = tree.siblings(identifiable);
            }
            break;
        }
        return siblings;
    }

    @Override
    public CategoryView[] path(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] path = new CategoryView[0];
        CategoryView identifiable = CategoryView.identifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.contain(identifiable)) {
                path = t.path(identifiable);
            }
        }
        return path;
    }

    @Override
    public int depth(String id) {
        CategoryView identifiable = CategoryView.identifiableCategoryView(id);
        for (Tree<CategoryView> tree : trees) {
            if (tree.contain(identifiable))
                return tree.depth(identifiable);
        }
        return 0;
    }

    @Override
    public void handleEvent(DomainEvent domainEvent) {

    }

    @Override
    public Class subscribedToEventType() {
        return null;
    }
}
