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
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.EnumMap;

import static org.testng.Assert.*;

/**
 * MadeInHandler 集成测试（真实网络请求）
 * 使用 EnumMap 作为事件 Map
 */
public class MadeInHandlerTest {

    private MadeInHandler handler;

    @BeforeClass
    public void setUp() {
        // 每个测试类只创建一个 handler，共享缓存
        handler = new MadeInHandler();
    }

    // ===================== 核心功能测试 =====================

    /**
     * 测试国内城市（如“北京”）→ 应返回 Domestic
     */
    @Test
    public void testDomesticCity() throws Exception {
        ItemImportEvent event = createEvent("北京");
        handler.onEvent(event);

        String result = event.map.get(ItemMapping.MADE_IN);
        assertNotNull(result);

        assertTrue(result.contains("\"_class\":\"Domestic\""));
        assertTrue(result.contains("\"code\":"));
        // 可选：验证包含“北京市”
        assertTrue(result.contains("\"madeIn\":\"北京市\"") || result.contains("\"madeIn\":\"北京\""));
    }

    /**
     * 测试进口国家（如“美国”）→ 应返回 Imported
     */
    @Test
    public void testImportedCountry() throws Exception {
        ItemImportEvent event = createEvent("美国");
        handler.onEvent(event);

        String result = event.map.get(ItemMapping.MADE_IN);
        assertNotNull(result);
        assertTrue(result.contains("\"_class\":\"Imported\""));
        assertTrue(result.contains("\"code\":"));
        // 美国缩写可能是 US 或 USA
        assertTrue(result.contains("\"madeIn\":\"US\"") || result.contains("\"madeIn\":\"美国\""));
    }

    /**
     * 测试县级地区（如“朝阳区”）→ 应追溯父级，最终返回 Domestic（北京市）
     */
    @Test
    public void testCountyLevel() throws Exception {
        ItemImportEvent event = createEvent("朝阳区");
        handler.onEvent(event);

        String result = event.map.get(ItemMapping.MADE_IN);

        assertNotNull(result);
        // 朝阳区属于北京市，因此应返回 Domestic
        assertTrue(result.contains("\"_class\":\"Domestic\""));
        // 北京市的 code 为 110000
        assertTrue(result.contains("\"code\":110100"));
        assertTrue(result.contains("\"madeIn\":\"北京市\""));
    }

    /**
     * 测试未知地区 → 应返回 UNORIGINATED
     */
    @Test
    public void testUnknown() throws Exception {
        ItemImportEvent event = createEvent("不存在");
        handler.onEvent(event);

        String result = event.map.get(ItemMapping.MADE_IN);
        assertNotNull(result);
        assertTrue(result.contains("\"_class\":\"UNORIGINATED\""));
        assertTrue(result.contains("\"code\":" + MadeIn.UNORIGINATED.code()));
        assertTrue(result.contains("\"madeIn\":\"" + MadeIn.UNORIGINATED.madeIn() + "\""));
    }

    /**
     * 测试空字符串（或仅空白）→ 直接返回 UNORIGINATED，不发起网络请求
     */
    @Test
    public void testBlank() throws Exception {
        ItemImportEvent event = createEvent("   ");
        long start = System.currentTimeMillis();
        handler.onEvent(event);
        long cost = System.currentTimeMillis() - start;

        String result = (String) event.map.get(ItemMapping.MADE_IN);
        assertTrue(result.contains("\"_class\":\"UNORIGINATED\""));
        // 空值不会调用网络，耗时应该极短（<50ms）
        assertTrue(cost < 100, "耗时 " + cost + "ms，应小于100ms");
    }

    // ===================== 缓存测试 =====================

    /**
     * 测试缓存：第二次查询相同地名应该直接命中缓存，不重复请求网络
     * 通过比较两次耗时来验证
     */
    @Test
    public void testCacheHit() throws Exception {
        String query = "上海";

        // 第一次查询（应该发起网络请求）
        ItemImportEvent event1 = createEvent(query);

        long start1 = System.currentTimeMillis();
        handler.onEvent(event1);
        long cost1 = System.currentTimeMillis() - start1;

        // 第二次查询（应该缓存命中）
        ItemImportEvent event2 = createEvent(query);
        long start2 = System.currentTimeMillis();
        handler.onEvent(event2);
        long cost2 = System.currentTimeMillis() - start2;

        // 第二次应明显更快（缓存命中）
        assertTrue(cost2 < cost1, "缓存命中应更快，但 cost2=" + cost2 + " >= cost1=" + cost1);
        System.out.println("缓存命中应更快，但 cost2=" + cost2 + " >= cost1=" + cost1);

        // 验证两次结果一致
        String result1 = event1.map.get(ItemMapping.MADE_IN);
        String result2 = event2.map.get(ItemMapping.MADE_IN);
        assertEquals(result1, result2);
    }

    /**
     * 测试缓存中已存在，即使网络断开（模拟），仍能返回缓存结果。
     * 但这里无法模拟网络断开，仅示意：首次查询成功后，缓存生效。
     * 实际可通过将 handler 中的 httpClient 替换为 mock 来测，但此处不涉及。
     * 我们通过连续调用验证缓存命中。
     */
    @Test(dependsOnMethods = "testCacheHit")
    public void testCachePersists() throws Exception {
        // 先确保缓存中有“上海”
        //testCacheHit(); // 依赖方法已执行，但现在重新调用会命中缓存
        // 再次直接测试
        ItemImportEvent event = createEvent("上海");
        long start = System.currentTimeMillis();
        handler.onEvent(event);
        long cost = System.currentTimeMillis() - start;
        // 应该非常快
        assertTrue(cost < 50, "缓存命中应极快，实际 " + cost + "ms");
        System.out.println("缓存命中应极快，实际 " + cost + "ms");
    }

    // ===================== 辅助方法 =====================

    /**
     * 创建 ItemImportEvent，使用 EnumMap 存放键值对
     */
    private ItemImportEvent createEvent(String value) {
        ItemImportEvent event = new ItemImportEvent();
        EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
        map.put(ItemMapping.MADE_IN, value);
        event.map = map; // 注意：如果业务代码中 event.map 是 Map<String, String>，此处需调整
        return event;
    }

    // 如果您想测试网络异常（如超时），可以设置较短的超时时间（但需修改生产代码配置）
    // 此处略，因为真实网络难以控制。
}