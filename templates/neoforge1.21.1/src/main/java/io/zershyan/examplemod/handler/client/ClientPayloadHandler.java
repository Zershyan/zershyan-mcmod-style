package io.zershyan.examplemod.handler.client;

import io.zershyan.examplemod.registry.packet.ExampleSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {

    private ClientPayloadHandler() {
    }

    /** 接收服务端同步的状态；客户端只做展示 */
    public static void handleSync(ExampleSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer player = minecraft.player;
            if (level == null || player == null) return;
            ClientState.setValue(packet.value());
        });
    }
}
