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
import catalog.hoprxi.core.infrastructure.ArangoDBUtil;
import catalog.hoprxi.core.infrastructure.view.CategoryView;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.application.Tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
    private static final int DESCENDANT_DEPTH = 3;
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
                ArangoCursor<VPackSlice> cursor = catalog.query(query, VPackSlice.class);
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
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, VPackSlice.class);
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
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, VPackSlice.class);
        return transform(cursor);
    }

    @Override
    public CategoryView[] descendants(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] descendants = new CategoryView[0];
        CategoryView identifiable = CategoryView.createIdentifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.has(identifiable)) {
                if (!t.value(identifiable).isLeaf()) {
                    descendants = t.descendants(identifiable);
                    if (descendants.length < 1) {
                        synchronized (ArangoDBCategoryQueryService.class) {
                            queryAndFillDescendants(t, id);
                            descendants = t.descendants(identifiable);
                            break;
                        }
                    }
                }
            }
        }
        return descendants;
    }

    private void queryAndFillDescendants(Tree<CategoryView> t, String id) {
        final String query = "WITH category,subordinate\n" +
                "FOR c,s,p in 1..@depth OUTBOUND @startVertex subordinate\n" +
                "LET SUB =  (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e)\n" +
                "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':SUB == [],'depth':LENGTH(p.edges)}";
        final Map<String, Object> bindVars = new MapBuilder().put("depth", DESCENDANT_DEPTH).put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, VPackSlice.class);
        CategoryView[] categoryViews = transform(cursor);
        for (CategoryView v : categoryViews) {
            t.addChild(CategoryView.createIdentifiableCategoryView(v.getParentId()), v);
            try {
                Field depth = v.getClass().getDeclaredField("depth");
                depth.setAccessible(true);
                if (!v.isLeaf() && (int) depth.get(v) == DESCENDANT_DEPTH) {
                    queryAndFillDescendants(t, v.getId());//recursion
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't get categoryView depth");
            }
        }
    }

    @Override
    public CategoryView[] siblings(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] siblings = new CategoryView[0];
        CategoryView identifiable = CategoryView.createIdentifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.has(identifiable)) {
                siblings = t.siblings(identifiable);
                if (siblings.length < 1) {
                    synchronized (ArangoDBCategoryQueryService.class) {
                        System.out.println("sibling");
                        final String query = "WITH category,subordinate\n" +
                                "FOR c in 1..1 INBOUND @startVertex subordinate\n" +
                                "FOR s in 1..1 OUTBOUND c._id subordinate\n" +
                                "LET SUB =  (FOR v,e in 1..1 OUTBOUND s._id subordinate RETURN e)\n" +
                                "RETURN {'_key':s._key,'parentId':s.parentId,'name':s.name,'description':s.description,'leaf':SUB == []}";
                        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
                        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
                        siblings = transform(cursor);
                        for (CategoryView s : siblings)
                            t.addChild(CategoryView.createIdentifiableCategoryView(s.getParentId()), s);
                        siblings = t.siblings(identifiable);
                        break;
                    }
                }
            }
        }
        return siblings;
    }

    @Override
    public CategoryView[] searchName(String regularExpression) {
        final String query = "WITH category,subordinate\n" +
                "FOR c IN category FILTER c.name.name =~ @regularExpression || c.name.alias =~ @regularExpression || c.name.mnemonic =~ @regularExpression\n" +
                "LET SUB =  (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e)\n" +
                "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':SUB == []}";
        final Map<String, Object> bindVars = new MapBuilder().put("regularExpression", regularExpression).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(cursor);
    }

    @Override
    public CategoryView[] path(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        CategoryView[] path = new CategoryView[0];
        CategoryView identifiable = CategoryView.createIdentifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.has(identifiable)) {
                path = t.path(identifiable);
                if (path.length < 1) {
                    synchronized (ArangoDBCategoryQueryService.class) {
                        final String query = "WITH category,subordinate\n" +
                                "FOR c,subordinate IN INBOUND SHORTEST_PATH @startVertex TO @targetVertex GRAPH core\n" +
                                "LET SUB =  (FOR v,e in 1..1 OUTBOUND c._id subordinate RETURN e)\n" +
                                "RETURN {'_key':c._key,'parentId':c.parentId,'name':c.name,'description':c.description,'leaf':SUB == []}";
                        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).put("startVertex", "category/" + t.root().getId()).get();
                        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
                        path = transform(cursor);
                        for (int i = path.length - 1; i >= 0; i--) {
                            t.addChild(CategoryView.createIdentifiableCategoryView(path[i].getParentId()), path[i]);
                        }
                        path = t.path(identifiable);
                        break;
                    }
                }
            }
        }
        return path;
    }

    @Override
    public int depth(String id) {
        CategoryView identifiable = CategoryView.createIdentifiableCategoryView(id);
        for (Tree<CategoryView> t : trees) {
            if (t.has(identifiable))
                return t.depth(identifiable);
        }
        return 0;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void refresh() {
        trees = null;
        root();
        for (Tree<CategoryView> t : trees) {
            descendants(t.root().getId());
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
                LOGGER.debug("Can't rebuild categoryView");
        }
        String description = slice.get("description").getAsString();
        boolean isLeaf = slice.get("leaf").getAsBoolean();
        CategoryView result = new CategoryView(parentId, id, name, description, null, isLeaf, false);
        if (!slice.get("depth").isNone()) {
            try {
                Field depth = result.getClass().getDeclaredField("depth");
                depth.setAccessible(true);// 添加访问权限，才能访问私有属性， 不然会报错
                depth.set(result, slice.get("depth").getAsInt());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't set categoryView private depth");
            }
        }
        return result;
    }


    private CategoryView[] transform(ArangoCursor<VPackSlice> cursor) {
        List<CategoryView> list = new ArrayList<>();
        while (cursor.hasNext()) {
            list.add(rebuild(cursor.next()));
        }
        return list.toArray(new CategoryView[0]);
    }
}
