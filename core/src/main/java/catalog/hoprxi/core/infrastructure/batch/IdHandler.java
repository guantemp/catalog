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
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import salt.hoprxi.id.LongId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.2 builder 2026-07-05
 */
public class IdHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{7,19}$");

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) {
        String id = itemImportEvent.map.get(ItemMapping.ID);
        if (id == null || id.isBlank() || !ID_PATTERN.matcher(id).matches()) {
            itemImportEvent.map.put(ItemMapping.ID, String.valueOf(LongId.generate()));
            return;
        }
        if (ID_PATTERN.matcher(id).matches() && IdHandler.isExist(id))
            itemImportEvent.addWrong(Verify.REPEAT_ID);
    }

    private static boolean isExist(String id) {
        final String query = "select id from item where id = ?";
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, id);
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

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }
}
