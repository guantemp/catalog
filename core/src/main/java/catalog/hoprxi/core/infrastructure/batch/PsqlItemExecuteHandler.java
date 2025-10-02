/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.infrastructure.DataSourceUtil;
import com.lmax.disruptor.EventHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-09
 */
public class PsqlItemExecuteHandler implements EventHandler<ExecuteSqlEvent> {
    private static final AtomicInteger number = new AtomicInteger(0);
    private final Connection connection;
    private final Statement statement;
    private StringJoiner sql = new StringJoiner(",", "insert into item (id,\"name\",barcode,category_id,brand_id,grade,made_in,spec,shelf_life,last_receipt_price,retail_price,member_price,vip_price,show) values ", "");

    public PsqlItemExecuteHandler() throws SQLException {
        connection = DataSourceUtil.getConnection();
        System.out.println("connection.getAutoCommit():" + connection.getAutoCommit());
        //if (connection.getAutoCommit())
        connection.setAutoCommit(false);
        System.out.println("connection.getAutoCommit():" + connection.getAutoCommit());
        statement = connection.createStatement();
    }

    @Override
    public void onEvent(ExecuteSqlEvent executeSqlEvent, long l, boolean b) throws Exception {
        //System.out.println("sql:" + executeSqlEvent.sql+":"+b);
        if ("LAST_ROW".equals(executeSqlEvent.sql)) {
            System.out.println("LAST_ROW:::" + sql.toString());
            if (number.intValue() != 0) {
                statement.addBatch(sql.toString());
                statement.executeBatch();
                connection.commit();
            }
            statement.close();
            connection.setAutoCommit(true);
            connection.close();
        } else {
            sql.add(executeSqlEvent.sql);
            int i = number.incrementAndGet();
            if (i % 256 == 0) {
                System.out.println(sql);
                statement.addBatch(sql.toString());
                sql = new StringJoiner(",", "insert into item (id,\"name\",barcode,category_id,brand_id,grade,made_in,spec,shelf_life,last_receipt_price,retail_price,member_price,vip_price,show) values ", "");
            }
            if (i % 1024 == 0) {
                statement.executeBatch();
                connection.commit();
                statement.clearBatch();
                //System.out.println(i);
            }
        }
    }
}
