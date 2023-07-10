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

import catalog.hoprxi.core.application.batch.BrandImportExportService;
import catalog.hoprxi.core.domain.model.brand.Brand;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-01-07
 */
public class PsqlBrandImportExport implements BrandImportExportService {
    public void importFromExcel(InputStream is) {

    }

    public void export(Iterable<Brand> brands, OutputStream os) throws IOException {
        SXSSFWorkbook workbook = null;
        BufferedOutputStream bufferedOutPut = null;
        /*
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤#,##0.0000")//"#,##0.00### ¤"
                .build());
         */
        try {
            workbook = new SXSSFWorkbook();
            workbook.setCompressTempFiles(true);
            // 创建sheet
            SXSSFSheet sheet = workbook.createSheet("商品品牌");
        } finally {
            if (bufferedOutPut != null)
                bufferedOutPut.close();
            if (workbook != null)
                workbook.dispose();
            if (workbook != null)
                workbook.close();
        }
        for (Brand brand : brands) {

        }
    }

    private void setHeader(SXSSFWorkbook workbook, SXSSFSheet sheet) {
        sheet.setColumnWidth(0, 19 * 256);//id
        sheet.setColumnWidth(1, 38 * 256);//品名
        sheet.setColumnWidth(2, 172 * 35);//别名
        sheet.setColumnWidth(3, 104 * 35);//主页url
        sheet.setColumnWidth(4, 112 * 35);//logourl
        sheet.setColumnWidth(5, 8 * 35);//since 创始年
        sheet.setColumnWidth(6, 128 * 35);//story 品牌故事
        //第一行冻结置顶,colSplit表示要冻结的列数；rowSplit表示要冻结的行数；leftmostColumn表示右边区域[可见]的首列序号；topRow表示下边区域[可见]的首行序号；
        sheet.createFreezePane(0, 1, 0, 1);
        //第一行
        SXSSFRow row = sheet.createRow(0);
        //格式
        CellStyle topStyle = workbook.createCellStyle();
        Font topFont = workbook.createFont();
        topFont.setFontHeightInPoints((short) 14);
        topFont.setBold(true);
        topFont.setFontName("仿宋");
        topStyle.setFont(topFont);
        setTopCellStyle(topStyle);
        // 创建列
        SXSSFCell cell0 = row.createCell(0);
        cell0.setCellStyle(topStyle);
        cell0.setCellValue("ID");
        // 创建列
        SXSSFCell cell1 = row.createCell(1);
        cell1.setCellStyle(topStyle);
        cell1.setCellValue("品名");
        // 创建列
        SXSSFCell cell2 = row.createCell(2);
        cell2.setCellStyle(topStyle);
        cell2.setCellValue("别名");
    }

    private void setTopCellStyle(CellStyle style) {
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());  //设置底边框颜色;
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());//设置左边框颜色;
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());  //设置右边框颜色;
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());  //设置顶边框颜色;
        //设置背景颜色;
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);     //填充  foreground  色的方式
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    @Override
    public void importBrandFrom(InputStream is) {

    }
}
