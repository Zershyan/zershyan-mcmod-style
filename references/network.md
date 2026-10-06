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
public final class <ModId>Packets {
    public static void doRegister(IEventBus modEventBus) {
        modEventBus.addListener(<ModId>Packets::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(RidePlayerData.TYPE, RidePlayerData.STREAM_CODEC, ServerPayloadHandler::ridePlayer);
        registrar.playToClient(SoundData.TYPE, SoundData.STREAM_CODEC, ClientPayloadHandler::playSound);
    }
}
```

- 协议版本用 mod 版本：`ModList.get().getModContainerById(MODID).map(ModContainer::getModInfo).map(IModInfo::getVersion).map(Object::toString).orElse("unknown")`。
- 处理器按端分文件：`ServerPayloadHandler` / `ClientPayloadHandler`。

## 处理器

处理器内使用 payload 自身的实例访问器读取字段，不要写 `static handle(Payload payload, IPayloadContext)` 这种重复传参形式。

## 发送

用 `PacketDistributor`：`sendToPlayer`、`sendToServer`、`sendToAllPlayers` 等。

## 安全边界

服务端必须基于服务器状态重新校验权限、目标、数量、距离与状态，**不信任客户端字段**。

## 完成前检查

- Codec 与 record 分量是否一致？
- 方向（toClient / toServer）是否正确？
- 客户端专属处理器是否只在客户端加载？
- 服务端是否重新校验了客户端提交的数据？