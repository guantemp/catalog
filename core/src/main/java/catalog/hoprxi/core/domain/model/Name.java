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

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/***
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK21
 * @version 0.2 builder 2026-06-09
 */
public final class Name {
    private static final int MAX_LENGTH = 256;
    private String name;
    private String shortName;
    /**
     * 空名称对象（空对象模式）
     * <p>调用 rename 仍返回自身，避免空指针</p>
     */
    public static final Name EMPTY = new Name("", "");

    /**
     * 构造方法：传入正式名称与简称
     *
     * @param name      正式名称，不能为 null，长度不超过 256
     * @param shortName 简称，不能为 null，长度不超过 256
     */
    public Name(String name, String shortName) {
        if (name == null && shortName == null) {
            throw new IllegalArgumentException("name and shortName cannot be null at the same time. At least one must be provided.");
        }
        this.name = (name != null) ? name.trim() : "";
        this.shortName = (shortName != null) ? shortName.trim() : "";

        if (this.name.length() > MAX_LENGTH || this.shortName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("name or shortName length range is 0-%d", MAX_LENGTH)
            );
        }
    }

    public Name(String name) {
        this(name, "");
    }

    /**
     * 静态工厂方法：通过正式名称创建 Name 实例
     *
     * @param name 正式名称
     * @return Name 实例
     */
    public static Name of(String name) {
        return new Name(name);
    }

    public String shortName() {
        return shortName;
    }

    public String name() {
        return name;
    }

    /**
     * 重命名，生成新的 Name 对象
     * <p>值对象不可变，修改属性时返回新实例</p>
     *
     * @param name      新正式名称
     * @param shortName 新简称
     * @return 新的 Name 实例（或自身，若未变化）
     */
    public Name rename(String name, String shortName) {
        // 1. 快速拦截：EMPTY 是不可变的，直接返回自身，避免任何无效计算和对象创建
        if (this == EMPTY) {
            return this;
        }

        // 2. 优雅处理 null 与相等回退逻辑
        String newName = Optional.ofNullable(name)
                .filter(n -> !this.name.equals(n))
                .orElse(this.name);

        String newShortName = Optional.ofNullable(shortName)
                .filter(s -> !this.shortName.equals(s))
                .orElse(this.shortName);

        // 3. 引用比较：只要有任何一个属性发生了变化，就返回新实例
        if (Objects.equals(newName, this.name) && Objects.equals(newShortName, this.shortName)) {
            return this;
        }
System.out.println(String.format("rename %s to %s", newName, newShortName));
        return new Name(newName, newShortName);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Name.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("shortName='" + shortName + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Name name1 = (Name) o;
        return Objects.equals(name, name1.name) && Objects.equals(shortName, name1.shortName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(shortName);
        return result;
    }
}
