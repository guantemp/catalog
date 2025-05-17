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

package catalog.hoprxi.core.application.handler;

import catalog.hoprxi.core.application.command.BrandCreateCommand;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandCreated;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.util.DomainRegistry;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-01-07
 */
public class BrandCreateHandler implements Handler<BrandCreateCommand, String> {

    private final BrandRepository repository = new PsqlBrandRepository();

    @Override
    public String handle(BrandCreateCommand command) {
        Name name = new Name(command.getName(), command.getAlias());
        AboutBrand about = new AboutBrand(command.getLogo(), command.getHomepage(), command.getSince(), command.getStory());
        Brand brand = new Brand(repository.nextIdentity(), name, about);
        repository.save(brand);
        //领域事件：新建品牌
        BrandCreated created = new BrandCreated(brand.id(), brand.name().name(), brand.name().mnemonic(), brand.name().alias());
        if (brand.about() != null)
            created = new BrandCreated(brand.id(), brand.name().name(), brand.name().mnemonic(), brand.name().alias());
        DomainRegistry.domainEventPublisher().publish(created);
        return null;
    }

    @Override
    public String undo() {

        return null;
    }
}
