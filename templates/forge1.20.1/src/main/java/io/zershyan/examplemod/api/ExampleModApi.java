package io.zershyan.examplemod.api;

import io.zershyan.examplemod.ExampleMod;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 对外门面，内部包不反向依赖本包。 */
public class ExampleModApi {
    public static final String MODID = ExampleMod.MODID;

    public static float tryGetPartialTick(Level level) {
        return level.isClientSide() ? clientGetPartialTick() : 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static float clientGetPartialTick() {
        return net.minecraft.client.Minecraft.getInstance().getFrameTime();
    }
}
