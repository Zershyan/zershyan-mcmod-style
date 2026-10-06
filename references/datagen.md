# Datagen 与资源

> 本文代码为**范式**，占位符按目标项目替换：`<ModId>` → 主类名，`<modid>` → modid 字符串，`<Dep>` → 依赖名，`<BLOCK>` → 方块名。`Mod*` 前缀按混淆情况取舍（见 [naming-and-layout.md](naming-and-layout.md)）。

## 核心原则

- **所有可以用 datagen 生成的资源都必须使用 datagen 生成**；如无必要**不允许手写任何资源文件**。
- 不要直接修改生成产物，应修改声明源或 Provider。
- 生成产物目录：`src/generated/resources`，在 `build.gradle` 中作为 resources 源目录。

```groovy
sourceSets.main.resources {
    srcDir('src/generated/resources')
    exclude("**/*.bbmodel")
    exclude("**/.cache/**")
}
```

## 结构

```
datagen/
├── DataGeneratorHandler.java
├── lang/           语言数据源
│   ├── <ModId>Lang.java            抽象基类
│   ├── <ModId>KeyLang.java
│   ├── <ModId>ConfigLang.java
│   └── <ModId>RegistryLang.java
├── provider/       Provider
└── util/           辅助类型（LazyComponent、ConfigLangEntry）
```

## 注册入口

```java
@EventBusSubscriber(modid = <ModId>.MODID)
public class DataGeneratorHandler {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        ExistingFileHelper helper = event.getExistingFileHelper();
        PackOutput packOutput = event.getGenerator().getPackOutput();

        event.createProvider(ModItemTagProvider::new);
        event.createProvider(ModBlockTagProvider::new);
        event.createProvider(ModLangProvider::runEnUs);
        event.createProvider(ModLangProvider::runZhCn);
        event.createProvider(ModLootTableProvider::new);
        event.createProvider(PackMetadataProvider::new);
        event.addProvider(new ModSoundProvider(packOutput, helper));
        event.addProvider(new ModItemModelProvider(packOutput, helper));
        event.addProvider(new ModBlockStateProvider(packOutput, helper));
    }
}
```

- 无参构造的 Provider 用 `event.createProvider(X::new)`；需要 `PackOutput` / `ExistingFileHelper` / `HolderLookup.Provider` 的用 `event.addProvider(new X(...))`。
- 只注册项目实际需要的 Provider，不要为了齐全而引入空 Provider。

## 语言数据源（固定范式）

### 抽象基类

`<ModId>Lang` 是所有语言数据的基类，聚合各子类并统一对外提供。

```java
public abstract class <ModId>Lang {
    private static final List<Entry> LangEntries = new ArrayList<>();
    protected final List<Entry> entries = new ArrayList<>();

    public static void stringEntry(String key, String enDesc, String zhDesc) {
        LangEntries.add(new FinalEntry<>(key, enDesc, zhDesc));
    }

    public static List<FinalEntry<?>> getAllLang() {
        List<FinalEntry<?>> entryList = new ArrayList<>();
        Stream.of( //添加lang类
                new <ModId>KeyLang(),
                new <ModId>ConfigLang(),
                new <ModId>RegistryLang()
        ).map(<ModId>Lang::initLang).map(
                list -> list.stream().map(Entry::toLang).toList()
        ).forEach(entryList::addAll);
        entryList.addAll(LangEntries.stream().map(Entry::toLang).toList());
        return entryList;
    }

    /** 按 ResourceLocation 生成语言键；各领域按需增加同类方法 */
    public static String getSoundDesc(ResourceLocation location) {
        return location.toLanguageKey("sound", "desc");
    }

    protected static String getKeyDesc(String name) {
        return <ModId>.id(name).toLanguageKey("key", "name");
    }

    protected void addEntry(Entry entry) {
        this.entries.add(entry);
    }

    protected <T> void addFinalEntry(T key, String enUs, String zhCn) {
        this.entries.add(new FinalEntry<>(key, enUs, zhCn));
    }

    private List<Entry> initLang() {
        init(); // 无返回初始化
        return init(entries); // 有返回初始化
    }

    protected List<Entry> init(List<Entry> entries) {
        return entries;
    }

    protected void init() {
    }

    public interface Entry {
        FinalEntry<?> toLang();
    }

    public record Lang(String enUs, String zhCn) {
    }

    public record FinalEntry<T>(T key, Lang lang) implements Entry {
        public FinalEntry(T key, String enDesc, String zhDesc) {
            this(key, new Lang(enDesc, zhDesc));
        }

        @Override
        public FinalEntry<?> toLang() {
            return this;
        }
    }
}
```

