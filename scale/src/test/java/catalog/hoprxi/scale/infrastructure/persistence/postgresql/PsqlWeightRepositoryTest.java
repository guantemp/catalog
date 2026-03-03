/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.persistence.postgresql;

import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.domain.model.Weight;
import catalog.hoprxi.scale.domain.model.WeightRepository;
import catalog.hoprxi.scale.domain.model.price.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.javamoney.moneta.Money;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.application.DatabaseSpecDecrypt;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class PsqlWeightRepositoryTest {
    static {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465", "slave.tooo.top:6543:P$Qwe123465Pg");
    }

    private static final WeightRepository repository = new PsqlWeightRepository();
    private static final CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());

    @BeforeClass
    public void beforeClass() {
        Config config = ConfigFactory.load("scale").resolve();
        List<? extends Config> writes = config.getConfigList("datasources.write.shards");
        Config write = writes.getFirst();
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", write.getString("hikari.dataSourceClassName"));
        String host = write.getString("db.host");
        int port = write.getInt("db.port");
        props.setProperty("dataSource.serverName", host);
        props.setProperty("dataSource.portNumber", String.valueOf(port));

        String entry = host + ":" + port;
        String user = DatabaseSpecDecrypt.decrypt(entry, write.getString("db.user"));
        String password = DatabaseSpecDecrypt.decrypt(entry, write.getString("db.password"));
        props.setProperty("dataSource.user", user);
        props.setProperty("dataSource.password", password);
        props.setProperty("dataSource.databaseName", write.getString("db.databaseName"));

        props.put("maximumPoolSize", write.hasPath("hikari.maximumPoolSize") ? write.getInt("hikari.maximumPoolSize") : 5);
        props.put("dataSource.logWriter", new PrintWriter(System.out));

        System.out.println(props);

        WeightRetailPrice retailPrice = new WeightRetailPrice(new WeightPrice(Money.of(9.99, currency), WeightUnit.KILOGRAM));
        WeightVipPrice vipPrice = new WeightVipPrice(new WeightPrice(Money.of(7.99, currency), WeightUnit.KILOGRAM));
        WeightLastReceiptPrice lastReceiptPrice = new WeightLastReceiptPrice(new WeightPrice(Money.of(4.87, currency), WeightUnit.KILOGRAM));
        Weight apple = new Weight(new Plu(1), new Name("苹果", "apple"), new Domestic("610528", "富平县"), new Specification("90#"), GradeEnum.ONE_LEVEL, new ShelfLife(7),
              lastReceiptPrice, retailPrice, WeightMemberPrice.ZERO_KILOGRAM_RMB, vipPrice, 55308263825858876L, Brand.UNDEFINED.id());
        repository.save(apple);
    }

    @AfterClass
    public void afterClass() {
        //repository.delete(new Plu(1));
    }

    @Test
    public void testFind() {
        Weight appale=repository.find(1);
        System.out.println(appale);
    }

    @Test
    public void testNextPlu() {
    }

    @Test
    public void testDelete() {
    }

    @Test
    public void testSave() {
    }

    @Test
    public void testIsPluExists() {
    }
}