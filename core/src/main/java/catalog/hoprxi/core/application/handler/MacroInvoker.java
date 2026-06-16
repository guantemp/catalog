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

package catalog.hoprxi.core.application.handler;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/6/16
 * @since JDK 21
 */


import catalog.hoprxi.core.application.command.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @param <T> 当前操作的领域对象类型（如 Category, Order）
 */
public class MacroInvoker<T> {
    private final List<Command> commands = new ArrayList<>();
    private final UnitOfWork<T> uow;

    public MacroInvoker(UnitOfWork<T> uow) {
        this.uow = uow;
    }

    public MacroInvoker<T> addCommand(Command command) {
        this.commands.add(command);
        return this;
    }

    /**
     * 批量绑定 Handler 并执行
     */
    public <C extends Command> MacroInvoker<T> bind(Class<C> cmdType, Consumer<C> handlerAction) {
        for (Command cmd : commands) {
            if (cmdType.isInstance(cmd)) {
                handlerAction.accept(cmdType.cast(cmd));
            }
        }
        return this;
    }

    /**
     * 【终极收口】完全不认识 Repository！
     * 外部传入已经加载好的实体，以及一个用于落库的 Runnable
     */
    public void execute(T loadedEntity, Consumer<T> persistAction) {
        // 1. 向 UoW 登记最终的内存对象
        uow.trackDirty(loadedEntity);
        // 2. 统一提交事务并发布事件
        uow.commit(persistAction);
    }
}
