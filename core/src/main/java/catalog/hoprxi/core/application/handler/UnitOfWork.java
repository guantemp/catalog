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

import catalog.hoprxi.core.util.DomainRegistry;
import event.hoprxi.domain.model.DomainEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @param <T> 当前操作的领域对象类型
 */
public class UnitOfWork<T> {
    // 强类型追踪实体
    private T dirtyEntity;

    // 【终极优化】严格限定为 DomainEvent 类型，并利用 HashSet 天然去重
    private final Set<DomainEvent> pendingDomainEvents = new HashSet<>();

    public void trackDirty(T entity) {
        this.dirtyEntity = entity;
    }

    public void addEvent(DomainEvent event) {
        this.pendingDomainEvents.add(event);
    }

    /**
     * 统一提交：先物理落库，再发布事件
     */
    public void commit(Consumer<T> persistAction) {
        if (dirtyEntity != null) {
            persistAction.accept(dirtyEntity);
        }

        for (DomainEvent event : pendingDomainEvents) {
            DomainRegistry.domainEventPublisher().publish(event); // 假设存在静态事件总线，且只接受 DomainEvent 类型
        }

        // 清理状态
        dirtyEntity = null;
        pendingDomainEvents.clear();
    }
}