新增语言来源时，在 `getAllLang()` 的 `Stream.of(...)` 中追加该子类。

### 键构造辅助方法

用 `ResourceLocation.toLanguageKey(...)` 生成键，避免手拼字符串。基类中按项目实际领域增删：

| 形态 | 用途 |
|---|---|
| `getSoundDesc(location)` | 声音字幕 |
| `getKeyDesc(name)` | 按键名 |
| `<领域>Desc(location)` | 其他需要跟随资源路径的键 |

### KeyLang（通用文本）

前缀常量化，条目用三件套登记：

```java
public class <ModId>KeyLang extends <ModId>Lang {
    private static final List<FinalEntry<String>> TranslatableLang = new ArrayList<>();
    private static final String ModId = <ModId>.MODID;
    private static final String ModName = <ModId>.class.getSimpleName();

    public static final MutableComponent CreativeTab = entry("item_group." + ModId + "." + ModId, ModName, "<显示名>");
    public static final MutableComponent Resource = entry(ModId + ".resources", "Resources for " + ModName, ModName + "资源");

    private static final String CommandPrefix = "command." + ModId + ".";
    private static final String ErrorPrefix = "error." + ModId + ".";
    private static final String TooltipPrefix = "tooltip." + ModId + ".";
    private static final String DescriptionPrefix = "description." + ModId + ".";
    private static final String MessagePrefix = "message." + ModId + ".";

    public static final LazyComponent CommandFail = entryLazy(CommandPrefix + "fail",
            "The command is executed failed: %s",
            "命令执行失败: %s");
    public static final MutableComponent CommandForbidden = entry(CommandPrefix + "forbidden",
            "You cannot use this command now.",
            "你当前无法使用该命令");
    public static final String KeyAction = entryString(getKeyDesc("action"), "Action", "动作");

    private static String entryString(String key, String enUs, String zhCn) {
        TranslatableLang.add(new FinalEntry<>(key, enUs, zhCn));
        return key;
    }

    private static MutableComponent entry(String key, String enUs, String zhCn) {
        return Component.translatable(entryString(key, enUs, zhCn));
    }

    private static LazyComponent entryLazy(String key, String enUs, String zhCn) {
        return new LazyComponent(entryString(key, enUs, zhCn));
    }

    @Override
    public List<Entry> init(List<Entry> entries) {
        entries.addAll(TranslatableLang);
        return entries;
    }
}
```

**三件套的选择**：

| 方法 | 返回 | 用途 |
|---|---|---|
| `entryString` | `String` | 只要键，配合 `toLanguageKey` 系列使用 |
| `entry` | `MutableComponent` | 无参或少参的即时组件 |
| `entryLazy` | `LazyComponent` | 带 `%s` 占位符，调用时传参 |

### ConfigLang（配置项）

配置项键由 `ConfigLangEntry` 统一生成：

```java
public record ConfigLangEntry(String name) {
    public String key() {
        return <ModId>.MODID + ".configuration." + name;
    }
}
```

```java
public class <ModId>ConfigLang extends <ModId>Lang {
    private static final List<FinalEntry<String>> TranslatableLang = new ArrayList<>();

    // 分类
    public static final ConfigLangEntry General = entry("General", "General", "通用配置");

    // Server
    public static final ConfigLangEntry <ConfigName> = entry("<ConfigName>", "<English>", "<中文>");

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
```

`entry` 的 `name` 必须与配置类中 `define(...)` 的键一致（大小写敏感），否则配置界面显示原始键。

### RegistryLang（注册项）

注册项的语言用 `addFinalEntry`，并按注册类型提供专用登记方法：

```java
public class <ModId>RegistryLang extends <ModId>Lang {
    private void entryItem(DeferredItem<?> item, String enUs, String zhCn) {
        addFinalEntry(item.asItem(), enUs, zhCn);
    }

    private void entryBlock(DeferredBlock<?> block, String enUs, String zhCn) {
        addFinalEntry(block.get(), enUs, zhCn);
    }

    private void entrySound(Holder<SoundEvent> sound, String enUs, String zhCn) {
        addFinalEntry(getSoundDesc(sound.value().getLocation()), enUs, zhCn);
    }

    @Override
    protected void init() {
        entryItem(<ModId>Items.<ITEM>, "<English>", "<中文>");
        entryBlock(<ModId>Blocks.<BLOCK>, "<English>", "<中文>");
        entrySound(<ModId>Sounds.<SOUND>, "<English>", "<中文>");
    }
}
```

