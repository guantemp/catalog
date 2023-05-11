/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi;

import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.webapp.UploadServlet;
import com.fasterxml.jackson.core.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;
import org.testng.annotations.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-07-09
 */
public class AppTest {

    @Test
    public void testConfig() {
        Config test = ConfigFactory.load("database");
        //System.out.println(test.getString("read.hikari.maximumPoolSize"));
        List<? extends Config> reads = test.getConfigList("reads");
        System.out.println(reads.size());
        int sum = 0;
        for (Config c : reads) {
            sum = sum + (c.hasPath("weight") ? c.getInt("weight") : 1);
            System.out.println(c.getString("host"));
        }
        System.out.println("sum: " + sum);
        //System.out.println(test.getList("write").parallelStream().);
        String filePath = UploadServlet.class.getResource("/").toExternalForm();
        String[] sss = filePath.split("/");
        StringJoiner joiner = new StringJoiner("/", "", "/");
        for (int i = 0, j = sss.length - 1; i < j; i++) {
            joiner.add(sss[i]);
        }
        joiner.add("upload");
        System.out.println(joiner);
        AtomicInteger number = new AtomicInteger(0);
        for (int i = 0; i < 10; i++)
            System.out.println(number.incrementAndGet());
    }

    @Test
    void testOther() throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String[] test = {"69832423", "69821412", "697234", "998541", "69841", "市政府撒的", "9782"};
        String[] result = Arrays.stream(test).filter(s -> Pattern.compile("^698\\d*").matcher(s).matches()).toArray(String[]::new);
        //result="/".split("/");
        System.out.println(result.length);
        for (String s : result)
            System.out.println(s);
        System.out.println("pattern(.*?.*?):" + Pattern.compile(".*?.*?").matcher("45n").matches());
        System.out.println("replace:" + "M&M's缤纷妙享包\\162克".replaceAll("\\\\", "\\\\\\\\"));
        CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤#,##0.0000")//"#,##0.00### ¤"
                .build());
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(10.00, currency), Unit.DAI));

        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(System.out, JsonEncoding.UTF8).useDefaultPrettyPrinter();
        generator.writeStartObject();
        generator.writeNumberField("offset", 0);
        generator.writeNumberField("limit", 15);
        generator.writeNumberField("total", Unit.values().length);
        generator.writeArrayFieldStart("units");
        for (Unit unit : Unit.values()) {
            generator.writeString(unit.toString());
        }
        generator.writeEndArray();
        generator.writeObjectField("grade", Grade.QUALIFIED.name());
        generator.writeObjectFieldStart("retailPrice");
        generator.writeStringField("value", format.format(retailPrice.price().amount()));
        generator.writeStringField("unit", retailPrice.price().unit().toString());
        generator.writeEndObject();
        generator.writeStringField("retailPrice", format.format(retailPrice.price().amount()) + "/" + retailPrice.price().unit().toString());
        //generator.write
        generator.writeRaw("\n\"name\" : " + "\"guantemp\"," + '\n' + "\"age\" : " + 21);
        generator.flush();

        System.out.println("money:" + new BigDecimal("1E+1").toPlainString());

        String name = null, alias = null, mnemonic = null;
        JsonParser parser = jasonFactory.createParser("{\"name\":\"undefined\",\"mnemonic\":\"undefined\",\"alias\":\"我想改变\"}".getBytes(StandardCharsets.UTF_8));
        JsonToken jsonToken;
        while (!parser.isClosed()) {
            jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME == jsonToken) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name":
                        name = parser.getValueAsString();
                        break;
                    case "alias":
                        alias = parser.getValueAsString();
                        break;
                    case "mnemonic":
                        mnemonic = parser.getValueAsString();
                        break;
                }
            }
        }
        System.out.println("namtrhrtuyretuyrwte" + name);
        System.out.println(mnemonic);
        System.out.println(alias);

        StringBuilder insertSql = new StringBuilder("insert into category(id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values(")
                .append(-1).append(",").append(-1).append(",'").append("{\"name\":\"undefined\",\"mnemonic\":\"undefined\",\"alias\":\"我想改变\"}").append("','")
                .append("dxgdfger").append("','").append(URI.create("https://www.example.com:8081/?k1=1&k1=2&k2=3&%E5%90%8D%E5%AD%97=%E5%BC%A0%E4%B8%89").toASCIIString())
                .append("',").append(-1).append(",").append(2).append(",").append(2 + 1).append(")");
        System.out.println("handle category set \"left\"=0-\"left\"-" + (5 - 4 - 1) + ",\"right\"=0-\"right\"-" + (5 - 4 - 1) + " where left<0");

        Constructor<Name> nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
        nameConstructor.setAccessible(true);
        System.out.println(nameConstructor.newInstance("中文变量", "3252", "dsgfd"));

        for (String s : "awr//er/qw/asfd".split("[^(//)]/")) {
            System.out.println(s);
        }
    }

}