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

import catalog.hoprxi.core.application.batch.ItemImportService;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-29
 */
public class PsqlItemImport implements ItemImportService {


    public void importItemXlsFrom(InputStream is) throws IOException, SQLException {
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);
        try (Connection connection = PsqlUtil.getConnection()) {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            StringJoiner sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) values", "");
            for (int i = 1, j = sheet.getLastRowNum(); i < j; i++) {
                Row row = sheet.getRow(i);
                StringJoiner values = extracted(row);
                sql.add(values.toString());
                if (i % 513 == 0) {
                    statement.addBatch(sql.toString());
                    sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) values", "");
                }
                if (i == j - 1) {
                    statement.addBatch(sql.toString());
                }
                if (i % 12289 == 0) {
                    statement.executeBatch();
                    connection.commit();
                    connection.setAutoCommit(true);
                    connection.setAutoCommit(false);
                    statement = connection.createStatement();
                }
                if (i == j - 1) {
                    statement.executeBatch();
                    connection.commit();
                    connection.setAutoCommit(true);
                }
            }
        }
        workbook.close();
    }

    private StringJoiner extracted(Row row) {
        StringJoiner cellJoiner = new StringJoiner(",", "(", ")");
        int divisor = row.getPhysicalNumberOfCells();
        String name = null;
        double longitude = 0.0, latitude = 0.0;
        for (int k = row.getFirstCellNum(); k < row.getLastCellNum(); k++) {
            Cell cell = row.getCell(k);
            switch (k % divisor) {
                case 0:
                    break;
                case 1:
                    name = cell.getStringCellValue();
                    break;
                case 2:
                    int code = (int) cell.getNumericCellValue();
                    cellJoiner.add(String.valueOf(code));
                    break;

                case 3:
                    cellJoiner.add("'" + "'");
                    break;
                case 4:
                    longitude = cell.getNumericCellValue();
                    break;
                case 5:
                    latitude = cell.getNumericCellValue();
                    cellJoiner.add("'" + "'");
                    break;
                case 6:
                    int level = (int) cell.getNumericCellValue();
                    switch (level) {
                        case 0:
                            cellJoiner.add("'COUNTRY'");
                            break;
                        case 1:
                            cellJoiner.add("'PROVINCE'");
                            break;
                        case 2:
                            cellJoiner.add("'CITY'");
                            break;
                        case 3:
                            cellJoiner.add("'COUNTY'");
                            break;
                        case 4:
                            cellJoiner.add("'TOWN'");
                            break;
                    }
                    break;
            }
        }
        return cellJoiner;
    }

    @Override
    public void importItemCsvFrom(InputStream is) {

    }


}
