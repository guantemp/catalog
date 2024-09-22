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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.KeyStoreLoad;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Year;

import static org.testng.Assert.assertEquals;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-10-18
 */
public class PsqlBrandQueryTest {
    private static BrandQuery query = new PsqlBrandQuery();
    private static BrandRepository repository = new PsqlBrandRepository();

    @BeforeClass
    public void beforeClass() throws MalformedURLException {
        KeyStoreLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "slave.tooo.top:9200"});
        URL logo = new URL("https://www.hikvision.com/cn/images/logo.png");
        AboutBrand ab = new AboutBrand(new URL("https://www.hikvision.com/cn/"), logo, Year.of(2001), "海康威视是以视频为核心的物联网解决方案提供商，面向全球提供综合安防、智慧业务与大数据服务。" +
                "海康威视秉承“专业、厚实、诚信”的经营理念，坚持将“成就客户、价值为本、诚信务实、追求卓越”核心价值观内化为行动准则，不断发展视频技术，服务人类。");
        Brand hikvision = new Brand("495651176959596552", new Name("海康威视", "HIKVISION"), ab);
        repository.save(hikvision);

        logo = new URL("https://www.dahuatech.com/bocweb/web/img/logo.png");
        ab = new AboutBrand(new URL("https://www.dahuatech.com/"), logo, Year.of(2005), "浙江大华技术股份有限公司是全球领先的以视频为核心的智慧物联解决方案提供商和运营服务商，" +
                "以技术创新为基础，提供端到端的视频监控解决方案、系统及服务，为城市运营、企业管理、个人消费者生活创造价值。");
        Brand hua = new Brand("495651176959596578", new Name("大华", "@hua"), ab);
        repository.save(hua);

        logo = new URL("http://20702897.s21i.faiusr.com/4/ABUIABAEGAAgm4Xc_gUojMuX9AEw4gE4VQ.png");
        ab = new AboutBrand(new URL(" http://www.dsppa.com/"), logo, Year.of(1988), "广州市迪士普音响科技有限公司始创于1988年，是一家集公共广播系统、智能无纸化会议系统、电子教学系统和政务大厅协同办公系统的研发、生产和销售为一体的大型国家级高新技术企业");
        Brand dsppa = new Brand("495651176959596546", new Name("迪士普", "dsppa"), ab);
        repository.save(dsppa);

        Brand my = new Brand("495651176959596602", new Name("官的"));
        repository.save(my);

        Brand chenguang = new Brand("495651176959596634", new Name("晨光", "M&G"));
        repository.save(chenguang);

        repository.save(Brand.UNDEFINED);
    }
/*
    @AfterClass
    public void afterClass() {
        repository.delete("495651176959596552");
        repository.delete("495651176959596578");
        repository.delete("495651176959596546");
        repository.delete("495651176959596602");
        repository.delete("495651176959596634");
        repository.delete(Brand.UNDEFINED.id());
    }
 */

    @Test
    public void testQueryAll() {
        Brand[] brands = query.queryAll(0, 5);
        assertEquals(brands.length, 5);
        brands = query.queryAll(1, 5);
        assertEquals(brands.length, 5);
        brands = query.queryAll(4, 5);
        assertEquals(brands.length, 2);
        brands = query.queryAll(5, 5);
        assertEquals(brands.length, 1);
        for (Brand brand : brands)
            System.out.println(brand);
    }

    @Test
    public void testQueryByName() {
        Brand[] brands = query.queryByName("康威");
        assertEquals(brands.length, 1);
        brands = query.queryByName("^康威");
        assertEquals(brands.length, 0);
        brands = query.queryByName("^迪士普$");
        assertEquals(brands.length, 1);
        brands = query.queryByName("康威|@hua");
        assertEquals(brands.length, 2);
        brands = query.queryByName("康威|dh|dsp|kw");
        assertEquals(brands.length, 3);

    }

    @Test
    public void testSize() {
        assertEquals(query.size(), 6);
    }
}