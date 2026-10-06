---
name: zershyan-mcmod-style
description: 用于在 io.zershyan 系列 Minecraft 模组仓库中编码、改造、构建或排查问题，覆盖该系列自有的包结构、注册入口、mixin 组织、命名规则与构建配置。当任务涉及 Zershyan 名下的模组工程、需要遵循其既有范式，或需要在这些仓库间保持一致时使用；通用 NeoForge API 问题用 neoforge-mod-style。
---

# Zershyan 模组工程规范

本 skill 承载 io.zershyan 系列模组仓库自己的约定，即无法从 NeoForge 官方文档推出的项目事实。通用 NeoForge / Java API 用法见 `neoforge-mod-style`；仓库清单（远程链接、modid、MC 版本、分支）见 `zershyan-mc-mods/AGENTS.md`。

## 使用契约

1. **先确认项目事实**：读目标仓库的 `gradle.properties`、`build.gradle`、模组元数据和现有代码，不凭记忆推断。
2. **按主题取文档**：从 [references/index.md](references/index.md) 按当前任务选文档，不一次性读完全部。
3. **未收录内容必须先问用户**：遇到本 skill 没有覆盖、或你没有把握的情况，先向用户提问，不要自行决定。
4. **范式不统一是常态**：各仓库存在真实差异。遇到与本 skill 不符的仓库，以该仓库实际代码为准，并把它当作需要修正本 skill 的信号。

## 全局不可违反的边界

- **注册相关内容一律放 `registry` 包**，即使其注册方式是事件（如 overlay 用 `<ModId>Overlays` + `doRegister`，而非 `@EventBusSubscriber`）。
- **注册入口方法名统一 `doRegister`**。
- **`api` 包不被其他包依赖**：需要 api 功能时由 api 调用其他包，反向依赖不允许。
- **能由 datagen 生成的资源必须走 datagen**，不手写资源文件。
- **可选依赖的调用必须走 `compat/` 包**，所有第三方 API 调用包在 `testLoadedAndRun` / `testLoadedAndCall` 内，依赖缺失时模组仍要能启动（见 [references/compat.md](references/compat.md)）。
- **不要擅自启用混淆**，也不要擅自引入本 skill 未列出的依赖或工具。

## 决策前必须询问用户的事项

以下情况一律先问，不要自行决定：

- 新建 mod 时的**目标 MC 版本**（未指定时）。
- 新建 mod 时**是否需要 `api` 包**。
- 遇到本 skill **未列举的注册类型**，判断其是否应写入 `registry`。
- 遇到本 skill **未覆盖的任何规范点**。

## 按任务加载

见 [references/index.md](references/index.md)。

## 工程模板

`templates/neoforge1.21.1/` 是可编译运行的完整 NeoForge 1.21.1 工程（modid `examplemod`）。新建同世代项目时复制它再改 modid 与包名，详见 [references/build.md](references/build.md)。