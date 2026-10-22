# 网络

## 位置

payload 类放 **`registry/packet/`**，注册类 **`registry/<ModId>Packets.java`**，与「注册相关内容全在 registry」一致。

```
registry/
├── <ModId>Packets.java      doRegister(modEventBus)
└── packet/
    ├── SwitchChatModePacket.java
    ├── MarkUntieEntityPacket.java
    └── struggle_game/           ← 相关包可再分子包
        ├── BreakFreePacket.java
        └── AppendArrowsPacket.java
```

## 加载器分代

- NeoForge 1.21.1 使用以下 `CustomPacketPayload` / `StreamCodec` / `PayloadRegistrar` 范式。
- Forge 1.20.1 使用文末的 `SimpleChannel` 范式，不引入这些新版网络 API。

## payload 写法

用 `record` 实现 `CustomPacketPayload`，声明 `TYPE` 与 `STREAM_CODEC` 两个静态常量：

```java
public record RidePlayerData(UUID uuid) implements CustomPacketPayload {
    public static final Type<RidePlayerData> TYPE = new Type<>(<ModId>.id("ride_player"));

    public static final StreamCodec<ByteBuf, RidePlayerData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RidePlayerData::uuid,
            RidePlayerData::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- `TYPE` 用 `id(...)` 构造，资源路径用 `lower_snake_case`。
- `STREAM_CODEC` 用 `StreamCodec.composite`，字段顺序与 record 分量一致。

## 注册

```java
package io.zershyan.<modid>.registry;

import io.zershyan.<modid>.<ModId>;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;

public class <ModId>Packets {
    /** 协议版本取本 mod 版本（含构建数），不硬编码；两端不一致时拒绝连接 */
    @NotNull
    private static final String PROTOCOL_VERSION = ModList.get()
            .getModContainerById(<ModId>.MODID)
            .map(ModContainer::getModInfo)
            .map(IModInfo::getVersion)
            .map(Object::toString)
            .orElse("unknown");

