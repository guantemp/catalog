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
import salt.hoprxi.tree.Tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-12-01
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

    private ArangoDatabase catalog;

    public ArangoDBCategoryQueryService(String databaseName) {
        this.catalog = ArangoDBUtil.getResource().db(databaseName);
        this.refresh();
    }

    public static ArangoDBCategoryQueryService getInstance(String databaseName) {
        return new ArangoDBCategoryQueryService(databaseName);
    }

    public void refresh() {
        final String query = "FOR d IN category FILTER d._key == d.parentId RETURN d";
        ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        List<CategoryView> list = new ArrayList<>();
        while (cursor.hasNext()) {
            list.add(rebuild(cursor.next()));
        }
        trees = new Tree[list.size()];
        for (int i = 0, j = list.size(); i < j; i++) {
            trees[i] = new Tree<>(list.get(i));
        }
    }

    @Override
    public CategoryView[] root() {
        final String query = "FOR d IN category FILTER d._key == d.parentId RETURN d";
        ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        return this.transform(cursor);
    }

    @Override
    public CategoryView[] children(String id) {
/*
        CategoryView temp = new CategoryView(id, id, "temp");
        for (Tree<CategoryView> t : trees) {
            if (t.has(temp)) {
                System.out.println(t.value(temp));
            }
        }

 */
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR i,s in 1..1 OUTBOUND @startVertex subordinate\n" +
                "LET sub =  (FOR v,e in 1..1 OUTBOUND i._id subordinate RETURN e)\n" +
                "RETURN {'_key':i._key,'parentId':i.parentId,'name':i.name,'description':i.description,'has': sub == [] ? false : true}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        CategoryView[] categories = this.transform(cursor);
        return categories;
    }

    @Override
    public CategoryView[] siblings(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR p IN 1..1 INBOUND @startVertex subordinate\n" +
                "FOR v IN 1..1 OUTBOUND p._id subordinate RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return this.transform(cursor);
    }

    @Override
    public CategoryView[] descendants(String id) {
        return new CategoryView[0];
    }

    @Override
    public CategoryView[] path(String id) {
        return new CategoryView[0];
    }

    @Override
    public CategoryView find(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR i IN category FILTER i._key == @key\n" +
                "LET sub =  (FOR v,e in 1..1 OUTBOUND i._id subordinate RETURN e)\n" +
                "RETURN {'_key':i._key,'parentId':i.parentId,'name':i.name,'description':i.description,'has': sub == [] ? false : true}";
        final Map<String, Object> bindVars = new MapBuilder().put("key", id).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        while (slices.hasNext())
            return rebuild(slices.next());
        return null;
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
        String description = null;
        if (!slice.get("description").isNone())
            description = slice.get("description").getAsString();
        boolean isLeaf = false;
        if (!slice.get("has").isNone())
            isLeaf = slice.get("has").getAsBoolean();
        return name == null ? null : new CategoryView(parentId, id, name, description, isLeaf, false, null);
    }


    private CategoryView[] transform(ArangoCursor<VPackSlice> cursor) {
        List<CategoryView> list = new ArrayList<>();
        while (cursor.hasNext()) {
            list.add(rebuild(cursor.next()));
        }
        return list.toArray(new CategoryView[list.size()]);
    }
}
