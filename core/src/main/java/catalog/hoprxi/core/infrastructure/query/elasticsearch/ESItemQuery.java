/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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


import catalog.hoprxi.core.application.query.ItemQuery;
import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.domain.model.barcode.BarcodeValidServices;
import catalog.hoprxi.core.infrastructure.ESUtil;
import com.fasterxml.jackson.core.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2025/10/24
 */

public class ESItemQuery implements ItemQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger("catalog.hoprxi.core.Item");
    private static final String SINGLE_PREFIX = "/" + ESUtil.customized() + "_item";
    private static final String SEARCH_ENDPOINT = SINGLE_PREFIX + "/_search";
    private static final int AGGS_SIZE = 15;
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    //private static final int MAX_SIZE = 9999;
    private static final int BUFFER_SIZE = 4096; //4KB缓冲区
    private static final int BATCH_BUFFER_SIZE = 16 * 1024;// 16KB缓冲区
    private static final int SINGLE_BUFFER_SIZE = 4096;

    @Override
    public InputStream find(long id) {
        Request request = new Request("GET", "/item/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            Response response = ESUtil.restClient().performRequest(request);
            JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {
                    generator.writeStartObject();
                    while (parser.nextToken() != null) {
                        if ("_meta".equals(parser.currentName())) break;
                        generator.copyCurrentEvent(parser);
                    }
                    generator.writeEndObject();
                }
            }
            generator.flush();
            return new ByteBufInputStream(buffer, true);
        } catch (ResponseException e) {
            if (buffer.refCnt() > 0) buffer.release();
            LOGGER.error("The item(id={}) not found", id, e);
            throw new SearchException(String.format("The item(id=%s) not found", id), e);
        } catch (IOException e) {
            if (buffer.refCnt() > 0) buffer.release();
            LOGGER.error("I/O failed", e);
            throw new SearchException("Error: Elasticsearch timeout or no connection", e);
        }
        /*
        PipedInputStream pipedInput = new PipedInputStream(BUFFER_SIZE);
        PipedOutputStream pipedOutput = new PipedOutputStream(pipedInput);
        Thread parseThread = new Thread(() -> {
            try (OutputStream out = pipedOutput; // 保持 PipedOutputStream 打开直到写完
                 JsonGenerator generator = JSON_FACTORY.createGenerator(out)) {
                Response response = ESUtil.restClient().performRequest(request);
                try (JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent())) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {
                            generator.writeStartObject();
                            while (parser.nextToken() != null) {
                                if ("_meta".equals(parser.currentName())) {
                                    break;
                                }
                                generator.copyCurrentEvent(parser);
                            }
                            generator.writeEndObject();
                            break; // 只处理第一个 _source
                        }
                    }
                    generator.flush();
                }
            } catch (Exception e) {
                try {
                    pipedOutput.close(); // 异常时主动关闭管道
                } catch (IOException ignored) {
                    LOGGER.error("PipedOutputStream not closed", ignored);
                    throw new RuntimeException(ignored);
                }
                LOGGER.error("The item(id={}) not found", id, e);
                throw new SearchException(String.format("The item(id=%s) not found", id), e);
            }
        });
        parseThread.setDaemon(true);
        parseThread.start();
        return pipedInput; // 返回后由调用方负责关闭
     */
    }

    public CompletableFuture<InputStream> findAsync(long id) {
        Request request = new Request("GET", "/item/_doc/" + id);//PREFIX+"/_doc/"
        request.setOptions(ESUtil.requestOptions());
        CompletableFuture<InputStream> future = new CompletableFuture<>();
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        try {
            ESUtil.restClient().performRequestAsync(request, new ResponseListener() {

                @Override
                public void onSuccess(Response response) {
                    try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os); JsonParser parser = JSON_FACTORY.createParser(response.getEntity().getContent());) {
                        while (parser.nextToken() != null) {
                            if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {
                                generator.writeStartObject();
                                while (parser.nextToken() != null) {
                                    if ("_meta".equals(parser.currentName())) break;
                                    generator.copyCurrentEvent(parser);
                                }
                                generator.writeEndObject();
                            }
                        }
                        generator.flush();
                        future.complete(new ByteBufInputStream(buffer, true));
                    } catch (JsonParseException e) {
                        if (buffer.refCnt() > 0) buffer.release();
                        LOGGER.error("The item(id={}) not found", id, e);
                        future.completeExceptionally(new SearchException(String.format("The item(id=%s) not found", id), e));
                    } catch (IOException e) {
                        if (buffer.refCnt() > 0) buffer.release();
                        LOGGER.error("I/O failed", e);
                        future.completeExceptionally(new SearchException("Error: Elasticsearch timeout or no connection", e));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // 可选：将异常信息转为 InputStream 返回，而不是抛异常
                    String errorMsg = "Elasticsearch request failed: " + e.getMessage();
                    byte[] errorBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
                    InputStream errorStream = new ByteArrayInputStream(errorBytes);
                    future.complete(errorStream); // 或 future.completeExceptionally(exception);
                    // future.completeExceptionally(new SearchException(String.format("The item(id=%s) not found", id), e));
                }
            });
        } catch (Exception e) {
            // 同步阶段异常（如参数错误）
            future.completeExceptionally(e);
        }

        future.whenComplete((is, throwable) -> {
            if (throwable != null) {
                if (buffer != null && buffer.refCnt() > 0) {
                    buffer.release();
                }
            }
            // 注意：如果成功，buffer 已交给 ByteBufInputStream(autoRelease=true)，不应再 release
        });
        return future;
    }

    @Override
    public InputStream findByBarcode(String barcode) {
        if (!BarcodeValidServices.valid(barcode)) throw new IllegalArgumentException("Not valid barcode ctr");
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(this.writeFindByBarcodeJson(barcode));
        try {
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.warn("No search was found for anything resembling barcode {} item ", barcode, e);
            throw new SearchException(String.format("No search was found for anything resembling barcode %s", barcode), e);
        }
    }

    private String writeFindByBarcodeJson(String barcode) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeObjectFieldStart("query");
            generator.writeObjectFieldStart("bool");
            generator.writeObjectFieldStart("filter");

            generator.writeObjectFieldStart("term");
            generator.writeStringField("barcode.raw", barcode);
            generator.writeEndObject();

            generator.writeEndObject();
            generator.writeEndObject();//end bool
            generator.writeEndObject();//end query
            //generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    @Override
    public InputStream search(ItemQueryFilter[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(this.writeSearchJson(filters, size, searchAfter, sortField));
        try {
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("No search was found for anything resembling item ", e);
            throw new SearchException(String.format("No search was found for anything items from %s", 2), e);
        }
    }

    private String writeSearchJson(ItemQueryFilter[] filters, int size, String searchAfter, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("size", size);
            this.writeMain(generator, filters);
            this.writeSortField(generator, sortField);
            this.writeSearchAfter(generator, searchAfter);
            this.writeAggs(generator, AGGS_SIZE);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
        }
        return writer.toString();
    }

    private void writeSearchAfter(JsonGenerator generator, String searchAfter) throws IOException {
        if (searchAfter == null || searchAfter.isBlank()) return;
        generator.writeArrayFieldStart("search_after");
        generator.writeString(searchAfter);
        generator.writeEndArray();
    }

    @Override
    public InputStream search(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        Request request = new Request("GET", "/item/_search");
        request.setOptions(ESUtil.requestOptions());
        request.setJsonEntity(this.writeSearchJson(filters, offset, size, sortField));
        try {
            Response response = ESUtil.restClient().performRequest(request);
            return this.reorganization(response.getEntity().getContent());
        } catch (IOException e) {
            LOGGER.error("No search was found for anything items ", e);
            throw new SearchException(String.format("No search was found for anything items from %s", 2), e);
        }
    }

    private String writeSearchJson(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
            generator.writeStartObject();
            generator.writeNumberField("from", offset);
            generator.writeNumberField("size", size);
            this.writeMain(generator, filters);
            this.writeSortField(generator, sortField);
            this.writeAggs(generator, AGGS_SIZE);
            generator.writeBooleanField("track_scores", false);
            generator.writeEndObject();//root
        } catch (IOException e) {
            LOGGER.error("Cannot assemble request JSON", e);
            throw new IllegalStateException("Cannot assemble request JSON");
        }
        //System.out.println(writer);
        return writer.toString();
    }


    private void writeMain(JsonGenerator generator, ItemQueryFilter[] filters) throws IOException {
        generator.writeObjectFieldStart("query");
        if (filters == null || filters.length == 0) {
            generator.writeObjectFieldStart("match_all");
            generator.writeEndObject();//match_all
        } else {
            generator.writeObjectFieldStart("bool");
            generator.writeArrayFieldStart("filter");
            for (ItemQueryFilter filter : filters) {
                filter.filter(generator);
            }
            generator.writeEndArray();//end must
            generator.writeEndObject();//end bool
        }
        generator.writeEndObject();//end query
    }

    private void writeSortField(JsonGenerator generator, SortFieldEnum sortField) throws IOException {
        generator.writeArrayFieldStart("sort");
        generator.writeStartObject();
        generator.writeStringField(sortField.field(), sortField.sort());
        generator.writeEndObject();
        generator.writeEndArray();
    }


    private void writeAggs(JsonGenerator generator, int aggsSize) throws IOException {
        generator.writeObjectFieldStart("aggs");

        generator.writeObjectFieldStart("brand_aggs");
        generator.writeObjectFieldStart("multi_terms");
        generator.writeNumberField("size", aggsSize);
        generator.writeArrayFieldStart("terms");
        generator.writeStartObject();
        generator.writeStringField("field", "brand.id");
        generator.writeEndObject();
        generator.writeStartObject();
        generator.writeStringField("field", "brand.name");
        generator.writeEndObject();
        generator.writeEndArray();
        generator.writeEndObject();
        generator.writeEndObject();//end brand_aggs

        generator.writeObjectFieldStart("category_aggs");
        generator.writeObjectFieldStart("multi_terms");
        generator.writeNumberField("size", aggsSize);
        generator.writeArrayFieldStart("terms");
        generator.writeStartObject();
        generator.writeStringField("field", "category.id");
        generator.writeEndObject();
        generator.writeStartObject();
        generator.writeStringField("field", "category.name");
        generator.writeEndObject();
        generator.writeEndArray();
        generator.writeEndObject();
        generator.writeEndObject();//end category_aggs

        generator.writeEndObject();//end eggs
    }

    private InputStream reorganization(InputStream is) throws IOException {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(BATCH_BUFFER_SIZE);
        boolean transferMark = false;
        try (OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator generator = JSON_FACTORY.createGenerator(os); JsonParser parser = JSON_FACTORY.createParser(is);) {
            generator.writeStartObject();
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    if ("hits".equals(fieldName)) {
                        this.parseHits(parser, generator);
                    } else if ("aggregations".equals(fieldName)) {
                        do {
                            generator.copyCurrentEvent(parser);
                            parser.nextToken();
                        } while (!(parser.currentToken() == JsonToken.END_OBJECT && "aggregations".equals(parser.currentName())));
                        generator.writeEndObject();
                    }
                }
            }
            generator.writeEndObject();
            generator.flush();
            ByteBufInputStream result = new ByteBufInputStream(buffer, true);// 创建输入流并转移所有权
            transferMark = true;
            return result;
        } finally {
            if (!transferMark && buffer != null && buffer.refCnt() > 0) {
                buffer.release(); // 只有在失败时才需要手动释放
            }
        }
    }

    private void parseHits(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.END_OBJECT && "hits".equals(parser.currentName())) break;
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                if ("total".equals(fieldName)) {
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME && "value".equals(parser.currentName())) {
                            parser.nextToken();
                            generator.writeNumberField("total", parser.getValueAsInt());
                            break;
                        }
                    }
                } else if ("hits".equals(fieldName)) {
                    generator.writeArrayFieldStart("items");
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            generator.writeStartObject();
                            this.parserSource(parser, generator);
                            this.parserSort(parser, generator);
                            generator.writeEndObject();
                        }
                        if (parser.currentToken() == JsonToken.END_ARRAY && "hits".equals(parser.currentName())) {
                            break;
                        }
                    }
                    generator.writeEndArray();
                }
            }
        }
    }

    private void parserSource(JsonParser parser, JsonGenerator generator) throws IOException {
        while (parser.nextToken() != null) {
            if (parser.currentToken() == JsonToken.START_OBJECT && "_source".equals(parser.currentName())) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == JsonToken.FIELD_NAME && "_meta".equals(parser.currentName())) break;
                    generator.copyCurrentEvent(parser);
                }
            }
            if (parser.currentToken() == JsonToken.END_OBJECT && "_source".equals(parser.currentName())) break;
        }
    }

    private void parserSort(JsonParser parser, JsonGenerator generator) throws IOException {
        if (parser.nextToken() == JsonToken.FIELD_NAME && "sort".equals(parser.currentName())) {
            generator.copyCurrentEvent(parser);
            while (parser.nextToken() != null) {
                generator.copyCurrentEvent(parser);
                if (parser.currentToken() == JsonToken.END_ARRAY && "sort".equals(parser.currentName())) break;
            }
        }
    }
}
