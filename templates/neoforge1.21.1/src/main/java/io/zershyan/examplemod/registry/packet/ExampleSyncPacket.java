package io.zershyan.examplemod.registry.packet;

import io.zershyan.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 服务端 → 客户端：同步状态 */
public record ExampleSyncPacket(int value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExampleSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ExampleMod.id("example_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExampleSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExampleSyncPacket::value,
            ExampleSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
