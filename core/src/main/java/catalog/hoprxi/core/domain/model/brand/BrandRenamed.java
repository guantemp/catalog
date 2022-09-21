/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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
package catalog.hoprxi.core.domain.model.brand;

import event.hoprxi.domain.model.DomainEvent;

import java.time.LocalDateTime;


/**
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @version 0.0.2 builder 2019-05-15
 * @since JDK8.0
 */
public class BrandRenamed implements DomainEvent {
    private String name;
    private String mnemonic;
    private String alias;
    private LocalDateTime occurredOn;
    private String id;
    private int version;


    /**
     * @param id
     * @param name
     * @param mnemonic
     * @param alias
     */
    public BrandRenamed(String id, String name, String mnemonic, String alias) {
        this.id = id;
        this.name = name;
        this.mnemonic = mnemonic;
        this.alias = alias;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }


    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    /**
     * @return the id
     */
    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String mnemonic() {
        return mnemonic;
    }

    public String alias() {
        return alias;
    }

    @Override
    public int version() {
        return version;
    }
}
