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

package catalog.hoprxi.core.application.command;

import java.net.URI;
import java.util.Objects;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-07-05
 */
public class CategoryChangeIconCommand implements Command {
    private long id;
    private URI icon;

    public CategoryChangeIconCommand(long id, URI icon) {
        this.id = id;
        this.icon = icon;
    }

    public URI icon() {
        return icon;
    }

    public long id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryChangeIconCommand)) return false;

        CategoryChangeIconCommand that = (CategoryChangeIconCommand) o;

        if (id != that.id) return false;
        return Objects.equals(icon, that.icon);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        return result;
    }
}
