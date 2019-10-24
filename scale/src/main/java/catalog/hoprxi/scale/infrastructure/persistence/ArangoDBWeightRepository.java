/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.persistence;

import catalog.foxtail.core.domain.model.*;
import catalog.foxtail.core.infrastructure.persistence.ArangoDBUtil;
import catalog.foxtail.fresh.domain.model.Plu;
import catalog.foxtail.fresh.domain.model.Weight;
import catalog.foxtail.fresh.domain.model.WeightRepository;
import catalog.foxtail.fresh.domain.model.WeightUnit;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.DocumentField;
import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import mi.foxtail.id.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-05-04
 */
public class ArangoDBWeightRepository implements WeightRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBWeightRepository.class);
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

    private ArangoDatabase catalog = ArangoDBUtil.getDatabase();

    @Override
    public int nextPlu() {
        return 0;
    }

    @Override
    public boolean isPluExists(int plu) {
        if (plu < 0 || plu > 99999)
            return false;
        final String query = "FOR v IN plu FILTER v.plu == @plu RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("plu", plu).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    @Override
    public Weight fromPlu(int plu) {
        if (isPluExists(plu)) {
            final String query = "WITH weight,plu\n" +
                    "LET plu=(FOR v1 IN plu FILTER v1.plu == @plu RETURN v1)\n" +
                    "FOR v IN 1..1 INBOUND plu[0]._id scale\n" +
                    "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
            final Map<String, Object> bindVars = new MapBuilder().put("plu", plu).get();
            ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
            if (slices.hasNext()) {
                try {
                    return rebuild(slices.next());
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Can't rebuild sku", e);
                }
            }
        }
        return null;
    }

    @Override
    public Weight[] belongingToBrand(String brandId, int offset, int limit) {
        final String query = "WITH brand,weight,plu\n" +
                "FOR v,e IN 1..1 INBOUND @startVertex belong_fresh LIMIT @offset,@limit\n" +
                "LET plu = (FOR v1,e1 IN 1..1 OUTBOUND v._id scale RETURN v1)\n" +
                "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "brand/" + brandId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Weight[] belongingToCategory(String categoryId, int offset, int limit) {
        final String query = "WITH category,weight,plu\n" +
                "FOR v,e IN 1..1 INBOUND @startVertex belong_fresh LIMIT @offset,@limit\n" +
                "LET plu = (FOR v1,e1 IN 1..1 OUTBOUND v._id scale RETURN v1)\n" +
                "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + categoryId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Weight find(String id) {
        final String query = "WITH weight,plu\n" +
                "FOR v IN weight FILTER v._key == @key\n" +
                "LET plu = (FOR v1,e IN 1..1 OUTBOUND v._id scale RETURN v1)\n" +
                "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
        final Map<String, Object> bindVars = new MapBuilder().put("key", id).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            try {
                return rebuild(slices.next());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild sku", e);
            }
        }
        return null;
    }

    private Weight rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        String id = slice.get("id").getAsString();
        Plu plu = new Plu(slice.get("plu").getAsInt());
        Name name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        Specification spec = Specification.rebulid(slice.get("spec").get("value").getAsString());
        WeightUnit unit = WeightUnit.valueOf(slice.get("unit").getAsString());
        Grade grade = Grade.valueOf(slice.get("grade").getAsString());
        PlaceOfProduction placeOfProduction = new PlaceOfProduction(slice.get("placeOfProduction").get("locality").getAsString());
        ShelfLife shelfLife = new ShelfLife(slice.get("shelfLife").get("days").getAsInt());
        String brandId = slice.get("brandId").getAsString();
        String categoryId = slice.get("categoryId").getAsString();
        return new Weight(id, plu, name, spec, unit, grade, placeOfProduction, shelfLife, brandId, categoryId);
    }

    @Override
    public Weight[] findAll(int offset, int limit) {
        Weight[] weights = ArangoDBUtil.calculationCollectionSize(catalog, Weight.class, offset, limit);
        if (weights.length == 0)
            return weights;
        final String query = "WITH weight,plu\n" +
                "FOR v IN weight LIMIT @offset,@limit\n" +
                "LET plu = (FOR v1,e IN 1..1 OUTBOUND v._id scale RETURN v1)\n" +
                "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
        final Map<String, Object> bindVars = new MapBuilder().put("offset", offset).put("limit", limit).get();
        final ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        try {
            for (int i = 0; slices.hasNext(); i++)
                weights[i] = rebuild(slices.next());
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Can't rebuild weight", e);
        }
        return weights;
    }

    private Weight[] transform(ArangoCursor<VPackSlice> slices) {
        List<Weight> weightList = new ArrayList<>();
        while (slices.hasNext()) {
            try {
                weightList.add(rebuild(slices.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild sku", e);
            }
        }
        return weightList.toArray(new Weight[0]);
    }

    @Override
    public String nextIdentity() {
        return new ObjectId().id();
    }

    @Override
    public void remove(String id) {
        boolean exists = catalog.collection("weight").documentExists(id);
        if (!exists)
            return;
        final String removeScale = "WITH weight,plu\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex scale REMOVE v IN plu REMOVE e IN scale";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "weight/" + id).get();
        catalog.query(removeScale, bindVars, null, VPackSlice.class);
        catalog.graph("fresh").vertexCollection("weight").deleteVertex(id);
    }

    @Override
    public void save(Weight weight) {
        boolean exists = catalog.collection("weight").documentExists(weight.id());
        ArangoGraph graph = catalog.graph("fresh");
        if (exists) {
            VertexUpdateEntity vertex = graph.vertexCollection("weight").updateVertex(weight.id(), weight);
            updateScaleEdge(catalog, vertex, weight.plu());
            if (isBrandIdChanged(catalog, vertex, weight.brandId()))
                insertBelongEdgeOfBrand(graph, vertex, weight.brandId());
            if (isCategoryIdChanged(catalog, vertex, weight.categoryId()))
                insertBelongEdgeOfCategory(graph, vertex, weight.categoryId());
        } else {
            VertexEntity vertex = graph.vertexCollection("weight").insertVertex(weight);
            insertScaleEdge(graph, vertex, weight.plu());
            insertBelongEdgeOfBrand(graph, vertex, weight.brandId());
            insertBelongEdgeOfCategory(graph, vertex, weight.categoryId());
        }
    }


    private boolean isCategoryIdChanged(ArangoDatabase catalog, VertexUpdateEntity weightVertex, String categoryId) {
        final String query = "WITH weight\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong_fresh FILTER v._id =~ '^category' && v._key != @categoryId REMOVE e IN belong_fresh RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", weightVertex.getId()).put("categoryId", categoryId).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private boolean isBrandIdChanged(ArangoDatabase catalog, VertexUpdateEntity weightVertex, String brandId) {
        final String query = "WITH weight\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong_fresh FILTER v._id =~ '^brand' && v._key != @brandId REMOVE e IN belong_fresh RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", weightVertex.getId()).put("brandId", brandId).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private void insertBelongEdgeOfCategory(ArangoGraph graph, DocumentEntity weightVertex, String categoryId) {
        VertexEntity categoryVertex = graph.vertexCollection("category").getVertex(categoryId, VertexEntity.class);
        graph.edgeCollection("belong_fresh").insertEdge(new BelongEdge(weightVertex.getId(), categoryVertex.getId()));
    }

    private void insertBelongEdgeOfBrand(ArangoGraph graph, DocumentEntity weightVertex, String brandId) {
        VertexEntity brandVertex = graph.vertexCollection("brand").getVertex(brandId, VertexEntity.class);
        graph.edgeCollection("belong_fresh").insertEdge(new BelongEdge(weightVertex.getId(), brandVertex.getId()));
    }

    private void insertScaleEdge(ArangoGraph graph, DocumentEntity weightVertex, Plu plu) {
        VertexEntity pluVertex = graph.vertexCollection("plu").insertVertex(plu);
        graph.edgeCollection("scale").insertEdge(new ScaleEdge(weightVertex.getId(), pluVertex.getId()));
    }

    private void updateScaleEdge(ArangoDatabase arangoDatabase, DocumentEntity startVertex, Plu plu) {
        final String query = "WITH weight,plu\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex scale FILTER v.plu != @plu REMOVE v IN plu REMOVE e IN scale RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).put("plu", plu.plu()).get();
        ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            insertScaleEdge(arangoDatabase.graph("fresh"), startVertex, plu);
        }
    }

    @Override
    public int size() {
        final String query = " RETURN LENGTH(weight)";
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        if (cursor.hasNext())
            return cursor.next().getAsInt();
        return 0;
    }

    @Override
    public Weight[] fromMnemonic(String mnemonic) {
        final String query = "WITH weight,plu\n" +
                "FOR v IN weight FILTER v.name.mnemonic =~ @mnemonic\n" +
                "LET plu = (FOR v1,e1 IN 1..1 OUTBOUND v._id scale RETURN v1)\n" +
                "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
        final Map<String, Object> bindVars = new MapBuilder().put("mnemonic", mnemonic).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Weight[] fromName(String name) {
        final String query = "WITH weight,plu\n" +
                "FOR v IN weight FILTER v.name.name =~ @name\n" +
                "LET plu = (FOR v1,e1 IN 1..1 OUTBOUND v._id scale RETURN v1)\n" +
                "RETURN {'id':v._key,'plu':plu[0].plu,'name':v.name,'spec':v.spec,'unit':v.unit,'grade':v.grade,'placeOfProduction':v.placeOfProduction,'shelfLife':v.shelfLife,'brandId':v.brandId,'categoryId':v.categoryId}";
        final Map<String, Object> bindVars = new MapBuilder().put("name", name).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    private static class ScaleEdge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;

        @DocumentField(DocumentField.Type.TO)
        private String to;

        private ScaleEdge(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    private static class BelongEdge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;

        @DocumentField(DocumentField.Type.TO)
        private String to;

        private BelongEdge(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
