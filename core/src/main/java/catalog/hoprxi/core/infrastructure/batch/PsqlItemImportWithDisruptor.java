/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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
import catalog.hoprxi.core.application.batch.ItemMapping;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.poi.ss.usermodel.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class PsqlItemImportWithDisruptor implements ItemImportService {
    private static final ItemMapping[] DEFAULT_CORR = ItemMapping.values();

    @Override
    public void importItemFromXsl(InputStream is, ItemMapping[] itemMappings) throws IOException {
        if (itemMappings == null || itemMappings.length == 0)
            itemMappings = DEFAULT_CORR;
        Disruptor<ItemImportEvent> disruptor = new Disruptor<>(
                ItemImportEvent::new,
                256,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );

        disruptor.handleEventsWith(new IdHandler(), new NameHandler(), new BarcodeHandler(), new CategoryHandler(), new BrandHandler(),
                new GrandHandler(), new MadeinHandler(), new SpecHandler(), new ShelfLifeHandler(), new LastReceiptPriceHandler(), new RetailPriceHandler(),
                new MemeberPriceHandler(), new VipPriceHandler()).then(new AssembleHandler(), new FailedValidationHandler());
        disruptor.start();

        RingBuffer<ItemImportEvent> ringBuffer = disruptor.getRingBuffer();
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 1, j = sheet.getLastRowNum(); i <= j; i++) {
            Row row = sheet.getRow(i);
            EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
            for (int m = 0, n = itemMappings.length; m < n; m++) {
                if (itemMappings[m] == ItemMapping.IGNORE || itemMappings[m] == ItemMapping.LAST_ROW)
                    continue;
                Cell cell = row.getCell(m);
                map.put(itemMappings[m], readCellValue(cell));
            }
            if (i == j) {
                map.put(ItemMapping.LAST_ROW, String.valueOf(j));
                //System.out.println("读excel最后：" + map);
            }
            ringBuffer.publishEvent((event, sequence, rMap) -> event.setMap(rMap), map);
        }
        disruptor.shutdown();
    }

    private String readCellValue(Cell cell) {
        if (cell == null || cell.toString().trim().isEmpty()) {
            return null;
        }
        String result = null;
        switch (cell.getCellType()) {
            case NUMERIC:   //数字
                if (DateUtil.isCellDateFormatted(cell)) {//注意：DateUtil.isCellDateFormatted()方法对“2019年1月18日"这种格式的日期，判断会出现问题，需要另行处理
                    //DateTimeFormatter dtf;
                    SimpleDateFormat sdf;
                    short format = cell.getCellStyle().getDataFormat();
                    if (format == 20 || format == 32) {
                        sdf = new SimpleDateFormat("HH:mm");
                    } else if (format == 14 || format == 31 || format == 57 || format == 58) {
                        // 处理自定义日期格式：m月d日(通过判断单元格的格式id解决，id的值是58)
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                        double value = cell.getNumericCellValue();
                        Date date = DateUtil.getJavaDate(value);
                        result = sdf.format(date);
                    } else {// 日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    }
                    result = sdf.format(cell.getDateCellValue());// 日期
                } else {
                    //System.out.println(cell.getCellStyle().getDataFormatString());
                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMaximumFractionDigits(4);
                    nf.setRoundingMode(RoundingMode.HALF_EVEN);
                    nf.setGroupingUsed(false);
                    result = nf.format(cell.getNumericCellValue());
                    /*
                    BigDecimal bd = new BigDecimal(cell.getNumericCellValue());
                    bd.setScale(3, RoundingMode.HALF_UP);
                    result = bd.toPlainString();
                     */
                }
                break;
            case STRING:    //字符串
                result = cell.getStringCellValue().trim();
                break;
            case BOOLEAN:   //布尔
                Boolean booleanValue = cell.getBooleanCellValue();
                result = booleanValue.toString();
                break;
            case FORMULA:   // 公式
                result = cell.getCellFormula();
                break;
            case BLANK:     // 空值
            case ERROR:// 故障
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    public void importItemFromCsv(InputStream is, ItemMapping[] itemMappings) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        Stream<String> lines = reader.lines();
        lines.skip(1).map(line -> line.split(",")).forEach(columns -> {
            EnumMap<ItemMapping, String> map = new EnumMap<>(ItemMapping.class);
            for (int m = 0, n = itemMappings.length; m < n; m++) {
                if (itemMappings[m] == ItemMapping.IGNORE || itemMappings[m] == ItemMapping.LAST_ROW)
                    continue;
                map.put(itemMappings[m], columns[m]);
            }
            System.out.println(map.get(ItemMapping.NAME));
        });
    }

    @Override
    public void importItemFromTxt(InputStream is, ItemMapping[] itemMappings) throws IOException {

    }


}
