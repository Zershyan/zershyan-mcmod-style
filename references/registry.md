# 注册与 registry

## 核心原则

**所有与注册有关的内容必须放在 `registry` 包下，即使其注册方式是事件。**

典型例子：注册 overlay 在 NeoForge / Forge 都是通过事件完成的，但依据本原则，仍要有 `<ModId>Overlays` 类，且内部使用 `doRegister`，**而不是 `@EventBusSubscriber`**。

## 判定标准

| 内容 | 是否进 registry |
|---|---|
| 注册项本身（物品、方块、声音、属性…） | 是 |
| 注册项的属性 / 行为定义（如 `item/properties/`、`item/behavior/`） | 是 |
| 注册时必需的数据（如 `attachment/sub/` 下的 DTO） | 是 |
| 注册动作（`doRegister`） | 是 |
| 自定义注册表及其注册项 | 是 |
| 仅使用已注册对象 | 否 → 功能域 |

边界不清时**先问用户**。

## 注册入口

- 每个注册领域一个类，唯一入口方法名 **`doRegister`**。
- 入口参数显式表达所需上下文：`IEventBus`（mod 总线 / forge 总线）、`ModContainer` 等。
- 主类中不得散落 `addListener(...)`、`registerConfig(...)`、`DeferredRegister.register(...)`。

```java
public final class <ModId>Items {
    public static void doRegister(IEventBus modBus) {
        // 注册细节收拢在此
    }
}
```

## 常见注册类型（参考清单，非穷举）

参考清单已剔除 mod 特有的非平台接口项。

**注册类**：`<ModId>Items`、`<ModId>Sounds`、`<ModId>Blocks`、`<ModId>BlockEntityTypes`、`<ModId>Entities`、`<ModId>Packets`、`<ModId>Configs`、`<ModId>Commands`、`<ModId>Attributes`、`<ModId>Attachments`、`<ModId>CreativeTab`、`<ModId>DataComponents`、`<ModId>Overlays`、`<ModId>KeyMappings`、`<ModId>EntityLayers`、`<ModId>Registries`（启用混淆时前缀换成 `Mod`）。

**子包**：`item/`、`block/`、`block/entity/`、`command/`、`config/`、`packet/`、`overlay/`、`attachment/`、`data_component/`、`renderer/`。

**遇到清单之外、且属于注册的类型时，先询问用户是否写入 `registry`。**

## 自定义注册表

`<ModId>Registries` 专门用于**注册自定义注册表**（自定义 Registry），不是杂项注册的收容处。

```java
public class ExampleModRegistries {
    public static final ResourceKey<Registry<IExampleStyle>> CUSTOM_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ExampleMod.id("custom_registry"));

    public static final Registry<IExampleStyle> CUSTOM_REGISTRY =
            new RegistryBuilder<>(CUSTOM_REGISTRY_KEY).create();

    public static void createRegistry(NewRegistryEvent event) {
        event.register(CUSTOM_REGISTRY);
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(ExampleModRegistries::createRegistry);
    }
}
```

自定义注册表建好后，具体注册项用 `DeferredRegister.create(该注册表, MODID)` 写入，并配套一个注册入口类：

```java
public class ExampleModCustomRegistries {
    public static final DeferredRegister<IExampleStyle> REGISTER =
            DeferredRegister.create(ExampleModRegistries.CUSTOM_REGISTRY, ExampleMod.MODID);

    public static final Holder<IExampleStyle> SIMPLE = REGISTER.register("simple", SimpleExampleStyle::new);

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
```

对应的目录结构：

```
registry/
├── ExampleModRegistries.java              自定义注册表声明
├── ExampleModCustomRegistries.java        注册项入口
└── custom_registry/                       注册表名作子包
    ├── builder/IExampleStyle.java         类型接口
    └── SimpleExampleStyle.java            具体实现
```

要点：

- 注册表名（如 `custom_registry`）同时是子包名与资源路径。
- 类型接口放该子包的 `builder/`，具体实现平铺在子包内。
- 注册项的语言键用 `toLanguageKey("<注册表名>", "desc")`，在语言基类提供对应辅助方法。
- `RegistryBuilder` 的注册动作挂在 mod 总线的 `NewRegistryEvent` 上。

## Forge 1.20.1 差异

- `DeferredRegister`、`RegistryObject`、`ForgeRegistries` 来自 `net.minecraftforge.registries`；不使用 NeoForge 的 `DeferredItem`、`DeferredBlock`。
- 自定义注册表仍归 `<ModId>Registries`。在 mod 总线的 `NewRegistryEvent` 中调用 `event.create(new RegistryBuilder<T>().setName(key.location()))`，保留返回的 `Supplier<IForgeRegistry<T>>`，事件后才访问其值；具体注册项通过 `DeferredRegister.create(key, MODID)` 注册。
- ItemStack 自定义数据改用 NBT，不建 `<ModId>DataComponents`，也不创建 `registry/data_component/`。
- overlay 使用 `RegisterGuiOverlaysEvent` / `IGuiOverlay`，仍由 `<ModId>Overlays.doRegister(modBus)` 挂监听，不挪进 handler。
- 附件类型通过 forgeattachment 的注册表键注册，见 [forgeattachment.md](forgeattachment.md)。

## registry 内部子包约定

- `registry/config/` —— 配置，见 [config.md](config.md)
- `registry/packet/` —— 网络包，见 [network.md](network.md)
- `registry/command/` —— 命令，见 [commands.md](commands.md)
- `registry/attachment/` —— 附件数据类，见 [attachments.md](attachments.md)

## 注意

部分注册的 `doRegister` 入参是具体事件（而非总线），如 `<ModId>EntityLayers.doRegister(RegisterLayerDefinitions event)`、`<ModId>KeyMappings.doRegister(RegisterKeyMappingsEvent event)`。是否采用这种形式取决于该注册 API 的提供方式，以既有代码为准。