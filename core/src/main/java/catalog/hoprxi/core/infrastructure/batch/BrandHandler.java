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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.2 builder 2025-10-02
 */
public class BrandHandler implements EventHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{12,19}$");
    private static final String DELIMITER = "/";
    private static final BrandRepository BRAND_REPO = new PsqlBrandRepository();
    private static final Logger log = LoggerFactory.getLogger(BrandHandler.class);

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        String brand = itemImportEvent.map.get(ItemMapping.BRAND);
        if (brand == null || brand.isBlank() || brand.equalsIgnoreCase("undefined") || brand.equalsIgnoreCase(Label.BRAND_UNDEFINED)) {
            itemImportEvent.map.put(ItemMapping.BRAND, String.valueOf(Brand.UNDEFINED.id()));
            return;
        }
        if (ID_PATTERN.matcher(brand).matches()) {//数字，可能是id
            if (!this.find(Long.parseLong(brand)))//没有查到该id
                itemImportEvent.map.put(ItemMapping.BRAND, String.valueOf(Brand.UNDEFINED.id()));
            return;
        }

        String[] ss = brand.split(DELIMITER);//name/alias
        String query = "^" + ss[0] + "$";
        if (ss.length > 1)
            query = query + "|^" + ss[1] + "$";

        long id = findIDByName(query);
        //System.out.println("brand:"+id);
        if (id != Brand.UNDEFINED.id()) {//has find id
            itemImportEvent.map.put(ItemMapping.BRAND, String.valueOf(id));
        } else {//新建
            Brand temp = ss.length > 1 ? new Brand(BRAND_REPO.nextIdentity(), new Name(ss[0], ss[1])) : new Brand(BRAND_REPO.nextIdentity(), ss[0]);
            BRAND_REPO.save(temp);
            itemImportEvent.map.put(ItemMapping.BRAND, String.valueOf(temp.id()));
        }
    }

    private long findIDByName(String name) {
        final String query = "select id from brand where name::jsonb->>'name' ~ ? " +
                             "union select id from brand where name::jsonb->>'mnemonic' ~ ? " +
                             "union select id from brand where name::jsonb->>'alias' ~ ?";
        try (Connection connection = PsqlUtil.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, name);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next())
                return rs.getLong("id");
        } catch (SQLException e) {
            log.error("e: ", e);
            //LOGGER.error("Can't rebuild brand with (name = {})", name, e);
        }
        return Brand.UNDEFINED.id();
    }

    private boolean find(long id) {
        final String query = "select id from brand where id = ?";
        try (Connection connection = PsqlUtil.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setLong(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next())
                return true;
        } catch (SQLException e) {
            log.error("e: ", e);
            //LOGGER.error("Can't rebuild brand with (name = {})", name, e);
        }
        return false;
    }
}
