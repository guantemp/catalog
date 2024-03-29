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

package catalog.hoprxi.scale.infrastructure.persistence;


import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.WeightRepository;
import catalog.hoprxi.scale.domain.model.weight_price.*;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentField;
import com.arangodb.entity.VertexEntity;
import com.arangodb.model.VertexUpdateOptions;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.MonetaryAmount;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2019-10-31
 */
public class ArangoDBWeightRepository implements WeightRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBWeightRepository.class);
    private static final VertexUpdateOptions UPDATE_OPTIONS = new VertexUpdateOptions().keepNull(false);
    private static Constructor<Name> nameConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query Name class has such constructor", e);
        }
    }

    private ArangoDatabase catalog = ArangoDBUtil.getDatabase();

    @Override
    public Plu nextPlu() {
        return null;
    }

    @Override
    public boolean isPluExists(int plu) {
        if (plu < 0 || plu > 99999)
            return false;
        final String query = "FOR p IN plu FILTER p._key == @plu RETURN p";
        final Map<String, Object> bindVars = new MapBuilder().put("plu", String.valueOf(plu)).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    @Override
    public Weight find(int plu) {
        final String query = "WITH plu,weight\n" +
                "FOR p IN plu FILTER p._key == @plu\n" +
                "FOR w IN 1..1 OUTBOUND p._id scale FILTER w._id =~ '^weight/'\n" +
                "RETURN {'plu':TO_NUMBER(p._key),'name':w.name,'madeIn':w.madeIn,'spec':w.spec,'grade':w.grade,'shelfLife':w.shelfLife,'retailPrice':w.retailPrice,'memberPrice':w.memberPrice,'vipPrice':w.vipPrice,'categoryId':w.categoryId,'brandId':w.brandId}";
        final Map<String, Object> bindVars = new MapBuilder().put("plu", String.valueOf(plu)).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            try {
                return rebuild(slices.next());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild weight", e);
            }
        }
        return null;
    }

    @Override
    public Weight[] belongingToBrand(String brandId, int offset, int limit) {
        final String query = "WITH brand,plu,weight\n" +
                "FOR v IN 1..1 INBOUND @startVertex belong_scale\n" +
                "FOR w IN 1..1 OUTBOUND v._id scale FILTER w._id =~ '^weight/' LIMIT @offset,@limit\n" +
                "RETURN {'plu':TO_NUMBER(v._key),'name':w.name,'madeIn':w.madeIn,'spec':w.spec,'grade':w.grade,'shelfLife':w.shelfLife,'retailPrice':w.retailPrice,'memberPrice':w.memberPrice,'vipPrice':w.vipPrice,'categoryId':w.categoryId,'brandId':w.brandId}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "brand/" + brandId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Weight[] belongingToCategory(String categoryId, int offset, int limit) {
        final String query = "WITH category,plu,weight\n" +
                "FOR v IN 1..1 INBOUND @startVertex belong_scale\n" +
                "FOR w IN 1..1 OUTBOUND v._id scale FILTER w._id =~ '^weight/' LIMIT @offset,@limit\n" +
                "RETURN {'plu':TO_NUMBER(v._key),'name':w.name,'madeIn':w.madeIn,'spec':w.spec,'grade':w.grade,'shelfLife':w.shelfLife,'retailPrice':w.retailPrice,'memberPrice':w.memberPrice,'vipPrice':w.vipPrice,'categoryId':w.categoryId,'brandId':w.brandId}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + categoryId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    private Weight rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Plu plu = new Plu(slice.get("plu").getAsNumber().intValue());
        Name name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        VPackSlice madeInSlice = slice.get("madeIn");
        MadeIn madeIn = null;
        if (!madeInSlice.isNone() && !madeInSlice.isNull()) {
            String className = madeInSlice.get("_class").getAsString();
            if (Domestic.class.getName().equals(className)) {
                madeIn = new Domestic(madeInSlice.get("code").getAsString(), madeInSlice.get("city").getAsString());
            } else if (Imported.class.getName().equals(className)) {
                madeIn = new Imported(madeInSlice.get("code").getAsString(), madeInSlice.get("country").getAsString());
            }
        }
        Specification spec = Specification.valueOf(slice.get("spec").get("value").getAsString());
        Grade grade = Grade.valueOf(slice.get("grade").getAsString());
        ShelfLife shelfLife = ShelfLife.rebuild(slice.get("shelfLife").get("days").getAsInt());

        VPackSlice retailPriceSlice = slice.get("retailPrice");
        VPackSlice amountSlice = retailPriceSlice.get("weightPrice").get("amount");
        MonetaryAmount amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        WeightUnit unit = WeightUnit.valueOf(retailPriceSlice.get("weightPrice").get("weightUnit").getAsString());
        WeightRetailPrice retailPrice = new WeightRetailPrice(new WeightPrice(amount, unit));

        VPackSlice memberPriceSlice = slice.get("memberPrice");
        String priceName = memberPriceSlice.get("name").getAsString();
        amountSlice = memberPriceSlice.get("weightPrice").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = WeightUnit.valueOf(memberPriceSlice.get("weightPrice").get("weightUnit").getAsString());
        WeightMemberPrice memberPrice = new WeightMemberPrice(priceName, new WeightPrice(amount, unit));

        VPackSlice vipPriceSlice = slice.get("vipPrice");
        priceName = vipPriceSlice.get("name").getAsString();
        amountSlice = vipPriceSlice.get("weightPrice").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = WeightUnit.valueOf(vipPriceSlice.get("weightPrice").get("weightUnit").getAsString());
        WeightVipPrice vipPrice = new WeightVipPrice(priceName, new WeightPrice(amount, unit));

        String brandId = slice.get("brandId").getAsString();
        String categoryId = slice.get("categoryId").getAsString();
        return new Weight(plu, name, madeIn, spec, grade, shelfLife, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    @Override
    public Weight[] findAll(int offset, int limit) {
        Weight[] weights = ArangoDBUtil.calculationCollectionSize(catalog, Weight.class, offset, limit);
        if (weights.length == 0)
            return weights;
        final String query = "WITH plu,weight\n" +
                "FOR p IN plu\n" +
                "FOR w IN 1..1 OUTBOUND p._id scale FILTER w._id =~ '^weight/' LIMIT @offset,@limit\n" +
                "RETURN {'plu':TO_NUMBER(p._key),'name':w.name,'madeIn':w.madeIn,'spec':w.spec,'grade':w.grade,'shelfLife':w.shelfLife,'retailPrice':w.retailPrice,'memberPrice':w.memberPrice,'vipPrice':w.vipPrice,'categoryId':w.categoryId,'brandId':w.brandId}";
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
                    LOGGER.debug("Can't rebuild weight", e);
            }
        }
        return weightList.toArray(new Weight[weightList.size()]);
    }

    @Override
    public void remove(Plu plu) {
        boolean exists = catalog.collection("plu").documentExists(String.valueOf(plu.plu()));
        if (exists) {
            final String remove = "WITH plu,weight\n" +
                    "FOR v,e IN 1..1 OUTBOUND @startVertex scale REMOVE v IN weight REMOVE e IN scale";
            final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "plu/" + plu.plu()).get();
            catalog.query(remove, bindVars, null, VPackSlice.class);
            catalog.graph("scale").vertexCollection("plu").deleteVertex(String.valueOf(plu.plu()));
        }
    }

    @Override
    public void save(Weight weight) {
        boolean exists = catalog.collection("plu").documentExists(String.valueOf(weight.plu().plu()));
        if (exists) {
            ArangoGraph graph = catalog.graph("scale");
            VertexEntity vertex = graph.vertexCollection("plu").getVertex(String.valueOf(weight.plu().plu()), VertexEntity.class);
            if (isCategoryIdChanged(catalog, vertex, weight.categoryId()))
                insertBelongEdgeOfCategory(graph, vertex, weight.categoryId());
            if (isBrandIdChanged(catalog, vertex, weight.brandId()))
                insertBelongEdgeOfBrand(graph, vertex, weight.brandId());
            final String query = "WITH plu,weight\n" +
                    "FOR w IN 1..1 OUTBOUND @startVertex scale RETURN w._key";
            final Map<String, Object> bindVars = new MapBuilder().put("startVertex", vertex.getId()).get();
            final ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
            if (slices.hasNext()) {
                graph.vertexCollection("weight").updateVertex(slices.next().getAsString(), weight, UPDATE_OPTIONS);
            }
        } else {
            ArangoGraph graph = catalog.graph("scale");
            VertexEntity vertexPlu = graph.vertexCollection("plu").insertVertex(weight.plu());
            VertexEntity vertexWeight = graph.vertexCollection("weight").insertVertex(weight);
            graph.edgeCollection("scale").insertEdge(new ScaleEdge(vertexPlu.getId(), vertexWeight.getId()));
            insertBelongEdgeOfCategory(graph, vertexPlu, weight.categoryId());
            insertBelongEdgeOfBrand(graph, vertexPlu, weight.brandId());
        }
    }


    private boolean isCategoryIdChanged(ArangoDatabase catalog, VertexEntity vertex, String categoryId) {
        final String query = "WITH plu\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong_scale FILTER v._id =~ '^category' && v._key != @categoryId REMOVE e IN belong_scale RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", vertex.getId()).put("categoryId", categoryId).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private boolean isBrandIdChanged(ArangoDatabase catalog, VertexEntity vertex, String brandId) {
        final String query = "WITH plu\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong_scale FILTER v._id =~ '^brand' && v._key != @brandId REMOVE e IN belong_scale RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", vertex.getId()).put("brandId", brandId).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private void insertBelongEdgeOfCategory(ArangoGraph graph, VertexEntity pluVertex, String categoryId) {
        VertexEntity categoryVertex = graph.vertexCollection("category").getVertex(categoryId, VertexEntity.class);
        graph.edgeCollection("belong_scale").insertEdge(new BelongEdge(pluVertex.getId(), categoryVertex.getId()));
    }

    private void insertBelongEdgeOfBrand(ArangoGraph graph, VertexEntity pluVertex, String brandId) {
        VertexEntity brandVertex = graph.vertexCollection("brand").getVertex(brandId, VertexEntity.class);
        graph.edgeCollection("belong_scale").insertEdge(new BelongEdge(pluVertex.getId(), brandVertex.getId()));
    }

    @Override
    public int size() {
        final String query = "RETURN LENGTH(weight)";
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        if (cursor.hasNext())
            return cursor.next().getAsInt();
        return 0;
    }

    @Override
    public Weight[] fromName(String name) {
        final String query = "WITH plu,weight\n" +
                "FOR w IN weight FILTER w.name.name =~ @name || w.name.alias =~ @name || w.name.mnemonic =~ @name\n" +
                "FOR p IN 1..1 INBOUND w._id scale\n" +
                "RETURN {'plu':TO_NUMBER(p._key),'name':w.name,'madeIn':w.madeIn,'spec':w.spec,'grade':w.grade,'shelfLife':w.shelfLife,'retailPrice':w.retailPrice,'memberPrice':w.memberPrice,'vipPrice':w.vipPrice,'categoryId':w.categoryId,'brandId':w.brandId}";
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
