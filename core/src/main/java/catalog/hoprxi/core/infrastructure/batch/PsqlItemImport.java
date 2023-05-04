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

package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemImportService;
import catalog.hoprxi.core.application.query.BrandQueryService;
import catalog.hoprxi.core.application.query.CategoryQueryService;
import catalog.hoprxi.core.application.query.ItemQueryService;
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.Grade;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.barcode.InvalidBarcodeException;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.Unit;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlBrandQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlCategoryQueryService;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlItemQueryService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.poi.ss.usermodel.*;
import salt.hoprxi.id.LongId;
import salt.hoprxi.to.PinYin;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2023-03-26
 */
public class PsqlItemImport implements ItemImportService {
    private static final String AREA_URL = "https://hoprxi.tooo.top/area/v1/areas";
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{12,19}$");
    private static final Map<String, Barcode> BARCODE_MAP = new HashMap<>();
    private static CloseableHttpClient httpClient;
    private static final ItemQueryService ITEM_QUERY = new PsqlItemQueryService("catalog");
    private static final BrandQueryService BRAND_QUERY = new PsqlBrandQueryService("catalog");
    private static final BrandRepository BRAND_REPO = new PsqlBrandRepository("catalog");
    private static final CategoryQueryService CATEGORY_QUERY = new PsqlCategoryQueryService("catalog");
    private static final Corresponding[] CORR = new Corresponding[]{};
    private final CategoryRepository categoryRepository = new PsqlCategoryRepository("catalog");
    private final EnumMap<Corresponding, String> map = new EnumMap<>(Corresponding.class);

    static {
        CategoryView[] root = CATEGORY_QUERY.root();
        Name rootName = new Name("商品分类", "root");
        for (CategoryView v : root) {
            if (v.getName().equals(rootName)) {
                CORE_PARENT_ID = v.getId();
                break;
            }
        }
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        // Allow TLSv1.2 protocol only
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext).setTlsVersions(TLS.V_1_2).build();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslSocketFactory)
                .build();
        //适配http以及https请求 通过new创建PoolingHttpClientConnectionManager
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        httpClientBuilder.setConnectionManager(connManager).evictExpiredConnections().evictIdleConnections(TimeValue.ofSeconds(5)).disableAutomaticRetries();
        httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .build());
        httpClient = httpClientBuilder.build();
    }

    private static String CORE_PARENT_ID;

    private final JsonFactory jasonFactory = JsonFactory.builder().build();

    public void importItemXlsFrom(InputStream is, Corresponding[] correspondings) throws IOException, SQLException {
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);
        try (Connection connection = PsqlUtil.getConnection()) {
            //connection.setAutoCommit(false);
            //Statement statement = connection.createStatement();
            StringJoiner sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) values", "");
            for (int i = 1, j = sheet.getLastRowNum(); i < j; i++) {
                Row row = sheet.getRow(i);
                StringJoiner values = extracted(row, correspondings);
                System.out.println(values);
                sql.add(values.toString());
                if (i % 513 == 0) {
                    //statement.addBatch(sql.toString());
                    sql = new StringJoiner(",", "insert into item (id,name,barcode,category_id,brand_id,grade,made_in,spec,shelf_life,retail_price,member_price,vip_price) values", "");
                }
                if (i == j - 1) {
                    //statement.addBatch(sql.toString());
                }
                if (i % 12289 == 0) {
                    //statement.executeBatch();
                    //connection.commit();
                    // connection.setAutoCommit(true);
                    //connection.setAutoCommit(false);
                    // statement = connection.createStatement();
                }
                if (i == j - 1) {
                    // statement.executeBatch();
                    // connection.commit();
                    // connection.setAutoCommit(true);
                }
            }
        }
        workbook.close();
    }

    private StringJoiner extracted(Row row, Corresponding[] correspondings) throws IOException {
        for (int i = 0, j = correspondings.length; i < j; i++) {
            if (correspondings[i] == Corresponding.IGNORE)
                continue;
            Cell cell = row.getCell(i);
            map.put(correspondings[i], readCellValue(cell));
        }
        StringJoiner cellJoiner = new StringJoiner(",", "(", ")");
        cellJoiner.add(processId(map.get(Corresponding.ID))).add(processName(map.get(Corresponding.NAME), map.get(Corresponding.ALIAS)))
                .add(processBarcode(map.get(Corresponding.BARCODE))).add(processCategory(map.get(Corresponding.CATEGORY))).add(processBrand(map.get(Corresponding.BRAND)))
                .add(processGrade(map.get(Corresponding.GRADE))).add(processMadein(map.get(Corresponding.MADE_IN))).add(map.get(Corresponding.SPEC))
                .add(map.get(Corresponding.SPEC)).add(processRetailPrice(map.get(Corresponding.RETAIL_PRICE), map.get(Corresponding.UNIT)));
        return cellJoiner;
    }


    private String processId(String id) {
        if (id != null && !id.isEmpty() && ID_PATTERN.matcher(id).matches())
            return id;
        return String.valueOf(LongId.generate());
    }

    private String processName(String name, String alias) {
        StringJoiner stringJoiner = new StringJoiner(",", "{", "}");
        stringJoiner.add("\"name\":\"" + name + "\"");
        stringJoiner.add("\"mnemonic\":\"" + PinYin.toShortPinYing(name) + "\"");
        stringJoiner.add("\"alias\":\"" + (alias == null ? name : alias) + "\"");
        return stringJoiner.toString();
    }

    private String processBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty())
            //设置规则生成店内码
            return BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(1, 1, "21")[0].toPlanString();
        Barcode bar = null;
        try {
            bar = BarcodeGenerateServices.createBarcode(barcode);
        } catch (InvalidBarcodeException e) {
            try {
                bar = BarcodeGenerateServices.createBarcodeWithChecksum(barcode);
            } catch (InvalidBarcodeException i) {
                //publish error barcode
                System.out.println(i + ":" + barcode);
            }
        }
        if (bar == null) {
            return "nothing";
            //publish InvalidBarcode
        }