    public static void doRegister(IEventBus modEventBus) {
        modEventBus.addListener(<ModId>Packets::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // 发到服务端
        registrar.playToServer(AaaPacket.TYPE, AaaPacket.STREAM_CODEC, AaaPacket::handle);
        registrar.playToServer(BbbPacket.TYPE, BbbPacket.STREAM_CODEC, BbbPacket::handle);

        // 发到客户端
        registrar.playToClient(CccPacket.TYPE, CccPacket.STREAM_CODEC, CccPacket::handle);
    }
}
```

- 处理器写在 payload 自身（`XxxPacket::handle`），见下「处理器」。
- 兼容层的 payload 要在同一处接进来（见 [compat.md](compat.md)）：

```java
private static void register(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
    registrar.playToServer(...);
    registrar.playToClient(...);
    // 兼容层的 payload 由工厂统一注册
    <ModId>CompatFactory.registerNetwork(registrar);
}
```

### 协议版本

**不要写 `event.registrar("1")` 这类硬编码字符串。** 协议版本必须取 **mod 自身的版本号**：payload 的格式会随构建变化，硬编码的固定值无法在格式变更时阻止新旧端互连，会导致解析错误甚至崩溃。

- `PROTOCOL_VERSION` 用 `ModList.get().getModContainerById(MODID)` 取本 mod 的元数据，再取 `IModInfo::getVersion` 的字符串形式。
- 取不到时回退 `"unknown"`（正常加载不会走到，但不能留 null）。
- 加 `@NotNull` 标注，避免 IDE 空值告警。
- 按版本号规约，包内版本恒带构建数（`0.0.1-1.21.1+build.2`），因此**协议版本天然随每次构建变化**。这是刻意的：payload 格式随构建变动，宁可拒绝连接也不要解析出错。两端构建号不一致时，NeoForge 会以版本不匹配为由拒绝连接。
- 需要放宽时（如只在小版本内互通），改用 `mod_version` 而非完整版本号，但**不要退回硬编码的 `"1"`**。

## 处理器

处理器**写在 payload 自身**，是 `handle(IPayloadContext)` 实例方法，注册时用 `XxxPacket::handle`：

```java
public record SwitchChatModePacket() implements CustomPacketPayload {
    public static final Type<SwitchChatModePacket> TYPE = new Type<>(<ModId>.id("switch_chat_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchChatModePacket> STREAM_CODEC =
            StreamCodec.unit(new SwitchChatModePacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ChatModeCommand.switchChatMode(context.player().createCommandSourceStack()));
    }
}
```

要点：

- 方法签名必须是 `public void handle(IPayloadContext context)`，**不要**写 `static handle(Payload payload, IPayloadContext context)` 这种把 payload 重复传参的形式 —— payload 自己就是 `this`，用实例访问器直接读字段。
- 业务逻辑较重时，`handle` 内委托给领域方法（上例委托给 `ChatModeCommand`），不要把逻辑堆在 payload 里。
- 所有游戏状态读写都要包在 `context.enqueueWork(...)` 内，切到主线程执行。
- 客户端专属的处理器（`playToClient` 的）可以直接引用客户端类，因为该方法只在客户端执行。
- 服务端侧的处理器必须**重新校验**客户端提交的一切，见「安全边界」。

带字段与校验的完整例子：

```java
public record ExampleRequestPacket(int requestId) implements CustomPacketPayload {
    // ... TYPE / STREAM_CODEC / type() 同上 ...

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            // 服务端基于自身状态重新校验，不信任客户端字段
            if (requestId < 0) {
                <ModId>.LOGGER.warn("Rejected request with negative id {}", requestId);
                return;
            }
            int value = player.getData(<ModId>Attachments.EXAMPLE_STATUS.get()).getValue();
            PacketDistributor.sendToPlayer(player, new ExampleSyncPacket(value));
        });
    }
}
```

## 发送

用 `PacketDistributor`：`sendToPlayer`、`sendToServer`、`sendToAllPlayers` 等。

## 安全边界

服务端必须基于服务器状态重新校验权限、目标、数量、距离与状态，**不信任客户端字段**。

## 完成前检查

- Codec 与 record 分量是否一致？
- 方向（toClient / toServer）是否正确？
- 客户端专属处理器是否只在客户端加载？
- 服务端是否重新校验了客户端提交的数据？

## Forge 1.20.1：SimpleChannel

`registry/<ModId>Packets` 使用 `net.minecraftforge.network.simple.SimpleChannel`，注册入口为 `doRegister()`；协议版本仍取本模组完整元数据版本，客户端和服务端版本检查都用 `PROTOCOL_VERSION::equals`。

```java
public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        <ModId>.id(<ModId>.MODID), () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

public static void doRegister() {
    INSTANCE.messageBuilder(<Feature>RequestPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .decoder(<Feature>RequestPacket::new)
            .encoder(<Feature>RequestPacket::encode)
            .consumerMainThread(<Feature>RequestPacket::handle)
            .add();
    <ModId>CompatFactory.registerNetwork(INSTANCE);
}
```

多个消息需使用互不重复、顺序稳定的编号；兼容层注册同一 channel 时也不能复用已有编号。

- 每个包保留自身的 `encode(FriendlyByteBuf)`、解码构造器与 `handle(Supplier<NetworkEvent.Context>)`，不拆出公共包处理器。
- 上述上下文是 `net.minecraftforge.network.NetworkEvent.Context`，**不是**后续版本的 `CustomPayloadEvent.Context`。
- `consumerMainThread` 已调度到主线程并标记消息已处理，包内不再重复 `enqueueWork`。若改用网络线程消费者，必须自行调度和标记。
- 服务端 `supplier.get().getSender()` 可能为 `null`，必须检查，随后按服务器真实状态验证请求。
- 发到服务端：`INSTANCE.sendToServer(message)`。
- 发给指定玩家：`INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message)`，其中 `PacketDistributor` 来自 `net.minecraftforge.network`。
- forgeattachment 自己的附件流编解码器不改变本模组普通消息的 SimpleChannel 方案。