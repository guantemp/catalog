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

import catalog.hoprxi.core.application.query.BrandQueryService;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.ArangoDBUtil;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.DocumentField;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.cache.Cache;
import salt.hoprxi.cache.CacheFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2021-10-16
 */
public class ArangoDBBrandQueryService implements BrandQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBBrandQueryService.class);
    private static Constructor<Name> nameConstructor;
    private static Cache<String, Brand> cache = CacheFactory.build("brand");

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

    public ArangoDBBrandQueryService(String databaseName) {
        this.catalog = ArangoDBUtil.getResource().db(databaseName);
    }

    @Override
    public Brand[] queryAll(int offset, int limit) {
        Brand[] brands = ArangoDBUtil.calculationCollectionSize(catalog, Brand.class, offset, limit);
        if (brands.length == 0)
            return brands;
        final String query = "FOR v IN brand LIMIT @offset,@limit RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("offset", offset).put("limit", limit).get();
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        try {
            for (int i = 0; cursor.hasNext(); i++) {
                brands[i] = rebuild(cursor.next());
            }
        } catch (MalformedURLException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Can't rebuild brand", e);
        }
        return brands;
    }

    @Override
    public int size() {
        int count = 0;
        final String countQuery = " RETURN LENGTH(brand)";
        final ArangoCursor<VPackSlice> countCursor = catalog.query(countQuery, null, null, VPackSlice.class);
        if (countCursor.hasNext()) {
            count = countCursor.next().getAsInt();
        }
        return count;
    }

    @Override
    public Brand[] queryByName(String name) {
        final String query = "FOR v IN brand FILTER v.name.name =~ @name || v.name.alias =~ @name ||v.name.mnemonic =~ @name RETURN v";
        final Map<String, Object> bindVars = new MapBuilder().put("name", name).get();
        final ArangoCursor<VPackSlice> cursor = catalog.query(query, bindVars, null, VPackSlice.class);
        return transform(cursor);
    }

    private Brand rebuild(VPackSlice slice) throws MalformedURLException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (slice == null)
            return null;
        String id = slice.get(DocumentField.Type.KEY.getSerializeName()).getAsString();
        if (id.equals(Brand.UNDEFINED.id()))
            return Brand.UNDEFINED;
        Name name = nameConstructor.newInstance(slice.get("name").get("name").getAsString(), slice.get("name").get("mnemonic").getAsString(), slice.get("name").get("alias").getAsString());
        //about
        VPackSlice aboutSlice = slice.get("about");
        if (!aboutSlice.isNone()) {
            String story = aboutSlice.get("story").getAsString();
            Year since = Year.of(aboutSlice.get("since").get("year").getAsInt());
            VPackSlice homepageSlice = aboutSlice.get("homepage");
            URL homePage = new URL(homepageSlice.get("protocol").getAsString(),
                    homepageSlice.get("host").getAsString(), homepageSlice.get("port").getAsInt(), homepageSlice.get("file").getAsString());
            VPackSlice logoSlice = aboutSlice.get("logo");
            URL logo = new URL(logoSlice.get("protocol").getAsString(),
                    logoSlice.get("host").getAsString(), logoSlice.get("port").getAsInt(), logoSlice.get("file").getAsString());
            return new Brand(id, name, new AboutBrand(homePage, logo, since, story));
        }
        return new Brand(id, name, null);
    }

    private Brand[] transform(ArangoCursor<VPackSlice> cursor) {
        List<Brand> brandList = new ArrayList<>();
        while (cursor.hasNext()) {
            try {
                brandList.add(rebuild(cursor.next()));
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                     MalformedURLException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't rebuild brand", e);
            }
        }
        return brandList.toArray(new Brand[0]);
    }
}
