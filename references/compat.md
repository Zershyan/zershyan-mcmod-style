# 第三方模组兼容（compat）

## 目的

把对**可选依赖**（第三方 mod）的调用集中到 `compat/` 包，保证依赖缺失时模组仍能正常启动：不加载对应类、不注册对应内容、不崩溃。

## 结构

```
compat/
├── ICompatUtils.java              兼容工具接口
├── <ModId>CompatFactory.java      兼容工厂（统一入口）
├── <被兼容modid>/                  每个可选依赖一个子包
│   ├── XxxCompat.java             该依赖的兼容实现（implements ICompatUtils）
│   └── ...                        该依赖自己的 api / event / utils / registry
└── <另一个modid>/
    └── ...
```

- 子包**按被兼容 mod 的 modid 命名**（`bettercombat/`、`playeranimator/`、`patchouli/`）。
- 每个子包内可以有自己的完整结构（`api/`、`event/`、`utils/`、`registry/`），因为兼容代码往往自成体系。
- 该依赖**独有**的注册项、事件、mixin 接口都收在这个子包内，不要散到主包的 `registry/`。

## ICompatUtils

```java
public interface ICompatUtils {
    default boolean testLoadedAndRun(Runnable runnable) {
        if (isModLoaded()) runnable.run();
        else return false;
        return true;
    }

    default <T> T testLoadedAndCall(Callable<T> callable, T errorResult) {
        try {
            if (isModLoaded()) return callable.call();
        } catch (Exception ignored) {
        }
        return errorResult;
    }

    default <T> T testLoadedAndCall(Callable<T> callable, Callable<T> elseCall, T errorResult) {
        try {
            if (isModLoaded()) return callable.call();
            else return elseCall.call();
        } catch (Exception e) {
            return errorResult;
        }
    }

    default void addCommonListener(IEventBus forgeBus, IEventBus modBus) {
    }

    default void addClientListener(IEventBus forgeBus, IEventBus modBus) {
    }

    default void addListener(IEventBus forgeBus, IEventBus modBus) {
        addCommonListener(forgeBus, modBus);
        if (<isClient>) {
            addClientListener(forgeBus, modBus);
        }
    }

    default void init(IEventBus forgeBus, IEventBus modBus) {
        addListener(forgeBus, modBus);
    }

    default void initial(IEventBus forgeBus, IEventBus modBus) {
        try {
            testLoadedAndRun(() -> init(forgeBus, modBus));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    default void safelyRun(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
        }
    }

    default void initNetwork(PayloadRegistrar registrar) {
        try {
            testLoadedAndRun(() -> registerNetwork(registrar));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    default void registerNetwork(PayloadRegistrar registrar) {
    }

    boolean isModLoaded();
}
```

要点：

- `isModLoaded()` 是唯一必须实现的方法，用 `ModList.get().isLoaded(modid)`。
- `testLoadedAndRun` / `testLoadedAndCall` 是**唯一的依赖检查入口**：所有对第三方 API 的调用都要包在里面，这样依赖缺失时类不会被链接。
- `addCommonListener` / `addClientListener` 由子类覆写；客户端分支由基类统一判断，子类不用自己判断端。
- `initNetwork` 让兼容层能注册自己的 payload。
- 日志用 `LOGGER`（见 [logging-and-util.md](logging-and-util.md)）。

## 加载器差异

`ICompatUtils` 是与加载器相关的，两个世代各有一处不同。**接口方法名与整体格式完全一致**，只改这两处：

| 项 | NeoForge（1.21.x） | Forge（1.20.1） |
|---|---|---|
| `IEventBus` 导入 | `net.neoforged.bus.api.IEventBus` | `net.minecraftforge.eventbus.api.IEventBus` |
| 客户端判断 | `FMLLoader.getDist() == Dist.CLIENT` | `FMLEnvironment.dist == Dist.CLIENT` |
| 网络参数 | `PayloadRegistrar` | `SimpleChannel` |

### NeoForge 版

