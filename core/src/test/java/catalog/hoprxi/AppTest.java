/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.Unit;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;
import org.testng.annotations.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-07-09
 */
public class AppTest {

    @Test
    public void testMain() throws IOException {
        String[] test = {"69832423", "69821412", "697234", "998541", "69841", "市政府撒的", "9782"};
        String[] result = Arrays.stream(test).filter(s -> Pattern.compile("^698\\d*").matcher(s).matches()).toArray(String[]::new);
        //result="/".split("/");
        System.out.println(result.length);
        for (String s : result)
            System.out.println(s);
        System.out.println("pattern:" + Pattern.compile(".*?.*?").matcher("45n").matches());
        System.out.println("undefined");
        CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤#,##0.0000")//"#,##0.00### ¤"
                .build());
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(10.00, currency), Unit.DAI));
       /*
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
        StringBuilder sb = new StringBuilder("\n\"name\" : ");
        sb.append("\"guantemp\",").append('\n').append("\"age\" : ").append(21);
        generator.writeRaw(sb.toString());
        generator.flush();
        generator.close();
*/
        System.out.println("moeny:" + new BigDecimal("1E+1").toPlainString());
/*
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

 */

        StringBuilder insertSql = new StringBuilder("insert into category(id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values(")
                .append(-1).append(",").append(-1).append(",'").append("{\"name\":\"undefined\",\"mnemonic\":\"undefined\",\"alias\":\"我想改变\"}").append("','")
                .append("dxgdfger").append("','").append(URI.create("https://www.example.com:8081/?k1=1&k1=2&k2=3&%E5%90%8D%E5%AD%97=%E5%BC%A0%E4%B8%89").toASCIIString())
                .append("',").append(-1).append(",").append(2).append(",").append(2 + 1).append(")");
        System.out.println("update category set \"left\"=0-\"left\"-" + (5 - 4 - 1) + ",\"right\"=0-\"right\"-" + (5 - 4 - 1) + " where left<0");
    }
}