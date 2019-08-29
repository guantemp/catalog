/*
 *  Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

package catalog.hoprxi.core.domain.model;

import catalog.hoprxi.core.domain.model.barcode.EANUPCBarcode;
import event.foxtail.alpha.domain.model.DomainEvent;

import java.time.LocalDateTime;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-05-02
 */
public class SkuBarcodeChanged implements DomainEvent {
    private String id;
    private EANUPCBarcode barcode;
    private LocalDateTime occurredOn;
    private int version;

    public SkuBarcodeChanged(String id, EANUPCBarcode barcode) {
        this.id = id;
        this.barcode = barcode;
        occurredOn = LocalDateTime.now();
        version = 1;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public int version() {
        return version;
    }

    public String id() {
        return id;
    }

    public EANUPCBarcode barcode() {
        return barcode;
    }
}
