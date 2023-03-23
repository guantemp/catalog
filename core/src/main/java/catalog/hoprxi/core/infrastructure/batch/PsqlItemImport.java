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
import catalog.hoprxi.core.application.query.BrandQueryService;
import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlBrandQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlCategoryQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-29
 */
public class PsqlItemImport implements ItemImportService {
    private static final ItemQueryService ITEM_QUERY = new PsqlItemQueryService("catalog");
    private static final BrandQueryService BRAND_QUERY = new PsqlBrandQueryService("catalog");
    private static final BrandRepository BRAND_REPO = new PsqlBrandRepository("catalog");
    private static final CategoryQueryService CATEGORY_QUERY = new PsqlCategoryQueryService("catalog");
    private final CategoryRepository categoryRepository = new PsqlCategoryRepository("catalog");


    public void importItemXlsFrom(InputStream is, int[] shineUpon) throws IOException, SQLException {
        if (shineUpon == null || shineUpon.length == 0)
            shineUpon = new int[]{0, 1, 2, 3, 4, 5, -6, 7, 8, 9, 10, 11, 12, 13, 14};
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);
        try (Connection connection = PsqlUtil.getConnection()) {
            //connection.setAutoCommit(false);
            //Statement statement = connection.createStatement();
            StringJoiner sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) values", "");
            for (int i = 1, j = sheet.getLastRowNum(); i < j; i++) {
                Row row = sheet.getRow(i);
                StringJoiner values = extracted(row, shineUpon);
                sql.add(values.toString());
                if (i % 513 == 0) {
                    //statement.addBatch(sql.toString());
                    sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) values", "");
                }
                if (i == j - 1) {
                    //statement.addBatch(sql.toString());
                }
                if (i % 12289 == 0) {
                    //statement.executeBatch();
                    //connection.commit();
                    // connection.setAutoCommit(true);
                    //connection.setAutoCommit(false);
                    // statement = connection.createStatement();
                }
                if (i == j - 1) {
                    // statement.executeBatch();
                    // connection.commit();
                    // connection.setAutoCommit(true);
                }
            }
        }
        workbook.close();
    }

    private StringJoiner extracted(Row row, int[] shineUpon) {
        String[] temps = new String[shineUpon.length];
        for (int i = 0; i < shineUpon.length; i++) {
            int posi = shineUpon[i];
            if (posi < 0)
                continue;
            Cell cell = row.getCell(posi);
            temps[i] = readCellValue(cell);
        }
        for (String s : temps)
            System.out.print(s + ',');
        System.out.println();
        processBarcode(temps[4]);
        processBrand(temps[14]);
        StringJoiner cellJoiner = new StringJoiner(",", "(", ")");
        return cellJoiner;
    }

    private String processBarcode(String barcode) {
        Barcode bar = null;
        try {
            bar = BarcodeGenerateServices.createBarcode(barcode);
        } catch (Exception e) {
            System.out.println(e + ":" + barcode);
            try {
                bar = BarcodeGenerateServices.createBarcodeWithChecksum(barcode);
            } catch (Exception i) {
                System.out.println(i);
            }
        }
        if (bar == null) {
            return "";
            //publish InvalidBarcode
        }
        /*
        ItemView[] itemViews = itemQueryService.queryByBarcode(bar.toPlanString());
        if (itemViews.length != 0) {
            //publish Already exists
            return "";
        }
        */
        return bar.toPlanString();
    }

    private String processRetailPrice(String price, String unit) {
        return "";
    }

    private String processBrand(String brand) {
        if (brand == null || brand.isEmpty() || brand.equalsIgnoreCase("undefined") || brand.equalsIgnoreCase(Label.BRAND_UNDEFINED))
            return Brand.UNDEFINED.id();
        Brand[] brands = BRAND_QUERY.queryByName("^" + brand + "$");
        if (brands.length != 0)
            return brands[0].id();
        Brand temp = new Brand(BRAND_REPO.nextIdentity(), brand);
        String[] ss = brand.split("/");
        if (ss.length > 1) {
            brands = BRAND_QUERY.queryByName("^" + ss[0] + "$|^" + ss[1] + "$");
            if (brands.length != 0)
                return brands[0].id();
            temp = new Brand(BRAND_REPO.nextIdentity(), new Name(ss[0], ss[1]));
            //System.out.println(ss[1]);
        }
        BRAND_REPO.save(temp);
        return temp.id();
    }

    private String processCategory(String category) {
        if (category == null || category.isEmpty() || category.equalsIgnoreCase("undefined") || category.equalsIgnoreCase(Label.CATEGORY_UNDEFINED))
            return Category.UNDEFINED.id();
        String[] ss = category.split("/");
        return "";
    }

    private String processMadein(String madein) {
        return "";
    }

    private String readCellValue(Cell cell) {
        if (cell == null || cell.toString().trim().isEmpty()) {
            return null;
        }
        String returnValue = null;
        switch (cell.getCellType()) {
            case NUMERIC:   //数字
                if (DateUtil.isCellDateFormatted(cell)) {//注意：DateUtil.isCellDateFormatted()方法对“2019年1月18日"这种格式的日期，判断会出现问题，需要另行处理
                    DateTimeFormatter dtf;
                    SimpleDateFormat sdf;
                    short format = cell.getCellStyle().getDataFormat();
                    if (format == 20 || format == 32) {
                        sdf = new SimpleDateFormat("HH:mm");
                    } else if (format == 14 || format == 31 || format == 57 || format == 58) {
                        // 处理自定义日期格式：m月d日(通过判断单元格的格式id解决，id的值是58)
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                        double value = cell.getNumericCellValue();
                        Date date = DateUtil.getJavaDate(value);
                        returnValue = sdf.format(date);
                    } else {// 日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    }
                    try {
                        returnValue = sdf.format(cell.getDateCellValue());// 日期
                    } catch (Exception e) {
                        try {
                            throw new Exception("exception on get date data !".concat(e.toString()));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMaximumFractionDigits(3);
                    nf.setRoundingMode(RoundingMode.HALF_EVEN);
                    nf.setGroupingUsed(false);
                    returnValue = nf.format(cell.getNumericCellValue());
                    /*
                    BigDecimal bd = new BigDecimal(cell.getNumericCellValue());
                    bd.setScale(3, RoundingMode.HALF_UP);
                    returnValue = bd.toPlainString();
                     */
                }
                break;
            case STRING:    //字符串
                returnValue = cell.getStringCellValue().trim();
                break;
            case BOOLEAN:   //布尔
                Boolean booleanValue = cell.getBooleanCellValue();
                returnValue = booleanValue.toString();
                break;
            case BLANK:     // 空值
                break;
            case FORMULA:   // 公式
                returnValue = cell.getCellFormula();
                break;
            case ERROR:     // 故障
                break;
            default:
                break;
        }
        return returnValue;
    }

    @Override
    public void importItemCsvFrom(InputStream is) {

    }


}
