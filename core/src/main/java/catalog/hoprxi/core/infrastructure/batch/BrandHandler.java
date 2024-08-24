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

import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlBrandQuery;
import com.lmax.disruptor.EventHandler;

import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class BrandHandler implements EventHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{12,19}$");
    private static final BrandQuery BRAND_QUERY = new PsqlBrandQuery();
    private static final BrandRepository BRAND_REPO = new PsqlBrandRepository("catalog");

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        String brand = itemImportEvent.map.get(ItemMapping.BRAND);
        if (brand == null || brand.isEmpty() || brand.equalsIgnoreCase("undefined") || brand.equalsIgnoreCase(Label.BRAND_UNDEFINED)) {
            itemImportEvent.map.put(ItemMapping.BRAND, Brand.UNDEFINED.id());
            return;
        }
        if (ID_PATTERN.matcher(brand).matches()) {
            //System.out.println("我直接用的id：" + brand);
            return;
        }
        String[] ss = brand.split("/");
        String query = "^" + ss[0] + "$";
        if (ss.length > 1)
            query = query + "|^" + ss[1] + "$";
        Brand[] brands = BRAND_QUERY.queryByName(query);
        if (brands.length != 0) {
            itemImportEvent.map.put(ItemMapping.BRAND, brands[0].id());
        } else {
            Brand temp = ss.length > 1 ? new Brand(BRAND_REPO.nextIdentity(), new Name(ss[0], ss[1])) : new Brand(BRAND_REPO.nextIdentity(), ss[0]);
            BRAND_REPO.save(temp);
            itemImportEvent.map.put(ItemMapping.BRAND, temp.id());
        }
        //System.out.println("brand:" +itemImportEvent.map.get(Corresponding.BRAND));
    }
}
