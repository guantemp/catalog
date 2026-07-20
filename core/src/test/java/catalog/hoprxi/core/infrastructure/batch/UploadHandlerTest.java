package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemMapping;
import org.testng.annotations.Test;

import java.util.EnumMap;

import static org.testng.Assert.*;

public class UploadHandlerTest {

    // 多个测试条形码（请确保对应图片文件存在于配置的 img_directory 下）
    private static final String[] TEST_BARCODES = {
            "6924743920347",
            "6924758258404",
            "9999999999",
            "6903148115855"
    };

    private final UploadHandler handler = new UploadHandler();

    @Test(dependsOnMethods = {"testEventHasWrong"})
    public void testUploadMultipleFiles() throws Exception {
        for (int i = 0; i < TEST_BARCODES.length; i++) {
            String barcode = TEST_BARCODES[i];
            ItemImportEvent event = createEvent(barcode, false);
            // 如果是最后一个，添加LAST_ROW
            if (i == TEST_BARCODES.length - 1) {
                event.map.put(ItemMapping.LAST_ROW, "true");
            }
            handler.onEvent(event, 0, false);
            String show = event.map.get(ItemMapping.SHOW);
            System.out.println("Barcode: " + barcode + ", SHOW: " + show);
        }
    }

    @Test
    public void testEventHasWrong() throws Exception {
        // 测试第一个条形码，标记为有错误
        String barcode = TEST_BARCODES[0];
        ItemImportEvent event = createEvent(barcode, true);
        handler.onEvent(event, 0, false);
        assertNull(event.map.get(ItemMapping.SHOW), "hasWrong=true 时不应上传");
    }

    // ---------- 辅助方法 ----------
    private ItemImportEvent createEvent(String barcode, boolean hasWrong) {
        ItemImportEvent event = new ItemImportEvent();
        EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
        map.put(ItemMapping.BARCODE, "\"" + barcode + "\""); // 模拟带引号
        event.map = map;
        if(hasWrong)
            event.addWrong(Verify.BARCODE_REPEAT,barcode);
        return event;
    }
}