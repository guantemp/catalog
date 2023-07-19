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

package catalog.hoprxi.show.domain.model;

import java.net.URI;
import java.util.Arrays;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-07-19
 */
public class MainImage {
    private static final int MAX_IMAGES_NUM = 6;
    private URI[] images;

    public MainImage(URI[] images) {
        setImages(images);
    }

    private void setImages(URI[] images) {
        if (images == null)
            images = new URI[0];
        if (images.length > MAX_IMAGES_NUM)
            throw new IllegalArgumentException("Supports max images number is: " + MAX_IMAGES_NUM);
        for (URI image : images)
            checkImage(image);
        this.images = images;
    }

    public URI[] images() {
        return images;
    }

    private boolean checkImage(URI image) {
        return true;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MainImage.class.getSimpleName() + "[", "]")
                .add("images=" + Arrays.toString(images))
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MainImage)) return false;

        MainImage mainImage = (MainImage) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(images, mainImage.images);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(images);
    }
}
