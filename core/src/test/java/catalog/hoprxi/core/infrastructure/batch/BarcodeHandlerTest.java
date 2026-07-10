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
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Set;

import static org.testng.Assert.*;

public class BarcodeHandlerTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg",  "slave.tooo.top:9200"});
    }

    private BarcodeHandler handler;

    @BeforeMethod
    public void setUp() throws Exception {
        handler = new BarcodeHandler();
        // 重置静态缓存和黑名单，避免测试间干扰
        resetStaticCache();
    }

    // ===================== 测试数据（请根据实际数据库修改） =====================
    // 假设数据库中已存在的条码（用于测试已存在场景）
    private static final String EXISTING_BARCODE = "6901234567890"; // 请替换为实际存在的条码
    // 假设数据库中不存在的条码（用于测试未存在场景）
    private static final String NON_EXISTING_BARCODE = "1234567890123"; // 请替换为实际不存在的条码
    // 一个有效的条码（含正确校验码）
    private static final String VALID_BARCODE = "6901234567890"; // 可复用已存在或不存在
    // 一个校验码错误的条码（但可补全）
    private static final String INVALID_CHECKSUM_BARCODE = "690123456789"; // 少一位
    // 完全无效的条码（无法补全）
    private static final String COMPLETELY_INVALID_BARCODE = "abc123";

    // ===================== 测试用例 =====================

    /**
     * 测试传入空条码 → 生成店内码（EAN-8），成功放入缓存
     */
    @Test
    public void testEmptyBarcode_GenerateInStore() throws Exception {
        ItemImportEvent event = createEvent(null);
        handler.onEvent(event);

        String barcode = event.map.get(ItemMapping.BARCODE);
        assertNotNull(barcode);
        assertTrue(barcode.startsWith("'") && barcode.endsWith("'"));
        String raw = barcode.substring(1, barcode.length() - 1);
        // 默认 EAN-8，长度为8
        assertEquals(raw.length(), 8);
        // 检查缓存中是否包含该条码
        assertTrue(getCache().contains(raw));
        // 不应有错误
        assertTrue(event.wrong.isEmpty());
    }

    /**
     * 测试传入数据库中不存在的条码 → 成功处理
     */
    @Test
    public void testBarcode_NotExistInDb() throws Exception {
        // 假设 NON_EXISTING_BARCODE 在数据库中不存在
        ItemImportEvent event = createEvent(NON_EXISTING_BARCODE);
        handler.onEvent(event);

        String result = event.map.get(ItemMapping.BARCODE);
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"));
        String raw = result.substring(1, result.length() - 1);
        assertEquals(raw, NON_EXISTING_BARCODE);
        assertTrue(getCache().contains(raw));
        assertTrue(event.wrong.isEmpty());
    }

    /**
     * 测试传入数据库中已存在的条码 → 标记 BARCODE_EXIST
     */
    @Test
    public void testBarcode_AlreadyExistInDb() throws Exception {
        // 假设 EXISTING_BARCODE 在数据库中存在
        ItemImportEvent event = createEvent(EXISTING_BARCODE);
        handler.onEvent(event);

        assertNull(event.map.get(ItemMapping.BARCODE));
        assertTrue(event.wrong.contains(Verify.BARCODE_EXIST));
        // 缓存应添加该条码（因为 isExist 中补入了缓存）
        assertTrue(getCache().contains(EXISTING_BARCODE));
        assertTrue(getBlacklist().contains(EXISTING_BARCODE));
    }

    /**
     * 测试传入校验码错误的条码（可补全）→ 补全后成功处理
     */
    @Test
    public void testInvalidChecksum_Corrected() throws Exception {
        // 假设 INVALID_CHECKSUM_BARCODE 可补全
        ItemImportEvent event = createEvent(INVALID_CHECKSUM_BARCODE);
        handler.onEvent(event);

        String result = event.map.get(ItemMapping.BARCODE);
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"));
        String raw = result.substring(1, result.length() - 1);
        // 补全后长度应为13（EAN-13）
        assertEquals(raw.length(), 13);
        assertTrue(getCache().contains(raw));
        assertTrue(event.wrong.isEmpty());
    }

    /**
     * 测试传入完全无效的条码 → 标记 BARCODE_CHECK_SUM_ERROR
     */
    @Test
    public void testInvalidBarcode_Unrecoverable() throws Exception {
        ItemImportEvent event = createEvent(COMPLETELY_INVALID_BARCODE);
        handler.onEvent(event);

        assertNull(event.map.get(ItemMapping.BARCODE));
        assertTrue(event.wrong.contains(Verify.BARCODE_CHECK_SUM_ERROR));
        // 缓存不应有该条码
        assertFalse(getCache().contains(COMPLETELY_INVALID_BARCODE));
    }

    /**
     * 测试缓存命中（本批次重复）→ 标记 BARCODE_REPEAT
     */
    @Test
    public void testBarcodeRepeat_CacheHit() throws Exception {
        String barcode = "9876543210123";
        // 手动放入缓存模拟之前已处理
        getCache().add(barcode);

        ItemImportEvent event = createEvent(barcode);
        handler.onEvent(event);

        assertNull(event.map.get(ItemMapping.BARCODE));
        assertTrue(event.wrong.contains(Verify.BARCODE_REPEAT));
        assertTrue(getBlacklist().contains(barcode));
    }

    /**
     * 测试空条码生成店内码时，若生成的条码在缓存中已存在（本批次重复）→ 标记 BARCODE_REPEAT
     * 此场景较难精确模拟（因为生成值动态），暂时忽略，可以通过反射修改 START 来测试。
     * 这里仅作占位，可自行实现。
     */
    @Test(enabled = false)
    public void testEmptyBarcode_GenerateInStore_Repeat() {
        // 可通过反射修改 START 的初始值，使得两次生成相同条码，然后测试
        // 此处不实现
    }

    // ===================== 辅助方法 =====================

    private ItemImportEvent createEvent(String value) {
        ItemImportEvent event = new ItemImportEvent();
        EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
        map.put(ItemMapping.BARCODE, value);
        event.map = map;
        return event;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getCache() throws Exception {
        Field field = BarcodeHandler.class.getDeclaredField("BARCODE_CACHE");
        field.setAccessible(true);
        return (Set<String>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getBlacklist() throws Exception {
        Field field = BarcodeHandler.class.getDeclaredField("REPEAT_BARCODE_BLACKLIST");
        field.setAccessible(true);
        return (Set<String>) field.get(null);
    }

    private void resetStaticCache() throws Exception {
        getCache().clear();
        getBlacklist().clear();
        // 如果需要重置 START，也可以在此通过反射设置，但一般不影响测试
    }
}