/*
        ItemView[] itemViews = ITEM_QUERY.queryByBarcode("^" + bar.toPlanString() + "$");
        if (itemViews.length != 0) {
            //publish Already exists
            System.out.println("Already exists");
            return "exists";
        }
 */
        if (BARCODE_MAP.containsValue(bar)) {
            System.out.println("find repeat barcode:" + barcode);
            //publish repeat barcode
            return "repeat";
        }
        BARCODE_MAP.put(barcode, bar);
        return bar.toPlanString();
    }

    private String processCategory(String category) {
        if (category == null || category.isEmpty() || category.equalsIgnoreCase("undefined") || category.equalsIgnoreCase(Label.CATEGORY_UNDEFINED))
            return Category.UNDEFINED.id();
        if (ID_PATTERN.matcher(category).matches()) {
            return category;
        }
        String[] ss = category.split("/");
        CategoryView[] categoryViews = CATEGORY_QUERY.queryByName("^" + ss[ss.length - 1] + "$");
        if (categoryViews.length >= 1) {
            return categoryViews[0].getId();
        } else {
            String parentId = CORE_PARENT_ID;
            Category temp;
            for (String s : ss) {
                categoryViews = CATEGORY_QUERY.queryByName("^" + s + "$");
                if (categoryViews.length == 0) {
                    temp = new Category(parentId, categoryRepository.nextIdentity(), s);
                    categoryRepository.save(temp);
                    //System.out.println("新建：" + temp);
                    parentId = temp.id();
                } else {
                    parentId = categoryViews[0].getId();
                }
            }
            //System.out.println("正确的类别id：" + parentId);
            return parentId;
        }
    }

    private String processBrand(String brand) {
        if (brand == null || brand.isEmpty() || brand.equalsIgnoreCase("undefined") || brand.equalsIgnoreCase(Label.BRAND_UNDEFINED))
            return Brand.UNDEFINED.id();
        if (ID_PATTERN.matcher(brand).matches()) {
            //System.out.println("我直接用的id：" + brand);
            return brand;
        }
        String[] ss = brand.split("/");
        String query = "^" + ss[0] + "$";
        if (ss.length > 1)
            query = query + "|^" + ss[1] + "$";
        Brand[] brands = BRAND_QUERY.queryByName(query);
        if (brands.length != 0)
            return brands[0].id();
        Brand temp = ss.length > 1 ? new Brand(BRAND_REPO.nextIdentity(), new Name(ss[0], ss[1])) : new Brand(BRAND_REPO.nextIdentity(), ss[0]);
        BRAND_REPO.save(temp);
        return temp.id();
    }

    private String processGrade(String grade) {
        Grade g = Grade.of(grade);
        return g.name();
    }


    private String processMadein(String madein) throws IOException {
        ClassicHttpRequest httpGet = ClassicRequestBuilder.get(AREA_URL).build();
        // 表单参数
        List<NameValuePair> nvps = new ArrayList<>();
        // GET 请求参数
        nvps.add(new BasicNameValuePair("search", "^" + madein + "$"));
        nvps.add(new BasicNameValuePair("filters", "city,country,county"));
        // 增加到请求 URL 中
        try {
            URI uri = new URIBuilder(new URI(AREA_URL))
                    .addParameters(nvps)
                    .build();
            httpGet.setUri(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        httpClient.execute(httpGet, response -> {
            //System.out.println(response.getCode() + " " + response.getReasonPhrase() + " " + response.getVersion());
            final HttpEntity entity = response.getEntity();

            // do something useful with the response body
            // and ensure it is fully consumed
            //System.out.println(madein + ":" + EntityUtils.toString(entity));
            //EntityUtils.consume(entity);
            System.out.println(processmMadeinJson(entity.getContent()));
            return null;
        });
        return "";
    }

    public String processmMadeinJson(InputStream inputStream) throws IOException {
        String code = null, name = null, parentCode = null, parentName = null, level = null;
        boolean parent = false;
        JsonParser parser = jasonFactory.createParser(inputStream);
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "code":
                        if (parent)
                            parentCode = parser.getValueAsString();
                        else
                            code = parser.getValueAsString();
                        break;
                    case "name":
                        if (!parser.isExpectedStartObjectToken()) {
                            if (parent)
                                parentName = parser.getValueAsString();
                            else
                                name = parser.getValueAsString();
                        }
                        break;
                    case "abbreviation":
                        name = parser.getValueAsString();
                        break;
                    case "parent":
                        parent = true;
                        break;
                    case "level":
                        level = parser.getValueAsString();
                        break;
                    default:
                        break;
                }
            }
        }
        if (code == null)
            return "{\"_class\":\"catalog.hoprxi.core.domain.model.madeIn.Black\" \"code\":" + MadeIn.UNKNOWN.code() + " \"name\":\"" + MadeIn.UNKNOWN.madeIn() + "\"}";
        if (level != null && level.equals("COUNTRY"))
            return "{\"_class\":\"catalog.hoprxi.core.domain.model.madeIn.Imported\" \"code\":" + parentCode + " \"name\":\"" + parentName + "\"}";
        return "{\"_class\":\"catalog.hoprxi.core.domain.model.madeIn.Domestic\" \"code\":" + code + " \"name\":\"" + name + "\"}";
    }

    private String processRetailPrice(String price, String unit) throws IOException {
        Unit unit1 = Unit.of(unit);
        StringJoiner cellJoiner = new StringJoiner(",", "{", "}");
        cellJoiner.add("\"number\": " + price + ",\n");
        cellJoiner.add("\"currencyCode\": \"CNY\",\n");
        cellJoiner.add("\"unit\": \"" + unit1.name() + "\"\n");
        return cellJoiner.toString();
    }

    private String readCellValue(Cell cell) {
        if (cell == null || cell.toString().trim().isEmpty()) {
            return null;
        }
        String result = null;
        switch (cell.getCellType()) {
            case NUMERIC:   //数字
                if (DateUtil.isCellDateFormatted(cell)) {//注意：DateUtil.isCellDateFormatted()方法对“2019年1月18日"这种格式的日期，判断会出现问题，需要另行处理
                    //DateTimeFormatter dtf;
                    SimpleDateFormat sdf;
                    short format = cell.getCellStyle().getDataFormat();
                    if (format == 20 || format == 32) {
                        sdf = new SimpleDateFormat("HH:mm");
                    } else if (format == 14 || format == 31 || format == 57 || format == 58) {
                        // 处理自定义日期格式：m月d日(通过判断单元格的格式id解决，id的值是58)
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                        double value = cell.getNumericCellValue();
                        Date date = DateUtil.getJavaDate(value);
                        result = sdf.format(date);
                    } else {// 日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    }
                    try {
                        result = sdf.format(cell.getDateCellValue());// 日期
                    } catch (Exception e) {
                        try {
                            throw new Exception("exception on get date data !".concat(e.toString()));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    //System.out.println(cell.getCellStyle().getDataFormatString());
                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMaximumFractionDigits(4);
                    nf.setRoundingMode(RoundingMode.HALF_EVEN);
                    nf.setGroupingUsed(false);
                    result = nf.format(cell.getNumericCellValue());
                    /*
                    BigDecimal bd = new BigDecimal(cell.getNumericCellValue());
                    bd.setScale(3, RoundingMode.HALF_UP);
                    result = bd.toPlainString();
                     */
                }
                break;
            case STRING:    //字符串
                result = cell.getStringCellValue().trim();
                break;
            case BOOLEAN:   //布尔
                Boolean booleanValue = cell.getBooleanCellValue();
                result = booleanValue.toString();
                break;
            case FORMULA:   // 公式
                result = cell.getCellFormula();
                break;
            case BLANK:     // 空值
            case ERROR:     // 故障
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    public void importItemCsvFrom(InputStream is) {

    }


}
