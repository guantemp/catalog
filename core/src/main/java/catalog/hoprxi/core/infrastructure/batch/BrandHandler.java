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
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.2 builder 2025-10-02
 */
public class BrandHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,19}$");
    private static final String DELIMITER;
    private static final BrandRepository repository = new PsqlBrandRepository();
    private static final Logger LOGGER = LoggerFactory.getLogger(BrandHandler.class);
    // 【核心修复 1】：引入线程安全的内存缓存，彻底解放数据库
    private static final Map<String, Long> BRAND_CACHE = new ConcurrentHashMap<>(1024);

    static {
        Config config = ConfigFactory.load("import");
        DELIMITER = config.hasPath("brand_delimiter") ? config.getString("brand_delimiter") : "/";
    }

    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) throws Exception {
        event.brandId = Brand.UNBRANDED.id();
        String brand = event.map.get(ItemMapping.BRAND);
        if (brand == null || brand.isBlank()
            || brand.equalsIgnoreCase(Brand.UNBRANDED.name().name())
            || brand.equalsIgnoreCase(Brand.UNBRANDED.name().shortName())
            || brand.equalsIgnoreCase(Label.UNBRANDED))
            return;
        if (ID_PATTERN.matcher(brand).matches()) {//数字，可能是id
            long id = Long.parseLong(brand);
            if (id == Brand.UNBRANDED.id())
                return;
            if (BrandHandler.isExists(id))//没有查到该id,错误的id
                event.brandId = id;
            return;
        }

        // 3. 解析品牌名称（假设格式为 "Name/ShortName"）
        String[] ss = brand.split(DELIMITER);//name/name/name
        String name = ss[0].trim();
        String shortName = ss.length > 1 ? ss[1].trim() : null;

        // 先查数据库（带缓存）
        //System.out.println("dbid:"+dbId);
        // 未找到，创建新品牌

        event.brandId = BRAND_CACHE.computeIfAbsent(name, k -> {
            // 先查数据库（带缓存）
            long dbId = BrandHandler.findIdByName(name, shortName);
            //System.out.println("dbid:"+dbId);
            if (dbId != Long.MIN_VALUE) {
                return dbId;
            }
            // 未找到，创建新品牌
            Brand newBrand = (shortName != null)
                    ? new Brand(repository.nextIdentity(), new Name(name, shortName))
                    : new Brand(repository.nextIdentity(), name);
            //System.out.println(newBrand);
            repository.save(newBrand);
            return newBrand.id();
        });
    }

    private static long findIdByName(String name, String shortName) {
        final String query = """
                SELECT id FROM brand 
                WHERE name::jsonb->>'name' = ? OR name::jsonb->>'shortName' = ?
                ORDER BY (name::jsonb->>'name' = ?) DESC 
                LIMIT 1""";
        try (Connection connection = PsqlUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, shortName != null ? shortName : name);
            ps.setString(3, name); // 对应 ORDER BY
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        } catch (SQLException e) {
            LOGGER.error("查询品牌失败: {}", name, e);
        }
        return Long.MIN_VALUE;
    }

    private static boolean isExists(long id) {
        if (id == Brand.UNBRANDED.id()) return true;
        String query = "SELECT id FROM brand WHERE id = ?";
        try (Connection conn = PsqlUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check existence of brand id={}", id, e);
            return false;
        }
    }


    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }
}
