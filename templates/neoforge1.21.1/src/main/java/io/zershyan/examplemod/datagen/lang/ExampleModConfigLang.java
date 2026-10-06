package io.zershyan.examplemod.datagen.lang;

import io.zershyan.examplemod.datagen.util.ConfigLangEntry;

import java.util.ArrayList;
import java.util.List;

public class ExampleModConfigLang extends ExampleModLang {
    private static final List<FinalEntry<String>> TranslatableLang = new ArrayList<>();

    // 分类
    public static final ConfigLangEntry General = entry("General", "General", "通用配置");

    // Common
    public static final ConfigLangEntry EnableFeature = entry("EnableFeature", "Enable Feature", "启用示例功能");

    // Client
    public static final ConfigLangEntry ShowOverlay = entry("ShowOverlay", "Show Overlay", "显示示例叠层");

    // Server
    public static final ConfigLangEntry MaxValue = entry("MaxValue", "Max Value", "数值上限");

    private static ConfigLangEntry entry(String name, String enUs, String zhCn) {
        ConfigLangEntry langEntry = new ConfigLangEntry(name);
        TranslatableLang.add(new FinalEntry<>(langEntry.key(), enUs, zhCn));
        return langEntry;
    }

    @Override
    protected List<Entry> init(List<Entry> entries) {
        entries.addAll(TranslatableLang);
        return entries;
    }
}
