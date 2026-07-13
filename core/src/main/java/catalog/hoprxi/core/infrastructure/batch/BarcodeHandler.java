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
    static final Set<String> BARCODE_BLACKLIST = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger START;
    private static final String PREFIX;
    private static final String BARCODE_TYPE;
    private static final Logger LOGGER = LoggerFactory.getLogger(BarcodeHandler.class);

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
        // 1. 获取或生成标准条码（原始值，不带引号）
        String rawBarcode = getOrGenerateBarcode(event);
        if (rawBarcode == null) {
            // 如果是无效条码且无法补全，已添加错误并放入原值，直接返回
            return;
        }

        // 2. 检查重复和数据库存在性，并处理
        boolean isDuplicateOrExist = checkAndHandleDuplicateOrExist(event, rawBarcode);
        if (isDuplicateOrExist) {
            // 已放入带引号的值并添加错误，直接返回
            return;
        }

        // 3. 成功：加入缓存并放入带引号的值
        BARCODE_CACHE.add(rawBarcode);
        event.map.put(ItemMapping.BARCODE, "'" + rawBarcode + "'");
    }

    /**
     * 获取或生成标准条码（原始字符串，不带引号）。
     * 如果条码为空，则生成店内码；如果非空，则校验并补全。
     * 若无法补全，会添加错误并放入原始输入值，返回 null。
     */
    private String getOrGenerateBarcode(ItemImportEvent event) {
        String barcode = event.map.get(ItemMapping.BARCODE);
        if (barcode == null || barcode.isBlank()) {
            // 生成店内码
            return BARCODE_TYPE.equals("ean_13")
                    ? BarcodeGenerateServices.inStoreEAN_13BarcodeGenerate(START.getAndIncrement(), PREFIX).toPlanString()
                    : BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(START.getAndIncrement(), PREFIX).toPlanString();
        }

        // 非空条码：校验并补全
        try {
            Barcode bar = BarcodeGenerateServices.createBarcode(barcode);
            return bar.toPlanString();
        } catch (InvalidBarcodeException e) {
            try {
                Barcode bar = BarcodeGenerateServices.createBarcodeCompleteChecksum(barcode);
                return bar.toPlanString();
            } catch (IllegalArgumentException f) {
                // 无法补全，保留原始输入
                event.addWrong(Verify.BARCODE_CHECK_SUM_ERROR);
                event.map.put(ItemMapping.BARCODE, "'" + barcode + "'");
                return null;
            }
        }
    }

    /**
     * 检查缓存和数据库，处理重复或已存在情况。
     * 若命中，添加错误，加入黑名单，并放入带引号的值，返回 true。
     * 否则返回 false。
     */
    private boolean checkAndHandleDuplicateOrExist(ItemImportEvent event, String rawBarcode) {
        // 缓存命中 → 重复
        if (BARCODE_CACHE.contains(rawBarcode)) {
            event.addWrong(Verify.BARCODE_REPEAT);
            BARCODE_BLACKLIST.add(rawBarcode);
            event.map.put(ItemMapping.BARCODE, "'" + rawBarcode + "'");
            return true;
        }

        // 数据库存在
        if (BarcodeHandler.isExist(rawBarcode)) {
            BARCODE_CACHE.add(rawBarcode);
            event.addWrong(Verify.BARCODE_EXIST);
            BARCODE_BLACKLIST.add(rawBarcode);
            event.map.put(ItemMapping.BARCODE, "'" + rawBarcode + "'");
            return true;
        }

        // 尝试原子性放入缓存（处理并发冲突）
        if (!BARCODE_CACHE.add(rawBarcode)) {
            // 其他线程已插入，视为重复
            event.addWrong(Verify.BARCODE_REPEAT);
            BARCODE_BLACKLIST.add(rawBarcode);
            event.map.put(ItemMapping.BARCODE, "'" + rawBarcode + "'");
            return true;
        }
        // 没有命中任何错误
        return false;
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
