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
package catalog.hoprxi.time_count.domain.model;



import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2019-10-24
 */
public class Time {
    private long brandId;
    private long categoryId;
    private long id;
    private TimeUnit unit;
    private String mnemonicCode;
    private String name;
    private Set<BufferedImage> pictures;


    public Time(long id, String name, String mnemonicCode,
                TimeUnit unit, long brandId, long categoryId,
                Set<BufferedImage> pictures) {
        super();
        this.id = id;
        setName(name);
        this.mnemonicCode = mnemonicCode;
        setTimeUnit(unit);
        this.brandId = brandId;
        this.categoryId = categoryId;
        setPictures(pictures);
    }


    /**
     * @return the brand
     */
    public long brandId() {
        return brandId;
    }

    /**
     * @return the categoryId
     */
    public long categoryId() {
        return categoryId;
    }


    /**
     * @return the id
     */
    public long id() {
        return id;
    }

    /**
     * @return the unit
     */
    public TimeUnit unit() {
        return unit;
    }

    /**
     * @return the mnemonic
     */
    public String mnemonicCode() {
        return mnemonicCode;
    }

    /**
     * @return the newName
     */
    public String name() {
        return name;
    }


    /**
     * @return the pictures
     */
    public Set<BufferedImage> pictures() {
        return pictures;
    }


    /**
     * @param unit
     * @throws IllegalArgumentException if unit isn't mass unit
     */
    protected void setTimeUnit(TimeUnit unit) {
        Objects.requireNonNull(unit, "unit required");
        this.unit = unit;
    }

    /**
     * @param name the newName to set
     */
    protected void setName(String name) {
        this.name = Objects.requireNonNull(name, "newName required").trim();
    }

    /**
     * @param pictures the pictures to set
     */
    protected void setPictures(Set<BufferedImage> pictures) {
        if (null == pictures)
            pictures = new HashSet<BufferedImage>(0);
        this.pictures = pictures;
    }

}