需要新的注册类型时，按同一形态增加一个 `entry<Type>` 私有方法，内部调用 `addFinalEntry`。

## 辅助类型

### LazyComponent

带占位符的延迟组件，调用时传参，并对数字做统一格式化：

```java
public record LazyComponent(String key) {
    private static Object[] formatNumbers(Object[] args, String format) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Float || args[i] instanceof Double) {
                args[i] = formatOptimized(format, ((Number) args[i]).doubleValue());
            }
        }
        return args;
    }

    private static String formatOptimized(String format, double value) {
        DecimalFormat df = new DecimalFormat(format, DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(value);
    }

    public MutableComponent get(Object... args) {
        return Component.translatable(this.key, args);
    }

    public MutableComponent getNumber2f(Object... args) {
        return get(formatNumbers(args, "#.##"));
    }

    public MutableComponent getNumber1f(Object... args) {
        return get(formatNumbers(args, "#.#"));
    }
}
```

## Provider

### ModLangProvider

双 locale 静态工厂 + 泛型分发：

```java
public class ModLangProvider extends LanguageProvider {
    private static final String enUs = "en_us";
    private static final String zhCn = "zh_cn";
    private final String locale;

    public ModLangProvider(PackOutput output, String locale) {
        super(output, <ModId>.MODID, locale);
        this.locale = locale;
    }

    public static ModLangProvider runZhCn(PackOutput output) {
        return new ModLangProvider(output, zhCn);
    }

    public static ModLangProvider runEnUs(PackOutput output) {
        return new ModLangProvider(output, enUs);
    }

    @Override
    protected void addTranslations() {
        switch (locale) {
            case enUs -> <ModId>Lang.getAllLang().forEach(langEntity ->
                    addTranslation(langEntity.key(), langEntity.lang().enUs())
            );
            case zhCn -> <ModId>Lang.getAllLang().forEach(langEntity ->
                    addTranslation(langEntity.key(), langEntity.lang().zhCn())
            );
        }
    }

    private <T> void addTranslation(T o, String desc) {
        switch (o) {
            case Item object -> add(object, desc);
            case Block object -> add(object, desc);
            case String object -> add(object, desc);
            case ItemStack object -> add(object.getItem(), desc);
            case MobEffect object -> add(object, desc);
            case EntityType<?> object -> add(object, desc);
            case TagKey<?> object -> add(object, desc);
            default -> {
                LOGGER.error("Unknown object type: {}", o.getClass());
                add(o.toString(), desc);
            }
        }
    }
}
```

### ModSoundProvider

遍历声音注册表，用语言的 sound 键作 subtitle：

```java
public class ModSoundProvider extends SoundDefinitionsProvider {
    public ModSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, <ModId>.MODID, helper);
    }

    @Override
    public void registerSounds() {
        <ModId>Sounds.SOUNDS.forEach(sound -> {
            SoundEvent soundEvent = sound.value();
            ResourceLocation name = soundEvent.getLocation();
            String translatableKey = <ModId>Lang.getSoundDesc(name);
            add(soundEvent, definition().with(sound(name)).subtitle(translatableKey));
        });
    }
}
```

同一声音有多个变体文件时，用 `name.withSuffix("_" + i)` 依次添加：

```java
SoundDefinition.Sound[] sounds = new SoundDefinition.Sound[number];
for (int i = 0; i < number; i++) {
    sounds[i] = sound(name.withSuffix("_" + i));
}
add(soundEvent, definition().with(sounds).subtitle(translatableKey));
```

### ModItemModelProvider

遍历物品注册表调 `basicItem`：

```java
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, <ModId>.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        Set<DeferredHolder<Item, ? extends Item>> items = new LinkedHashSet<>();
        items.addAll(<ModId>Items.REGISTER.getEntries());
        items.stream().map(DeferredHolder::get).forEach(this::basicItem);
    }
}
```

不需要生成模型的物品（如运行时动态贴图）在流中排除。

### ModBlockStateProvider

遍历方块注册表构建状态与模型：

