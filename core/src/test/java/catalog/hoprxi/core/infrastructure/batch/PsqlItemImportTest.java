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
 * @version 0.0.1 builder 2023-02-28
 */
public class PsqlItemImportTest {
    private PsqlItemImport itemImport = new PsqlItemImport();

    @Test
    public void testImportItemXlsFrom() throws IOException, SQLException {
        //ClassLoader loader = Thread.currentThread().getContextClassLoader();
        //URL url = loader.getResource("item.xls");
        //F:/developer/catalog/jc.xls   new int[]{-1, -1, 0, -1, 1, 2, -1, 3, 6, 5, -1, 8, -1, -1, 4})
        //e:/导出数据商品库.xlsx new int[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13}
        File file = new File("F:/developer/catalog/jc.xls");
        FileInputStream fis = new FileInputStream(file);

        itemImport.importItemXlsFrom(fis, new int[]{-1, -1, 0, -1, 1, 2, -1, 3, 6, 5, -1, 8, -1, -1, 4});
    }

    @Test
    public void testImportItemCsvFrom() {
    }
}