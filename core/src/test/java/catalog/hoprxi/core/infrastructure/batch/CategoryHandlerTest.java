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
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.*;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.1 builder 2026-07-12
 */
public class CategoryHandlerTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private CategoryHandler handler;

    // ===================== 预定义常量（请根据实际数据库修改） =====================
    private static final long INVALID_ID = Long.MIN_VALUE; // 确保数据库不存在

    @BeforeMethod
    public void setUp() {
        handler = new CategoryHandler();
        // 不清理缓存，让所有测试共享同一个缓存实例
    }

    // ---------- 辅助方法 ----------
    private ItemImportEvent createEvent(String categoryValue) {
        ItemImportEvent event = new ItemImportEvent();
        EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
        map.put(ItemMapping.CATEGORY, categoryValue);
        event.map = map;
        return event;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Long> getCache() throws Exception {
        Field cacheField = CategoryHandler.class.getDeclaredField("CATEGORY_CACHE");
        cacheField.setAccessible(true);
        return (ConcurrentHashMap<String, Long>) cacheField.get(null);
    }

    // ===================== 测试用例（按 priority 顺序执行） =====================

    // -------- 优先级 1：不改变缓存 / 快速返回 --------
    @Test(priority = 1)
    public void testCategoryNull() throws Exception {
        ItemImportEvent event = createEvent(null);
        handler.onEvent(event);
        assertEquals(event.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));
        event = createEvent("   ");
        handler.onEvent(event);
        assertEquals(event.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));
        event = createEvent(Label.UNCATEGORIZED);
        handler.onEvent(event);
        assertEquals(event.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));

        ItemImportEvent event1 = createEvent(Category.UNCATEGORIZED.name().name());
        handler.onEvent(event1);
        assertEquals(event1.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));

        ItemImportEvent event2 = createEvent(Category.UNCATEGORIZED.name().shortName());
        handler.onEvent(event2);
        assertEquals(event2.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));

        event = createEvent(String.valueOf(INVALID_ID));
        handler.onEvent(event);
        assertEquals(event.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));

        // 单个斜杠
        ItemImportEvent event3 = createEvent("/");
        handler.onEvent(event3);
        assertEquals(event3.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));

        // 多个斜杠
        ItemImportEvent event4 = createEvent("//");
        handler.onEvent(event4);
        assertEquals(event4.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));

        long uncatId = Category.UNCATEGORIZED.id();
        ItemImportEvent event6 = createEvent(String.valueOf(uncatId));
        handler.onEvent(event6);
        assertEquals(event6.map.get(ItemMapping.CATEGORY), String.valueOf(uncatId));

        // 包含空格的斜杠
        ItemImportEvent event5 = createEvent("/ /");
        handler.onEvent(event5);
        assertEquals(event5.map.get(ItemMapping.CATEGORY), String.valueOf(Category.UNCATEGORIZED.id()));
    }

    // -------- 优先级 2：创建路径（会往缓存/数据库插入数据） --------
    @Test(priority = 2)
    public void testPathAllMissing() throws Exception {
        String path = "全新/分类/层级";
        ItemImportEvent event = createEvent(path);
        handler.onEvent(event);
        String leafId = event.map.get(ItemMapping.CATEGORY);
        assertNotNull(leafId);
        assertNotEquals(leafId, String.valueOf(Category.UNCATEGORIZED.id()));
    }

    @Test(priority = 2)
    public void testPathPartialMissing() throws Exception {
        // 先创建 "电子/手机"
        ItemImportEvent event1 = createEvent("电子/手机");
        handler.onEvent(event1);
        String phoneId = event1.map.get(ItemMapping.CATEGORY);

        // 再创建 "电子/手机/苹果"，应生成新节点
        ItemImportEvent event2 = createEvent("电子/手机/苹果");
        handler.onEvent(event2);
        String appleId = event2.map.get(ItemMapping.CATEGORY);
        assertNotEquals(appleId, phoneId);
        assertNotEquals(appleId, String.valueOf(Category.UNCATEGORIZED.id()));
    }

    @Test(priority = 2)
    public void testPathAllExist() throws Exception {
        String path = "电子/手机/苹果";
        // 第一次调用，创建完整路径（如果之前未创建）
        ItemImportEvent event1 = createEvent(path);
        handler.onEvent(event1);
        String leafId = event1.map.get(ItemMapping.CATEGORY);

        // 第二次调用，应缓存命中，返回相同 ID
        ItemImportEvent event2 = createEvent(path);
        handler.onEvent(event2);
        assertEquals(event2.map.get(ItemMapping.CATEGORY), leafId);
    }

    // -------- 优先级 3：依赖已有数据的测试 --------
    @Test(priority = 3, dependsOnMethods = "testPathAllExist")
    public void testCacheHit() throws Exception {
        String path = "电子/手机/苹果";
        ItemImportEvent event = createEvent(path);
        handler.onEvent(event);
        // 由于之前已经创建过，缓存应直接命中，不会查询数据库（但无法直接验证，只能确保返回正确）
        String id = event.map.get(ItemMapping.CATEGORY);
        assertNotNull(id);
        // 验证缓存中存在该键
        ConcurrentHashMap<String, Long> cache = getCache();
        // 注意缓存键格式为 parentId_name，我们无法精确知道，但可以确认 size > 0
        assertFalse(cache.isEmpty());
    }

    @Test(priority = 3)
    public void testCategoryValidIdExists() throws Exception {
        // 自己创建一条路径获得一个真实 ID
        String path = "测试/有效ID";
        ItemImportEvent createEvent = createEvent(path);
        handler.onEvent(createEvent);
        String createdId = createEvent.map.get(ItemMapping.CATEGORY);
        assertNotNull(createdId);

        // 用该 ID 作为输入，应返回相同 ID
        ItemImportEvent idEvent = createEvent(createdId);
        handler.onEvent(idEvent);
        assertEquals(idEvent.map.get(ItemMapping.CATEGORY), createdId);
    }


    // ===================== 所有测试完成后打印最终缓存状态 =====================

    @AfterClass
    public static void printFinalCacheState() throws Exception {
        Field cacheField = CategoryHandler.class.getDeclaredField("CATEGORY_CACHE");
        cacheField.setAccessible(true);
        ConcurrentHashMap<String, Long> cache = (ConcurrentHashMap<String, Long>) cacheField.get(null);

        System.out.println("===== Final Category Cache State =====");
        System.out.println("Cache size: " + cache.size());
        for (Map.Entry<String, Long> entry : cache.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println("========================================");
    }

}