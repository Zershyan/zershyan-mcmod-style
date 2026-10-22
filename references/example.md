# 示例代码

## 默认不写

**一般情况下不需要写 example，除非用户额外要求。**

## 启用条件

写 example 时，要求**开发环境与开关同时满足**：

```java
if (!FMLEnvironment.production && StartupConfig.enableExample.get()) {
    ExampleHandler.doRegister(modEventBus, neoEventBus);
}
```

- 开发环境判断用 `!FMLEnvironment.production`（`FMLEnvironment` 只有 `public static final boolean production` 字段，**没有** `isProduction()` 方法）。
- 开关来自 `gradle.properties`，如 `enableExample=true`。
- 两者缺一不可：避免示例代码进入正式包，也便于单独关闭。

## 组织

`example/` 包内可包含自己的完整子结构，包括它自己的 registry：

```
example/
├── ExampleHandler.java
└── registry/
    ├── ExampleAttackEffects.java
    └── atkEffect/
        ├── QuickAttackEffect.java
        └── RepeatAttackEffect.java
```

- example 内的注册类同样用 `doRegister`。
- example 的注册调用由主类在开关内触发，不进入正常注册链。

## 完成前检查

- 是否同时判断了开发环境与开关？
- 示例内容是否完全隔离在 `example/` 包内？
- 正式构建中示例代码是否不会被注册？