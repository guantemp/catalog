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

package catalog.hoprxi.core.rest;


import catalog.hoprxi.core.domain.model.price.UnitEnum;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.stream.StreamMessage;
import com.linecorp.armeria.common.stream.StreamWriter;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2025/9/22
 */

public class UnitService {
    private final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Get("/units")
    public HttpResponse query(@Param("pretty") @Default("false") boolean pretty) {
        StreamWriter<HttpObject> stream = StreamMessage.streaming();
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
            if (pretty) gen.useDefaultPrettyPrinter();
            gen.writeStartObject();
            gen.writeNumberField("total", UnitEnum.values().length);
            gen.writeArrayFieldStart("units");
            for (UnitEnum unit : UnitEnum.values()) {
                gen.writeString(unit.toString());
            }
            gen.writeEndArray();
            gen.flush();
            stream.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.wrap(os.toByteArray()));
            stream.close();
        } catch (IOException e) {
            stream.write(ResponseHeaders.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaderNames.CONTENT_TYPE, MediaType.JSON_UTF_8));
            stream.write(HttpData.ofUtf8("{\"status\":\"error\",\"code\":500,\"message\":\"Error,it's %s\"}", e.getMessage()));
            stream.close();
        }
        return HttpResponse.of(stream);
    }
}
