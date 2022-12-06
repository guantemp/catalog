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
package catalog.hoprxi.core.infrastructure;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.Protocol;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.module.jdk8.VPackJdk8Module;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.lang.reflect.Array;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2200-08-04
 */
public class ArangoDBUtil {
    private static final Config config;

    static {
        Config cache = ConfigFactory.load("core");
        Config units = ConfigFactory.load("database");
        config = cache.withFallback(units);
    }

    /**
     * @return
     */
    public static ArangoDB getResource() {
        ArangoDB.Builder builder = new ArangoDB.Builder();
        builder.useProtocol(Protocol.VST).host(config.getString("arangodb_write.host"), config.getInt("arangodb_write.port"));
        builder.registerModule(new VPackJdk8Module()).maxConnections(8).user(config.getString("arangodb_write.user")).password(config.getString("arangodb_write.password"));
        return builder.build();
    }

    /**
     * @param host
     * @param port
     * @param user
     * @param password
     * @return
     */
    public static ArangoDB getResource(String host, int port, String user, String password) {
        ArangoDB.Builder builder = new ArangoDB.Builder();
        builder.useProtocol(Protocol.VST).host(host, port);
        builder.registerModule(new VPackJdk8Module()).maxConnections(8).user(user).password(password);
        return builder.build();
    }

    /**
     * @return
     */
    public static ArangoDatabase getDatabase() {
        return getResource().db(config.hasPath("databaseName") ? config.getString("databaseName") : "catalog");
    }


    /**
     * @param arangoDatabase
     * @param t
     * @param offset
     * @param limit
     * @param <T>
     * @return
     */
    public static <T> T[] calculationCollectionSize(ArangoDatabase arangoDatabase, Class<T> t, long offset, int limit) {
        return calculationCollectionSize(arangoDatabase, t, t.getSimpleName().toLowerCase(), offset, limit);

    }

    @SuppressWarnings("unchecked")
    public static <T> T[] calculationCollectionSize(ArangoDatabase arangoDatabase, Class<T> t, String collectionName, long offset, int limit) {
        if (offset < 0L)
            offset = 0L;
        if (limit < 0)
            limit = 0;
        long count = 0;
        final String countQuery = " RETURN LENGTH(" + collectionName + ")";
        final ArangoCursor<VPackSlice> countCursor = arangoDatabase.query(countQuery, null, null, VPackSlice.class);
        while (countCursor.hasNext()) {
            count = countCursor.next().getAsLong();
        }
        int difference = (int) (count - offset);
        if (difference <= 0)
            return (T[]) Array.newInstance(t, 0);
        int capacity = Math.min(difference, limit);
        return (T[]) Array.newInstance(t, capacity);
    }
}