```java
package io.zershyan.<modid>.compat;

import io.zershyan.<modid>.<ModId>;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public interface ICompatUtils {
    Logger LOGGER = LoggerFactory.getLogger(ICompatUtils.class);

    // ... testLoadedAndRun / testLoadedAndCall 同上 ...

    default void addListener(IEventBus forgeBus, IEventBus modBus) {
        addCommonListener(forgeBus, modBus);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            addClientListener(forgeBus, modBus);
        }
    }

    default void initNetwork(PayloadRegistrar registrar) {
        try {
            testLoadedAndRun(() -> registerNetwork(registrar));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    default void registerNetwork(PayloadRegistrar registrar) {
    }

    boolean isModLoaded();
}
```

### Forge 版

```java
package io.zershyan.<modid>.compat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public interface ICompatUtils {
    Logger LOGGER = LoggerFactory.getLogger(ICompatUtils.class);

    // ... testLoadedAndRun / testLoadedAndCall 同上 ...

    default void addListener(IEventBus forgeBus, IEventBus modBus) {
        addCommonListener(forgeBus, modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            addClientListener(forgeBus, modBus);
        }
    }

    default void initNetwork(SimpleChannel channel) {
        try {
            testLoadedAndRun(() -> registerNetwork(channel));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    default void registerNetwork(SimpleChannel channel) {
    }

    boolean isModLoaded();
}
```

## 兼容工厂

工厂负责持有全部兼容实现，并在主类里一次性触发。

```java
public class <ModId>CompatFactory {
    private static final Set<ICompatUtils> compatUtils = new HashSet<>();

    public static final <被兼容mod>Compat <字段名> = addCompat(new <被兼容mod>Compat());

    private static <T extends ICompatUtils> T addCompat(T utils) {
        if (compatUtils.add(utils)) {
            return utils;
        } else throw new IllegalStateException("CompatUtils already added.");
    }

    public static void register(IEventBus forgeEventBus, IEventBus modEventBus) {
        compatUtils.forEach(compatUtils -> compatUtils.initial(forgeEventBus, modEventBus));
    }

    public static void registerNetwork(PayloadRegistrar registrar) {
        compatUtils.forEach(compatUtils -> compatUtils.initNetwork(registrar));
    }
}
```

要点：

- 工厂类名 `<ModId>CompatFactory`（无混淆时；混淆时 `CompatFactory`）。
- 每个兼容实现在工厂里声明为 `public static final` 字段，便于其他地方直接取用。
- `addCompat` 用 `Set` 去重，重复添加直接抛异常，避免静默重复注册。
- `registerNetwork` 让兼容层接入主 mod 的 payload 注册流程，**网络注册处必须调用它**，否则兼容层的 payload 不会注册。

## 兼容实现

```java
public class ExampleCompat implements ICompatUtils {
    public static final String MODID = "<被兼容modid>";

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    @Override
    public void addCommonListener(IEventBus forgeBus, IEventBus modBus) {
        forgeBus.register(ExampleServerRegistry.class);
        ExampleEntities.register(modBus);
    }

    @Override
    public void addClientListener(IEventBus forgeBus, IEventBus modBus) {
        forgeBus.register(ExampleClientRegistry.class);
    }

    @Override
    public void registerNetwork(PayloadRegistrar registrar) {
        registrar.playToServer(ExampleCompatData.TYPE, ExampleCompatData.STREAM_CODEC, ExampleCompatData::handle);
    }
}
```

## 主类接线

```java
public <ModId>(IEventBus modEventBus, Dist dist, ModContainer modContainer) {
    IEventBus neoEventBus = NeoForge.EVENT_BUS;

    <ModId>Items.doRegister(modEventBus);
    // ...

    //compat
    <ModId>CompatFactory.register(neoEventBus, modEventBus);
}
```

网络注册处：

```java
private static void register(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
    registrar.playToServer(...);
    // 兼容层的 payload 由工厂统一注册
    <ModId>CompatFactory.registerNetwork(registrar);
}
```

## 对外暴露

需要让其他 mod 调用的兼容能力，走 `api` 包转发（见 [api.md](api.md)）：api 内部调 compat，compat 不反向依赖 api。

## 完成前检查

- 所有第三方 API 调用是否都包在 `testLoadedAndRun` / `testLoadedAndCall` 里？
- 依赖缺失时模组是否仍能启动（实际移除该依赖测一次）？
- 兼容子包是否按被兼容 modid 命名？
- 主类是否调用了 `CompatFactory.register`？网络注册处是否调用了 `CompatFactory.registerNetwork`？
- 兼容代码是否只在对应依赖存在时才注册？