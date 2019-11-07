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

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.Unit;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.MemberPrice;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.VipPrice;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBUtil;
import catalog.hoprxi.scale.domain.model.Count;
import catalog.hoprxi.scale.domain.model.CountRepository;
import catalog.hoprxi.scale.domain.model.Plu;
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
 * @author <a href="wwc.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-05-11
 */
public class ArangoDBCountRepository implements CountRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBWeightRepository.class);
    private static final VertexUpdateOptions UPDATE_OPTIONS = new VertexUpdateOptions().keepNull(false);
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
    public Count find(int plu) {
        final String query = "WITH plu,count\n" +
                "FOR p IN plu FILTER p._key == @plu\n" +
                "FOR c IN 1..1 OUTBOUND p._id scale FILTER c._id =~ '^count/'\n" +
                "RETURN {'plu':TO_NUMBER(p._key),'name':c.name,'madeIn':c.madeIn,'spec':c.spec,'grade':c.grade,'shelfLife':c.shelfLife,'retailPrice':c.retailPrice,'memberPrice':c.memberPrice,'vipPrice':c.vipPrice,'categoryId':c.categoryId,'brandId':c.brandId}";
        final Map<String, Object> bindVars = new MapBuilder().put("plu", String.valueOf(plu)).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            try {
                return rebuild(slices.next());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild count", e);
            }
        }
        return null;
    }

    @Override
    public Count[] findAll() {
        Count[] counts = new Count[this.size()];
        if (counts.length == 0)
            return counts;
        final String query = "WITH count,plu\n" +
                "FOR p IN plu\n" +
                "FOR c IN 1..1 OUTBOUND p._id scale FILTER c._id =~ '^count/'\n" +
                "RETURN {'plu':TO_NUMBER(p._key),'name':c.name,'madeIn':c.madeIn,'spec':c.spec,'grade':c.grade,'shelfLife':c.shelfLife,'retailPrice':c.retailPrice,'memberPrice':c.memberPrice,'vipPrice':c.vipPrice,'categoryId':c.categoryId,'brandId':c.brandId}";
        final ArangoCursor<VPackSlice> slices = catalog.query(query, null, null, VPackSlice.class);
        try {
            for (int i = 0; slices.hasNext(); i++)
                counts[i] = rebuild(slices.next());
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Can't rebuild count", e);
        }
        return counts;
    }

    @Override
    public Count[] belongingToBrand(String brandId, int offset, int limit) {
        final String query = "WITH brand,plu,count\n" +
                "FOR v IN 1..1 INBOUND @startVertex belong_scale\n" +
                "FOR c IN 1..1 OUTBOUND v._id scale FILTER c._id =~ '^count/' LIMIT @offset,@limit\n" +
                "RETURN {'plu':TO_NUMBER(c._key),'name':c.name,'madeIn':c.madeIn,'spec':c.spec,'grade':c.grade,'shelfLife':c.shelfLife,'retailPrice':c.retailPrice,'memberPrice':c.memberPrice,'vipPrice':c.vipPrice,'categoryId':c.categoryId,'brandId':c.brandId}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "brand/" + brandId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Count[] belongingToCategory(String categoryId, int offset, int limit) {
        final String query = "WITH category,plu,weight\n" +
                "FOR v IN 1..1 INBOUND @startVertex belong_scale\n" +
                "FOR c IN 1..1 OUTBOUND v._id scale FILTER c._id =~ '^weight/' LIMIT @offset,@limit\n" +
                "RETURN {'plu':TO_NUMBER(v._key),'name':c.name,'madeIn':c.madeIn,'spec':c.spec,'grade':c.grade,'shelfLife':c.shelfLife,'retailPrice':c.retailPrice,'memberPrice':c.memberPrice,'vipPrice':c.vipPrice,'categoryId':c.categoryId,'brandId':c.brandId}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + categoryId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public void remove(Plu plu) {
        boolean exists = catalog.collection("plu").documentExists(String.valueOf(plu.plu()));
        if (!exists)
            return;
        final String remove = "WITH plu,count\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex scale REMOVE v IN weight REMOVE e IN scale";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "plu/" + plu.plu()).get();
        catalog.query(remove, bindVars, null, VPackSlice.class);
        catalog.graph("scale").vertexCollection("plu").deleteVertex(String.valueOf(plu));
    }

    @Override
    public void save(Count count) {
        boolean exists = catalog.collection("plu").documentExists(String.valueOf(count.plu().plu()));
        if (exists) {
            ArangoGraph graph = catalog.graph("scale");
            VertexEntity vertex = graph.vertexCollection("plu").getVertex(String.valueOf(count.plu().plu()), VertexEntity.class);
            if (isCategoryIdChanged(catalog, vertex, count.categoryId()))
                insertBelongEdgeOfCategory(graph, vertex, count.categoryId());
            if (isBrandIdChanged(catalog, vertex, count.brandId()))
                insertBelongEdgeOfBrand(graph, vertex, count.brandId());
            final String query = "WITH plu,count\n" +
                    "FOR v,e IN 1..1 OUTBOUND @startVertex scale RETURN v._key";
            final Map<String, Object> bindVars = new MapBuilder().put("startVertex", vertex.getId()).get();
            final ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
            if (slices.hasNext()) {
                graph.vertexCollection("count").updateVertex(slices.next().getAsString(), count, UPDATE_OPTIONS);
            }
        } else {
            ArangoGraph graph = catalog.graph("scale");
            VertexEntity vertexPlu = graph.vertexCollection("plu").insertVertex(count.plu());
            VertexEntity vertexCount = graph.vertexCollection("count").insertVertex(count);
            graph.edgeCollection("scale").insertEdge(new ScaleEdge(vertexPlu.getId(), vertexCount.getId()));
            insertBelongEdgeOfCategory(graph, vertexPlu, count.categoryId());
            insertBelongEdgeOfBrand(graph, vertexPlu, count.brandId());
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

    private Count rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Plu plu = new Plu(slice.get("plu").getAsNumber().intValue());
        Name name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        VPackSlice madeInSlice = slice.get("madeIn");
        MadeIn madeIn = null;
        if (!madeInSlice.isNull()) {
            String className = madeInSlice.get("_class").getAsString();
            if (Domestic.class.getName().equals(className)) {
                madeIn = new Domestic(madeInSlice.get("province").getAsString(), madeInSlice.get("city").getAsString());
            } else if (Imported.class.getName().equals(className)) {
                madeIn = new Imported(madeInSlice.get("country").getAsString());
            }
        }
        Specification spec = Specification.rebulid(slice.get("spec").get("value").getAsString());
        Grade grade = Grade.valueOf(slice.get("grade").getAsString());
        ShelfLife shelfLife = ShelfLife.rebuild(slice.get("shelfLife").get("days").getAsInt());

        VPackSlice priceSlice = slice.get("retailPrice").get("price");
        VPackSlice amountSlice = priceSlice.get("amount");
        MonetaryAmount amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        Unit unit = Unit.valueOf(priceSlice.get("unit").getAsString());
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));

        VPackSlice memberPriceSlice = slice.get("memberPrice");
        String priceName = memberPriceSlice.get("name").getAsString();
        amountSlice = memberPriceSlice.get("price").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = Unit.valueOf(memberPriceSlice.get("price").get("unit").getAsString());
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));

        VPackSlice vipPriceSlice = slice.get("vipPrice");
        priceName = vipPriceSlice.get("name").getAsString();
        amountSlice = vipPriceSlice.get("price").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = Unit.valueOf(vipPriceSlice.get("price").get("unit").getAsString());
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));

        String categoryId = slice.get("categoryId").getAsString();
        String brandId = slice.get("brandId").getAsString();
        return new Count(plu, name, madeIn, spec, grade, shelfLife, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    private Count[] transform(ArangoCursor<VPackSlice> slices) {
        List<Count> list = new ArrayList<>();
        while (slices.hasNext()) {
            try {
                list.add(rebuild(slices.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild count", e);
            }
        }
        return list.toArray(new Count[list.size()]);
    }

    @Override
    public int size() {
        final String query = "RETURN LENGTH(count)";
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        if (cursor.hasNext())
            return cursor.next().getAsInt();
        return 0;
    }

    @Override
    public Count[] fromName(String name) {
        final String query = "WITH count,plu\n" +
                "FOR c IN count FILTER c.name.name =~ @name || c.name.alias =~ @name || c.name.mnemonic =~ @name\n" +
                "FOR p IN 1..1 INBOUND c._id scale\n" +
                "RETURN {'plu':TO_NUMBER(p._key),'name':c.name,'spec':c.spec,'unit':c.unit,'grade':c.grade,'placeOfProduction':c.placeOfProduction,'shelfLife':c.shelfLife,'retailPrice':c.retailPrice,'memberPrice':c.memberPrice,'vipPrice':c.vipPrice,'categoryId':c.categoryId,'brandId':c.brandId}";
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
