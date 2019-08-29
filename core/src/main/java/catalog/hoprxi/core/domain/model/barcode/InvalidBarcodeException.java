/*
 * Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

/***
 * @author <a href="www.foxtail.cc/authors/guan xianghuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2018-05-30
 */
public class InvalidBarcodeException extends RuntimeException {
    /**
     * 仅包含message, 没有cause, 不记录栈异常, 性能最高
     *
     * @param message
     */
    public InvalidBarcodeException(String message) {
        super(message, null, false, false);
    }

    /**
     * @param message
     * @param cause
     */
    public InvalidBarcodeException(String message, Throwable cause) {
        super(message, cause, false, true);
    }
}
