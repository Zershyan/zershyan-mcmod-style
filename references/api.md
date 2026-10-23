# 对外 API

## 创建前必须询问

**每个 mod 创建时必须明确询问用户是否需要 `api` 包**，不需要则不创建。

## 位置与命名

`api/<ModId>Api.java`，命名用 `Api`（不是 `API`），与主类名前缀一致。

## 分层

**按需分层**：简单 mod 只一个 `XxxApi.java`，复杂 mod 才建 `helper/` 与 `events/` 子包。

```
// 简单
api/<ModId>Api.java

// 复杂
api/
├── <ModId>Api.java
├── helper/
│   ├── <Feature>Helper.java
│   └── <Feature>Helper.java
└── events/
    └── <Feature>Event.java
```

## 依赖方向（硬性）

- **本模组内部其他包不得依赖 `api` 包**；外部模组通过 API 调用不在此禁令内。
- API 门面调用内部领域实现；内部代码复用该实现，不反向绕回 API。
- `api` 内容需**尽量少依赖其他包**，因为它是对外的稳定面。

```
允许：  api → handler / registry / util
禁止：  handler / registry / util → api
```

## 写法

静态门面（用 `Api` 后缀）：

```java
public class <ModId>Api {
    public static final String MODID = <ModId>.MODID;
}
```

实例包装：

```java
public class <ModId>Api {
    private final Entity entity;

    <ModId>Api(Entity entity) {
        this.entity = entity;
    }

    public static <ModId>Api get(Entity entity) {
        return new <ModId>Api(entity);
    }
}
```

## 客户端方法

API 的客户端专属方法按项目约定标注 `@OnlyIn(Dist.CLIENT)`，并只在客户端路径调用。注解不能替代实际类加载隔离；不能在通用静态初始化中引用 Minecraft 等客户端类。以下计时 API 仅对应 NeoForge 1.21.1，其他版本核对签名：

```java
@OnlyIn(Dist.CLIENT)
public static float clientGetPartialTick() {
    return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
}
```

## Javadoc

对外 API 使用简明中文 Javadoc，按需标注 `@param` / `@return`，说明调用端、前置条件和失败结果，不重复方法名。

## 完成前检查

- api 是否被其他包反向依赖？
- api 是否引入了不必要的内部依赖？
- 客户端方法是否标注了 `@OnlyIn(Dist.CLIENT)`？
- 公开方法签名是否稳定（改签名会破坏下游 mod）？