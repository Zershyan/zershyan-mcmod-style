# 主类与双端结构

## 双端隔离

**按加载器分代，两种写法不可混用。**

### NeoForge（1.21.x 及以后）

构造器直接收 `Dist` 参数，用它判断分端。`Dist` 是 NeoForge 特有的。

```java
@Mod(<ModId>.MODID)
public class <ModId> {
    public static final String MODID = "<modid>";
    public static final Logger LOGGER = LoggerFactory.getLogger(<ModId>.class);

    public <ModId>(IEventBus modEventBus, Dist dist, ModContainer modContainer) {
        IEventBus neoEventBus = NeoForge.EVENT_BUS;

        <ModId>Items.doRegister(modEventBus);
        <ModId>Sounds.doRegister(modEventBus);
        <ModId>Packets.doRegister(modEventBus);
        <ModId>Configs.doRegister(modContainer);
        <ModId>Commands.doRegister(neoEventBus);

        if (dist.isClient()) {
            <ModId>Overlays.doRegister(modEventBus);
            <ModId>KeyMappings.doRegister(modEventBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
```

### Forge（1.20.1）

构造器**没有** `Dist` 参数，用 `DistExecutor.unsafeRunWhenOn` 隔离客户端。

```java
@Mod(<ModId>.MODID)
public class <ModId> {
    public static final String MODID = "<modid>";
    public static final Logger LOGGER = LoggerFactory.getLogger(<ModId>.class);

    public <ModId>() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        <ModId>Items.doRegister(modBus);
        <ModId>Commands.doRegister(forgeBus, modBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            <ModId>ClientSetup.doRegister(modBus);
            <ModId>Overlays.doRegister(modBus);
        });
    }
}
```

## 主类职责

- 主类**只编排** `doRegister(...)` 调用与启动上下文，不直接写底层注册细节（不出现散落的 `addListener(...)`、`registerConfig(...)`、`DeferredRegister.register(...)`）。
- 构造器内先取 `IEventBus modEventBus` 与 `NeoForge.EVENT_BUS`（Forge 为 `MinecraftForge.EVENT_BUS`），再依次调用各注册入口。
- 客户端专属注册必须在 `dist.isClient()`（或 `DistExecutor.unsafeRunWhenOn`）内部，避免专用服务器链接客户端类。

## 主类固定成员

```java
public static final String MODID = "<modid>";
public static final Logger LOGGER = LoggerFactory.getLogger(<ModId>.class);

public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
}
```

`LOGGER` 与 `id()` 的完整规范见 [logging-and-util.md](logging-and-util.md)。

## 完成前检查

- 客户端注册是否都在分端判断内？
- 通用代码是否静态引用了客户端专属类？
- 每个注册领域是否都通过唯一的 `doRegister(...)` 入口？主类是否混入了底层注册调用？
- 事件总线是否挂对了（mod 总线 vs forge 总线）？