# 命令

## 位置

命令类放 `registry/command/`，注册入口 `registry/<ModId>Commands.java`。

```
registry/
├── <ModId>Commands.java       doRegister(neoEventBus)
└── command/
    ├── ChatModeCommand.java
    └── exception/
        └── CommandComponentException.java
```

## 注册入口

```java
public final class <ModId>Commands {
    public static void doRegister(IEventBus neoBus) {
        neoBus.addListener(<ModId>Commands::commonCommandRegister);
    }

    public static void commonCommandRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("<modid>");
        ChatModeCommand.register(builder);
        event.getDispatcher().register(builder);
    }
}
```

- 命令注册监听挂在 forge / neo 总线上（`RegisterCommandsEvent`）。
- 根命令字面量用 modid 或功能名。

## 子命令

每个子命令一个类，提供 `static register(builder)` 供 `<ModId>Commands` 调用：

```java
public final class ChatModeCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(literal("chatmode")
                .then(argument("mode", StringArgumentType.word())
                        .executes(ChatModeCommand::execute)));
    }
}
```

## 命令消息

命令的反馈消息用可翻译组件，语言键走 datagen，见 [datagen.md](datagen.md)。

## 完成前检查

- 命令注册是否挂在正确的总线（`RegisterCommandsEvent` 在 forge / neo 总线）？
- 权限要求是否与命令的危险程度匹配？
- 参数类型与执行线程是否正确？
- 消息是否可本地化，而不是硬编码字符串？