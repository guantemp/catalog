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

import catalog.hoprxi.core.application.batch.ItemExportService;
import catalog.hoprxi.core.application.query.ItemQuery2;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQuery2;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-02-28
 */
public class PsqlItemExport implements ItemExportService {

    //     列头单元格样式
    public static void setColumnTopStyle(CellStyle style) {
        //设置背景颜色;
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_ORANGE.getIndex());
        //solid 填充  foreground  前景色
        style.setFillPattern(FillPatternType.DIAMONDS);
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
        //设置自动换行;
        style.setWrapText(false);
        //设置水平对齐的样式为居中对齐;
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置垂直对齐的样式为居中对齐;
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    public static CellStyle getStyle(SXSSFWorkbook workbook) {
        // 设置字体
        Font font = workbook.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 12);
        //字体加粗
//        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        //设置字体名字
        font.setFontName("仿宋");
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

    private static CellStyle getCurrencyCell(SXSSFWorkbook workbook) {
        CellStyle cellstyle = getStyle(workbook);
        XSSFDataFormat dataFormat = workbook.getXSSFWorkbook().createDataFormat();
        cellstyle.setDataFormat(dataFormat.getFormat("¥* #,##0.00##;¥* -#,##0.00##"));//.getBuiltinFormat("0.00%"),
        return cellstyle;
    }

