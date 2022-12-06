/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.*;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-29
 */
public class PsqlItemImportExport {

    public static void export(OutputStream outputStream) throws IOException {
        ItemQueryService query = new PsqlItemQueryService("catalog");
        ItemView[] itemViews = query.findAll(0, 1000);
        HSSFWorkbook workbook = null;
        BufferedOutputStream bufferedOutPut = null;
        try {
            workbook = new HSSFWorkbook();
            // 创建页
            HSSFSheet sheet = workbook.createSheet("商品信息");
            //设置列宽
            sheet.setColumnWidth(0, 256 * 35);
            sheet.setColumnWidth(1, 256 * 35);
            sheet.setColumnWidth(2, 256 * 35);
            sheet.setColumnWidth(3, 256 * 35);
            sheet.setColumnWidth(4, 256 * 35);
            sheet.setColumnWidth(5, 256 * 35);
            sheet.setColumnWidth(6, 256 * 35);
            sheet.setColumnWidth(7, 256 * 35);
            sheet.setColumnWidth(8, 256 * 35);
            sheet.setColumnWidth(9, 256 * 35);
            sheet.setColumnWidth(10, 256 * 35);
            sheet.setColumnWidth(11, 256 * 35);
            sheet.setColumnWidth(12, 256 * 35);
            sheet.setColumnWidth(13, 256 * 35);
            sheet.setColumnWidth(14, 256 * 35);


            // 创建行
            HSSFRow firstrow = sheet.createRow(0);
            // 创建列
            HSSFCell cell0 = firstrow.createCell(0);
            cell0.setCellStyle(getColumnTopStyle(workbook));
            cell0.setCellValue("ID");
            // 创建列
            HSSFCell cell1 = firstrow.createCell(1);
            cell1.setCellStyle(getColumnTopStyle(workbook));
            cell1.setCellValue("品名");
            // 创建列
            HSSFCell cell2 = firstrow.createCell(2);
            cell2.setCellStyle(getColumnTopStyle(workbook));
            cell2.setCellValue("别名");
            // 创建列
            HSSFCell cell3 = firstrow.createCell(3);
            cell3.setCellStyle(getColumnTopStyle(workbook));
            cell3.setCellValue("快捷拼音");
            // 创建列
            HSSFCell cell4 = firstrow.createCell(4);
            cell4.setCellStyle(getColumnTopStyle(workbook));
            cell4.setCellValue("条码");
            // 创建列
            HSSFCell cell5 = firstrow.createCell(5);
            cell5.setCellStyle(getColumnTopStyle(workbook));
            cell5.setCellValue("规格");
            // 创建列
            HSSFCell cell6 = firstrow.createCell(6);
            cell6.setCellStyle(getColumnTopStyle(workbook));
            cell6.setCellValue("等级");
            // 创建列
            HSSFCell cell7 = firstrow.createCell(7);
            cell7.setCellStyle(getColumnTopStyle(workbook));
            cell7.setCellValue("保质期");
            // 创建列
            HSSFCell cell8 = firstrow.createCell(8);
            cell8.setCellStyle(getColumnTopStyle(workbook));
            cell8.setCellValue("产地");
            // 创建列
            HSSFCell cell9 = firstrow.createCell(9);
            cell9.setCellStyle(getColumnTopStyle(workbook));
            cell9.setCellValue("产地");
            // 创建列
            HSSFCell cell10 = firstrow.createCell(10);
            cell10.setCellStyle(getColumnTopStyle(workbook));
            cell10.setCellValue("零售价");
            // 创建列
            HSSFCell cell11 = firstrow.createCell(11);
            cell11.setCellStyle(getColumnTopStyle(workbook));
            cell11.setCellValue("会员价");
            // 创建列
            HSSFCell cell12 = firstrow.createCell(12);
            cell12.setCellStyle(getColumnTopStyle(workbook));
            cell12.setCellValue("VIP价");
            // 创建列
            HSSFCell cell13 = firstrow.createCell(13);
            cell13.setCellStyle(getColumnTopStyle(workbook));
            cell13.setCellValue("类别");
            // 创建列
            HSSFCell cell14 = firstrow.createCell(14);
            cell14.setCellStyle(getColumnTopStyle(workbook));
            cell14.setCellValue("品牌");

            for (ItemView itemView : itemViews) {
                HSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
                //设置单元格的值，并且设置样式
                HSSFCell cell00 = row.createCell(0);
                cell00.setCellStyle(getStyle(workbook));
                cell00.setCellValue(itemView.id());
                //设置单元格的值，并且设置样式
                HSSFCell cell01 = row.createCell(1);
                cell01.setCellStyle(getStyle(workbook));
                cell01.setCellValue(itemView.name().name());
                //设置单元格的值，并且设置样式
                HSSFCell cell02 = row.createCell(2);
                cell02.setCellStyle(getStyle(workbook));
                cell02.setCellValue(itemView.name().name());
                //设置单元格的值，并且设置样式
                HSSFCell cell03 = row.createCell(3);
                cell03.setCellStyle(getStyle(workbook));
                cell03.setCellValue(itemView.name().name());
                //设置单元格的值，并且设置样式
                HSSFCell cell04 = row.createCell(4);
                cell04.setCellStyle(getStyle(workbook));
                cell04.setCellValue(itemView.name().name());
            }
            File file = new File("E:/导出数据商品库.xls");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            getColumnTopStyle(workbook);
            bufferedOutPut = new BufferedOutputStream(fileOutputStream);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } finally {
            if (bufferedOutPut != null)
                bufferedOutPut.close();
            if (workbook != null)
                workbook.close();
        }
    }

    public static HSSFCellStyle getStyle(HSSFWorkbook workbook) {
        // 设置字体
        HSSFFont font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 12);
        //字体加粗
//        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        HSSFCellStyle style = workbook.createCellStyle();
        //设置背景颜色;
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LEMON_CHIFFON.getIndex());
        //solid 填充  foreground  前景色
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;

    }

    /*
     * 列头单元格样式
     */
    public static HSSFCellStyle getColumnTopStyle(HSSFWorkbook workbook) {
        // 设置字体
        HSSFFont font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 14);
        //字体加粗
        font.setBold(true);
        //设置字体名字
        font.setFontName("黑体");
        //设置样式;
        HSSFCellStyle style = workbook.createCellStyle();
        //设置背景颜色;
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_ORANGE.getIndex());
        //solid 填充  foreground  前景色
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        //设置底边框;
        style.setBorderBottom(BorderStyle.THIN);
        //设置底边框颜色;
        style.setBottomBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //设置左边框;
        style.setBorderLeft(BorderStyle.THIN);
        //设置左边框颜色;
        style.setLeftBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //设置右边框;
        style.setBorderRight(BorderStyle.THIN);
        //设置右边框颜色;
        style.setRightBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //设置顶边框;
        style.setBorderTop(BorderStyle.THIN);
        //设置顶边框颜色;
        style.setTopBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        //在样式用应用设置的字体;
        style.setFont(font);
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public void batchImport(InputStream inputStream) {

    }
}
