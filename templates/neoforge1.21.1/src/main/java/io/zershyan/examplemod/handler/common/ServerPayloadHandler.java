package io.zershyan.examplemod.handler.common;

import io.zershyan.examplemod.ExampleMod;
import io.zershyan.examplemod.registry.ExampleModAttachments;
import io.zershyan.examplemod.registry.packet.ExampleRequestPacket;
import io.zershyan.examplemod.registry.packet.ExampleSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ServerPayloadHandler.class);

    private ServerPayloadHandler() {
    }

    /** 处理客户端请求；服务端自行校验，不信任客户端字段 */
    public static void handleRequest(ExampleRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            int requestId = packet.requestId();
            if (requestId < 0) {
                LOGGER.warn("Rejected request with negative id {} from {}", requestId, player.getGameProfile().getName());
                return;
            }
            int value = player.getData(ExampleModAttachments.EXAMPLE_STATUS.get()).getValue();
            PacketDistributor.sendToPlayer(player, new ExampleSyncPacket(value));
        });
    }

    /** 服务端 → 服务端：把状态推给指定玩家 */
    public static void syncTo(ServerPlayer player) {
        int value = player.getData(ExampleModAttachments.EXAMPLE_STATUS.get()).getValue();
        PacketDistributor.sendToPlayer(player, new ExampleSyncPacket(value));
    }

    static {
        LOGGER.debug("{} loaded", ExampleMod.MODID);
    }
}
