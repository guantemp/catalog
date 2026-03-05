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

package catalog.hoprxi.scale.infrastructure.persistence;


import catalog.hoprxi.scale.infrastructure.Setup;
import org.testng.annotations.Test;

import java.util.Properties;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/25
 */
public class SetupTest {

    @Test
    public void setup() throws Exception {
        /*
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", write.getString("hikari.dataSourceClassName"));
        String host = write.getString("db.host");
        int port = write.getInt("db.port");
        props.setProperty("dataSource.serverName", host);
        props.setProperty("dataSource.portNumber", String.valueOf(port));

        String entry = host + ":" + port;
        String user = DatabaseSpecDecrypt.decrypt(entry, write.getString("db.user"));
        String password = DatabaseSpecDecrypt.decrypt(entry, write.getString("db.password"));
        props.setProperty("dataSource.user", user);
        props.setProperty("dataSource.password", password);
        props.setProperty("dataSource.databaseName", write.getString("db.databaseName"));


         */
        Setup.setup();
    }
}