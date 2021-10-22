/*
 * Copyright (c) 2021. www.hoprxi.com All Rights Reserved.
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
import catalog.hoprxi.core.domain.model.price.Price;
import catalog.hoprxi.core.domain.model.price.RetailPrice;
import catalog.hoprxi.core.domain.model.price.Unit;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.format.CurrencyStyle;
import org.junit.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() throws IOException, ServletException {
        String[] test = {"69832423", "69821412", "697234", "998541", "69841", "市政府撒的", "9782"};
        long start = System.currentTimeMillis();
        String[] result = Arrays.stream(test).filter(s -> Pattern.compile("^698\\d*").matcher(s).matches()).toArray(String[]::new);
        for (String s : result)
            System.out.println(s);
        System.out.println("pattern:" + Pattern.compile(".*?.*?").matcher("45n").matches());
        System.out.println("excel： " + (System.currentTimeMillis() - start));
        CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤ #,##0.00###")//"#,##0.00### ¤"
                .build());
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(19.55419, currency), Unit.DAI));
        JsonFactory jasonFactory = new JsonFactory();
        JsonGenerator generator = jasonFactory.createGenerator(System.out, JsonEncoding.UTF8)
                .setPrettyPrinter(new DefaultPrettyPrinter());
        generator.writeStartObject();
        generator.writeNumberField("offset", 0);
        generator.writeNumberField("limit", 15);
        generator.writeNumberField("total", Unit.values().length);
        generator.writeArrayFieldStart("units");
        for (Unit unit : Unit.values()) {
            generator.writeString(unit.toString());
        }
        generator.writeEndArray();
        generator.writeObjectField("grade", Grade.QUALIFIED.toString());
        generator.writeObjectFieldStart("retailPrice");
        generator.writeStringField("value", format.format(retailPrice.price().amount()));
        generator.writeStringField("unit", retailPrice.price().unit().toString());
        generator.writeEndObject();
        generator.writeStringField("retailPrice", format.format(retailPrice.price().amount()) + "/" + retailPrice.price().unit().toString());
        generator.writeEndObject();
        generator.flush();
        generator.close();
    }
}
