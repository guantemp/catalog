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

package catalog.hoprxi.core.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.*;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
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
import mi.hoprxi.id.LongId;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.MonetaryAmount;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2018-06-04
 */
public class ArangoDBItemRepository implements ItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBItemRepository.class);
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
    public Item[] belongToBrand(String brandId, int offset, int limit) {
        final String query = "WITH brand,item,barcode\n" +
                "FOR s,b IN 1..1 INBOUND @startVertex belong LIMIT @offset,@limit\n" +
                "LET barcode = (FOR v,e IN 1..1 OUTBOUND s._id has RETURN v)\n" +
                "RETURN {'item':s,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "brand/" + brandId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }


    @Override
    public Item[] belongToCategory(String categoryId, long offset, int limit) {
        final String query = "WITH category,belong,item,has,barcode\n" +
                "FOR s,b IN 1..1 INBOUND @startVertex belong LIMIT @offset,@limit\n" +
                "LET barcode = (FOR v,e IN 1..1 OUTBOUND s._id has RETURN v)\n" +
                "RETURN {'item':s,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "category/" + categoryId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Item find(String id) {
        final String query = "WITH item,barcode\n" +
                "FOR s IN item FILTER s._key == @key \n" +
                "LET barcode = (FOR v,e IN 1..1 OUTBOUND s._id has RETURN v)\n" +
                "RETURN {'item':s,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("key", id).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        while (slices.hasNext()) {
            try {
                return rebuild(slices.next());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild item", e);
            }
        }
        return null;
    }

    private Item[] transform(ArangoCursor<VPackSlice> slices) {
        List<Item> itemList = new ArrayList<>();
        while (slices.hasNext()) {
            try {
                itemList.add(rebuild(slices.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild item", e);
            }
        }
        return itemList.toArray(new Item[itemList.size()]);
    }

    private Item rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(slice.get("barcode").getAsString());
        //item
        VPackSlice item = slice.get("item");
        String id = item.get(DocumentField.Type.KEY.getSerializeName()).getAsString();
        Name name = nameConstructor.newInstance(item.get("name").get("name").getAsString(), item.get("name").get("mnemonic").getAsString(), item.get("name").get("alias").getAsString());
        VPackSlice madeInSlice = item.get("madeIn");
        MadeIn madeIn = null;
        String className = madeInSlice.get("_class").getAsString();
        if (Domestic.class.getName().equals(className)) {
            madeIn = new Domestic(madeInSlice.get("province").getAsString(), madeInSlice.get("city").getAsString());
        } else if (Imported.class.getName().equals(className)) {
            madeIn = new Imported(madeInSlice.get("country").getAsString());
        }
        Specification spec = new Specification(item.get("spec").get("value").getAsString());
        Grade grade = Grade.valueOf(item.get("grade").getAsString());

        VPackSlice retailPriceSlice = item.get("retailPrice");
        VPackSlice amountSlice = retailPriceSlice.get("price").get("amount");
        MonetaryAmount amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        Unit unit = Unit.valueOf(retailPriceSlice.get("price").get("unit").getAsString());
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));

        VPackSlice memberPriceSlice = item.get("memberPrice");
        String priceName = memberPriceSlice.get("name").getAsString();
        amountSlice = memberPriceSlice.get("price").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = Unit.valueOf(memberPriceSlice.get("price").get("unit").getAsString());
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));

        VPackSlice vipPriceSlice = item.get("vipPrice");
        priceName = vipPriceSlice.get("name").getAsString();
        amountSlice = vipPriceSlice.get("price").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = Unit.valueOf(vipPriceSlice.get("price").get("unit").getAsString());
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));

        String brandId = item.get("brandId").getAsString();
        String categoryId = item.get("categoryId").getAsString();
        return new Item(id, barcode, name, madeIn, spec, grade, retailPrice, memberPrice, vipPrice, categoryId, brandId);
    }

    @Override
    public Item[] findAll(long offset, int limit) {
        Item[] items = ArangoDBUtil.calculationCollectionSize(catalog, Item.class, offset, limit);
        if (items.length == 0)
            return items;
        final String query = "WITH item,barcode\n" +
                "FOR s IN item LIMIT @offset,@limit\n" +
                "LET barcode = (FOR v,e IN 1..1 OUTBOUND s._id has RETURN v)\n" +
                "RETURN {'item':s,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("offset", offset).put("limit", limit).get();
        final ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        try {
            for (int i = 0; slices.hasNext(); i++)
                items[i] = rebuild(slices.next());
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Can't rebuild weight", e);
        }
        return items;
    }

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        boolean exists = catalog.collection("item").documentExists(id);
        if (exists) {
            final Map<String, Object> bindVars = new MapBuilder().put("startVertex", "item/" + id).get();
            final String removeHas_2 = "WITH item,has,barcode\n" +
                    "FOR v,e IN 1..1 OUTBOUND @startVertex has REMOVE v IN barcode";
            catalog.query(removeHas_2, bindVars, null, VPackSlice.class);
            catalog.graph("core").vertexCollection("item").deleteVertex(id);
        }
    }

    @Override
    public void save(Item item) {
        boolean exists = catalog.collection("item").documentExists(item.id());
        ArangoGraph graph = catalog.graph("core");
        if (exists) {
            VertexUpdateEntity itemVertex = graph.vertexCollection("item").updateVertex(item.id(), item, UPDATE_OPTIONS);
            if (isBrandIdChanged(catalog, itemVertex, item.brandId()))
                insertBelongEdgeOfBrand(graph, itemVertex, item.brandId());
            if (isCategoryIdChanged(catalog, itemVertex, item.categoryId()))
                insertBelongEdgeOfCategory(graph, itemVertex, item.categoryId());
            updateBarcode(catalog, itemVertex, item.barcode());
        } else {
            VertexEntity itemVertex = graph.vertexCollection("item").insertVertex(item);
            insertBelongEdgeOfBrand(graph, itemVertex, item.brandId());
            insertBelongEdgeOfCategory(graph, itemVertex, item.categoryId());
            insertHasEdgeOfBarcode(graph, itemVertex, item.barcode());
        }
    }

    private boolean isBrandIdChanged(ArangoDatabase arangoDatabase, DocumentEntity startVertex, String brandId) {
        final String query = "WITH item,belong\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong FILTER v._id =~ '^brand' && v._key != @brandId REMOVE e IN belong RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).put("brandId", brandId).get();
        ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private boolean isCategoryIdChanged(ArangoDatabase arangoDatabase, DocumentEntity startVertex, String categoryId) {
        final String query = "WITH item\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex belong FILTER v._id =~ '^category' && v._key != @categoryId REMOVE e IN belong RETURN e";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).put("categoryId", categoryId).get();
        ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
        return slices.hasNext();
    }

    private void insertBelongEdgeOfBrand(ArangoGraph graph, DocumentEntity itemVertex, String brandId) {
        VertexEntity brandVertex = graph.vertexCollection("brand").getVertex(brandId, VertexEntity.class);
        graph.edgeCollection("belong").insertEdge(new BelongEdge(itemVertex.getId(), brandVertex.getId()));
    }

    private void insertBelongEdgeOfCategory(ArangoGraph graph, DocumentEntity itemVertex, String categoryId) {
        VertexEntity categoryVertex = graph.vertexCollection("category").getVertex(categoryId, VertexEntity.class);
        graph.edgeCollection("belong").insertEdge(new BelongEdge(itemVertex.getId(), categoryVertex.getId()));
    }

    private void updateBarcode(ArangoDatabase arangoDatabase, DocumentEntity startVertex, Barcode barcode) {
        final String query = "WITH item,barcode\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex has FILTER v.barcode != @barcode REMOVE v IN barcode REMOVE e IN has RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).put("barcode", barcode.barcode()).get();
        ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            insertHasEdgeOfBarcode(arangoDatabase.graph("core"), startVertex, barcode);
        }
    }

    /*
        private void updateBarcodeBook(ArangoDatabase arangoDatabase, DocumentEntity startVertex, EANUPCBarcode barcode) {
            Barcode[] barcodes = barcode.barcodes();
            //remove v not in barcodes and e in has
            StringBuilder sb = new StringBuilder("WITH item,has\n" +
                    "FOR v,e IN 1..1 OUTBOUND '").append(startVertex.getId()).append("' has FILTER v._id =~ '^barcode' && v.barcode NOT IN[");
            for (int i = barcodes.length - 1; i >= 0; i--) {
                sb.append("'").append(barcodes[i].barcode()).append("'");
                if (i != 0)
                    sb.append(",");
            }
            sb.append("] REMOVE e IN has REMOVE v IN barcode");
            arangoDatabase.query(sb.toString(), null, null, VPackSlice.class);
            //find existed
            final String query = "WITH item,has\n" +
                    "FOR v,e IN 1..1 OUTBOUND @startVertex has FILTER v._id =~ '^barcode' RETURN v";
            final Map<String, Object> bindVars = new MapBuilder().put("startVertex", startVertex.getId()).get();
            ArangoCursor<VPackSlice> slices = arangoDatabase.query(query, bindVars, null, VPackSlice.class);
            if (!slices.hasNext())
                insertHasEdgeOfBarcode(arangoDatabase.graph("core"), startVertex, new BarcodeBook.Builder(barcodes).build());
            else {
                List<Barcode> temp = new ArrayList<>();
                while (slices.hasNext()) {
                    VPackSlice slice = slices.next();
                    Barcode barcode = new Barcode(BarcodeSpecification.valueOf(slice.get("spec").getAsString()), slice.get("barcode").getAsString());
                    temp.add(barcode);
                }
                Barcode[] noRepeat = fromArrayRemoveRepeat(Barcode.class, barcodes, temp.toArray(new Barcode[0]));
                insertHasEdgeOfBarcode(arangoDatabase.graph("core"), startVertex, new BarcodeBook.Builder(noRepeat).build());
            }
        }

         */
    private <T> T[] fromArrayRemoveRepeat(Class<T> type, T[] source, T[] repeat) {
        int i = 0;
        T[] temp = (T[]) Array.newInstance(type, source.length);
        for (T t1 : source) {
            boolean noRepeat = true;
            for (T t2 : repeat) {
                if (t1 == t2 || t1.equals(t2)) {
                    noRepeat = false;
                    break;
                }
            }
            if (noRepeat) {
                temp[i] = t1;
                i++;
            }
        }
        T[] result = (T[]) Array.newInstance(type, i);
        System.arraycopy(temp, 0, result, 0, i);
        return result;
    }


    private void insertHasEdgeOfBarcode(ArangoGraph graph, DocumentEntity itemVertex, Barcode barcode) {
        VertexEntity barcodeVertex = graph.vertexCollection("barcode").insertVertex(barcode);
        graph.edgeCollection("has").insertEdge(new HasEdge(itemVertex.getId(), barcodeVertex.getId()));
    }

    @Override
    public long size() {
        final String query = " RETURN LENGTH(item)";
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        for (; cursor.hasNext(); ) {
            return cursor.next().getAsLong();
        }
        return 0l;
    }

    @Override
    public Item[] fromBarcode(String barcode) {
        final String query = "WITH barcode,item\n" +
                "FOR b IN barcode FILTER b.barcode =~ @barcode\n" +
                "FOR item,e IN 1..1 INBOUND b has\n" +
                "LET barcode = (FOR v,h IN 1..1 OUTBOUND item._id has RETURN v)\n" +
                "RETURN {item,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("barcode", barcode).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Item[] fromMnemonic(String mnemonic) {
        final String query = "WITH item,barcode\n" +
                "FOR s IN item FILTER s.name.mnemonic =~ @mnemonic\n" +
                "LET barcode = (FOR v,h IN 1..1 OUTBOUND s._id has RETURN v)\n" +
                "RETURN {'item':s,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("mnemonic", mnemonic).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public Item[] fromName(String name) {
        final String query = "WITH item,barcode\n" +
                "FOR s IN item FILTER s.name.name =~ @name\n" +
                "LET barcode = (FOR v,h IN 1..1 OUTBOUND s._id has  RETURN v)\n" +
                "RETURN {'item':s,'barcode':barcode[0].barcode}";
        final Map<String, Object> bindVars = new MapBuilder().put("name", name).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    private static class HasEdge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;
        @DocumentField(DocumentField.Type.TO)
        private String to;

        private HasEdge(String from, String to) {
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