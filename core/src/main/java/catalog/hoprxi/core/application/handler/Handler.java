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

package catalog.hoprxi.core.application.handler;


import catalog.hoprxi.core.application.command.Command;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.1 2025-09-18
 */
public interface Handler<T extends Command<R>, R> {
    /**
     *
     * @param command
     * @return
     */
    R execute(T command);

    //commandId保存在缓存中，仅当次有效，重启将被被清空
    default void undo() {
        throw new UnsupportedOperationException("Not unsupported");
    }

    default void redo() {
        throw new UnsupportedOperationException("Not unsupported");
    }
}
