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

import catalog.hoprxi.core.application.query.BrandQueryService;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.infrastructure.ElasticsearchUtil;
import com.fasterxml.jackson.core.JsonFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-02-14
 */
public class ElasticsearchBrandQueryService implements BrandQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchBrandQueryService.class);
    private static Constructor<Name> nameConstructor;
    private static final RequestOptions COMMON_OPTIONS;

    static {
        try {
            nameConstructor = Name.class.getDeclaredConstructor(String.class, String.class, String.class);
            nameConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Not query Name class has such constructor", e);
        }
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader(HttpHeaders.AUTHORIZATION, ElasticsearchUtil.encrypted())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        //builder.setHttpAsyncResponseConsumerFactory(
        //new HttpAsyncResponseConsumerFactory
        //.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }


    private final JsonFactory jasonFactory = JsonFactory.builder().build();


    /**
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public Brand[] queryAll(int offset, int limit) {
        return new Brand[0];
    }

    /**
     * @param id
     * @return
     */
    @Override
    public Brand query(String id) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(ElasticsearchUtil.host(), ElasticsearchUtil.port(), "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/brand/_doc/" + id);
        request.setOptions(COMMON_OPTIONS);
        try {
            Response response = client.performRequest(request);

            System.out.println(response.getStatusLine());
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println(responseBody);

        } catch (IOException e) {
            System.out.println("et:" + e);
            //throw new RuntimeException(e);
        }
/*
        Cancellable cancellable = client.performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                RequestLine requestLine = response.getRequestLine();
                HttpHost host = response.getHost();
                int statusCode = response.getStatusLine().getStatusCode();
                Header[] headers = response.getHeaders();
                 try {
            response = client.performRequest(request);
            response.getEntity().getContent();
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println(responseBody);

        } catch (IOException e) {
            System.out.println(e);
            //throw new RuntimeException(e);
        }
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
 */
        try {
            client.close();
        } catch (IOException e) {

        }
        return null;
    }

    private Brand rebuild(InputStream is) {
        return null;
    }

    /**
     * @param name is support regular
     * @return
     */
    @Override
    public Brand[] queryByName(String name) {
        return new Brand[0];
    }

    /**
     * @return
     */
    @Override
    public int size() {
        return 0;
    }
}
