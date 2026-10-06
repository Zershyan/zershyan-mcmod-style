package io.zershyan.examplemod.compat.examplelib;

import io.zershyan.examplemod.compat.ICompatUtils;
import io.zershyan.examplemod.compat.examplelib.event.ExampleLibClientSetupEvent;
import io.zershyan.examplemod.compat.examplelib.event.ExampleLibCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * 对 examplelib 的兼容实现。依赖缺失时整个子包的类都不会被链接。
 */
public class ExampleLibCompat implements ICompatUtils {
    public static final String MODID = "examplelib";

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    @Override
    public void addCommonListener(IEventBus forgeBus, IEventBus modBus) {
        forgeBus.register(ExampleLibCommonSetupEvent.class);
    }

    @Override
    public void addClientListener(IEventBus forgeBus, IEventBus modBus) {
        forgeBus.register(ExampleLibClientSetupEvent.class);
    }
}
