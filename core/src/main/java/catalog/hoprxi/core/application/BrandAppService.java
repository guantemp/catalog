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

package catalog.hoprxi.core.application;

import catalog.hoprxi.core.application.command.*;
import catalog.hoprxi.core.application.query.BrandQuery;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.*;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlBrandRepository;
import catalog.hoprxi.core.infrastructure.query.elasticsearch.ESBrandQuery;
import catalog.hoprxi.core.util.DomainRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Collection;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.2 2023-01-07
 */
public class BrandAppService {
    private final BrandRepository repository;

    public BrandAppService() {
        Config conf = ConfigFactory.load("databases");
        String provider = conf.hasPath("provider") ? conf.getString("provider") : "postgresql";
        switch ((provider)) {
            case "postgresql":
            case "psql":
            default:
                repository = new PsqlBrandRepository();
                break;
        }
    }

    public static BrandQuery getQuery() {
        return new ESBrandQuery();
    }

    public Brand createBrand(BrandCreateCommand command) {
        Name name = new Name(command.getName(), command.getAlias());
        AboutBrand about = new AboutBrand(command.getLogo(), command.getHomepage(), command.getSince(), command.getStory());
        Brand brand = new Brand(repository.nextIdentity(), name, about);
        repository.save(brand);
        //领域事件：新建品牌
        BrandCreated created = new BrandCreated(brand.id(), brand.name().name(), brand.name().mnemonic(), brand.name().alias());
        if (brand.about() != null)
            created = new BrandCreated(brand.id(), brand.name().name(), brand.name().mnemonic(), brand.name().alias());
        DomainRegistry.domainEventPublisher().publish(created);
        return brand;
    }

    public void handle(Collection<Command> commands) {
        Brand brand = null;
        for (Command command : commands) {
            if (command instanceof BrandRenameCommand) {
                if (brand == null)
                    brand = repository.find(((BrandRenameCommand) command).getId());
                rename(brand, (BrandRenameCommand) command);
            }
            if (command instanceof BrandChangeAboutCommand) {
                if (brand == null)
                    brand = repository.find(((BrandChangeAboutCommand) command).getId());
                changeBrandAbout(brand, (BrandChangeAboutCommand) command);
            }
        }
        repository.save(brand);
    }

    private void rename(Brand brand, BrandRenameCommand command) {
        if (brand != null) {
            brand.name().rename(command.getName(), command.getAlias());
        }
    }

    private void changeBrandAbout(Brand brand, BrandChangeAboutCommand command) {
        if (brand != null) {
            AboutBrand about = new AboutBrand(command.getLogo(), command.getHomepage(), command.getSince(), command.getStory());
            brand.changeAbout(about);
        }
    }

    public void delete(BrandDeleteCommand deleteCommand) {
        repository.remove(deleteCommand.getId());
        //发送领域事件
        DomainRegistry.domainEventPublisher().publish(new BrandDeleted(deleteCommand.getId()));
    }
}
