package io.zershyan.examplemod.registry.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_OVERLAY;

    static {
        SHOW_OVERLAY = BUILDER.comment("是否显示示例 HUD 叠层")
                .define("show_overlay", true);

        SPEC = BUILDER.build();
    }
}
