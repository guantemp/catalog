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

import catalog.hoprxi.core.domain.model.role.Role;
import catalog.hoprxi.core.domain.model.role.RoleRepository;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/6
 */
public class ArangoDBRoleRepository implements RoleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDBSkuRepository.class);
    private ArangoDatabase catalog = ArangoDBUtil.getDatabase();

    @Override
    public void save(Role role) {
        boolean exists = catalog.collection("role").documentExists(role.id());
        if (exists)
            catalog.collection("role").updateDocument(role.id(), role);
        else
            catalog.collection("role").insertDocument(role);
    }

    @Override
    public Role find(String id) {
        Role role = catalog.collection("role").getDocument(id, Role.class);
        return null;
    }

    @Override
    public void remove(String id) {
        ArangoGraph graph = catalog.graph("core");
        graph.vertexCollection("role").deleteVertex(id);
    }
}
