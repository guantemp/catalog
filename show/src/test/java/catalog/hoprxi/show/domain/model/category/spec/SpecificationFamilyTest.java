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

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-11-13
 */
public class SpecificationFamilyTest {
    @Test
    public void test() {
        Set<BrandSpecification> brandSpecs = new HashSet<>();
        brandSpecs.add(new BrandSpecification("联想"));
        brandSpecs.add(new BrandSpecification("dell"));
        brandSpecs.add(new BrandSpecification("hp"));
        brandSpecs.add(new BrandSpecification("华为"));
        brandSpecs.add(new BrandSpecification("清华同方"));
        SpecificationFamily<BrandSpecification> brandFamily = new SpecificationFamily("品牌", brandSpecs, 0, true, false);
        System.out.println(brandFamily);


        Set<ColourSpecification> colours = new HashSet<>();
        colours.add(new ColourSpecification("稳重黑"));
        colours.add(new ColourSpecification("紫色魅惑"));
        colours.add(new ColourSpecification("激情红"));
        colours.add(new ColourSpecification("白色"));
        SpecificationFamily<ColourSpecification> colourFamily = new SpecificationFamily("颜色", colours, 1, false, true);
        System.out.println(colourFamily);


        Set<Specification> specifications = new HashSet<>();
        specifications.add(new Specification("1920*1080"));
        specifications.add(new Specification("2560*1440"));
        specifications.add(new Specification("3250*1560"));
        specifications.add(new Specification("1080*2300"));
        SpecificationFamily<Specification> resolution_ratio = new SpecificationFamily("分辨率", specifications, 2, false, false);
        System.out.println(resolution_ratio);
    }

}