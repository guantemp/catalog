/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.application.command;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-07-26
 */
public class ItemRenameCommand implements Command {
    private final long id;
    private String name;
    private String alias;

    public ItemRenameCommand(long id, String name, String alias) {
        this.id = id;
        this.setName(name, alias);
    }

    private void setName(String name, String alias) {
        if (name == null && alias == null)
            throw new IllegalArgumentException("");
        this.name = name;
        this.alias = alias;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
    }
}
