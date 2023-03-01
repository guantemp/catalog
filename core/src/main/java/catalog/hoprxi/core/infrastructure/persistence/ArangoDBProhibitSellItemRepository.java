/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.domain.model.*;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.ArangoDBUtil;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.DocumentField;
import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.model.VertexUpdateOptions;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.annotations.Expose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-06-29
 */
public class ArangoDBProhibitSellItemRepository implements ProhibitSellItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBProhibitSellItemRepository.class);
    private static final VertexUpdateOptions UPDATE_OPTIONS = new VertexUpdateOptions().keepNull(false);
    private static Constructor<Name> nameConstructor;
    private static Constructor<ProhibitSellItem> stopSellSkuConstructor;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class);
            nameConstructor.setAccessible(true);
            stopSellSkuConstructor = ProhibitSellItem.class.getDeclaredConstructor(String.class, Barcode.class, Name.class, MadeIn.class, Unit.class, Specification.class,
                    Grade.class, ShelfLife.class, String.class, String.class);
            stopSellSkuConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query has such constructor", e);
        }
    }

    private ArangoDatabase catalog = ArangoDBUtil.getDatabase();

    public ProhibitSellItem[] belongToBrand(String brandId) {
        final String query = "WITH brand,belong,stop_sell_sku,has,barcode,attribute\n" +
                "FOR sku,e IN 1..1 INBOUND @startVertex belong LIMIT @offset,@limit\n" +
                "LET attributes = (FOR a,h IN 1..1 OUTBOUND sku._id has FILTER h.distinguish==true RETURN a)\n" +
                "LET barcodes = (FOR b,h IN 1..1 OUTBOUND sku._id has FILTER h.distinguish==false RETURN b)\n" +
                "RETURN {sku,barcodes,attributes}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "brand/" + brandId).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    private ProhibitSellItem[] transform(ArangoCursor<VPackSlice> slices) {
        List<ProhibitSellItem> skuList = new ArrayList<>();
        while (slices.hasNext()) {
            try {
                skuList.add(rebuild(slices.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild ProhibitSalesSku", e);
            }
        }
        return skuList.toArray(new ProhibitSellItem[0]);
    }

    private ProhibitSellItem rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(slice.get("barcode").getAsString());
        //Sku
        VPackSlice sku = slice.get("sku");
        String id = sku.get(DocumentField.Type.KEY.getSerializeName()).getAsString();
        Name name = nameConstructor.newInstance(sku.get("name").get("name").getAsString(), sku.get("name").get("mnemonic").getAsString());
        VPackSlice madeInSlice = sku.get("madeIn");
        MadeIn madeIn = null;
        String className = madeInSlice.get("_class").getAsString();
        if (Domestic.class.getName().equals(className)) {
            madeIn = new Domestic(madeInSlice.get("code").getAsInt(), madeInSlice.get("city").getAsString());
        } else if (Imported.class.getName().equals(className)) {
            madeIn = new Imported(madeInSlice.get("code").getAsInt(), madeInSlice.get("country").getAsString());
        }
        Unit unit = Unit.valueOf(sku.get("unit").getAsString());
        Specification spec = new Specification(sku.get("spec").get("value").getAsString());
        Grade grade = Grade.valueOf(sku.get("grade").getAsString());
        ShelfLife shelfLife = new ShelfLife(sku.get("shelfLife").get("days").getAsInt());
        String brandId = sku.get("brandId").getAsString();
        String categoryId = sku.get("categoryId").getAsString();
        return stopSellSkuConstructor.newInstance(id, barcode, name, madeIn, unit, spec, grade, shelfLife, brandId, categoryId);
    }

    public ProhibitSellItem[] belongToCategory(String categoryId) {
        return new ProhibitSellItem[0];
    }

    @Override
    public ProhibitSellItem find(String id) {
        return null;
    }

    public ProhibitSellItem[] findAll(long offset, int limit) {
        return new ProhibitSellItem[0];
    }

    @Override
    public void remove(String id) {

    }

    @Override
    public void save(ProhibitSellItem item) {
        ArangoGraph graph = catalog.graph("core");
        boolean exists = catalog.collection("stop_sell_sku").documentExists(item.id());
        if (exists) {
            VertexUpdateEntity skuVertex = graph.vertexCollection("stop_sell_sku").updateVertex(item.id(), item, UPDATE_OPTIONS);
            if (isBrandIdChanged(catalog, skuVertex, item.brandId()))
                insertBrandWithBelongEdge(graph, skuVertex, item.brandId());
            if (isCategoryIdChanged(catalog, skuVertex, item.categoryId()))
                insertCategoryWithBelongEdge(graph, skuVertex, item.categoryId());
            updateBarcodeBook(catalog, skuVertex, item.barcode());
        } else {
            VertexEntity skuVertex = graph.vertexCollection("stop_sell_sku").insertVertex(item);
            insertBrandWithBelongEdge(graph, skuVertex, item.brandId());
            insertCategoryWithBelongEdge(graph, skuVertex, item.categoryId());
            insertBarcodeWithHasEdge(graph, skuVertex, item.barcode());
        }
    }

    private boolean isBrandIdChanged(ArangoDatabase arangoDatabase, DocumentEntity startVertex, String brandId) {
        final String query = "WITH stop_sell_sku,belong\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong FILTER e.distinguish == true && v._key != @brandId REMOVE e IN belong RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).put("brandId", brandId).get();
        ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private void insertBrandWithBelongEdge(ArangoGraph graph, DocumentEntity skuVertex, String brandId) {
        VertexEntity brandVertex = graph.vertexCollection("brand").getVertex(brandId, VertexEntity.class);
        graph.edgeCollection("belong").insertEdge(new BelongEdge(skuVertex.getId(), brandVertex.getId(), true));
    }

    private boolean isCategoryIdChanged(ArangoDatabase arangoDatabase, DocumentEntity startVertex, String categoryId) {
        final String query = "WITH stop_sell_sku,belong\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong FILTER e.distinguish == false && v._key != @categoryId REMOVE e IN belong RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).put("categoryId", categoryId).get();
        ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private void insertCategoryWithBelongEdge(ArangoGraph graph, DocumentEntity skuVertex, String categoryId) {
        VertexEntity categoryVertex = graph.vertexCollection("category").getVertex(categoryId, VertexEntity.class);
        graph.edgeCollection("belong").insertEdge(new BelongEdge(skuVertex.getId(), categoryVertex.getId(), false));
    }

    private void updateBarcodeBook(ArangoDatabase arangoDatabase, DocumentEntity startVertex, Barcode book) {

    }


    private void insertBarcodeWithHasEdge(ArangoGraph graph, DocumentEntity skuVertex, Barcode barcode) {
        VertexEntity barcodeVertex = graph.vertexCollection("barcode").insertVertex(barcode);
        graph.edgeCollection("has").insertEdge(new HasEdge(skuVertex.getId(), barcodeVertex.getId(), false));

    }

    public long size() {
        return 0;
    }

    public ProhibitSellItem[] fromBarcode(String barcode) {
        return new ProhibitSellItem[0];
    }

    public ProhibitSellItem[] fromName(String name) {
        return new ProhibitSellItem[0];
    }

    private static class HasEdge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;

        @DocumentField(DocumentField.Type.TO)
        private String to;

        @Expose(serialize = true, deserialize = false)
        private boolean distinguish;

        private HasEdge(String from, String to, boolean distinguish) {
            this.from = from;
            this.to = to;
            this.distinguish = distinguish;
        }
    }

    private static class BelongEdge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;

        @DocumentField(DocumentField.Type.TO)
        private String to;
        @Expose(serialize = true, deserialize = false)
        private boolean distinguish;

        private BelongEdge(String from, String to, boolean distinguish) {
            this.from = from;
            this.to = to;
            this.distinguish = distinguish;
        }
    }
}
