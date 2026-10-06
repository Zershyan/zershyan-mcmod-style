package io.zershyan.examplemod.registry.packet;

import io.zershyan.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端 → 服务端：请求服务端下发状态 */
public record ExampleRequestPacket(int requestId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExampleRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ExampleMod.id("example_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExampleRequestPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExampleRequestPacket::requestId,
            ExampleRequestPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
