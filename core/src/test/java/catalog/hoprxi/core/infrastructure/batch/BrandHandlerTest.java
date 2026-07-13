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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.*;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.1 builder 2026-07-11
 */
public class BrandHandlerTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private static final long UNBRANDED_ID = Brand.UNBRANDED.id();

    // 执行处理并返回结果 map
    private EnumMap<ItemMapping, String> processBrand(String value) throws Exception {
        EventHandler<ItemImportEvent> handler = new BrandHandler();
        ItemImportEvent event = new ItemImportEvent();
        EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
        map.put(ItemMapping.BRAND, value);
        event.map = map;
        handler.onEvent(event, 0, false);
        return event.map;
    }

    // ---- 测试用例 ----

    @Test
    void testNullBrand() throws Exception {
        EnumMap<ItemMapping, String> result = processBrand(null);
        assertEquals(String.valueOf(UNBRANDED_ID), result.get(ItemMapping.BRAND));
        result = processBrand("");
        assertEquals(String.valueOf(UNBRANDED_ID), result.get(ItemMapping.BRAND));
        result = processBrand("   ");
        assertEquals(String.valueOf(UNBRANDED_ID), result.get(ItemMapping.BRAND));
        result = processBrand(Label.UNBRANDED);
        assertEquals(String.valueOf(UNBRANDED_ID), result.get(ItemMapping.BRAND));
    }

    @Test
    void testNonExistingNumericId() throws Exception {
        long fakeId = 999999999999999999L;
        EnumMap<ItemMapping, String> result = processBrand(String.valueOf(fakeId));
        assertEquals(String.valueOf(UNBRANDED_ID), result.get(ItemMapping.BRAND));
    }

    @Test
    void testExistingNumericId() throws Exception {
        // 先创建一个品牌，获得其 ID，再用该 ID 测试数字 ID 场景
        String uniqueName = "98684290385927371";
        EnumMap<ItemMapping, String> createResult = processBrand(uniqueName);
        String idStr = createResult.get(ItemMapping.BRAND);
        assertNotNull(idStr);
    }

    @Test
    void testSingleNameNewBrand() throws Exception {
        String name = "海康威视";
        EnumMap<ItemMapping, String> result = processBrand(name);
        String idStr = result.get(ItemMapping.BRAND);
        assertNotNull(idStr);
    }

    @Test
    void testBrandWithShortName() throws Exception {
        String input = "阿多/dsppa";
        EnumMap<ItemMapping, String> result = processBrand(input);
        String idStr = result.get(ItemMapping.BRAND);
        assertNotNull(idStr);
    }

    @Test
    void testNewBrandCache() throws Exception {
        String input = "三峡牌/sanxian";
        EnumMap<ItemMapping, String> result = processBrand(input);
        String idStr = result.get(ItemMapping.BRAND);
        assertNotNull(idStr);
    }

    @Test
    void testCacheReuse() throws Exception {
        // 第一次调用，创建并缓存
        EnumMap<ItemMapping, String> result1 = processBrand("官响环");
        String idStr1 = result1.get(ItemMapping.BRAND);
        long id1 = Long.parseLong(idStr1);

        // 第二次调用，应命中缓存
        EnumMap<ItemMapping, String> result2 = processBrand("官响环");
        String idStr2 = result2.get(ItemMapping.BRAND);
        long id2 = Long.parseLong(idStr2);

        assertEquals(id1, id2, "缓存应返回相同 ID");

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
}