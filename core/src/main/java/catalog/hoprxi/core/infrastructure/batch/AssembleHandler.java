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

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class AssembleHandler implements EventHandler<ItemImportEvent> {
    private AtomicInteger number = new AtomicInteger(0);
    private static final EventTranslatorOneArg<ExecuteSqlEvent, String> TRANSLATOR =
            (event, sequence, sql) -> event.sql = sql;
    private static Disruptor<ExecuteSqlEvent> executeDisruptor;
    private static RingBuffer<ExecuteSqlEvent> ringBuffer;


    private static CloseableHttpClient httpClient;

    static {
        executeDisruptor = new Disruptor<>(
                ExecuteSqlEvent::new,
                256,
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
        EnumMap<Corresponding, String> map = itemImportEvent.map;
        if (itemImportEvent.verify == Verify.OK) {
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            joiner.add(map.get(Corresponding.ID)).add(map.get(Corresponding.NAME)).add(map.get(Corresponding.BARCODE)).add(map.get(Corresponding.CATEGORY))
                    .add(map.get(Corresponding.BRAND)).add(map.get(Corresponding.GRADE)).add(map.get(Corresponding.MADE_IN)).add(map.get(Corresponding.SPEC))
                    .add(map.get(Corresponding.SHELF_LIFE)).add(map.get(Corresponding.LATEST_RECEIPT_PRICE)).add(map.get(Corresponding.RETAIL_PRICE))
                    .add(map.get(Corresponding.MEMBER_PRICE)).add(map.get(Corresponding.VIP_PRICE)).add(map.get(Corresponding.SHOW));
            //System.out.println(number.incrementAndGet());
            ringBuffer.publishEvent(TRANSLATOR, joiner.toString());
        }
        if (map.get(Corresponding.LAST_ROW) != null) {//最后一行
            ringBuffer.publishEvent(TRANSLATOR, "LAST_ROW");
            executeDisruptor.shutdown();
        }
    }
}
