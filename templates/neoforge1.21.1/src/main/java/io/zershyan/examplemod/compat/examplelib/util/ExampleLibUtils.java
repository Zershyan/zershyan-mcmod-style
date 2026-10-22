package io.zershyan.examplemod.compat.examplelib.util;

import io.zershyan.examplemod.ExampleMod;
import io.zershyan.examplemod.compat.ExampleModCompatFactory;

/**
 * 第三方 API 的实际调用点。本类只有在 examplelib 存在时才会被加载，
 * 因此可以直接引用其 API。
 */
public final class ExampleLibUtils {

    private ExampleLibUtils() {
    }

    /** 通过兼容工厂判断依赖是否可用，避免外部直接引用本类 */
    public static boolean isAvailable() {
        return ExampleModCompatFactory.ExampleLib.isModLoaded();
    }

    /** 需要第三方 API 的能力统一经 ICompatUtils 包装后对外暴露 */
    public static String describe() {
        return ExampleMod.LOGGER.getName();
    }
}
