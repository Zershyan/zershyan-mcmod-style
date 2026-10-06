package io.zershyan.examplemod.registry.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_VALUE;

    static {
        MAX_VALUE = BUILDER.comment("示例数值上限")
                .defineInRange("max_value", 100, 0, 10000);

        SPEC = BUILDER.build();
    }
}
