# Mixin 与 MixinPlugin

## refmap

**NeoForge 不需要 `refmap`**，NeoForge 内部会自动转换，不要写 `refmap` 字段。

Forge 的 refmap 不属于本 skill 规范范围，不处理。

## 必须使用 MixinPlugin

`<ModId>MixinPlugin extends IMixinConfigPlugin`，核心是重写 `shouldApplyMixin`：

```java
/**
 * mixin软件包下其他Mixin类请严格按照 "modid/modid/.../MixinXXX.class"来命名<br><br>
 * 程序会遍历软件包名，检查是否加载了modid与软件包名一致的mod<br>若无则不会加载该软件包的所有mixinClass<br><br>
 * 若遍历到的软件包名为client/server/common中的任意一个<br>其软件包内的所有mixinClass都会生效
 */
@Override
public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    List<ModInfo> modInfos = LoadingModList.get().getMods();
    List<String> modList = modInfos.stream().map(ModInfo::getModId).toList();
    String modIds = mixinClassName.replace(this.getClass().getPackageName() + ".", "").replaceAll("^(.*)(\\.).*$", "$1");
    for (String string : modIds.split("\\.")) {
        if ("client".equals(string) || "server".equals(string) || "common".equals(string)) {
            return true;
        } else if (!modList.contains(string)) return false;
    }
    return true;
}
```

该逻辑的语义：

- mixin 类包名中，`client` / `server` / `common` 段直接放行（本 mod 自己的 mixin）。
- 其余包名段被当作 modid 查 `LoadingModList`：**对应 mod 未安装则该包下所有 mixin 都不加载**。

## 包名规则

**以 modid 作为包名**，可嵌套实现多 mod 兼容。

```
mixin/
├── <本modid>/
│   ├── client/MixinMinecraft.java
│   └── common/MixinLivingEntity.java
├── <目标modid>/MixinXxx.java
└── <ModId>MixinPlugin.java
```

**多 mod 嵌套示例**：已装 ModA、又装了 ModB，要 mixin ModB → 包路径为

```
io.zershyan.<本modid>.mixin.moda.modb.MixinXXX
```

## 类命名

必须 `Mixin + 目标类名`：mixin `ServerPlayer` → `MixinServerPlayer`。

## Mixin 接口

- 统一放 `util/mixin/` 下，命名 `IMixin + 目标类名`。
- **必须与 mixin 类的包结构保持一致**。
- 为多 mod 兼容而写的接口，放 `util/mixin/` 下对应 mod 的子包：如 `util/mixin/playeranimator/IMixinXxx`。

## mixins.json

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "io.zershyan.<modid>.mixin",
  "compatibilityLevel": "JAVA_21",
  "plugin": "io.zershyan.<modid>.mixin.<ModId>MixinPlugin",
  "mixins": ["<modid>.common.MixinLivingEntity"],
  "client": ["<modid>.client.MixinMinecraft"],
  "injectors": { "defaultRequire": 1 },
  "overwrites": { "requireAnnotations": true }
}
```

不要写 `refmap`。

## 职责边界

Mixin 只负责精确开洞、桥接或传递结果，复杂玩法放到事件或领域逻辑（`handler/`）。