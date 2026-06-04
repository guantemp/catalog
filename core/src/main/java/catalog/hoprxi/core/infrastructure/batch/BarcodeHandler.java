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
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.barcode.InvalidBarcodeException;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.lmax.disruptor.EventHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class BarcodeHandler implements EventHandler<ItemImportEvent> {
    private static final Map<String, Barcode> BARCODE_CACHE = new HashMap<>(2480);
    private static final AtomicInteger START;
    private static final String PREFIX;
    private static final String BARCODE_TYPE;

    static {
        Config config = ConfigFactory.load("import");
        PREFIX = config.hasPath("barcode.prefix") ? config.getString("barcode.prefix") : "20";
        BARCODE_TYPE = config.hasPath("barcode.type") ? config.getString("barcode.type") : "ean_8";
        START = config.hasPath("barcode.start") ? new AtomicInteger(config.getInt("barcode.start")) : new AtomicInteger(1);
    }

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        String barcode = itemImportEvent.map.get(ItemMapping.BARCODE);
        itemImportEvent.verify = Verify.OK;
        if (barcode == null || barcode.isBlank()) {
            switch (BARCODE_TYPE) {    //根据设置规则生成店内码
                case "ean_8" ->
                        itemImportEvent.map.put(ItemMapping.BARCODE, BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(START.getAndIncrement(), PREFIX).toPlanString());
                case "ean_13" ->
                        itemImportEvent.map.put(ItemMapping.BARCODE, BarcodeGenerateServices.inStoreEAN_13BarcodeGenerate(START.getAndIncrement(), PREFIX).toPlanString());
            }
            return;
        }
        Barcode bar;
        try {
            bar = BarcodeGenerateServices.createBarcode(barcode);
        } catch (InvalidBarcodeException e) {
            try {//缺少效验和的计算生成
                bar = BarcodeGenerateServices.createBarcodeCompleteChecksum(barcode);
            } catch (IllegalArgumentException f) {
                itemImportEvent.verify = Verify.BARCODE_CHECK_SUM_ERROR;
                return;
            }
        }

        if (BARCODE_CACHE.containsValue(bar)) {
            itemImportEvent.verify = Verify.BARCODE_REPEAT;
            return;
        }
        BARCODE_CACHE.put(barcode, bar);

        if (BarcodeHandler.find(bar.toPlanString())) {
            itemImportEvent.verify = Verify.BARCODE_EXIST;
            return;
        }

        itemImportEvent.map.put(ItemMapping.BARCODE, "'" + bar.toPlanString() + "'");
        //System.out.println("barcode:"+itemImportEvent.map.get(Corresponding.BARCODE));
    }

    private static boolean find(String barcode) {
        final String query = "select id from item where barcode = ?";
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, barcode);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next())
                    return true;
            }
        } catch (SQLException e) {
            // log.error("e: ", e);
            //LOGGER.error("Can't rebuild brand with (name = {})", name, e);
        }
        return false;
    }
}
