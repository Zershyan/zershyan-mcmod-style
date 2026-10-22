# 参考文档路由索引

本文件只维护路由元数据，不重复主题规范。先读 `SKILL.md`，再从这里选文档。

## 加载协议

- **primary**：当前主题的唯一主文档。
- **companion**：任务明确跨越边界时才加载。
- 默认只读 1 个 primary；不确定主题是否存在时，直接读目标仓库现有代码。

## 主题路由

| 主题 | 触发关键词 | primary | companion |
|---|---|---|---|
| 主类与双端 | `@Mod`、主类、`Dist`、`DistExecutor`、客户端隔离、构造器 | [architecture.md](architecture.md) | [registry.md](registry.md) |
| 包结构与命名 | package、目录、命名、拼写、`util`、`handler`、`client` | [naming-and-layout.md](naming-and-layout.md) | [architecture.md](architecture.md) |
| 注册与 registry | 注册、`doRegister`、registry、注册项、overlay、sound、item、block | [registry.md](registry.md) | [naming-and-layout.md](naming-and-layout.md) |
| Mixin 与 MixinPlugin | Mixin、`MixinPlugin`、`shouldApplyMixin`、`IMixinConfigPlugin`、兼容 mixin、`IMixin` | [mixin.md](mixin.md) | [registry.md](registry.md) |
| 网络 | payload、`CustomPacketPayload`、`StreamCodec`、packet、`<ModId>Packets` | [network.md](network.md) | [registry.md](registry.md) |
| 配置 | config、`ModConfigSpec`、`<ModId>Configs`、ClientConfig、ServerConfig | [config.md](config.md) | [registry.md](registry.md) |
| 附件与能力 | Attachment、Capability、`<ModId>Attachments`、`<ModId>Capabilities` | [attachments.md](attachments.md) | [registry.md](registry.md) |
| Forge 附件依赖 | forgeattachment、ForgeAttachmentApi、ForgeAttachmentRegistries、Forge 1.20.1 附件、sigh Maven | [forgeattachment.md](forgeattachment.md) | [attachments.md](attachments.md)、[network.md](network.md) |
| 第三方模组兼容 | compat、可选依赖、`ICompatUtils`、`CompatFactory`、软兼容、`ModList.isLoaded`、类链接 | [compat.md](compat.md) | [registry.md](registry.md)、[mixin.md](mixin.md)（兼容 mixin 时） |
| 日志与工具 | 日志、`LOGGER`、`id()`、`ResourceLocation`、util 工具 | [logging-and-util.md](logging-and-util.md) | [naming-and-layout.md](naming-and-layout.md) |
| 构建与发布 | build.gradle、moddev、运行配置、Proguard、混淆、发布、新建项目、工程模板 | [build.md](build.md) | [naming-and-layout.md](naming-and-layout.md) |
| 版本号与打包 | 版本号、`mod_version`、`mod_build_number`、`buildAlpha`、`finalizeArtifacts`、jar 命名 | [versioning.md](versioning.md) | [build.md](build.md)（启用混淆时） |
| Datagen 与资源 | datagen、Provider、`GatherDataEvent`、语言、模型、声音、标签、资源生成 | [datagen.md](datagen.md) | [naming-and-layout.md](naming-and-layout.md) |
| 对外 API | api、门面、`XxxApi`、helper、events | [api.md](api.md) | [naming-and-layout.md](naming-and-layout.md) |
| 示例代码 | example、`isProduction`、示例开关 | [example.md](example.md) | [architecture.md](architecture.md) |

## 常见组合路由

| 任务 | 读取顺序 |
|---|---|
| 新建一个 mod | `build.md`（按版本选 NeoForge 或 Forge 模板）→ `naming-and-layout.md` → `architecture.md` → `registry.md`；Forge 1.20.1 再读 `forgeattachment.md` |
| 新增注册项（物品/方块/声音等） | `registry.md` → `datagen.md`（需要资源时） |
| 新增网络包 | `network.md` → `registry.md` |
| 新增命令 | `commands.md` → `registry.md` |
| 为第三方 mod 加兼容 mixin | `mixin.md` → `compat.md` → `registry.md` |
| 新增一个可选依赖兼容 | `compat.md` → `registry.md`（兼容层自带注册时） |
| 排查服务端客户端类缺失 | `architecture.md` → `mixin.md` |

## 维护约定

新增主题：在 `references/` 下建文档，再在表中加一行。文档名用小写连字符，与主题一一对应。链接保持一层深，不要嵌套引用。