    @Override
    public void exportToXls(OutputStream outputStream) throws IOException {
        ItemQuery2 query = new PsqlItemQuery2();
        ItemView[] itemViews = query.queryAll(0, 2000);
        SXSSFWorkbook workbook = null;
        BufferedOutputStream bufferedOutPut = null;
        /*
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.valueOf(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤#,##0.0000")//"#,##0.00### ¤"
                .build());
         */
        try {
            workbook = new SXSSFWorkbook();
            workbook.setCompressTempFiles(true);
            // 创建sheet
            SXSSFSheet sheet = workbook.createSheet("商品信息");
            //设置列宽
            sheet.setColumnWidth(0, 19 * 256);//id
            sheet.setColumnWidth(1, 38 * 256);//品名
            sheet.setColumnWidth(2, 172 * 35);//别名
            sheet.setColumnWidth(3, 104 * 35);//条码
            sheet.setColumnWidth(4, 112 * 35);//规格
            sheet.setColumnWidth(5, 64 * 35);//等级
            sheet.setColumnWidth(6, 88 * 35);//类别
            sheet.setColumnWidth(7, 72 * 35);//保质期
            sheet.setColumnWidth(8, 128 * 35);//产地
            sheet.setColumnWidth(9, 80 * 35);//计价单位
            sheet.setColumnWidth(10, 14 * 256);//零售价
            sheet.setColumnWidth(11, 14 * 256);//会员价
            sheet.setColumnWidth(12, 14 * 256);//VIP价
            sheet.setColumnWidth(13, 112 * 35);//"品牌"
            sheet.getDataValidationHelper();
            //第一行冻结置顶,ａ表示要冻结的列数；rowSplit表示要冻结的行数；ｃ表示右边区域[可见]的首列序号；topRow表示下边区域[可见]的首行序号；
            sheet.createFreezePane(0, 2, 0, 2);
            // 创建合并标题栏
            SXSSFRow head = sheet.createRow(0);
            head.setHeight((short) (16 * 35));
            SXSSFCell cell = head.createCell(0);
            CellStyle style = workbook.createCellStyle(); // 自定义单元格内容换行规则
            style.setWrapText(true);
            cell.setCellStyle(style);
            cell.setCellValue(new XSSFRichTextString("1、不要删除1,2行\n2、"));
            //合并列
            CellRangeAddress region = new CellRangeAddress(0, 0, 0, 13);
            sheet.addMergedRegion(region);
            // 创建分列标题栏
            SXSSFRow firstrow = sheet.createRow(1);
            SXSSFDrawing p = sheet.createDrawingPatriarch();
            CellStyle topStyle = workbook.createCellStyle();
            Font topFont = workbook.createFont();
            //设置字体大小
            topFont.setFontHeightInPoints((short) 14);
            //字体加粗
            topFont.setBold(true);
            //设置字体名字
            topFont.setFontName("仿宋");
            topStyle.setFont(topFont);
            setColumnTopStyle(topStyle);
            // 创建列
            SXSSFCell cell0 = firstrow.createCell(0);
            cell0.setCellStyle(topStyle);
            cell0.setCellValue("ID");
            // 创建列
            SXSSFCell cell1 = firstrow.createCell(1);
            cell1.setCellStyle(topStyle);
            cell1.setCellValue("品名");
            // 创建列
            SXSSFCell cell2 = firstrow.createCell(2);
            cell2.setCellStyle(topStyle);
            cell2.setCellValue("别名");
            // 创建列
            SXSSFCell cell4 = firstrow.createCell(3);
            cell4.setCellStyle(topStyle);
            cell4.setCellValue("条码");
            XSSFComment comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 3, 0, 5, 15));
            // 输入批注信息
            comment.setString(new XSSFRichTextString("条码规则：\n 1、条码支持EAN8,EAN13,UPC_A,UPC_E,ITF14\n" +
                    "2、最后一位为校验码，如果你不知道如何计算该校验码，请输入条码的前7位(ean8),前12位(EAN13),前6位(UPC_A),前11位(UPC_E),导入时系统將为你自动补全!\n" +
                    "3、导入时会验证校验码，有不符合要求的条码列不会被导入\n" +
                    "4、系统中已存在的条码，在结果表会导出系统中的数据与输入源数据对比，以便你再次导入时选择使用那条数据"));
            // 添加作者,选中单元格,看状态栏
            comment.setAuthor("guan-xianghuang");
            cell4.setCellComment(comment);
            // 创建列
            SXSSFCell cell5 = firstrow.createCell(4);
            cell5.setCellStyle(topStyle);
            cell5.setCellValue("规格");
            // 创建列
            SXSSFCell cell6 = firstrow.createCell(5);
            cell6.setCellStyle(topStyle);
            cell6.setCellValue("等级");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 5, 0, 7, 7));
            comment.setString(new XSSFRichTextString("等级规则：\n\n 1、请使用字样：不合格品、合格品、一等品、优等品\n2、留空或不是以上字符的默认为合格品"));
            comment.setAuthor("guan-xianghuang");
            cell6.setCellComment(comment);
            // 创建列
            SXSSFCell cell7 = firstrow.createCell(6);
            cell7.setCellStyle(topStyle);
            cell7.setCellValue("类别");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 6, 0, 10, 10));
            comment.setString(new XSSFRichTextString("类别规则：\n\n 1、查询并填入(https://hoprxi.tooo.top/catalog/core/v1/categories)ID值（白酒id:496796322118291471）\n2、使用如下格式：日化/洗涤用品/洗衣液、洗涤用品/洗衣液，不存在的类别将会新建（类如：日化、洗涤用品如不存在将被将新建在首层）,请特别注意名字（前后空格会自动删除）必须完全一致，防止建立过多的新类\n" +
                    "3、留空或使用'undefined',‘未定义’，'" + Label.CATEGORY_UNDEFINED + "'字符的导入为缺省未分类"));
            cell7.setCellComment(comment);
            // 创建列
            SXSSFCell cell8 = firstrow.createCell(7);
            cell8.setCellStyle(topStyle);
            cell8.setCellValue("保质期");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 7, 0, 9, 7));
            comment.setString(new XSSFRichTextString("保质期：\n1、单位为天(day)\n2、允许输入2/2d代表天，m/M代表月,y/Y代表年,比如：2或2d表示2天，2m/2M表示2个月60天，1.5y/1.5Y代表一年半540天"));
            cell8.setCellComment(comment);
            // 创建列
            SXSSFCell cell9 = firstrow.createCell(8);
            cell9.setCellStyle(topStyle);
            cell9.setCellValue("产地");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 8, 0, 11, 9));
            comment.setString(new XSSFRichTextString("产地规则：\n1、进口商品标明进口国，国产商品标记到到市\n2、获取行政区划码(https://hoprxi.tooo.top/area/v1/areas)，输入编号（510500 代表四川泸州市）\n3、输入例：乐山、乐山市、重庆市、四川.泸州，四川省泸州、四川省泸州市、四川省.泸州市、广西.南宁、广西壮族自治区南宁\n5、没有找到匹配地址或留空，返回空白地址"));
            cell9.setCellComment(comment);

            SXSSFCell cell3 = firstrow.createCell(9);
            cell3.setCellStyle(topStyle);
            cell3.setCellValue("计价单位");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 9, 0, 11, 3));
            comment.setString(new XSSFRichTextString("单位规则：\n留空或不符合系统提供的单位的缺省为PCS"));
            cell3.setCellComment(comment);
            // 创建列
            SXSSFCell cell10 = firstrow.createCell(10);
            cell10.setCellStyle(topStyle);
            cell10.setCellValue("零售价");
            // 创建列
            SXSSFCell cell11 = firstrow.createCell(11);
            cell11.setCellStyle(topStyle);
            cell11.setCellValue("会员价");
            // 创建列
            SXSSFCell cell12 = firstrow.createCell(12);
            cell12.setCellStyle(topStyle);
            cell12.setCellValue("VIP价");
            // 创建列
            SXSSFCell cell14 = firstrow.createCell(13);
            cell14.setCellStyle(topStyle);
            cell14.setCellValue("品牌");
            comment = (XSSFComment) p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, 13, 0, 15, 9));
            comment.setString(new XSSFRichTextString("品牌规则：\n\n 1、留空或使用'undefined',‘未定义’，'" + Label.BRAND_UNDEFINED + "'字符的导入为未定义品牌\n2、允许2个名称，如'好来/黑人','好来'是主名称,'黑人'是别名"));
            comment.setAuthor("guan-xianghuang");
            cell14.setCellComment(comment);

            // CellStyle style = workbook.createCellStyle();
            // Font font = workbook.createFont();
            //设置字体大小
            // font.setFontHeightInPoints((short) 12);
            //设置字体名字
            //topFont.setFontName("仿宋");
            //style.setFont(topFont);
            //setColumnTopStyle(topStyle);
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
                cell03.setCellStyle(getCurrencyCell(workbook));
                cell03.setCellValue(itemView.retailPrice().price().unit().toString());

                SXSSFCell cell010 = row.createCell(10);
                cell010.setCellStyle(getCurrencyCell(workbook));
                cell010.setCellValue(itemView.retailPrice().price().amount().getNumber().doubleValueExact());

                SXSSFCell cell011 = row.createCell(11);
                cell011.setCellStyle(getCurrencyCell(workbook));
                cell011.setCellValue(itemView.memberPrice().price().amount().getNumber().doubleValue());

                SXSSFCell cell012 = row.createCell(12);
                cell012.setCellStyle(getCurrencyCell(workbook));
                cell012.setCellValue(itemView.vipPrice().price().amount().getNumber().doubleValueExact());

                SXSSFCell cell013 = row.createCell(13);
                cell013.setCellStyle(getStyle(workbook));
                cell013.setCellValue(itemView.brandView().name());
            }
            DataValidation dataValidation = dropDownList(sheet, new String[]{"不合格品", "合格品", "一等品", "优等品"}, 1, 15, 5, 6);
            sheet.addValidationData(dataValidation);
            sheet.flushRows();
            bufferedOutPut = new BufferedOutputStream(outputStream);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } finally {
            if (bufferedOutPut != null)
                bufferedOutPut.close();
            if (workbook != null)
                workbook.dispose();
            if (workbook != null)
                workbook.close();
        }
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
