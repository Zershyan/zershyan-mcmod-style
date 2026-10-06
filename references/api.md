# 对外 API

## 创建前必须询问

**每个 mod 创建时必须明确询问用户是否需要 `api` 包**，不需要则不创建。

## 位置与命名

`api/<ModName>Api.java`，命名用 `Api`（不是 `API`）。

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

- **`api` 包不被其他包依赖**。
- 若需使用 api 的功能，由 api 调用其他包的逻辑；**不允许其他包依赖 api 包**。
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

api 中的客户端专属方法用 `@OnlyIn(Dist.CLIENT)` 标注，且只在客户端调用路径上可达：

```java
@OnlyIn(Dist.CLIENT)
public static float clientGetPartialTick() {
    return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
}
```

## Javadoc

对外 API 需要 Javadoc，用中文（见注释规范）。标注 `@param` / `@return`。

## 完成前检查

- api 是否被其他包反向依赖？
- api 是否引入了不必要的内部依赖？
- 客户端方法是否标注了 `@OnlyIn(Dist.CLIENT)`？
- 公开方法签名是否稳定（改签名会破坏下游 mod）？