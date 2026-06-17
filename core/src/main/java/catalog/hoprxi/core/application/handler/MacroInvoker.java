package catalog.hoprxi.core.application.handler;

import catalog.hoprxi.core.application.command.Command;

import java.util.*;
import java.util.function.Consumer;

public class MacroInvoker<T> {
    // 【保留 List】命令必须保留顺序，且允许同一命令出现多次（如批量加购）
    private final List<Command> commands = new ArrayList<>();

    // 【改为 Map】Key: 命令的 Class, Value: 对应的 Handler。天然去重 + O(1) 查找
    private final Map<Class<? extends Command>, AggregateHandler<? extends Command, T>> handlerMap = new LinkedHashMap<>();

    private final UnitOfWork<T> uow;

    public MacroInvoker(UnitOfWork<T> uow) {
        this.uow = uow;
    }

    public MacroInvoker<T> addCommand(Command command) {
        this.commands.add(command);

        return this;
    }

    /**
     * 【核心优化】绑定 Handler 时，自动提取其泛型绑定的 Command 类型作为 Key
     * 使用 LinkedHashMap 保证绑定的顺序
     */
    @SuppressWarnings("unchecked")
    public MacroInvoker<T> bind(AggregateHandler<? extends Command, T> handler) {
        // 获取当前 Handler 实现的所有接口
        Class<?>[] interfaces = handler.getClass().getInterfaces();

        for (Class<?> iface : interfaces) {
            // 只要找到了 AggregateHandler 接口
            if (iface == AggregateHandler.class) {
                // 【核心修复】直接从 iface (即 AggregateHandler) 上提取泛型参数
                // 因为 iface 本身就是 ParameterizedType (带有 <RenameCategoryCommand, Category>)
                java.lang.reflect.Type genericType = handler.getClass().getGenericInterfaces()[0];

                if (genericType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments();
                    Class<? extends Command> cmdType = (Class<? extends Command>) typeArgs[0];

                    handlerMap.put(cmdType, handler);
                    //System.out.println("成功绑定: " + cmdType.getSimpleName() + " -> " + handler.getClass().getSimpleName());
                    return this;
                }
            }
        }

        throw new IllegalArgumentException("无法解析 Handler 绑定的 Command 类型: " + handler.getClass().getName());
    }

    /**
     * 【终极收口】O(1) 极速匹配，统一落库
     */
    @SuppressWarnings("unchecked")
    public T execute(T loadedEntity, Consumer<T> persistAction) {
        // 1. 遍历命令，O(1) 匹配 Handler 并执行
        for (Command cmd : commands) {
            System.out.println("loadedEntity: " + loadedEntity);
            AggregateHandler<? extends Command, T> handler = handlerMap.get(cmd.getClass());
            if (handler == null) {
                throw new IllegalStateException("未找到处理该命令的 Handler: " + cmd.getClass().getSimpleName());
            }
            // 强转并执行
            ((AggregateHandler<Command, T>) handler).execute(loadedEntity, cmd);
        }
        // 2. 向 UoW 登记最终的内存对象
        uow.trackDirty(loadedEntity);
        System.out.println("loadedEntity: " + loadedEntity);
        // 3. 统一提交事务并发布事件
        uow.commit(persistAction);

        return loadedEntity;
    }
}