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

package catalog.hoprxi.scale.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.scale.application.query.ScaleQuery;
import catalog.hoprxi.scale.application.query.SqlClauseSpec;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.infrastructure.query.postgresql.spec.KeywordSqlClauseSpec;
import catalog.hoprxi.scale.infrastructure.query.postgresql.spec.RetailPriceSqlClauseSpec;
import io.netty.buffer.ByteBuf;
import org.javamoney.moneta.Money;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class PsqlScaleQueryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final ScaleQuery query = new PsqlScaleQuery();

    @Test(invocationCount = 5, threadPoolSize = 1)
    public void testFindAsync() throws InterruptedException {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.findAsync(new Plu(102)),
                query.findAsync(new Plu(100)),
                query.findAsync(new Plu(1))
        };
        PsqlScaleQueryTest.printResult(fluxes);
    }

    @Test()
    public void testSearchAsync() throws InterruptedException {
        Flux<ByteBuf>[] fluxes = new Flux[]{
                query.searchAsync(new SqlClauseSpec[]{new KeywordSqlClauseSpec("鱼")}, 0, 50, SortFieldEnum._ID),
                query.searchAsync(0, 20),
                query.searchAsync(new SqlClauseSpec[]{new RetailPriceSqlClauseSpec(Money.of(1.99, "CNY"), Money.of(199, "CNY"))},
                        0, 50, SortFieldEnum._LAST_RECEIPT_PRICE)
        };
        PsqlScaleQueryTest.printResult(fluxes);
    }

    private static void printResult(Flux<ByteBuf>[] fluxes) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(fluxes.length);
        // 为每个查询在独立线程中启动订阅
        for (int i = 0; i < fluxes.length; i++) {
            StringBuilder sb = new StringBuilder();
            final int queryIndex = i;
            new Thread(() -> {
                System.out.println("[Thread-" + Thread.currentThread().threadId() + "] Starting query #" + queryIndex);
                fluxes[queryIndex].subscribe(
                        byteBuf -> {
                            try {
                                sb.append(byteBuf.toString(StandardCharsets.UTF_8));
                            } finally {
                                byteBuf.release(); // 安全释放
                            }
                        },
                        error -> {
                            System.err.println("[Thread-" + Thread.currentThread().threadId() + "] Error: " + error.getMessage());
                            error.printStackTrace();
                        },
                        () -> {
                            //System.out.println("[Thread-" + Thread.currentThread().threadId() + "] Query #" + queryIndex + " completed");
                            System.out.println(sb);
                            latch.countDown();
                        }
                );
            }).start();
        }
        // 等待所有查询完成（30秒超时）
        latch.await();
    }
}