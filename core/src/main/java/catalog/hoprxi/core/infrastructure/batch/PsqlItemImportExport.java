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

import catalog.hoprxi.core.application.batch.ItemImportExportService;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.javamoney.moneta.format.CurrencyStyle;

import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-11-29
 */
public class PsqlItemImportExport implements ItemImportExportService {
    public static void importFromExcel(InputStream inputStream) {

    }

    public static OutputStream importFromCsv(InputStream inputStream) {
        return null;
    }

    public static void export(OutputStream outputStream) throws IOException {
        ItemQueryService query = new PsqlItemQueryService("catalog");
        ItemView[] itemViews = query.findAll(0, 1000);
        SXSSFWorkbook workbook = null;
        BufferedOutputStream bufferedOutPut = null;
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤#,##0.0000")//"#,##0.00### ¤"
                .build());
        try {
            workbook = new SXSSFWorkbook();
            // 创建页
            SXSSFSheet sheet = workbook.createSheet("商品信息");
            //设置列宽
            sheet.setColumnWidth(0, 148 * 35);//id
            sheet.setColumnWidth(1, 290 * 35);//品名
            sheet.setColumnWidth(2, 172 * 35);//别名
            sheet.setColumnWidth(3, 104 * 35);//条码
            sheet.setColumnWidth(4, 112 * 35);//规格
            sheet.setColumnWidth(5, 64 * 35);//等级
            sheet.setColumnWidth(6, 88 * 35);//类别
            sheet.setColumnWidth(7, 72 * 35);//保质期
            sheet.setColumnWidth(8, 128 * 35);//产地
            sheet.setColumnWidth(9, 80 * 35);//计价单位
            sheet.setColumnWidth(10, 112 * 35);//零售价
            sheet.setColumnWidth(11, 112 * 35);//会员价
            sheet.setColumnWidth(12, 112 * 35);//VIP价
            sheet.setColumnWidth(13, 112 * 35);//"品牌"
            sheet.getDataValidationHelper();

            // 创建行
            SXSSFRow firstrow = sheet.createRow(0);
            SXSSFDrawing p = sheet.createDrawingPatriarch();
            // 创建列
            SXSSFCell cell0 = firstrow.createCell(0);
            cell0.setCellStyle(getColumnTopStyle(workbook));
            cell0.setCellValue("ID");
            // 创建列
            SXSSFCell cell1 = firstrow.createCell(1);
            cell1.setCellStyle(getColumnTopStyle(workbook));
            cell1.setCellValue("品名");
            // 创建列
            SXSSFCell cell2 = firstrow.createCell(2);
            cell2.setCellStyle(getColumnTopStyle(workbook));
            cell2.setCellValue("别名");
            // 创建列
            SXSSFCell cell4 = firstrow.createCell(3);
            cell4.setCellStyle(getColumnTopStyle(workbook));
            cell4.setCellValue("条码");
            XSSFComment comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 3, 0, 5, 12));
            // 输入批注信息
            comment.setString(new XSSFRichTextString("条码规则：\n\n 1、支持EAN8,EAN13,UPCA,UPC条码\n2、最后一位为效验码，如果你不知道如何计算该效验码，请输入条码的前7位(ean8),前12位(EAN13),等待系统为你自动生成!"));
            // 添加作者,选中B5单元格,看状态栏
            comment.setAuthor("guan-xianghuang");
            cell4.setCellComment(comment);
            // 创建列
            SXSSFCell cell5 = firstrow.createCell(4);
            cell5.setCellStyle(getColumnTopStyle(workbook));
            cell5.setCellValue("规格");
            // 创建列
            SXSSFCell cell6 = firstrow.createCell(5);
            cell6.setCellStyle(getColumnTopStyle(workbook));
            cell6.setCellValue("等级");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 5, 0, 5, 6));
            // 输入批注信息
            comment.setString(new XSSFRichTextString("等级规则：\n\n 1、請使用：不合格品、合格品、一等品、优等品\n2、留空或不是以上字符的默認為合格品"));
            // 创建列
            SXSSFCell cell7 = firstrow.createCell(6);
            cell7.setCellStyle(getColumnTopStyle(workbook));
            cell7.setCellValue("类别");
            // 创建列
            SXSSFCell cell8 = firstrow.createCell(7);
            cell8.setCellStyle(getColumnTopStyle(workbook));
            cell8.setCellValue("保质期");
            // 创建列
            SXSSFCell cell9 = firstrow.createCell(8);
            cell9.setCellStyle(getColumnTopStyle(workbook));
            cell9.setCellValue("产地");

            SXSSFCell cell3 = firstrow.createCell(9);
            cell3.setCellStyle(getColumnTopStyle(workbook));
            cell3.setCellValue("计价单位");
            // 创建列
            SXSSFCell cell10 = firstrow.createCell(10);
            cell10.setCellStyle(getColumnTopStyle(workbook));
            cell10.setCellValue("零售价");
            // 创建列
            SXSSFCell cell11 = firstrow.createCell(11);
            cell11.setCellStyle(getColumnTopStyle(workbook));
            cell11.setCellValue("会员价");
            // 创建列
            SXSSFCell cell12 = firstrow.createCell(12);
            cell12.setCellStyle(getColumnTopStyle(workbook));
            cell12.setCellValue("VIP价");
            // 创建列
            SXSSFCell cell14 = firstrow.createCell(13);
            cell14.setCellStyle(getColumnTopStyle(workbook));
            cell14.setCellValue("品牌");

            for (ItemView itemView : itemViews) {
                // 创建行
                SXSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
                //设置单元格的值，并且设置样式
                SXSSFCell cell00 = row.createCell(0);
                cell00.setCellStyle(getStyle(workbook));
                cell00.setCellValue(itemView.id());
                //设置单元格的值，并且设置样式
                SXSSFCell cell01 = row.createCell(1);
                cell01.setCellStyle(getStyle(workbook));
                cell01.setCellValue(itemView.name().name());
                //设置单元格的值，并且设置样式
                SXSSFCell cell02 = row.createCell(2);
                cell02.setCellStyle(getStyle(workbook));
                cell02.setCellValue(itemView.name().alias());
                //设置单元格的值，并且设置样式
                SXSSFCell cell04 = row.createCell(3);
                cell04.setCellStyle(getStyle(workbook));
                cell04.setCellValue(String.valueOf(itemView.barcode().barcode()));

                SXSSFCell cell05 = row.createCell(4);
                cell05.setCellStyle(getStyle(workbook));
                cell05.setCellValue(itemView.spec().value());

                SXSSFCell cell06 = row.createCell(5);
                cell06.setCellStyle(getStyle(workbook));
                cell06.setCellValue(itemView.grade().toString());

                SXSSFCell cell07 = row.createCell(6);
                cell07.setCellStyle(getStyle(workbook));
                cell07.setCellValue(itemView.categoryView().name());

                SXSSFCell cell08 = row.createCell(7);
                cell08.setCellStyle(getStyle(workbook));
                cell08.setCellValue(itemView.shelfLife().days() + "天");

                SXSSFCell cell09 = row.createCell(8);
                cell09.setCellStyle(getStyle(workbook));
                cell09.setCellValue(itemView.madeIn().madeIn());

                SXSSFCell cell03 = row.createCell(9);
                cell03.setCellStyle(getStyle(workbook));
                cell03.setCellValue(itemView.retailPrice().price().unit().toString());

                SXSSFCell cell010 = row.createCell(10);
                cell010.setCellStyle(getStyle(workbook));
                cell010.setCellValue(format.format(itemView.retailPrice().price().amount()));

                SXSSFCell cell011 = row.createCell(11);
                cell011.setCellStyle(getStyle(workbook));
                cell011.setCellValue(format.format(itemView.memberPrice().price().amount()));

                SXSSFCell cell012 = row.createCell(12);
                cell012.setCellStyle(getStyle(workbook));
                cell012.setCellValue(format.format(itemView.vipPrice().price().amount()));

                SXSSFCell cell013 = row.createCell(13);
                cell013.setCellStyle(getStyle(workbook));
                cell013.setCellValue(itemView.brandView().name());
            }
            getColumnTopStyle(workbook);
            bufferedOutPut = new BufferedOutputStream(outputStream);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } finally {
            if (bufferedOutPut != null)
                bufferedOutPut.close();
            if (workbook != null)
                workbook.close();
        }
    }

    public static CellStyle getStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 12);
        //字体加粗
//        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
        //设置背景颜色;
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex());
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
    public static CellStyle getColumnTopStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 14);
        //字体加粗
        font.setBold(true);
        //设置字体名字
        font.setFontName("宋体");
        //设置样式;
        CellStyle style = workbook.createCellStyle();
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

    private DataValidation dropDownList(Sheet sheet, String[] textList, int firstRow, int endRow, int firstCol, int endCol) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        //加载下拉列表内容
        DataValidationConstraint constraint = helper.createExplicitListConstraint(textList);
        constraint.setExplicitListValues(textList);
        //设置数据有效性加载在哪个单元格上。四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList regions = new CellRangeAddressList((short) firstRow, (short) endRow, (short) firstCol, (short) endCol);
        //数据有效性对象
        return helper.createValidation(constraint, regions);
    }
}
