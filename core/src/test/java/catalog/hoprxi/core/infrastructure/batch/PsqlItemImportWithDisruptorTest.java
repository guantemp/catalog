/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class PsqlItemImportWithDisruptorTest {

    @Test
    public void testImportItemXlsFrom() throws IOException, SQLException {
        File file = new File("F:/developer/catalog/jc_1.xls");
        FileInputStream fis = new FileInputStream(file);
        Corresponding[] correspondings = new Corresponding[]{Corresponding.NAME, Corresponding.BARCODE,
                Corresponding.SPEC, Corresponding.CATEGORY, Corresponding.BRAND, Corresponding.UNIT, Corresponding.MADE_IN, Corresponding.LATEST_RECEIPT_PRICE, Corresponding.RETAIL_PRICE};
        PsqlItemImportWithDisruptor itemImport = new PsqlItemImportWithDisruptor();
        itemImport.importItemXlsFrom(fis, correspondings);
    }
}