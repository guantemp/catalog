/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.2 builder 2026-07-20
 */
public class PsqlItemExecuteHandler implements EventHandler<ExecuteSqlEvent> {
    private static final AtomicInteger number = new AtomicInteger(0);

    // 纯内存中攒 SQL 的容器
    private final List<String> batchBuffer = new ArrayList<>();

    // 记录当前 StringJoiner 里攒了多少条数据
    private int currentJoinerCount = 0;

    // 128条拼成一个完整的多值 INSERT 语句
    private StringJoiner sql = new StringJoiner(",", "insert into item (id,\"name\",barcode,category_id,brand_id,grade,made_in,spec,shelf_life,last_receipt_price,retail_price,member_price,vip_price,show) values ", "");

    @Override
    public void onEvent(ExecuteSqlEvent executeSqlEvent, long l, boolean b) throws Exception {
        if ("LAST_ROW".equals(executeSqlEvent.sql)) {
            // 【LAST_ROW 的职责】：专门负责把 StringJoiner 里最后没凑够 ? 条的尾巴数据闭合，塞进 batchBuffer
            if (currentJoinerCount > 0) {
                batchBuffer.add(sql.toString());
            }
            // 闭合完毕后，触发 flush 把 batchBuffer 里的所有数据全部提交入库
            flush();
        } else {
            // 【正常数据的职责】：校验并拼接
            if (executeSqlEvent.sql == null || executeSqlEvent.sql.trim().isEmpty() || !executeSqlEvent.sql.contains("(")) {
                System.err.println("警告：跳过非法的 SQL 数据 -> " + executeSqlEvent.sql);
                return;
            }
            sql.add(executeSqlEvent.sql);
            currentJoinerCount++;
            int i = number.incrementAndGet();
            // 每 128 条，拼成一个完整的多值 INSERT 语句，放入批量缓冲区
            if (currentJoinerCount == 128) {
                batchBuffer.add(sql.toString());
                // 重置 StringJoiner 和计数器，准备下一轮
                sql = new StringJoiner(",", "insert into item (id,\"name\",barcode,category_id,brand_id,grade,made_in,spec,shelf_life,last_receipt_price,retail_price,member_price,vip_price,show) values ", "");
                currentJoinerCount = 0;
            }
            // 每 1024 条，触发一次数据库批量提交
            if (i % 1024 == 0) {
                flush();
            }
        }
    }

    // 【flush 的职责】：纯粹的数据库执行器，只负责拿连接、执行、关连接
    private void flush() {
        long t1 = System.nanoTime();
        if (batchBuffer.isEmpty()) {
            return;
        }

        try (Connection connection = PsqlUtil.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                for (String sqlStr : batchBuffer) {
                    statement.addBatch(sqlStr);
                }
                statement.executeBatch();
                connection.commit();
                System.out.println("成功提交 " + number.get() + " 条数据");
            } catch (SQLException e) {
                connection.rollback();
                System.err.println("批量提交失败，错误信息: " + e.getMessage());
                throw e;
            }
            long t2 = System.nanoTime();
            System.out.println("成功提交: " + (t2 - t1) / 1_000_000 + " ms");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 执行完毕，清空内存缓冲区
            batchBuffer.clear();
        }
    }
}