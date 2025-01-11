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

import catalog.hoprxi.core.application.query.QueryFilter;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2025-01-03
 */
public class priceRangFilter implements QueryFilter {
    private PriceType type;
    private Number low;
    private Number high;

    public priceRangFilter(PriceType type, Number low, Number high) {
        this.type = type;
        this.low = low;
        this.high = high;
    }

    @Override
    public void filter(JsonGenerator generator) {
        try {
            generator.writeEndObject();
            generator.writeObjectFieldStart("rang");
            switch (type) {
                case RETAIL_PRICE:
                    generator.writeObjectFieldStart("retail_price.number");
                    break;
            }
            generator.writeObjectFieldStart("retail_price.number");
            generator.writeNumberField("gte", 1);
            generator.writeNumberField("lte", 10);
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeEndObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum PriceType {
        RETAIL_PRICE, last_receipt_price, vip_price, member_price
    }
}
