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
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import salt.hoprxi.crypto.util.AESUtil;
import salt.hoprxi.crypto.util.StoreKeyLoad;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-07-09
 */
public class AppTest {
    private static final MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
            .set(CurrencyStyle.SYMBOL).set("pattern", "¤###0.00###")
            .build());
    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @BeforeTest
    public void init() {
        StoreKeyLoad.loadSecretKey("keystore.jks", "Qwe123465",
                new String[]{"125.68.186.195:5432:P$Qwe123465Pg", "120.77.47.145:5432:P$Qwe123465Pg", "slave.tooo.top:9200", "125.68.186.195:9200:P$Qwe123465El"});
    }

    @Test
    public void testPattern() {
        String[] test = {"69832423", "69821412", "697234", "998541", "69841", "市政府撒的", "9782"};
        String[] result = Arrays.stream(test).filter(s -> Pattern.compile("^698\\d*").matcher(s).matches()).toArray(String[]::new);
        System.out.println(result.length);
        for (String s : result)
            System.out.println(s);
        System.out.println("pattern(.*?.*?):" + Pattern.compile(".*?.*?").matcher("45n").matches());
        System.out.println("replace:" + "M&M's缤纷妙享包\\162克".replaceAll("\\\\", "\\\\\\\\"));

        for (String s : "awr//er/qw/asfd".split("[^(/)]/")) {
            System.out.println("split:" + s);
        }
        String[] ss = "https://slave.tooo.top:9200".split(":");
        for (String s : ss)
            System.out.println(s);

        Pattern pattern = Pattern.compile(":P\\$.*");
        Matcher m = pattern.matcher("125.68.186.195:5432:P$Qwe123465Pg");
        if (m.find()) {
            System.out.println("find:" + m.replaceFirst(""));
            System.out.println("find: " + "125.68.186.195:5432:P$Qwe123465Pg".replaceFirst(":P\\$.*", ""));
        }
    }

    @Test
    public void testDecrtpt() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final Pattern ENCRYPTED = Pattern.compile("^ENC:.*");
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            if ("write".equals(database.getString("type"))) {
                if (database.getString("provider").equals("postgresql") || database.getString("provider").equals("psql") || database.getString("provider").equals("mysql")) {
                    String entry = database.getString("host") + ":" + database.getString("port");
                    String securedPlainText = database.getString("user");
                    if (ENCRYPTED.matcher(securedPlainText).matches()) {
                        securedPlainText = securedPlainText.split(":")[1];
                        byte[] aesData = Base64.getDecoder().decode(securedPlainText);
                        byte[] decryptData = AESUtil.decryptSpec(aesData, StoreKeyLoad.SECRET_KEY_PARAMETER.get(entry));
                        System.out.println("user:" + new String(decryptData, StandardCharsets.UTF_8));
                    }
                    securedPlainText = database.getString("password");
                    if (ENCRYPTED.matcher(securedPlainText).matches()) {
                        securedPlainText = securedPlainText.split(":")[1];
                        byte[] aesData = Base64.getDecoder().decode(securedPlainText);
                        byte[] decryptData = AESUtil.decryptSpec(aesData, StoreKeyLoad.SECRET_KEY_PARAMETER.get(entry));
                        System.out.println("password:" + new String(decryptData, StandardCharsets.UTF_8));
                    }
                }
            }
        }
    }

    @Test
    public void testConfig() {
        String filePath = Objects.requireNonNull(UploadServlet.class.getResource("/")).toExternalForm();
        String[] sss = filePath.split("/");
        StringJoiner joiner = new StringJoiner("/", "", "/");
        for (int i = 0, j = sss.length - 1; i < j; i++) {
            joiner.add(sss[i]);
        }
        joiner.add("upload");
        System.out.println("update file path: " + joiner);

        String[] providers = new String[]{"mysql", "postgresql", "Oracle", "Microsoft SQL Server", "Db2"};
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            Properties props = new Properties();
            String provider = database.getString("provider");
            for (String p : providers) {
                if (p.equalsIgnoreCase(provider)) {
                    props.setProperty("dataSourceClassName", database.getString("hikari.dataSourceClassName"));
                    props.setProperty("dataSource.serverName", database.getString("host"));
                    props.setProperty("dataSource.portNumber", database.getString("port"));
                    String entry = database.getString("host") + ":" + database.getString("port");
                    props.setProperty("dataSource.user", StoreKeyLoad.decrypt(entry, database.getString("user")));
                    props.setProperty("dataSource.password", StoreKeyLoad.decrypt(entry, database.getString("password")));
                    props.setProperty("dataSource.databaseName", database.getString("databaseName"));
                    props.put("maximumPoolSize", database.hasPath("hikari.maximumPoolSize") ? database.getInt("hikari.maximumPoolSize") : Runtime.getRuntime().availableProcessors() * 2 + 1);
                    props.put("dataSource.logWriter", new PrintWriter(System.out));
                    break;
                }
            }
            switch (database.getString("type")) {
                case "read":
                case "R":

                    break;
                case "write":
                case "W":
                case "read/write":
                case "R/W":
                    System.out.println(props);

                    break;
                default:
                    break;
            }
        }
    }

    @Test
    void testOther() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        StringBuilder insertSql = new StringBuilder("insert into category(id,parent_id,name,description,logo_uri,root_id,\"left\",\"right\") values(")
                .append(-1).append(",").append(-1).append(",'").append("{\"name\":\"undefined\",\"mnemonic\":\"undefined\",\"alias\":\"我想改变\"}").append("','")
                .append("dxgdfger").append("','").append(URI.create("https://www.example.com:8081/?k1=1&k1=2&k2=3&%E5%90%8D%E5%AD%97=%E5%BC%A0%E4%B8%89").toASCIIString())
                .append("',").append(-1).append(",").append(2).append(",").append(2 + 1).append(")");
        System.out.println("handle category set \"left\"=0-\"left\"-" + (5 - 4 - 1) + ",\"right\"=0-\"right\"-" + (5 - 4 - 1) + " where left<0");

        Constructor<Name> nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
        nameConstructor.setAccessible(true);
        System.out.println(nameConstructor.newInstance("中文变量", "3252", "dsgfd"));
    }

    @Test
    public void testWriteJson1() {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");

            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode.raw", "773301701166");
            generator.writeEndObject();

            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println(writer);

        writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeNumberField("from", 0);
            generator.writeNumberField("size", 200);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("should");

            generator.writeStartObject();
            generator.writeObjectFieldStart("multi_match");
            generator.writeStringField("query", "name");
            generator.writeArrayFieldStart("fields");
            generator.writeString("name.name");
            generator.writeString("name.alias");
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("name.mnemonic", "name");
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "desc");
            generator.writeEndObject();
            generator.writeEndArray();

            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println(writer);

        writer = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeNumberField("size", 500);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeArrayFieldStart("sort");
            generator.writeStartObject();
            generator.writeStringField("id", "desc");
            generator.writeEndObject();
            generator.writeEndArray();


            generator.writeArrayFieldStart("search_after");
            for (String s : new String[]{"13261704575891903 ", "13261583266136008"})
                generator.writeString(s);
            generator.writeEndArray();


            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println(writer);

        writer = new StringWriter();
        try (JsonGenerator generator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter()) {
            generator.writeStartObject();
            generator.writeNumberField("size", 9999);
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("must");

            generator.writeStartObject();
            generator.writeObjectFieldStart("term");
            generator.writeStringField("root_id", "1");
            generator.writeEndObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("left");
            generator.writeNumberField("gte", 66);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeObjectFieldStart("range");
            generator.writeObjectFieldStart("right");
            generator.writeNumberField("lte", 81);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();

            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(writer);
    }

    @Test
    public void testWriteJson2() throws IOException {
        CurrencyUnit currency = Monetary.getCurrency(Locale.getDefault());
        MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(AmountFormatQueryBuilder.of(Locale.getDefault())
                .set(CurrencyStyle.SYMBOL).set("pattern", "¤#,##0.0000")//"#,##0.00### ¤"
                .build());
        RetailPrice retailPrice = new RetailPrice(new Price(Money.of(10.00, currency), Unit.DAI));

        JsonGenerator generator = jsonFactory.createGenerator(System.out, JsonEncoding.UTF8).useDefaultPrettyPrinter();
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
    }

    @Test
    void testReadJson1() throws IOException {
        System.out.println("\n读json内部对象测试:");
        JsonParser parser = jsonFactory.createParser("{}");
        parser = jsonFactory.createParser("{\n" +
                "    \"name\": \"name的子对象\",\n" +
                "    \"retailPrice\": {\n" +
                "        \"unit\": \"盒\",\n" +
                "        \"number\": 45.3,\n" +
                "        \"currency\": \"CNY\"\n" +//USD
                "    },\n" +
                "    \"alias\": \"sut个好fsd\"\n" +
                "}");
        String name;
        Price price = null;
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME == jsonToken) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "name":
                        name = parser.getValueAsString();
                        System.out.println(name);
                        break;
                    case "retailPrice":
                        price = readPrice(parser);
                        break;
                }
            }
        }
        System.out.println(MONETARY_AMOUNT_FORMAT.format(Objects.requireNonNull(price).amount()) + "/" + price.unit());
        System.out.println("\n读json数组测试:");
        parser = jsonFactory.createParser("{\"images\": [\"https://hoprxi.tooo.top/images/6948597500302.jpg\",\"https://hoprxi.tooo.top/images/6948597500302.jpg\"]}");
        readArray(parser);
    }

    private Price readPrice(JsonParser parser) throws IOException {
        String currency = null, unit = null;
        Number number = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (JsonToken.FIELD_NAME == parser.currentToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "currency":
                        currency = parser.getValueAsString();
                        break;
                    case "unit":
                        unit = parser.getValueAsString();
                        break;
                    case "number":
                        number = parser.getNumberValue();
                        break;
                }
            }
        }
        return new Price(Money.of(number, currency), Unit.of(unit));
    }

    private void readArray(JsonParser parser) throws IOException {
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (jsonToken == JsonToken.START_ARRAY) {
                while (parser.nextToken() != JsonToken.END_ARRAY)
                    System.out.println(parser.getValueAsString());
            }
        }
    }

    @Test
    void testReadJson2() throws IOException {
        String name = null, alias = null, mnemonic = null;
        JsonParser parser = jsonFactory.createParser("{\"name\":\"undefined\",\"mnemonic\":\"undefined\",\"alias\":\"我想改变\"}".getBytes(StandardCharsets.UTF_8));
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
    }
}