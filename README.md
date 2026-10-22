# zershyan-mcmod-style

一个 Agent Skill，记录了 [Zershyan](https://github.com/Zershyan) 名下 Minecraft 模组工程的**自有约定**：包结构与命名、注册入口、网络与配置写法、Mixin 组织、版本号与打包流程、构建脚本骨架等。这些内容无法从 NeoForge 官方文档推导，属于项目事实。

通用 NeoForge / Forge API 用法不在本仓库范围内。

## 目录结构

| 路径 | 说明 |
|---|---|
| `SKILL.md` | Skill 入口：使用契约、全局不可违反的边界、必须询问用户的事项 |
| `references/` | 按主题拆分的规范文档，路由见 [references/index.md](references/index.md) |
| `templates/neoforge1.21.1/` | NeoForge 1.21.1 完整工程模板，已实测 `compileJava` 与 `runData` |
| `templates/forge1.20.1/` | Forge 47.3.4 / Java 17（LegacyForge）完整工程模板，已实测编译、datagen 与打包 |

两套模板的 modid 均为占位用的 `examplemod`，新建同世代项目时复制对应模板，再改 modid 与包名。

## 维护约定

- 规范变更后**模板必须跟着改**，并重新跑 `compileJava` 与 `runData` 验证，两者不允许不一致。
- 遇到与规范不符的仓库，以该仓库实际代码为准，同时把它当作修正本规范的信号。
