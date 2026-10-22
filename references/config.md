# 配置

## 位置

`config/` 作为 registry 子包：

```
registry/
├── <ModId>Configs.java       doRegister(ModContainer)
└── config/
    ├── ClientConfig.java
    ├── CommonConfig.java
    └── ServerConfig.java
```

## 配置类写法

按 `ModConfig.Type` 分文件，每类一个类。内部用 static 块构建 `ModConfigSpec`：

```java
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HIGHEST_TOOLTIP_EVENT;

    static {
        HIGHEST_TOOLTIP_EVENT = BUILDER.comment("是否采用至高优先级tooltip事件")
                .define("highest_tooltip_event", false);

        SPEC = BUILDER.build();
    }
}
```

- 配置项键名用 `lower_snake_case`。
- `comment(...)` 用中文，与注释规范一致（见 [naming-and-layout.md](naming-and-layout.md)）。

## 注册

`registry/<ModId>Configs.java`，入口 `doRegister(ModContainer)`，按类型分别注册：

```java
public final class <ModId>Configs {
    public static void doRegister(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static void registerClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
```

客户端配置的注册调用要放在分端判断内（NeoForge 用 `dist.isClient()`，Forge 用 `DistExecutor`），避免服务端加载客户端配置类。

## 配置界面

**NeoForge 1.21.1** 的客户端配置项需同时注册配置界面工厂，使配置界面可用：

```java
modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
```

## Forge 1.20.1

- 保留 `ClientConfig`、`CommonConfig`、`ServerConfig` 三类，使用 `ForgeConfigSpec`，不增加 Startup 配置。
- `<ModId>Configs.doRegister()` 内通过 `ModLoadingContext.get().registerConfig(...)` 注册；客户端配置先检查物理客户端，可用 `FMLLoader.getDist().isClient()`。
- **不注册配置界面**，不移植 NeoForge 的 `IConfigScreenFactory` 或 `ConfigurationScreen`，也不额外添加界面依赖。
- `define(...)` 的键与 `ConfigLangEntry` 的名称保持一致，通过 `.translation(langEntry.key())` 关联。

## 配置项的语言

配置项的显示名走 datagen 语言流程，见 [datagen.md](datagen.md)。

## 完成前检查

- 配置类型（COMMON / CLIENT / SERVER）是否与该配置的生效范围一致？
- 客户端配置是否只在客户端注册？
- 配置键名是否稳定（改名会导致用户配置丢失）？