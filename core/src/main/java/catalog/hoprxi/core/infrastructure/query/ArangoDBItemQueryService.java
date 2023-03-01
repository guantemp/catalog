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

package catalog.hoprxi.core.infrastructure.query;

import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.infrastructure.ArangoDBUtil;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
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
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-10-15
 */
public class ArangoDBItemQueryService implements ItemQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBItemQueryService.class);
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

    private final ArangoDatabase catalog;

    public ArangoDBItemQueryService(String databaseName) {
        this.catalog = ArangoDBUtil.getResource().db(databaseName);
    }

    @Override
    public ItemView query(String id) {
        final String query = "WITH brand,category,item,barcode\n" +
                "FOR i IN item FILTER i._key == @key\n" +
                "FOR b IN 1..1 OUTBOUND i._id has\n" +
                "FOR c IN 1..1 OUTBOUND i._id belong FILTER c._id =~ '^category'\n" +
                "FOR br IN 1..1 OUTBOUND i._id belong FILTER br._id =~ '^brand'\n" +
                "RETURN {'id':i._key,'name':i.name,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'name':br.name.name},'category':{'id':c._key,'name':c.name.name},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("key", id).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        if (slices.hasNext()) {
            try {
                return rebuild(slices.next());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild itemView", e);
            }
        }
        return null;
    }

    @Override
    public ItemView[] belongToBrand(String brandId, int offset, int limit) {
        final String query = "WITH brand,category,item,barcode\n" +
                "FOR br IN brand FILTER br._key == @brandId\n" +
                "FOR i IN 1..1 INBOUND br._id belong LIMIT @offset,@limit\n" +
                "FOR b IN 1..1 OUTBOUND i._id has\n" +
                "FOR c IN 1..1 OUTBOUND i._id belong FILTER c._id =~ '^category'\n" +
                "RETURN {'id':i._key,'name':i.name,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'name':br.name.name},'category':{'id':c._key,'name':c.name.name},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("brandId", brandId).put("offset", offset).put("limit", limit).get();
        AqlQueryOptions queryOptions = new AqlQueryOptions();
        queryOptions.cache(true);
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public ItemView[] belongToCategory(String categoryId, long offset, int limit) {
        final String query = "WITH brand,category,item,barcode\n" +
                "FOR c IN category FILTER c._key == @categoryId\n" +
                "FOR i IN 1..1 INBOUND c._id belong LIMIT @offset,@limit\n" +
                "FOR b IN 1..1 OUTBOUND i._id has\n" +
                "FOR br IN 1..1 OUTBOUND i._id belong FILTER br._id =~ '^brand'\n" +
                "RETURN {'id':i._key,'name':i.name,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'name':br.name.name},'category':{'id':c._key,'name':c.name.name},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("categoryId", categoryId).put("offset", offset).put("limit", limit).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, new AqlQueryOptions().cache(true), VPackSlice.class);
        return transform(slices);
    }

    @Override
    public ItemView[] belongToCategoryAndDescendants(String categoryId) {
        final String query = "WITH brand,category,item,barcode\n" +
                "FOR c IN category FILTER c._key == @categoryId\n" +
                "FOR i IN 1..1 INBOUND c._id belong LIMIT @offset,@limit\n" +
                "FOR b IN 1..1 OUTBOUND i._id has\n" +
                "FOR br IN 1..1 OUTBOUND i._id belong FILTER br._id =~ '^brand'\n" +
                "RETURN {'id':i._key,'name':i.name,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'name':br.name.name},'category':{'id':c._key,'name':c.name.name},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("categoryId", categoryId).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, new AqlQueryOptions().cache(true), VPackSlice.class);
        return transform(slices);
    }

    @Override
    public ItemView[] queryAll(long offset, int limit) {
        ItemView[] itemViews = ArangoDBUtil.calculationCollectionSize(catalog, ItemView.class, "item", offset, limit);
        if (itemViews.length == 0)
            return itemViews;
        final String query = "WITH item,barcode\n" +
                "FOR i IN item LIMIT @offset,@limit\n" +
                "FOR b IN 1..1 OUTBOUND i._id has\n" +
                "FOR c IN 1..1 OUTBOUND i._id belong FILTER c._id =~ '^category'\n" +
                "FOR br IN 1..1 OUTBOUND i._id belong FILTER br._id =~ '^brand'\n" +
                "RETURN {'id':i._key,'name':i.name,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'name':br.name.name},'category':{'id':c._key,'name':c.name.name},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("offset", offset).put("limit", limit).get();
        final ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        try {
            for (int i = 0; slices.hasNext(); i++)
                itemViews[i] = rebuild(slices.next());
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return itemViews;
    }

    @Override
    public long size() {
        final String query = " RETURN LENGTH(item)";
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, null, null, VPackSlice.class);
        if (cursor.hasNext()) {
            return cursor.next().getAsLong();
        }
        return 0L;
    }

    @Override
    public ItemView[] queryByBarcode(String barcode) {
        final String query = "WITH brand,category,item,barcode\n" +
                "FOR b IN barcode FILTER b.barcode =~ @barcode\n" +
                "FOR i IN 1..1 INBOUND b._id has\n" +
                "FOR c IN 1..1 OUTBOUND i._id belong FILTER c._id =~ '^category'\n" +
                "FOR br IN 1..1 OUTBOUND i._id belong FILTER br._id =~ '^brand'\n" +
                "RETURN {'id':i._key,'name':i.name,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'name':br.name.name},'category':{'id':c._key,'name':c.name.name},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("barcode", barcode).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    @Override
    public ItemView[] serach(String regularExpression) {
        final String query = "WITH brand,category,item,barcode\n" +
                "FOR i IN item FILTER i.regularExpression.regularExpression =~ @regularExpression || i.regularExpression.alias =~ @regularExpression || i.regularExpression.mnemonic =~ @regularExpression\n" +
                "FOR b IN 1..1 OUTBOUND i._id has\n" +
                "FOR c IN 1..1 OUTBOUND i._id belong FILTER c._id =~ '^category'\n" +
                "FOR br IN 1..1 OUTBOUND i._id belong FILTER br._id =~ '^brand'\n" +
                "RETURN {'id':i._key,'regularExpression':i.regularExpression,'barcode':b.barcode,'madeIn':i.madeIn,'spec':i.spec,'grade':i.grade,'brand':{'id':br._key,'regularExpression':br.regularExpression.regularExpression},'category':{'id':c._key,'regularExpression':c.regularExpression.regularExpression},'retailPrice':i.retailPrice,'memberPrice':i.memberPrice,'vipPrice':i.vipPrice}";
        final Map<String, Object> bindVars = new MapBuilder().put("regularExpression", regularExpression).get();
        ArangoCursor<VPackSlice> slices = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(slices);
    }

    private ItemView[] transform(ArangoCursor<VPackSlice> slices) {
        List<ItemView> itemList = new ArrayList<>();
        while (slices.hasNext()) {
            try {
                itemList.add(rebuild(slices.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild item", e);
            }
        }
        return itemList.toArray(new ItemView[0]);
    }

    private ItemView rebuild(VPackSlice slice) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Barcode barcode = BarcodeGenerateServices.createMatchingBarcode(slice.get("barcode").getAsString());

        String id = slice.get("id").getAsString();
        Name name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        VPackSlice madeInSlice = slice.get("madeIn");
        MadeIn madeIn = null;
        String className = madeInSlice.get("_class").getAsString();
        if (Domestic.class.getName().equals(className)) {
            madeIn = new Domestic(madeInSlice.get("code").getAsString(), madeInSlice.get("city").getAsString());
        } else if (Imported.class.getName().equals(className)) {
            madeIn = new Imported(madeInSlice.get("code").getAsString(), madeInSlice.get("country").getAsString());
        }
        Specification spec = new Specification(slice.get("spec").get("value").getAsString());
        Grade grade = Grade.valueOf(slice.get("grade").getAsString());
        ItemView itemView = new ItemView(id, barcode, name, madeIn, spec, grade);

        VPackSlice brandSlice = slice.get("brand");
        ItemView.BrandView brandView = new ItemView.BrandView(brandSlice.get("id").getAsString(), brandSlice.get("name").getAsString());
        itemView.setBrandView(brandView);
        VPackSlice categorySlice = slice.get("category");
        ItemView.CategoryView categoryView = new ItemView.CategoryView(categorySlice.get("id").getAsString(), categorySlice.get("name").getAsString());
        itemView.setCategoryView(categoryView);

        VPackSlice retailPriceSlice = slice.get("retailPrice");
        VPackSlice amountSlice = retailPriceSlice.get("price").get("amount");
        MonetaryAmount amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        Unit unit = Unit.valueOf(retailPriceSlice.get("price").get("unit").getAsString());
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        itemView.setRetailPrice(retailPrice);

        VPackSlice memberPriceSlice = slice.get("memberPrice");
        String priceName = memberPriceSlice.get("name").getAsString();
        amountSlice = memberPriceSlice.get("price").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = Unit.valueOf(memberPriceSlice.get("price").get("unit").getAsString());
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        itemView.setMemberPrice(memberPrice);

        VPackSlice vipPriceSlice = slice.get("vipPrice");
        priceName = vipPriceSlice.get("name").getAsString();
        amountSlice = vipPriceSlice.get("price").get("amount");
        amount = Money.of(amountSlice.get("number").getAsBigDecimal(), amountSlice.get("currency").get("baseCurrency").get("currencyCode").getAsString());
        unit = Unit.valueOf(vipPriceSlice.get("price").get("unit").getAsString());
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        itemView.setVipPrice(vipPrice);

        return itemView;
    }
}
