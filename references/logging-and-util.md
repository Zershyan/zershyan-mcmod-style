# 日志与工具

## 日志

变量名统一 `LOGGER`，实现统一 `LoggerFactory.getLogger(Xxx.class)`。

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class <ModId> {
    public static final String MODID = "<modid>";
    public static final Logger LOGGER = LoggerFactory.getLogger(<ModId>.class);
}
```

**为什么不用 `LogUtils.getLogger()`**：它靠反射调用栈取类名，重构或包装后可能取错，且绑定 Minecraft API。`LoggerFactory.getLogger(Xxx.class)` 显式传类，是标准 SLF4J 用法，重构安全。

**为什么变量名用 `LOGGER`**：`log` 不符合常量命名惯例，`LOG` 语义过窄；`LOGGER` 是该系列既有约定。

可见性：

- 主类：`public static final Logger LOGGER`，供全项目引用。
- 其他类：各自声明 `private static final Logger LOGGER`，用本类 class。

```java
public final class <ModId>Commands {
    private static final Logger LOGGER = LoggerFactory.getLogger(<ModId>Commands.class);

    public static void doRegister(IEventBus bus) {
        bus.addListener(<ModId>Commands::commonCommandRegister);
    }
}
```

## id() 工具

主类内提供，用于构造本 mod 的 `ResourceLocation`：

```java
public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
}
```

- 调用形式：`<ModId>.id("ride_player")`。
- 路径用 `lower_snake_case`。
- 不要在其他类里重复定义 `MODID` 常量；需要时引用主类的 `MODID`。

## 工具包

- 统一 `util/`（单数），不用 `utils/`。
- mixin 接口固定在 `util/mixin/`，见 [mixin.md](mixin.md)。
- 通用工具类按用途分子包，如 `util/data/`。

## 完成前检查

- 是否用了 `LoggerFactory` 而非 `LogUtils`？
- 变量名是否为 `LOGGER`？
- 是否重复定义了 `MODID`？