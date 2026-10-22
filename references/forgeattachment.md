# Forge 1.20.1 附件依赖（forgeattachment）

## 适用范围

Forge 1.20.1 的新工程通过 **forgeattachment** 依赖使用附件，不把 NeoForge 的附件类直接搬进来，也不重新实现一套 Capability 存储。已有 Capability 工程不因此强制迁移。

- 目标环境：Minecraft 1.20.1、Forge 47.3.4、Java 17。
- 构建插件：`net.neoforged.moddev.legacyforge`。
- forgeattachment 是**必需依赖**，不是可选兼容；无需包进 `compat/`。
- 注册入口与数据类仍按 [attachments.md](attachments.md) 的 `registry/<ModId>Attachments`、`registry/attachment/` 组织。
- 依赖提供实体、方块实体附件；不要据此假定区块、世界或 ItemStack 也支持附件。ItemStack 数据在这一世代用 NBT。

## 固定仓库与版本变量

**固定使用 sigh Maven，不换成其他镜像或本地 jar，不把依赖源码复制到下游工程。**

```properties
# gradle.properties
forgeattachment_version=0.0.1-1.20.1+build.0
```

```groovy
// build.gradle
repositories {
    maven {
        name = 'MafuyuNexus'
        url = 'https://maven.sighs.cc/repository/maven-public/'
    }
}

dependencies {
    modImplementation "io.zershyan:forgeattachment:${forgeattachment_version}"
}
```

完整坐标为 `io.zershyan:forgeattachment:0.0.1-1.20.1+build.0`。使用版本变量，但变量必须有明确版本值，不用 `+`、范围或 `latest.release`。这里的 `+build.0` 是版本号的一部分，不是动态版本通配符。

Forge 的 `META-INF/mods.toml` 同时声明必需依赖，不能使用 NeoForge 的 `type="required"`：

```toml
[[dependencies."<modid>"]]
modId="forgeattachment"
mandatory=true
versionRange="[0.0.1,)"
ordering="NONE"
side="BOTH"
```

不要额外打包一份依赖。运行环境需要同时安装下游模组与 forgeattachment。

## 关键导入

| 用途 | 类型 |
|---|---|
| 附件类型 | `io.zershyan.forgeattachment.attachment.AttachmentType` |
| 持有者接口 | `io.zershyan.forgeattachment.attachment.IAttachmentHolder` |
| 实体、方块实体便捷门面 | `io.zershyan.forgeattachment.api.ForgeAttachmentApi` |
| 注册表键 | `io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries.ATTACHMENT_TYPES_KEY` |
| 带注册表访问的缓冲区 | `io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf` |
| 流编解码器 | `io.zershyan.forgeattachment.network.codec.StreamCodec` |
| 基础流编解码器 | `io.zershyan.forgeattachment.network.codec.ByteBufCodecs` |
| 常用对象编解码器 | `io.zershyan.forgeattachment.api.Codecs`、`StreamCodecs` |

`FriendlyByteBuf` 仍使用 Minecraft 的 `net.minecraft.network.FriendlyByteBuf`。不要误导入 1.21.1 的 `net.minecraft.network.codec.StreamCodec` 或 `net.minecraft.network.RegistryFriendlyByteBuf`。

## 注册附件类型

```java
public class <ModId>Attachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(
            ForgeAttachmentRegistries.ATTACHMENT_TYPES_KEY, <ModId>.MODID);

    public static final RegistryObject<AttachmentType<<Feature>Data>> STATUS =
            REGISTER.register("status", <Feature>Data::createType);

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
```

主类只调用 `<ModId>Attachments.doRegister(modBus)`。Forge 使用 `net.minecraftforge.registries.DeferredRegister`、`RegistryObject` 和 `net.minecraftforge.eventbus.api.IEventBus`。

## 数据、持久化与同步编解码器

```java
public class <Feature>Data {
    public static final Codec<<Feature>Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("value").forGetter(<Feature>Data::getValue)
    ).apply(instance, <Feature>Data::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, <Feature>Data> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
            <Feature>Data::getValue,
            <Feature>Data::new
    );

    private int value;

    public <Feature>Data(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static AttachmentType<<Feature>Data> createType() {
        return AttachmentType.builder(() -> new <Feature>Data(0))
                .serialize(CODEC)
                .copyOnDeath()
                .sync(STREAM_CODEC)
                .build();
    }
}
```

