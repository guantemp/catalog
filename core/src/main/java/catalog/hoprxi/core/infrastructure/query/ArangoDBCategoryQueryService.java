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

package catalog.hoprxi.core.infrastructure.query;

import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBUtil;
import catalog.hoprxi.core.infrastructure.view.CategoryView;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.application.Tree;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-03-21
 */
public class ArangoDBCategoryQueryService implements CategoryQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBCategoryQueryService.class);
    private static Tree<CategoryView>[] trees;
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not find Name class has such constructor", e);
        }
    }

    private final ArangoDatabase catalog;

    public ArangoDBCategoryQueryService(String databaseName) {
        catalog = ArangoDBUtil.getResource().db(databaseName);
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
            synchronized (ArangoDBCategoryQueryService.class) {
                final String query = "WITH category\n" +
                        "FOR c IN category FILTER c._key == c.parentId\n" +
                        "LET SUB = (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e )\n" +
                        "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':SUB == []}";
                ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
                CategoryView[] categoryViews = transform(cursor);
                trees = new Tree[categoryViews.length];
                for (int i = 0, j = categoryViews.length; i < j; i++) {
                    trees[i] = Tree.root(categoryViews[i]);
                }
                return categoryViews;
            }
        }
    }

    @Override
    public CategoryView find(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView identifiable = CategoryView.createIdentifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.has(identifiable))
                return t.value(identifiable);
        }
        final String query = "WITH category\n" +
                "FOR c in category FILTER c._key == @key\n" +
                "LET SUB = (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e )\n" +
                "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':SUB == []}";
        final Map<String, Object> bindVars = new MapBuilder().put("key", id).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            identifiable = rebuild(slices.next());
            CategoryView parent = CategoryView.createIdentifiableCategoryView(identifiable.getParentId());
            for (Tree<CategoryView> t : trees)
                if (t.has(parent)) {
                    t.addChild(parent, identifiable);
                    break;
                }
            return identifiable;
        }
        return null;
    }

    @Override
    public CategoryView[] children(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] children = new CategoryView[0];
        CategoryView identifiable = CategoryView.createIdentifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.has(identifiable)) {
                if (!t.value(identifiable).isLeaf()) {
                    children = t.children(identifiable);
                    if (children.length < 1) {
                        synchronized (ArangoDBCategoryQueryService.class) {
                            children = queryChildren(id);
                        }
                        for (CategoryView c : children)
                            t.addChild(identifiable, c);
                        break;
                    }
                }
            }
        }
        return children;
    }

    private CategoryView[] queryChildren(String id) {
        final String query = "WITH category,subordinate\n" +
                "FOR c,s in 1..1 OUTBOUND @startVertex subordinate\n" +
                "LET SUB =  (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e)\n" +
                "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':SUB == []}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(cursor);
    }

    @Override
    public CategoryView[] descendants(String id) {
        return new CategoryView[0];
    }

    @Override
    public CategoryView[] siblings(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category,subordinate\n" +
                "FOR p IN 1..1 INBOUND @startVertex subordinate\n" +
                "FOR v IN 1..1 OUTBOUND p._id subordinate RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return this.transform(cursor);
    }

    @Override
    public CategoryView[] searchName(String name) {
        return new CategoryView[0];
    }

    @Override
    public CategoryView[] path(String id) {
        return new CategoryView[0];
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void refresh() {
        final String query = "WITH category\n" +
                "FOR c IN category FILTER c._key == c.parentId\n" +
                "LET sub = (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e )\n" +
                "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':sub == []}";
        ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        CategoryView[] categoryViews = transform(cursor);
        trees = (Tree<CategoryView>[]) Array.newInstance(Tree.class, categoryViews.length);
        for (int i = 0, j = categoryViews.length; i < j; i++) {
            trees[i] = Tree.root(categoryViews[i]);
        }
    }

    private CategoryView rebuild(VPackSlice slice) {
        if (slice == null)
            return null;
        String id = slice.get("_key").getAsString();
        String parentId = slice.get("parentId").getAsString();
        Name name = null;
        try {
            name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Can't rebuild category");
        }
        String description = slice.get("description").getAsString();
        boolean isLeaf = slice.get("leaf").getAsBoolean();
        return name == null ? null : new CategoryView(parentId, id, name, description, isLeaf, false, null);
    }


    private CategoryView[] transform(ArangoCursor<VPackSlice> cursor) {
        List<CategoryView> list = new ArrayList<>();
        while (cursor.hasNext()) {
            list.add(rebuild(cursor.next()));
        }
        return list.toArray(new CategoryView[0]);
    }
}
