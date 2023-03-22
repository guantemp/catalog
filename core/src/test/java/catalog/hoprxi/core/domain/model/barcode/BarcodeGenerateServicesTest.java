/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.domain.model.barcode;


import org.testng.Assert;
import org.testng.annotations.Test;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 2019-04-26
 */
public class BarcodeGenerateServicesTest {

    @Test(expectedExceptions = InvalidBarcodeException.class)
    public static void testIsbn() {
        ISBN isbn = new ISBN("9787512423046");
        Assert.assertNotNull(isbn);
        isbn = new ISBN("2200056789272");
    }

    @Test(expectedExceptions = InvalidBarcodeException.class)
    public static void testISSN() {
        ISSN issn = new ISSN("2200056789272");
    }

    @Test
    public void testCreateEANUPCBarcode() {
        Barcode barcode = BarcodeGenerateServices.createBarcode("2200056789296");
        System.out.println(barcode);
        barcode = BarcodeGenerateServices.createBarcodeWithChecksum("693023999160");
        System.out.println(barcode);
    }

    @Test
    public void testInStoreEAN_8Generate() {
        Barcode[] eans = BarcodeGenerateServices.inStoreEAN_8BarcodeGenerate(7542, 20, "23");
        for (Barcode e : eans)
            System.out.println(e);

    }

    @Test
    public void testInStoreEAN_8GenerateWithFilter() {
        Barcode[] eans = BarcodeGenerateServices.inStoreEAN_8BarcodeGenerateWithFilter(212, 2, "22", new int[]{4});
        for (Barcode e : eans)
            System.out.println(e);
    }

    @Test
    public void testInStoreEAN_13Generate() {
        Barcode[] eans = BarcodeGenerateServices.inStoreEAN_13BarcodeGenerate(5678921, 20, "21");
        for (Barcode e : eans)
            System.out.println(e);
        eans = BarcodeGenerateServices.inStoreEAN_13BarcodeGenerate(214, 10, "20", new int[]{4});
        System.out.println("Filter number 4(mantissa):");
        for (Barcode e : eans)
            System.out.println(e);
    }

    @Test
    public void testCouponBarcodeGenerate() {
        Barcode[] eans = BarcodeGenerateServices.couponBarcodeGenerate(325, 30);
        for (Barcode e : eans)
            System.out.println(e);
        System.out.println("Filter number 4(mantissa):");
        eans = BarcodeGenerateServices.couponBarcodeGenerate(110, 5, new int[]{4});
        for (Barcode e : eans)
            System.out.println(e);
    }
}