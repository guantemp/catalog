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

import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.PriceRepository;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.DocumentField;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.util.MapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.MonetaryAmount;
import java.util.Map;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/6
 */
public class ArangoDBPriceRepository implements PriceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBSkuRepository.class);
    private ArangoDatabase catalog = ArangoDBUtil.getDatabase();

    @Override
    public void save(Price price) {
        ArangoGraph graph = catalog.graph("core");
        final String query = "with sku,role\n" +
                "FOR v,e IN 1..1 OUTBOUND @startVertex price filter";
        final Map<String, Object> bindVars = new MapBuilder().put("skuId", price.skuId()).put("roleId", price.roleId()).get();
        ArangoCursor<EdgeEntity> edges = catalog.query(query, bindVars, null, EdgeEntity.class);
        if (edges.hasNext())
            graph.edgeCollection("price").updateEdge(edges.next().getId(), price.amount());
        graph.edgeCollection("price").insertEdge(new PriceEdge(price.skuId(), price.roleId(), price.amount()));
    }

    @Override
    public Price find(String skuId, String roleId) {
        return null;
    }

    @Override
    public void remove(Price price) {

    }

    private static class PriceEdge {
        @DocumentField(DocumentField.Type.FROM)
        private String from;
        @DocumentField(DocumentField.Type.TO)
        private String to;
        private MonetaryAmount amount;

        private PriceEdge(String from, String to, MonetaryAmount amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }
}
