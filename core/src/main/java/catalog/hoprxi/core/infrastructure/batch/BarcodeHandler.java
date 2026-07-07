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
import com.lmax.disruptor.WorkHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class BarcodeHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {
    private static final Set<String> BARCODE_CACHE = ConcurrentHashMap.newKeySet(2480);
    // 2. 记录所有发生过重复的条码（黑名单）
    static final Set<String> REPEAT_BARCODE_BLACKLIST = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger START;
    private static final String PREFIX;
    private static final String BARCODE_TYPE;
    private static final Logger LOGGER = LoggerFactory.getLogger(BrandHandler.class);

    static {
        Config config = ConfigFactory.load("import");
        PREFIX = config.hasPath("barcode.prefix") ? config.getString("barcode.prefix") : "20";
        BARCODE_TYPE = config.hasPath("barcode.type") ? config.getString("barcode.type") : "ean_8";
        START = config.hasPath("barcode.start") ? new AtomicInteger(config.getInt("barcode.start")) : new AtomicInteger(1);
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }

    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) {
        String barcode = event.map.get(ItemMapping.BARCODE);
        if (barcode == null || barcode.isBlank()) {//根据设置规则生成店内码
            if (BARCODE_TYPE.equals("ean_13")) {
                barcode = BarcodeGenerateServices.inStoreEAN_13BarcodeGenerate(START.getAndIncrement(), PREFIX).toPlanString();
            } else {
                barcode = BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(START.getAndIncrement(), PREFIX).toPlanString();
            }
            // 【快速失败】先查缓存，如果命中说明本批次已存在（或已标记存在）
            if (BARCODE_CACHE.contains(barcode)) {
                event.addWrong(Verify.BARCODE_REPEAT);
                REPEAT_BARCODE_BLACKLIST.add(barcode);
                return;
            }

            // 缓存未命中，查数据库
            if (BarcodeHandler.isExist(barcode)) {
                // 数据库存在，补入缓存（便于后续快速命中）
                BARCODE_CACHE.add(barcode);
                event.addWrong(Verify.BARCODE_EXIST);
                REPEAT_BARCODE_BLACKLIST.add(barcode);
                return;
            }

            // 数据库不存在，尝试原子性放入缓存
            if (!BARCODE_CACHE.add(barcode)) {
                // 其他线程并发插入了相同条码（极少发生，因为刚查过库）
                event.addWrong(Verify.BARCODE_REPEAT);
                REPEAT_BARCODE_BLACKLIST.add(barcode);
                return;
            }
            // 生成成功且无重复，加上单引号放入 map,放入cache
            event.map.put(ItemMapping.BARCODE, "'" + barcode + "'");
            return;
        }
        //检测正确吗？没有校验码的计算校验码
        Barcode bar;
        try {
            bar = BarcodeGenerateServices.createBarcode(barcode);
        } catch (InvalidBarcodeException e) {
            try {//缺少效验和的计算生成
                bar = BarcodeGenerateServices.createBarcodeCompleteChecksum(barcode);
            } catch (IllegalArgumentException f) {
                event.addWrong(Verify.BARCODE_CHECK_SUM_ERROR);
                return;
            }
        }
        // 原始数据中是否有重复barcode,有可能已经被提交到数据库，有这能是统一批次还未提交
        barcode = bar.toPlanString();

        // 【快速失败】先查缓存，如果命中说明本批次已存在（或已标记存在）
        if (BARCODE_CACHE.contains(barcode)) {
            event.addWrong(Verify.BARCODE_REPEAT);
            REPEAT_BARCODE_BLACKLIST.add(barcode);
            return;
        }

        // 缓存未命中，查数据库
        if (BarcodeHandler.isExist(barcode)) {
            // 数据库存在，补入缓存（便于后续快速命中）
            BARCODE_CACHE.add(barcode);
            event.addWrong(Verify.BARCODE_EXIST);
            REPEAT_BARCODE_BLACKLIST.add(barcode);
            return;
        }

        // 数据库不存在，尝试原子性放入缓存
        if (!BARCODE_CACHE.add(barcode)) {
            // 其他线程并发插入了相同条码（极少发生，因为刚查过库）
            event.addWrong(Verify.BARCODE_REPEAT);
            REPEAT_BARCODE_BLACKLIST.add(barcode);
            return;
        }
//加单引号是因为barcode再psql中设置为varchar(14)
        event.map.put(ItemMapping.BARCODE, "'" + barcode + "'");
        //System.out.println("barcode:"+itemImportEvent.map.get(Corresponding.BARCODE));
    }

    private static boolean isExist(String barcode) {
        final String query = "select id from item where barcode = ?";
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, barcode);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check barcode", e);
            return true; // 保守策略，认为已存在，阻止插入
        }
    }
}
