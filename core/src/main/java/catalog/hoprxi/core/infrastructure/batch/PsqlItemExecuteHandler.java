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

package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.lmax.disruptor.EventHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-09
 */
public class PsqlItemExecuteHandler implements EventHandler<ExecuteSqlEvent> {
    private static AtomicInteger number = new AtomicInteger(1);
    StringJoiner sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,latest_receipt_price,retail_price,member_price,vip_price) values ", "");
    private Connection connection = null;
    private Statement statement = null;

    public PsqlItemExecuteHandler() throws SQLException {
        connection = PsqlUtil.getConnection();
        //System.out.println("connection.getAutoCommit()" + connection.getAutoCommit());
        connection.setAutoCommit(false);
        statement = connection.createStatement();
    }

    @Override
    public void onEvent(ExecuteSqlEvent executeSqlEvent, long l, boolean b) throws Exception {
        if ("LAST_ROW".equals(executeSqlEvent.sql)) {
            System.out.println("LAST_ROW:::" + executeSqlEvent.sql);
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();

        } else {
            sql.add(executeSqlEvent.sql);
            int i = number.incrementAndGet();
            if (i % 128 == 0) {
                System.out.println(sql);
                statement.addBatch(sql.toString());
                sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,latest_receipt_price,retail_price,member_price,vip_price) values ", "");
            }
            if (i % 2048 == 0) {
                statement.executeBatch();
                connection.commit();
                statement.clearBatch();
                System.out.println(i);
                System.out.println(executeSqlEvent.count);
            }
        }
    }
}
