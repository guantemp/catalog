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

package catalog.hoprxi.core.infrastructure.persistence;

import catalog.hoprxi.core.domain.model.role.Role;
import catalog.hoprxi.core.domain.model.role.RoleRepository;
import org.junit.BeforeClass;
import org.junit.Test;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019/10/6
 */
public class ArangoDBRoleRepositoryTest {
    static RoleRepository roleRepository = new ArangoDBRoleRepository();

    @BeforeClass
    public static void setUpBeforeClass() {
        Role blackGold = new Role("blackgold", "黑金会员", "黑金会员价");
        Role platinum = new Role(" platinum", "铂金会员", "铂金价");
        Role enterprise = new Role(" enterprise", "企业", "企业采购价");
        Role monthly = new Role(" monthly", "月结", "月结价");
        Role cash = new Role(" cash", "现金采购", "现金价");
        Role tax = new Role("tax", "含3%普票", "含税价");
        Role VAT = new Role("VAT", "含10%增殖税", "增殖税价");
        roleRepository.save(blackGold);
        roleRepository.save(blackGold);
        roleRepository.save(platinum);
        roleRepository.save(enterprise);
        roleRepository.save(monthly);
        roleRepository.save(cash);
        roleRepository.save(tax);
        roleRepository.save(VAT);
        roleRepository.save(Role.ANONYMOUS);
    }

    @Test
    public void save() {
    }

    @Test
    public void find() {
    }

    @Test
    public void remove() {
    }
}