三个能力**分别按需启用**，不是所有附件都必须同时启用：

- `serialize(CODEC)`：逻辑服务端保存、读取数据。
- `copyOnDeath()`：玩家死亡重生或生物转化时复制；**先配置序列化器再调用**。依赖已经处理复制事件，下游不要再重复抄一份 `PlayerEvent.Clone` 复制逻辑。
- `sync(STREAM_CODEC)`：配置服务端到客户端的同步，不替代服务端对客户端请求的校验。

可变数据的默认值必须每个 holder 新建，不能让多个玩家共享同一实例。要稳定扩展存档字段时，按实际兼容需求设计 Codec 默认值，不随意改名。

## 访问与修改

Minecraft 的实体类没有源码声明的 `getData(...)`，不能照搬 NeoForge 的 `player.getData(...)`。可先检查持有者接口：

```java
if (player instanceof IAttachmentHolder holder) {
    <Feature>Data data = holder.getData(<ModId>Attachments.STATUS);
    data.setValue(newValue);
    holder.syncData(<ModId>Attachments.STATUS);
}
```

或者使用便捷门面：

```java
<Feature>Data data = ForgeAttachmentApi.getData(player, <ModId>Attachments.STATUS.get());
if (data != null) {
    data.setValue(newValue);
    if (player instanceof IAttachmentHolder holder) {
        holder.syncData(<ModId>Attachments.STATUS);
    }
}
```

- `getData(type)`：不存在时创建并存入默认值；该创建会触发已配置的同步。
- `getExistingData(type)` / `getExistingDataOrNull(type)`：只读已有数据，不创建默认值。
- `setData(type, value)`：替换整个数据，返回旧值，并触发已配置的同步。
- `removeData(type)`：移除附件，并触发已配置的移除同步。
- `getData(...).setValue(...)` 等**原地修改不会自动被检测**，需要显式 `syncData(type)`。
- 便捷门面不支持该对象时返回 `null`，调用者必须处理。
- 方块实体数据修改后，除同步外还需要 `blockEntity.setChanged()`，保证区块保存。

## 同步边界

- 默认 `sync(STREAM_CODEC)` 会发给能接收该 holder 的相关客户端，不等于只发给数据所属玩家。私密数据需用带过滤器的 `sync(...)` 重载明确接收者。
- 依赖处理实体开始跟踪、区块开始观察等初始同步时机；不要假定客户端任意时刻都有数据，也不要把客户端缓存当权威。
- 服务端处理业务校验与数据变更，客户端只展示已收到的状态。
- 不要每 tick 无条件同步；只在数据变更或确有刷新需求时发包。
- 附件的流编解码器由依赖提供；模组自己的普通网络消息仍用 Forge `SimpleChannel`，见 [network.md](network.md)。

## 编解码器工具选择

常用类型优先复用依赖公开的 `Codecs` / `StreamCodecs`，例如 ItemStack、UUID、ResourceLocation、BlockPos、ChunkPos 与集合包装方法；基础数值可用 `Codec.INT` 与 `ByteBufCodecs`，多字段数据使用 `RecordCodecBuilder` / `StreamCodec.composite`。

涉及注册表引用的数据使用依赖的 `RegistryFriendlyByteBuf`。集合、字符串和 NBT 的网络解码应设置合适上限，不接受客户端无限制输入。

## 模板与验证

接入范式见 `templates/forge1.20.1/`。至少检查：

1. Maven 坐标、版本变量、`mods.toml` 必需依赖相互对应。
2. 附件注册由主类经 `doRegister` 挂到 mod 总线。
3. 重进世界后的持久化、死亡复制、跟踪开始与后续变更的同步。
4. 客户端缺少对象或数据时不崩溃，专用服务器不链接客户端类。
5. 执行 `./gradlew compileJava` 与 `./gradlew runData`；依赖下载失败必须如实记录，不把离线源码核对当作完整 Gradle 验证通过。
