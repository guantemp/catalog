/*
 * Copyright (c) 2021. www.hoprxi.com All Rights Reserved.
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
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.DocumentField;
import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.id.LongId;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2020-05-05
 */
public class ArangoDBCategoryRepository implements CategoryRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBBrandRepository.class);
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

    public ArangoDBCategoryRepository(String databaseName) {
        this.catalog = ArangoDBUtil.getResource().db(databaseName);
    }

    @Override
    public Category[] belongTo(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex subordinate RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        List<Category> list = new ArrayList<>();
        try {
            while (cursor.hasNext())
                list.add(rebuild(cursor.next()));
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.debug("Can't rebuild category");
        }
        return list.toArray(new Category[0]);
    }

    @Override
    public Category[] belongTo(String id, int depth) {
        id = Objects.requireNonNull(id, "id required").trim();
        final String query = "WITH category\n" +
                "FOR v,e IN 1..@depth OUTBOUND @startVertex subordinate RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("depth", depth).put("startVertex", "category/" + id).get();
        ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        List<Category> list = new ArrayList<>();
        try {
            while (cursor.hasNext())
                list.add(rebuild(cursor.next()));
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.debug("Can't rebuild category");
        }
        return list.toArray(new Category[0]);
    }

    @Override
    public Category find(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        ArangoGraph graph = catalog.graph("core");
        VPackSlice slice = graph.vertexCollection("category").getVertex(id, VPackSlice.class);
        try {
            return rebuild(slice);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.debug("Can't rebuild category");
        }
        return null;
    }

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        id = Objects.requireNonNull(id, "id required").trim();
        ArangoGraph graph = catalog.graph("core");
        boolean exists = catalog.collection("category").documentExists(id);
        if (exists)
            graph.vertexCollection("category").deleteVertex(id);
    }

    @Override
    public Category[] root() {
        final String query = "FOR d IN category FILTER d._key == d.parentId RETURN d";
        ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        return transform(cursor);
    }

    @Override
    public void save(Category category) {
        ArangoGraph graph = catalog.graph("core");
        boolean exists = catalog.collection("category").documentExists(category.id());
        if (exists) {
            VertexUpdateEntity vertex = graph.vertexCollection("category").replaceVertex(category.id(), category);
            //if parentId is changed
            final String query = "WITH category\n" +
                    "FOR v,e IN 1..1 INBOUND @startVertex subordinate FILTER v._key != @parentId REMOVE e IN subordinate RETURN OLD";
            final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + category.id()).put("parentId", category.parentId()).get();
            ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
            if (cursor.hasNext() && !category.isRoot())
                insertSubordinateEdge(graph, category.parentId(), vertex);
        } else {
            VertexEntity vertex = graph.vertexCollection("category").insertVertex(category);
            if (!category.isRoot())
                insertSubordinateEdge(graph, category.parentId(), vertex);
        }
    }

    /**
     * @param graph
     * @param to
     * @param parentId
     */
    private void insertSubordinateEdge(ArangoGraph graph, String parentId, DocumentEntity to) {
        VertexEntity from = graph.vertexCollection("category").getVertex(parentId, VertexEntity.class);
        if (from != null)
            graph.edgeCollection("subordinate").insertEdge(new Edge(from.getId(), to.getId()));
    }

    private Category rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (slice == null)
            return null;
        String id = slice.get(DocumentField.Type.KEY.getSerializeName()).getAsString();
        String parentId = slice.get("parentId").getAsString();
        Name name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        String description = null;
        if (!slice.get("description").isNone())
            description = slice.get("description").getAsString();
        return new Category(parentId, id, name, description);
    }

    private Category[] transform(ArangoCursor<VPackSlice> cursor) {
        List<Category> categoryList = new ArrayList<>();
        while (cursor.hasNext()) {
            try {
                categoryList.add(rebuild(cursor.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild category", e);
            }
        }
        return categoryList.toArray(new Category[categoryList.size()]);
    }

    /*
    private void insertDesignateEdge(ArangoGraph graph, VertexEntity categoryVertex, Iterator<SpecificationFamily> specificationFamilyIterator) {
        while (specificationFamilyIterator.hasNext()) {
            SpecificationFamily family = specificationFamilyIterator.next();
            Iterator<Specification> specificationIterator = family.specifications();
            while (specificationIterator.hasNext()) {
                Specification property = specificationIterator.next();
                if (property instanceof BrandSpecification) {
                    VertexEntity brandVertex = graph.vertexCollection("brand").getVertex(((BrandSpecification) property).brandDescriptor().id(), VertexEntity.class);
                    if (brandVertex != null)
                        graph.edgeCollection("designate").insertEdge(new Edge(categoryVertex.getId(), brandVertex.getId()));
                }
            }
        }
    }
    */

    private static class Edge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;

        @DocumentField(DocumentField.Type.TO)
        private String to;

        public Edge(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
