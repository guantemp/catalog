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
package catalog.hoprxi.core.infrastructure.persistence;


import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.ArangoDBUtil;
import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentField;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.velocypack.VPackSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.id.LongId;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Year;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 2019-05-14
 */
public class ArangoDBBrandRepository implements BrandRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBBrandRepository.class);
    private static final DocumentUpdateOptions updateOptions = new DocumentUpdateOptions().serializeNull(false);
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

    public ArangoDBBrandRepository(String databaseName) {
        this.catalog = ArangoDBUtil.getResource().db(databaseName);
    }


    @Override
    public Brand find(String id) {
        ArangoGraph graph = catalog.graph("core");
        VPackSlice slice = graph.vertexCollection("brand").getVertex(id, VPackSlice.class);
        try {
            return rebuild(slice);
        } catch (MalformedURLException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.debug("Can't rebuild brand");
        }
        return null;
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

    @Override
    public String nextIdentity() {
        return String.valueOf(LongId.generate());
    }

    @Override
    public void remove(String id) {
        ArangoGraph graph = catalog.graph("core");
        boolean exists = catalog.collection("brand").documentExists(id);
        if (exists)
            graph.vertexCollection("brand").deleteVertex(id);
    }

    @Override
    public void save(Brand brand) {
        ArangoCollection collection = catalog.collection("brand");
        boolean exists = collection.documentExists(brand.id());
        if (exists) {
            collection.updateDocument(brand.id(), brand, updateOptions);
        } else {
            collection.insertDocument(brand);
        }
    }
}
