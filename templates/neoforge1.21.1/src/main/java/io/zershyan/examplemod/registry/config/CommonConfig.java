package io.zershyan.examplemod.registry.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_FEATURE;

    static {
        ENABLE_FEATURE = BUILDER.comment("是否启用示例功能")
                .define("enable_feature", true);

        SPEC = BUILDER.build();
    }
}
