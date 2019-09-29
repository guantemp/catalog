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

package catalog.hoprxi.core.application;

import catalog.hoprxi.core.application.command.ChangeBrandAboutCommand;
import catalog.hoprxi.core.application.command.CreateBrandCommand;
import catalog.hoprxi.core.application.command.RenameBrandCommand;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.brand.AboutBrand;
import catalog.hoprxi.core.domain.model.brand.Brand;
import catalog.hoprxi.core.domain.model.brand.BrandRepository;
import catalog.hoprxi.core.infrastructure.persistence.ArangoDBBrandRepository;

import java.net.URL;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-05-16
 */
public class BrandAppService {

    private BrandRepository repository = new ArangoDBBrandRepository();

    public Brand createBrand(CreateBrandCommand command) {
        Name name = new Name(command.getName(), command.getAlias());
        AboutBrand about = new AboutBrand(command.getLogo(), command.getHomepage(), command.getSince(), command.getStory());
        Brand brand = new Brand(repository.nextIdentity(), name, about);
        repository.save(brand);
        return brand;
    }

    public Brand brand(String name, String alias, URL logo) {
        return null;
    }

    public boolean isExistes(String id) {
        return repository.find(id) != null;
    }

    public Brand brand(String id) {
        return repository.find(id);
    }

    public void rename(RenameBrandCommand rename) {

    }

    public void changeBrandAbout(ChangeBrandAboutCommand changeBrandAboutCommand) {

    }
}
