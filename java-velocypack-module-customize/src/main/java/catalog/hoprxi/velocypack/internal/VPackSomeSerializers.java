/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.velocypack.internal;


import com.arangodb.velocypack.VPackSerializer;

import javax.money.MonetaryAmount;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-10-24
 */
public class VPackSomeSerializers {

    public static final VPackSerializer<MonetaryAmount> MONETARY_AMOUNT_V_PACK_SERIALIZER = (vPackBuilder, attribute, monetaryAmount, vPackSerializationContext) -> {
        vPackBuilder.add("currencyUnit", monetaryAmount.getCurrency().getCurrencyCode()).add("number", monetaryAmount.getNumber().toString());
      /*
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Output output = new Output(baos)) {
            Kryo kryo = pool.borrow();
            kryo.writeClassAndObject(output, locale);
            output.flush();
            vPackBuilder.add(attribute, ByteToHex.toHexStr(baos.toByteArray()));
            pool.release(kryo);
        }
        */
    };
}
