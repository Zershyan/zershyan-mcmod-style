package io.zershyan.examplemod.registry;

import io.zershyan.examplemod.ExampleMod;
import io.zershyan.examplemod.compat.ExampleModCompatFactory;
import io.zershyan.examplemod.handler.client.ClientPayloadHandler;
import io.zershyan.examplemod.handler.common.ServerPayloadHandler;
import io.zershyan.examplemod.registry.packet.ExampleRequestPacket;
import io.zershyan.examplemod.registry.packet.ExampleSyncPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.language.IModInfo;

public final class ExampleModPackets {
    /** 协议版本跟随 mod 版本，两侧不一致时拒绝连接 */
    public static final String PROTOCOL_VERSION = ModList.get()
            .getModContainerById(ExampleMod.MODID)
            .map(ModContainer::getModInfo)
            .map(IModInfo::getVersion)
            .map(Object::toString)
            .orElse("unknown");

    public static void doRegister(IEventBus modEventBus) {
        modEventBus.addListener(ExampleModPackets::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ExampleRequestPacket.TYPE, ExampleRequestPacket.STREAM_CODEC, ServerPayloadHandler::handleRequest);
        registrar.playToClient(ExampleSyncPacket.TYPE, ExampleSyncPacket.STREAM_CODEC, ClientPayloadHandler::handleSync);
        // 兼容层的 payload 由工厂统一注册
        ExampleModCompatFactory.registerNetwork(registrar);
    }
}
