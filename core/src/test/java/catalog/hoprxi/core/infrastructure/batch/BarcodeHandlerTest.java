package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemMapping;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;
/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.1 builder 2026-07-12
 */
public class BarcodeHandlerTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"slave.tooo.top:6543:P$Qwe123465Pg", "slave.tooo.top:9200"});
    }

    private BarcodeHandler handler;

    // ===================== 测试数据（请根据实际数据库修改） =====================
    private static final String EXISTING_BARCODE = "6908791201436";     // 数据库中已存在
    private static final String NON_EXISTING_BARCODE = "1234567890128"; // 数据库中不存在
    private static final String INVALID_CHECKSUM = "690123456789";      // 少一位，可补全
    private static final String COMPLETELY_INVALID = "abc123";
    private static final String DUPLICATE_BARCODE = "6901028212281";    // 数据库中不存在

    @BeforeMethod
    public void setUp() {
        handler = new BarcodeHandler();
        // 注意：不清理缓存和黑名单，让其累积所有测试过程中的数据
    }

    // ---------- 辅助方法 ----------

    private ItemImportEvent createEvent(String value) {
        ItemImportEvent event = new ItemImportEvent();
        Map<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
        map.put(ItemMapping.BARCODE, value);
        event.map = map;
        return event;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getCache() throws Exception {
        Field cacheField = BarcodeHandler.class.getDeclaredField("BARCODE_CACHE");
        cacheField.setAccessible(true);
        return (Set<String>) cacheField.get(null);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getBlacklist() throws Exception {
        // 字段名为 BARCODE_BLACKLIST
        Field blacklistField = BarcodeHandler.class.getDeclaredField("BARCODE_BLACKLIST");
        blacklistField.setAccessible(true);
        return (Set<String>) blacklistField.get(null);
    }

    // ===================== 测试用例（按 priority 顺序执行） =====================

    @Test(priority = 1)
    public void testEmptyBarcode_GenerateInStore() throws Exception {
        ItemImportEvent event = createEvent(null);
        handler.onEvent(event);

        String barcode = event.barcode;
        assertNotNull(barcode);
        assertTrue(barcode.startsWith("'") && barcode.endsWith("'"));
        String raw = barcode.substring(1, barcode.length() - 1);
        assertEquals(raw.length(), 8); // 默认 EAN-8
        assertTrue(event.wrong.isEmpty());
        assertTrue(getCache().contains(raw));
        assertFalse(getBlacklist().contains(raw));
    }

    @Test(priority = 2)
    public void testBarcode_NotExistInDb() throws Exception {
        ItemImportEvent event = createEvent(NON_EXISTING_BARCODE);
        handler.onEvent(event);

        String result = event.barcode;
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"));
        String raw = result.substring(1, result.length() - 1);
        assertEquals(raw, NON_EXISTING_BARCODE);
        assertTrue(event.wrong.isEmpty());
        assertTrue(getCache().contains(NON_EXISTING_BARCODE));
        assertFalse(getBlacklist().contains(NON_EXISTING_BARCODE));
    }

    @Test(priority = 3)
    public void testBarcode_AlreadyExistInDb() throws Exception {
        ItemImportEvent event = createEvent(EXISTING_BARCODE);
        handler.onEvent(event);

        // 数据库存在时，map 中保留原值（带引号）
        String result = event.barcode;
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"));
        assertEquals(result.substring(1, result.length() - 1), EXISTING_BARCODE);
        assertTrue(event.wrong.containsKey(Verify.BARCODE_REPEAT));
        assertTrue(getBlacklist().contains(EXISTING_BARCODE));
        assertTrue(getCache().contains(EXISTING_BARCODE));
    }

    @Test(priority = 4)
    public void testInvalidChecksum_Corrected() throws Exception {
        ItemImportEvent event = createEvent(INVALID_CHECKSUM);
        handler.onEvent(event);

        String result = event.barcode;
        assertNotNull(result);
        assertTrue(result.startsWith("'") && result.endsWith("'"));
        String raw = result.substring(1, result.length() - 1);
        assertEquals(raw.length(), 13);
        assertTrue(event.wrong.isEmpty());
        assertTrue(getCache().contains(raw));
        assertFalse(getBlacklist().contains(raw));
    }

    @Test(priority = 5)
    public void testInvalidBarcode_Unrecoverable() throws Exception {
        ItemImportEvent event = createEvent(COMPLETELY_INVALID);
        handler.onEvent(event);

        assertTrue(event.wrong.containsKey(Verify.BARCODE_CHECK_SUM_ERROR));
        assertFalse(getCache().contains(COMPLETELY_INVALID));
        assertFalse(getBlacklist().contains(COMPLETELY_INVALID));
    }

    @Test(priority = 6)   // 也可以使用 dependsOnMethods = "testBarcode_NotExistInDb" 等
    public void testDuplicateBarcode_ShouldMarkRepeat() throws Exception {


        // 第一次处理（应成功）
        ItemImportEvent event1 = createEvent(DUPLICATE_BARCODE);
        handler.onEvent(event1);
        String result1 = event1.barcode;
        assertNotNull(result1);
        assertTrue(result1.startsWith("'") && result1.endsWith("'"));
        assertEquals(result1.substring(1, result1.length() - 1), DUPLICATE_BARCODE);
        assertTrue(event1.wrong.isEmpty());
        assertTrue(getCache().contains(DUPLICATE_BARCODE));
        assertFalse(getBlacklist().contains(DUPLICATE_BARCODE));

        // 第二次处理相同条码（应报重复）
        ItemImportEvent event2 = createEvent(DUPLICATE_BARCODE);
        handler.onEvent(event2);
        // map 中仍保留原值（带引号）
        String result2 = event2.barcode;
        assertTrue(event2.wrong.containsKey(Verify.BARCODE_REPEAT));
        assertTrue(getBlacklist().contains(DUPLICATE_BARCODE));
        assertTrue(getCache().contains(DUPLICATE_BARCODE));

        assertNotNull(event2.map.get(ItemMapping.BARCODE));
        assertEquals(result1.substring(1, result1.length() - 1), event2.map.get(ItemMapping.BARCODE));
    }

    // ===================== 所有测试完成后打印缓存和黑名单 =====================

    @AfterClass
    public static void printFinalState() throws Exception {
        Field cacheField = BarcodeHandler.class.getDeclaredField("BARCODE_CACHE");
        cacheField.setAccessible(true);
        Set<String> cache = (Set<String>) cacheField.get(null);

        Field blacklistField = BarcodeHandler.class.getDeclaredField("BARCODE_BLACKLIST");
        blacklistField.setAccessible(true);
        Set<String> blacklist = (Set<String>) blacklistField.get(null);

        System.out.println("===== Final Barcode State =====");
        System.out.println("BARCODE_CACHE size: " + cache.size());
        System.out.println("BARCODE_CACHE content: " + cache);
        System.out.println("BARCODE_BLACKLIST size: " + blacklist.size());
        System.out.println("BARCODE_BLACKLIST content: " + blacklist);
        System.out.println("================================");
    }
}