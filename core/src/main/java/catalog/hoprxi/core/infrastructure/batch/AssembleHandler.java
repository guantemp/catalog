/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.batch.ItemMapping;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class AssembleHandler implements EventHandler<ItemImportEvent> {
    private AtomicInteger number = new AtomicInteger(0);
    private static final EventTranslatorOneArg<ExecuteSqlEvent, String> TRANSLATOR =
            (event, sequence, sql) -> event.sql = sql;
    private static Disruptor<ExecuteSqlEvent> executeDisruptor;
    private static RingBuffer<ExecuteSqlEvent> ringBuffer;


    static {
        executeDisruptor = new Disruptor<>(
                ExecuteSqlEvent::new,
                128,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );
        //executeDisruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler<>());
        try {
            executeDisruptor.handleEventsWith(new PsqlItemExecuteHandler());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        executeDisruptor.start();
        ringBuffer = executeDisruptor.getRingBuffer();
    }

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        EnumMap<ItemMapping, String> map = itemImportEvent.map;
        if (itemImportEvent.verify == Verify.OK) {
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            joiner.add(map.get(ItemMapping.ID)).add(map.get(ItemMapping.NAME)).add(map.get(ItemMapping.BARCODE)).add(map.get(ItemMapping.CATEGORY))
                    .add(map.get(ItemMapping.BRAND)).add(map.get(ItemMapping.GRADE)).add(map.get(ItemMapping.MADE_IN)).add(map.get(ItemMapping.SPEC))
                    .add(map.get(ItemMapping.SHELF_LIFE)).add(map.get(ItemMapping.LAST_RECEIPT_PRICE)).add(map.get(ItemMapping.RETAIL_PRICE))
                    .add(map.get(ItemMapping.MEMBER_PRICE)).add(map.get(ItemMapping.VIP_PRICE)).add(map.get(ItemMapping.SHOW));
            //System.out.println(number.incrementAndGet());
            ringBuffer.publishEvent(TRANSLATOR, joiner.toString());
        }
        if (map.get(ItemMapping.LAST_ROW) != null) {//最后一行
            ringBuffer.publishEvent(TRANSLATOR, "LAST_ROW");
            executeDisruptor.shutdown();
        }
    }
}
