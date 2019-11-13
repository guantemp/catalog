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

package catalog.hoprxi.show.domain.model.category.spec;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-11-13
 */
public class SpecificationFamilyTest {
    private static SpecificationFamily<BrandSpecification> brand = SpecificationFamily.<BrandSpecification>createBlankSpecFamily("品牌");
    private static SpecificationFamily<ColourSpecification> colour = SpecificationFamily.<BrandSpecification>createBlankSpecFamily("颜色");

    @BeforeClass
    public static void setUpBeforeClass() {
        Set<BrandSpecification> brands = new HashSet<>();
        brands.add(new BrandSpecification("联想"));
        brands.add(new BrandSpecification("dell"));
        brands.add(new BrandSpecification("hp"));
        brands.add(new BrandSpecification("华为"));
        brands.add(new BrandSpecification("清华同方"));
        brand = brand.addSpec(brands);
        Set<ColourSpecification> colours = new HashSet<>();
        colours.add(new ColourSpecification("稳重黑"));
        colours.add(new ColourSpecification("紫色魅惑"));
        colours.add(new ColourSpecification("激情红"));
        colours.add(new ColourSpecification("白色"));
        colour = colour.addSpec(colours);
    }

    @Test
    public void test() {
        Set<BrandSpecification> brands = brand.getSpecs();
        System.out.println(brand.name());
        for (BrandSpecification spec : brands) {
            System.out.println(spec);
        }
        Set<ColourSpecification> colours = colour.getSpecs();
        System.out.println(colour.name());
        for (ColourSpecification spec : colours) {
            System.out.println(spec);
        }
    }

}