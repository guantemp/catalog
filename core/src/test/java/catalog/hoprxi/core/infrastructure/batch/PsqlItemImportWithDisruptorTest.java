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
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class PsqlItemImportWithDisruptorTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"120.77.47.145:6543:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg"});
    }

    @Test
    public void testImportItemXls() throws IOException {
        File file = new File("F:/developer/catalog/jc.xls");
        FileInputStream fis = new FileInputStream(file);
        ItemMapping[] itemMappings = new ItemMapping[]{ItemMapping.NAME, ItemMapping.BARCODE, ItemMapping.SPEC, ItemMapping.CATEGORY,
                ItemMapping.BRAND, ItemMapping.UNIT, ItemMapping.MADE_IN, ItemMapping.LAST_RECEIPT_PRICE, ItemMapping.RETAIL_PRICE};
        PsqlItemImportWithDisruptor itemImport = new PsqlItemImportWithDisruptor();
        itemImport.importItemFromXsl(fis, itemMappings);
    }
/*
@Test
public void testImportItemFromCsv() throws IOException {
    File file = new File("F:/developer/catalog/jc.csv");
    FileInputStream fis = new FileInputStream(file);
    ItemMapping[] itemMappings = new ItemMapping[]{ItemMapping.NAME, ItemMapping.BARCODE, ItemMapping.SPEC, ItemMapping.CATEGORY,
            ItemMapping.BRAND, ItemMapping.UNIT, ItemMapping.MADE_IN, ItemMapping.LAST_RECEIPT_PRICE, ItemMapping.RETAIL_PRICE};
    PsqlItemImportWithDisruptor itemImport = new PsqlItemImportWithDisruptor();
    itemImport.importItemFromCsv(fis, itemMappings);
}
 */
}