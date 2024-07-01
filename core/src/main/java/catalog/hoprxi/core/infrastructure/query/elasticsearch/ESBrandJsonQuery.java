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

package catalog.hoprxi.core.infrastructure.query.elasticsearch;

import catalog.hoprxi.core.application.query.BrandJsonQuery;
import catalog.hoprxi.core.infrastructure.ElasticsearchUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-06-15
 */
public class ESBrandJsonQuery implements BrandJsonQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(ESBrandJsonQuery.class);
    private static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader(HttpHeaders.AUTHORIZATION, ElasticsearchUtil.encrypted())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        //builder.setHttpAsyncResponseConsumerFactory(
        //new HttpAsyncResponseConsumerFactory
        //.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    private final JsonFactory jsonFactory = JsonFactory.builder().build();

    @Override
    public String query(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ElasticsearchUtil.host(), ElasticsearchUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_doc/" + id);
        request.setOptions(COMMON_OPTIONS);
        String result = "";
        try {
            Response response = client.performRequest(request);
            //String responseBody = EntityUtils.toString(response.getEntity());
            //System.out.println(responseBody);
            JsonParser parser = jsonFactory.createParser(response.getEntity().getContent());
            while (!parser.isClosed()) {
                if (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "_source":
                            result = parserSingle(parser);
                            break;
                    }
                }
            }
            client.close();
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("e");
        }
        return result;
    }

    private String parserSingle(JsonParser parser) throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(writer);
        generator.writeStartObject();
        while (parser.nextToken() != null) {
            if ("_meta".equals(parser.getCurrentName()))
                break;
            generator.copyCurrentEvent(parser);
        }
        generator.writeEndObject();
        generator.close();
        parser.close();
        return writer.toString();
    }

    @Override
    public String queryAll(int offset, int limit) {
        return null;
    }

    @Override
    public String queryByName(String name) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ElasticsearchUtil.host(), ElasticsearchUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_search");
        request.setOptions(COMMON_OPTIONS);
        String sql = "{\n" +
                "    \"from\": 0,\n" +
                "    \"size\": 400,\n" +
                "    //\"profile\": true,\n" +
                "    \"query\": {\n" +
                "        \"bool\": {\n" +
                "            \"filter\": [\n" +
                "                {\n" +
                "                    \"bool\": {\n" +
                "                        \"should\": [\n" +
                "                            {\n" +
                "                                \"multi_match\": {\n" +
                "                                    \"query\": \"" + name + "\",\n" +
                "                                    \"fields\": [\n" +
                "                                        \"name.name\",\n" +
                "                                        \"name.alias\"\n" +
                "                                    ]\n" +
                "                                }\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"term\": {\n" +
                "                                    \"name.mnemonic\": \"" + name + "\"\n" +
                "                                }\n" +
                "                            }\n" +
                "                        ]\n" +
                "                    }\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    }\n" +
                "}";

        System.out.println(sql);
        request.setJsonEntity(sql);
        /*
        request.setEntity(new NStringEntity(
                "{\"json\":\"text\"}",
                ContentType.APPLICATION_JSON));
         */
        try {
            Response response = client.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println(responseBody);
        } catch (IOException e) {
            //System.out.println(e);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("e");
        }
        return null;
    }
}
