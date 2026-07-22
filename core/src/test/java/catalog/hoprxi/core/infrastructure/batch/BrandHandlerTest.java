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

import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import com.lmax.disruptor.EventHandler;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.1 builder 2026-07-11
 */
public class BrandHandlerTest {
    private static final long UNBRANDED_ID = Brand.UNBRANDED.id();

    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    // ---- 测试完成后打印 BRAND_CACHE ----
    @AfterClass
    static void printBrandCache() {
        try {
            Field field = BrandHandler.class.getDeclaredField("BRAND_CACHE");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Long> cache = (ConcurrentHashMap<String, Long>) field.get(null);
            System.out.println("========== BRAND_CACHE 内容 (size=" + cache.size() + ") ==========");
            for (Map.Entry<String, Long> entry : cache.entrySet()) {
                System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
            }
            System.out.println("==================================================");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("无法访问 BRAND_CACHE 字段: " + e.getMessage());
        }
    }

    // ---- 测试用例 ----

    // 执行处理并返回结果 map
    private ItemImportEvent processBrand(String value) throws Exception {
        EventHandler<ItemImportEvent> handler = new BrandHandler();
        ItemImportEvent event = new ItemImportEvent();
        Map<ItemMapping, String> map = new ConcurrentHashMap<>();
        map.put(ItemMapping.BRAND, value);
        event.map = map;
        handler.onEvent(event, 0, false);
        return event;
    }

    @Test
    void testNullBrand() throws Exception {
        ItemImportEvent result = processBrand("");
        assertEquals(result.brandId, UNBRANDED_ID);
        result = processBrand("   ");
        assertEquals(result.brandId, UNBRANDED_ID);
        result = processBrand(Label.UNBRANDED);
        assertEquals(result.brandId, UNBRANDED_ID);
        result = processBrand(String.valueOf(999999999999999999L));
        assertEquals(result.brandId, UNBRANDED_ID);
    }

    @Test
    void testExistingNumericId() throws Exception {
        // 先创建一个品牌，获得其 ID，再用该 ID 测试数字 ID 场景
        String uniqueName = "495651176959596546";
        ItemImportEvent result = processBrand(uniqueName);
        assertEquals(result.brandId, 495651176959596546L);
    }

    @Test
    void testSingleNameNewBrand() throws Exception {
        String name = "海康威视";
        ItemImportEvent result = processBrand(name);
        System.out.println(result.brandId);
        String input = "阿多/dsppa";
        result = processBrand(input);
        System.out.println(result.brandId);
        input = "三峡牌/sanxian";
        result = processBrand(input);
        System.out.println(result.brandId);
    }

    @Test
    void testCacheReuse() throws Exception {
        // 第一次调用，创建并缓存
        ItemImportEvent result1 = processBrand("官响环");
        long id1 = result1.brandId;

        // 第二次调用，应命中缓存
        ItemImportEvent result2 = processBrand("官响环");
        long id2 = result2.brandId;
        assertEquals(id1, id2, "缓存应返回相同 ID");

    }
}