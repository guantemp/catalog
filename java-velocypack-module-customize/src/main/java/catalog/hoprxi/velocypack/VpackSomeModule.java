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

package catalog.hoprxi.velocypack;

import catalog.hoprxi.velocypack.internal.VPackSomeDeserializers;
import catalog.hoprxi.velocypack.internal.VPackSomeSerializers;
import com.arangodb.velocypack.VPackModule;
import com.arangodb.velocypack.VPackSetupContext;

import javax.money.MonetaryAmount;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-10-24
 */
public class VpackSomeModule implements VPackModule {
    @Override
    public <C extends VPackSetupContext<C>> void setup(final C context) {
        context.registerDeserializer(MonetaryAmount.class, VPackSomeDeserializers.MONETARY_AMOUNT_V_PACK_DESERIALIZER);

        context.registerSerializer(MonetaryAmount.class, VPackSomeSerializers.MONETARY_AMOUNT_V_PACK_SERIALIZER);
    }
}