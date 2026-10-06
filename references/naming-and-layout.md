# 包结构与命名

## 顶层包结构

顶层包结构如下：

```
io/zershyan/<modid>/
├── <ModId>.java        主类
├── api/                对外门面（按需创建，创建前须问用户）
├── client/             顶层客户端包
├── compat/             第三方模组兼容（按被兼容 modid 分子包）
├── datagen/
│   ├── lang/           语言数据源
│   ├── provider/       Provider
│   └── util/           辅助类型
├── handler/
│   ├── client/
│   └── common/
├── mixin/
├── registry/           所有注册相关内容
└── util/               工具（单数）
```

- **按功能分包，端再嵌套**：顶层是功能域，需要分端时在功能域下再嵌 `client`/`common`。
- **`util/` 用单数**，不用 `utils/`。mixin 接口固定在 `util/mixin/`。
- **`handler/{client,common}/`**：只放事件回调与业务逻辑，**不放注册**（注册一律在 `registry`）。

## 命名规范

- 类名：`UpperCamelCase`；包名：`lower_snake_case`（多词用下划线，如 `data_component`、`gag_style`）。
- 缩写词统一**首字母大写**：`Api`、`Id`、`Url`，不用 `API`、`ID`、`URL`。
- **按正确英文拼写**：`animation`（不是 `animtion`）、`ServerTickHandler`（不是 `ServerTIckHandler`）。

## 前缀规范

- **注册类**：启用混淆的项目用 `Mod*`（`ModItems`、`ModSounds`）；未启用混淆的用 `<ModId>*`（`ExampleModItems`、`DSItems`）。
- **Handler 类**：以 `Handler` 结尾，如 `KeyPressHandler`、`UntieLivingHandler`。
- **Provider 类**：`Mod*Provider`，如 `ModLangProvider`、`ModSoundProvider`。
- **Mixin 类**：`Mixin + 目标类名`，见 [mixin.md](mixin.md)。
- **Mixin 接口**：`IMixin + 目标类名`，见 [mixin.md](mixin.md)。

## 判断归属

| 内容 | 归属 |
|---|---|
| 注册项本身、注册项的属性/行为定义、注册时必需的数据 | `registry/` |
| 事件回调、业务逻辑 | `handler/{client,common}/` |
| 纯工具方法 | `util/` |
| 对外暴露的稳定能力 | `api/` |
| 第三方模组兼容 | `compat/<被兼容modid>/` |
| 数据生成源与 Provider | `datagen/{lang,provider,util}/` |

边界不清时**先问用户**，不要自行扩大 `registry` 范围。