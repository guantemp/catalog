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
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBUtil;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentField;
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
    private static Tree<Category>[] trees;
    private static Constructor<Name> nameConstructor;
    private static CategoryQueryService categoryQueryService;

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
        List<Category> list = new ArrayList<>();
        while (cursor.hasNext()) {
            list.add(rebuild(cursor.next()));
        }
        trees = new Tree[list.size()];
        for (int i = 0, j = list.size(); i < j; i++) {
            trees[i] = new Tree<>(list.get(i));
        }
    }

    @Override
    public Category[] root() {
        final String query = "FOR d IN category FILTER d._key == d.parentId RETURN d";
        ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        return this.transform(cursor);
    }

    @Override
    public Category[] children(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex subordinate RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return this.transform(cursor);
    }

    @Override
    public Category[] silblings(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR p IN 1..1 INBOUND @startVertex subordinate\n" +
                "FOR v IN 1..1 OUTBOUND p._id subordinate RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return this.transform(cursor);
    }

    @Override
    public Category[] descendants(String id) {
        return new Category[0];
    }

    @Override
    public Category[] path(String id) {
        return new Category[0];
    }

    @Override
    public Category find(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        ArangoGraph graph = catalog.graph("core");
        VPackSlice slice = graph.vertexCollection("category").getVertex(id, VPackSlice.class);
        return rebuild(slice);
    }

    private Category rebuild(VPackSlice slice) {
        if (slice == null)
            return null;
        String id = slice.get(DocumentField.Type.KEY.getSerializeName()).getAsString();
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
        return name == null ? null : new Category(parentId, id, name, description);
    }


    private Category[] transform(ArangoCursor<VPackSlice> cursor) {
        List<Category> categoryList = new ArrayList<>();
        while (cursor.hasNext()) {
            categoryList.add(rebuild(cursor.next()));
        }
        return categoryList.toArray(new Category[categoryList.size()]);
    }
}
