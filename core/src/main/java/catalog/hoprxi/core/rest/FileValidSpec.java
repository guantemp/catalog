/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.rest;

import salt.hoprxi.to.ByteToHex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public enum FileValidSpec {
    // 图片类型
    IMAGE {
        private final String[] SUPPORTED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "tiff"};
        private final String[] HEADS = {
                "FFD8FF",     // JPEG
                "89504E47",   // PNG
                "47494638",   // GIF
                "424D",       // BMP
                "52494646",   // WEBP
                "00000100",   // ICO
                "49492A00",   // TIFF (小端)
                "4D4D002A"    // TIFF (大端)
        };

        @Override
        public boolean validFormat(File file) {
            return FileValidSpec.checkMagicNumber(file, HEADS);
        }

        @Override
        public String[] support() {
            return SUPPORTED_EXTENSIONS;
        }
    },// 文档类型
    DOCUMENT {
        private final String[] SUPPORTED_EXTENSIONS = {"pdf", "doc", "docx"};
        private final String[] HEADS = {
                "25504446",   // PDF
                "D0CF11E0",   // DOC
                "504B0304"    // DOCX
        };

        @Override
        public boolean validFormat(File file) {
            return FileValidSpec.checkMagicNumber(file, HEADS);
        }

        @Override
        public String[] support() {
            return SUPPORTED_EXTENSIONS;
        }
    };

    // 提取公共的 Magic Number 校验逻辑
    private static boolean checkMagicNumber(File file, String[] heads) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int readLen = fis.read(header);
            if (readLen < 4) return false;

            String hex = ByteToHex.toHexStr(header).toUpperCase();
            for (String head : heads) {
                if (hex.startsWith(head)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("读取文件头失败", e);
        }
    }

    // 抽象方法：由每个枚举值具体实现
    public abstract boolean validFormat(File file);

    public abstract String[] support();
}
