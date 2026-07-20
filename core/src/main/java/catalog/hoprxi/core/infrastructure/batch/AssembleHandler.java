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

import catalog.hoprxi.core.application.batch.ItemMapping;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.sql.SQLException;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class AssembleHandler implements EventHandler<ItemImportEvent> , WorkHandler<ItemImportEvent> {
    private static final EventTranslatorOneArg<ExecuteSqlEvent, String> TRANSLATOR =
            (event, sequence, sql) -> event.sql = sql;
    private static final Disruptor<ExecuteSqlEvent> executeDisruptor;
    private static final RingBuffer<ExecuteSqlEvent> ringBuffer;

    static {
        executeDisruptor = new Disruptor<>(
                ExecuteSqlEvent::new,
                1024,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );
        executeDisruptor.setDefaultExceptionHandler(new ExceptionHandler<ExecuteSqlEvent>() {
            @Override
            public void handleEventException(Throwable throwable, long l, ExecuteSqlEvent executeSqlEvent) {
                throwable.printStackTrace(); // 打印到控制台
            }

            @Override
            public void handleOnStartException(Throwable ex) {}
            @Override
            public void handleOnShutdownException(Throwable ex) {}
        });
        executeDisruptor.handleEventsWith(new PsqlItemExecuteHandler());
        executeDisruptor.start();
        ringBuffer = executeDisruptor.getRingBuffer();
    }

    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) {
        //long t1 = System.nanoTime();
        Map<ItemMapping, String> map = event.map;
        // 1. 如果前面（如校验和错误、后续重复）已经报错了，直接跳过组装 SQL
        if (event.hasWrong()) {
            // 注意：如果有错，也要判断是不是最后一行，如果是，依然要发 LAST_ROW 触发关闭
            if (map.get(ItemMapping.LAST_ROW) != null) {
                ringBuffer.publishEvent(TRANSLATOR, "LAST_ROW");
                executeDisruptor.shutdown();
            }
            return;
        }
        // 2. 【全局清算】：检查当前条码是否在“重复黑名单”里
        // 注意：此时 map 中的 barcode 带有单引号，需要去掉再比对
        String cleanBarcode = map.get(ItemMapping.BARCODE).replace("'", "");
        if (BatchBarcodeHandler.BARCODE_BLACKLIST.contains(cleanBarcode)) {
            // 说明它是第一条，但后面有重复的！在这里把它也变成错误！
            //event.addWrong(Verify.BARCODE_REPEAT, cleanBarcode);
            return; // 不组装 SQL
        }

        StringJoiner joiner = new StringJoiner(",", "(", ")");
        joiner.add(String.valueOf(event.generatedId)).add(event.basicInfo.nameJson()).add(event.barcode).add(String.valueOf(event.categoryId))
                .add(String.valueOf(event.brandId)).add("'" + event.basicInfo.grade().name() + "'").add(event.madeInJson).add(event.basicInfo.spec())
                .add(String.valueOf(event.basicInfo.shelfLife())).add(event.basicInfo.lastReceiptPriceJson()).add(event.basicInfo.retailPriceJson())
                .add(event.basicInfo.memberPriceJson()).add(event.basicInfo.vipPriceJson()).add(event.show);
        //System.out.println("joiner:"+joiner);
        ringBuffer.publishEvent(TRANSLATOR, joiner.toString());

        if (map.get(ItemMapping.LAST_ROW) != null) {//最后一行
            ringBuffer.publishEvent(TRANSLATOR, "LAST_ROW");
            System.out.println("AssembleHandler last_row yesyes");
            executeDisruptor.shutdown();
        }
        //long t2 = System.nanoTime();
        //System.out.println("AssembleHandler处理耗时 " + (t2 - t1) / 1_000_000 + " ms");
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }
}