```java
public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, <ModId>.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        <ModId>Blocks.<BLOCKS>.forEach((key, blockRegistryObject) -> {
            String path = blockRegistryObject.getId().getPath();
            BlockModelBuilder model = models().orientableVertical(
                    path,
                    mcLoc("block/" + <材质路径>),
                    <ModId>.id("block/" + path)
            );
            getVariantBuilder(blockRegistryObject.get()).forAllStates(state -> {
                // 按 BlockStateProperties 计算 rotationX / rotationY
                return ConfiguredModel.builder()
                        .modelFile(model)
                        .rotationX(rotationX)
                        .rotationY(rotationY)
                        .build();
            });
            simpleBlockItem(blockRegistryObject.get(), model);
        });
    }
}
```

简单方块用 `simpleBlock` / `cubeAll` 等现成方法，不要手写变体构建。

### 标签 Provider

标签由注册表侧的缓存（如 `Tag2ItemListCache` / `Tag2BlockListCache`）驱动，Provider 只做遍历输出：

```java
public class ModItemTagProvider extends ItemTagsProvider {
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        <ModId>Items.Tag2ItemListCache.forEach((tag, itemList) ->
                tag(tag).addAll(itemList.stream().map(DeferredHolder::getKey).toList())
        );
    }
}
```

```java
public class ModBlockTagProvider extends BlockTagsProvider {
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        <ModId>Blocks.Tag2BlockListCache.forEach((tag, itemList) ->
                tag(tag).addAll(itemList.stream().map(DeferredHolder::getKey).toList())
        );
    }
}
```

标签内容定义在注册表类中（声明与注册项绑定），Provider 不含业务判断。

### ModLootTableProvider

内嵌 `BlockLootSubProvider` 子类，`getKnownBlocks` 必须只返回本 mod 的方块：

```java
public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Set.of(), List.of(new SubProviderEntry(
                ModBlockLootSubProvider::new, LootContextParamSets.BLOCK
        )), lookupProvider);
    }

    private static class ModBlockLootSubProvider extends BlockLootSubProvider {
        protected ModBlockLootSubProvider(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            <ModId>Blocks.<BLOCKS>.values().forEach(block -> dropSelf(block.get()));
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return <ModId>Blocks.<BLOCKS>.values().stream().map(DeferredBlock::get).collect(Collectors.toSet());
        }
    }
}
```

`getKnownBlocks` 若返回了非本 mod 的方块，datagen 会覆盖其他模组的战利品表。

### PackMetadataProvider

包描述用语言键（`<ModId>KeyLang.Resource`），版本用 `DetectedVersion`：

```java
public class PackMetadataProvider extends PackMetadataGenerator {
    public PackMetadataProvider(PackOutput pOutput) {
        super(pOutput);
        add(PackMetadataSection.TYPE, new PackMetadataSection(
                <ModId>KeyLang.Resource,
                DetectedVersion.BUILT_IN.getPackVersion(PackType.SERVER_DATA)
        ));
    }
}
```

### 可选依赖的 Provider

依赖第三方 mod 的 Provider 同样放 `provider/`，继承其 Provider 基类。该类只在对应依赖存在时才注册：

```java
public class Mod<Dep>DataProvider extends <Dep>DataProvider {
    public Mod<Dep>DataProvider(PackOutput output, ExistingFileHelper fileHelper, CompletableFuture<HolderLookup.Provider> registries) {
        super(<ModId>.MODID, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider provider, ExistingFileHelper existingFileHelper) {
        // 按第三方 Provider 的 API 生成数据
    }
}
```

## Provider 命名

`ModLangProvider`、`ModSoundProvider`、`ModItemModelProvider`、`ModBlockStateProvider`、`ModItemTagProvider`、`ModBlockTagProvider`、`ModLootTableProvider`、`PackMetadataProvider`；第三方依赖的 Provider 用 `Mod<Dep>DataProvider`。

## 完成前检查

- 新增资源是否通过 Provider 产生，而不是直接改 `src/generated/resources`？
- 语言条目是否登记到对应的 Lang 子类（Key / Config / Registry）？
- 新增语言来源是否已加进 `getAllLang()` 的 `Stream.of(...)`？
- `ConfigLang` 的 `name` 是否与配置类 `define(...)` 的键完全一致？
- `getKnownBlocks` 是否只返回本 mod 的方块？
- 可选依赖的 Provider 是否只在依赖存在时注册？
- 跑过 `data` 运行配置确认生成成功？