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

package catalog.hoprxi.core.domain.model;

import org.junit.Assert;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-06-21
 */
public class GradeEnumTest {

    @Test
    public void testOf() {
        Assert.assertEquals(GradeEnum.ONE_LEVEL, GradeEnum.of("ONE_LEVEL"));
        Assert.assertEquals(GradeEnum.PREMIUM, GradeEnum.of("PREMIUM"));//优等品
        Assert.assertEquals(GradeEnum.QUALIFIED, GradeEnum.of("QUALIFIED"));
        Assert.assertEquals(GradeEnum.QUALIFIED, GradeEnum.of("qualifIED"));
    }
}