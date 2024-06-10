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
import catalog.hoprxi.core.domain.model.brand.Brand;
import com.fasterxml.jackson.core.JsonFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-02-14
 */
public class ElasticsearchBrandQueryService implements BrandQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchBrandQueryService.class);
    private static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader(HttpHeaders.AUTHORIZATION, "Basic ZWxhc3RpYzpRd2UxMjM0NjU=")
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                .addParameter("pretty", "true");
        //builder.setHttpAsyncResponseConsumerFactory(
        //new HttpAsyncResponseConsumerFactory
        //.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    private final String index;
    private final JsonFactory jasonFactory = JsonFactory.builder().build();


    public ElasticsearchBrandQueryService(String index) {
        this.index = Objects.requireNonNull(index, "The index is required");

    }

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
        RestClientBuilder builder = RestClient.builder(new HttpHost("slave.tooo.top", 9200, "https"));
        RestClient client = builder.build();
        Request request = new Request("GET", "/" + index + "/_doc/" + id);
        request.setOptions(COMMON_OPTIONS);
        System.out.println(request);
        Response response = null;
        try {
            response = client.performRequest(request);
            response.getEntity().getContent();
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println(responseBody);

        } catch (IOException e) {
            System.out.println(e);
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

    private Brand rebuilder(InputStream is) {
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
