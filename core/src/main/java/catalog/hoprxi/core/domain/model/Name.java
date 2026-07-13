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

/**
 * 值对象：名称（含全称和简称）。
 * <p>该对象可被 Category、Item、Brand、MadeIn 等实体复用。</p>
 * <ul>
 *   <li>全称（name）允许为空字符串，表示“无名”状态，通常用于占位或根节点。</li>
 *   <li>简称（shortName）为可选字段，若未提供或为空，统一存储为 null，避免数据库查询歧义。</li>
 *   <li>提供 EMPTY 常量以支持空对象模式。</li>
 * </ul>
 *
 * @author <a href="www.hoprxi.com/authors/guan xianghuang">guan xiangHuan</a>
 * @since JDK21
 * @version 0.3 builder 2026-07-12
 */
public final class Name {
    private static final int MAX_LENGTH = 256;
    private final String name;
    private final String shortName;
    /**
     * 空名称对象（空对象模式）
     * <p>调用 rename 仍返回自身，避免空指针</p>
     */
    public static final Name EMPTY = new Name("", null);

    /**
     * 构造方法。
     *
     * @param name      正式名称，可为 {@code null}（自动转为空字符串），首尾空格会被修剪。
     * @param shortName 简称，可为 {@code null} 或空字符串（统一转为 {@code null}），首尾空格会被修剪。
     * @throws IllegalArgumentException 若任一名称长度超过 256。
     */
    public Name(String name, String shortName) {
        // 1. 处理 name：null -> ""，并 trim
        String trimmedName = (name == null) ? "" : name.trim();
        // 2. 处理 shortName：null 或空字符串 -> null，并 trim（非空时）
        String trimmedShort = (shortName == null) ? null : shortName.trim();
        this.shortName = (trimmedShort == null || trimmedShort.isEmpty()) ? null : trimmedShort;

        // 3. 长度校验
        if (trimmedName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("name length exceeds " + MAX_LENGTH);
        }
        if (this.shortName != null && this.shortName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("shortName length exceeds " + MAX_LENGTH);
        }

        this.name = trimmedName;
    }

    public Name(String name) {
        this(name, null);
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
     * 重命名，生成新的 {@code Name} 实例（不变性）。
     * <p>
     * 如果传入的 {@code name} 和 {@code shortName} 经过规范化后与当前对象完全相同，
     * 则返回自身（避免创建不必要的对象）。
     * </p>
     * <p>
     * 规范化规则：
     * <ul>
     *   <li>若 {@code name} 为 {@code null}，表示不修改；否则取其 {@code trim()} 结果。</li>
     *   <li>若 {@code shortName} 为 {@code null}，表示不修改；否则取其 {@code trim()} 结果，
     *       若为空字符串则转为 {@code null}。</li>
     * </ul>
     *
     * @param name      新正式名称，若为 {@code null} 表示不修改；否则会用 {@code trim()} 后值。
     * @param shortName 新简称，若为 {@code null} 表示不修改；否则会用 {@code trim()} 后值（空串则置 {@code null}）。
     * @return 新的 {@code Name} 实例，如果与当前对象无变化则返回自身。
     */
    public Name rename(String name, String shortName) {
        // EMPTY 是不可变的，直接返回自身
        if (this == EMPTY) {
            return this;
        }

        // 规范化传入的参数
        String newName = (name == null) ? this.name : name.trim();
        // shortName：若传入null，保持原值；否则trim，若trim后为空则转为null
        String newShortName = this.shortName; // 默认原值
        if (shortName != null) {
            String trimmed = shortName.trim();
            newShortName = trimmed.isEmpty() ? null : trimmed;
        }

        // 如果规范化后的值与当前值相同，返回自身
        if (Objects.equals(newName, this.name) && Objects.equals(newShortName, this.shortName)) {
            return this;
        }

        // 否则创建新对象
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
