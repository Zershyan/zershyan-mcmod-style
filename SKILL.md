---
name: zershyan-mcmod-style
description: 为 Zershyan 的 Minecraft 模组工程提供包结构、注册入口、网络、配置、Mixin、Datagen 与构建约定。用于 io.zershyan 系列模组的开发、迁移、排错、代码审查及工程模板维护；先确认目标版本，再按主题加载参考文档。
---

# Zershyan 模组工程规范

本 skill 维护该系列模组的项目约定与可复用模板，不替代加载器文档。通用 API 按目标 Minecraft / 加载器版本查官方文档及依赖源码，不依赖其他 skill。

## 工作顺序

1. **确认项目事实**：先读工作区指令、目标仓库的 `gradle.properties`、`build.gradle`、模组元数据与相关代码。仓库清单见外部工作区的 `zershyan-mc-mods/AGENTS.md`（若提供），不假定它随本 skill 安装。
2. **按需阅读**：从 [主题索引](references/index.md) 选择主文档；只在任务跨主题时补读，不全量加载参考文档。
3. **区分规则与版本实现**：本文示例主要覆盖 Forge 1.20.1、NeoForge 1.21.1。其他版本先核实构造器、事件、映射和构建接口，不能直接套用。
4. **尊重已有工程**：仓库实现与本规范有差异时，先核对原因与迁移范围，不擅自统一旧代码；发现规范过时则同步修正。

## 核心约定

- **注册**：注册入口、注册项及注册时必需的数据归 `registry/`，入口统一 `doRegister`；事件驱动的注册也不例外。见 [注册](references/registry.md)。
- **分包与命名**：按功能分包，端侧按需嵌套；工具包用 `util/`。使用非注册事件的类以 `Handler` 结尾，普通领域逻辑不强行归入 handler。见 [包结构](references/naming-and-layout.md)。
- **API 调用**：任意包可调用本体 API（`api/`）；兼容 API 在 `compat/<modid>/api/`，只对本 compat 可见；本体调用 compat 一律按软调用处理。见 [对外 API](references/api.md)。
- **资源**：可生成的资源必须走 Datagen，修改声明源或 Provider，不直接改生成结果。见 [Datagen](references/datagen.md)。
- **版本与产物**：`mod_version` 只含模组版本；包内版本恒含 MC 版本与 `+build.N`。普通 `build` 沿用编号，发布 jar 名不带编号；`buildAlpha` 使用下一编号，成功后回写。见 [版本号](references/versioning.md)。
- **可选兼容**：第三方可选依赖的调用收在 `compat/`，经 `testLoadedAndRun` / `testLoadedAndCall` 检查，并隔离实际 API 引用；缺少依赖仍须能启动。本体可调用 compat，但须软调用（本体侧不出现第三方可选依赖类型，缺失依赖不崩溃）；各 compat 子包互不引用。见 [兼容](references/compat.md)。
- **ForgeAttachment 按需引入**：不用附件就不添加依赖；使用后声明为必需依赖，主动告知用户用途、安装要求与 GitHub / MC百科 / CurseForge 项目链接或未发布状态。仓库、坐标和链接统一维护在 [ForgeAttachment](references/forgeattachment.md)。Forge 1.20.1 的 ItemStack 数据用 NBT，不建立 DataComponents。
- **不擅自扩展工具链**：未经用户要求不启用混淆，不引入本 skill 未列出的依赖或工具。

## 需要用户决定

- 新建模组的目标 MC 版本（未指定时），以及是否需要 `api/` 包（尚未明确时）。
- 未列举注册类型的归属，或其他尚未覆盖、无法从现有约定确定的项目规范。

通用 API 疑问先查目标版本资料；项目规范的缺口再向用户确认，不猜测约定。

## 工程模板

| 目标 | 模板 | 技术基线 |
|---|---|---|
| Forge 1.20.1 | `templates/forge1.20.1/` | Java 17、LegacyForge、SimpleChannel、NBT |
| NeoForge 1.21.1 | `templates/neoforge1.21.1/` | Java 21、ModDevGradle、CustomPacketPayload |

模板的 modid 为 `examplemod`，用来演示常见结构，不是新模组的必选功能清单。复制后替换标识并裁剪不用的代码；Forge 模板含附件演示，无附件需求时必须连同依赖配置一起裁剪。创建步骤见 [构建与发布](references/build.md)。

## 维护与验证

规则影响代码时同步模板；完整实现优先链接模板，不在多份文档中复制。模板改动后运行 `compileJava`、`runData`；构建或打包改动还要检查产物。只记录实际执行的验证，不把历史通过或静态核对当作本轮测试结果。