/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.ShelfLife;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.WeightRepository;
import catalog.hoprxi.scale.domain.model.weight_price.WeightPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightRetailPrice;
import catalog.hoprxi.scale.domain.model.weight_price.WeightUnit;
import org.javamoney.moneta.Money;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/30
 */
public class ArangoDBWeightRepositoryTest {
    private static WeightRepository weightRepository = new ArangoDBWeightRepository();
    private static BrandRepository brandRepository = new ArangoDBBrandRepository();
    private static CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public static void setUpBeforeClass() {
        brandRepository.save(Brand.UNDEFINED);

        WeightRetailPrice retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(4.99, currency), WeightUnit.KILOGRAM));
        Weight apple = new Weight(new Plu(1), new Name("苹果", "apple"), new Domestic("陕西", "榆林"), new Specification("90g"), Grade.ONE_LEVEL, new ShelfLife(7),
                null, null, null, "fruits", Brand.UNDEFINED.id());
        Weight apple1 = new Weight(new Plu(2), new Name("昭通苹果", "zhaotong apple"), new Domestic("云南省", "昭通市"), Specification.UNDEFINED, Grade.SUPERFINE, new ShelfLife(7),
                null, null, null, "fruits", Brand.UNDEFINED.id());
        Weight marbled = new Weight(new Plu(3), new Name("五花肉", "marbled meat"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                null, null, null, "meat", Brand.UNDEFINED.id());
        Weight pig_feet = new Weight(new Plu(10), new Name("猪蹄", "pig\'s feet"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                null, null, null, "meat", Brand.UNDEFINED.id());
        Weight pig_intestine = new Weight(new Plu(5), new Name("猪蹄", "pig intestine"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                null, null, null, "meat", Brand.UNDEFINED.id());
        Weight tenderloin = new Weight(new Plu(4), new Name("里脊肉", " tenderloin"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                null, null, null, "meat", Brand.UNDEFINED.id());
        Weight grass_carp = new Weight(new Plu(4), new Name("草鱼", "grass carp"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                null, null, null, "aquatic", Brand.UNDEFINED.id());
        Weight crucian_carp = new Weight(new Plu(4), new Name("鲫鱼", "crucian carp"), null, Specification.UNDEFINED, Grade.QUALIFIED, ShelfLife.SAME_DAY,
                null, null, null, "aquatic", Brand.UNDEFINED.id());
    }

    @Test
    public void nextPlu() {
    }

    @Test
    public void isPluExists() {
    }

    @Test
    public void find() {
    }

    @Test
    public void belongingToBrand() {
    }

    @Test
    public void belongingToCategory() {
    }

    @Test
    public void findAll() {
    }

    @Test
    public void remove() {
    }

    @Test
    public void save() {
    }

    @Test
    public void size() {
    }

    @Test
    public void fromMnemonic() {
    }

    @Test
    public void fromName() {
    }
}