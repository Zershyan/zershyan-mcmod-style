# 构建与发布

## 新建项目

- **moddev 插件版本不固定，用最新**。
- **新建项目时若用户未指定 MC 版本，先询问版本**，再选对应模板。
- 加载器按 MC 世代区分：`1.20.1` → Forge；`1.21.1` → NeoForge 21.1.x；`26.1.2` → NeoForge 26.1.2.x（新版号体系，`loader_version_range=[4,)`）。

## 可复制的工程模板

`templates/neoforge1.21.1/` 是一个**可直接编译运行的完整工程**（modid `examplemod`，NeoForge 1.21.1，moddev 2.0.148）。新建同世代项目时优先复制它，再替换 modid 与包名，不要凭记忆重写。

```
templates/neoforge1.21.1/
├── build.gradle          构建脚本
├── settings.gradle
├── gradle.properties     mod 元数据
├── gradlew / gradlew.bat / gradle/wrapper/
├── tools/gen_placeholder_textures.py   占位贴图生成器（纯标准库）
└── src/main/
    ├── java/io/zershyan/examplemod/    api / client / compat / datagen / handler / mixin / registry / util
    ├── resources/                      mixins.json、textures
    └── templates/META-INF/neoforge.mods.toml
```

模板已实测通过 `compileJava` 与 `runData`（datagen 产出 lang / 模型 / 方块状态 / 战利品 / pack.mcmeta）。复制后需要做的事：

1. 替换包名 `io.zershyan.examplemod` 与类名前缀 `ExampleMod`。
2. 改 `gradle.properties` 的 `mod_id` / `mod_name` / `mod_license` / `mod_description`。
3. 按需删除用不到的注册类（模板覆盖了全部常见类型，空项目不必全留）。

### Forge 1.20.1 模板

`templates/forge1.20.1/` 是已实测编译与 datagen 的 Forge 47.3.4 / Java 17 工程模板，构建插件为 `net.neoforged.moddev.legacyforge`（当前模板 2.0.148，新建项目时仍需查询最新版本）。它与 NeoForge 模板共用包结构、`doRegister` 和版本号规约，但以下内容不能照搬：

- ItemStack 自定义数据使用 NBT，不建 `ModDataComponents`。
- 普通消息使用 `SimpleChannel`；三种配置使用 `ForgeConfigSpec`，不注册配置界面。
- 自定义注册表保留 Forge 的 `NewRegistryEvent` / `RegistryBuilder` 写法。
- 只有实际需要附件功能才引入 forgeattachment；不需要就不加。引入后为必需依赖，固定 sigh Maven 和明确版本变量，并向用户说明用途、安装要求及 GitHub / MC百科 / CurseForge 项目链接；未发布的平台明确标注。详见 [forgeattachment.md](forgeattachment.md)。
- 元数据位于 `src/main/resources/META-INF/mods.toml`，由 `processResources` 展开；依赖使用 `mandatory=true`，不是 NeoForge 的 `type="required"`。展开值 `mod_version` 必须取 Gradle 的 `version`。
- `mixins.json` 使用 `JAVA_17` 并声明 `<modid>.refmap.json`，不省略 refmap。

模板实际包含附件演示，因此保留 forgeattachment 依赖，**不代表每个新模组都需要它**。复制到不使用附件的项目时，先移除或改写附件注册、数据类、事件处理和网络包中的附件引用，再移除 `modImplementation`、`forgeattachment_version` 与 `mods.toml` 中的对应依赖；仅为它服务的 Maven 仓库配置也一并移除，共享仓库按其他依赖需求保留。不要只删依赖、留下无法编译的附件调用；裁剪后重新运行 `compileJava` 与 `runData`。

LegacyForge 自带 Mixin 集成，不另加 MixinGradle；成品 jar 的 manifest 仍需显式声明 Mixin 配置：

```groovy
legacyForge {
    version = "${minecraft_version}-${forge_version}"
    mixin {
        add sourceSets.main, "${mod_id}.refmap.json"
        config "${mod_id}.mixins.json"
    }
}

dependencies {
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}

tasks.named('jar', Jar).configure {
    manifest.attributes('MixinConfigs': "${mod_id}.mixins.json")
}
```

当前验证结果：已使用 `./gradlew --refresh-dependencies compileJava --rerun runData jar` 联网复测通过，实际重新执行 Java 编译、datagen、jar 与 `reobfJar`。Forge MCP、AutoRenamingTool 与 sigh Maven 的 forgeattachment POM / jar 下载均返回 HTTP 200。成品 jar 已检查 CRC、`MixinConfigs` manifest、非空 SRG refmap、元数据版本和 Java 17 字节码；16 项 datagen 资源全部入包，双语各 18 条。此前的环境依赖下载阻塞已解除；独立的附件 Codec / StreamCodec、双向消息编解码边界值测试也已通过。

开发态 `runData` 日志仍会提示 refmap 无法读取，但任务正常完成；成品 jar 内的 refmap 已单独确认存在且包含有效映射，不应把开发态警告当成成品缺失 refmap。

### 模板与 skill 必须同步（维护约定）

**skill 的规范一旦变更，模板必须跟着改**，两者不允许出现不一致：

- 改了命名规范（如注册类前缀）→ 模板里所有类名与引用都要改。
- 改了包结构（如新增 `compat/`、`renderer/`）→ 模板要建出对应目录与示例。
- 改了写法（如日志实现、`doRegister` 约定）→ 模板代码要同步。
- 改了构建约定（如 moddev 版本策略）→ 模板的构建脚本要同步。

改完模板后**必须重新验证**：`./gradlew compileJava` 与 `./gradlew runData` 都要通过，确认模板仍可编译、datagen 仍能产出资源。

## 版本号

**整个模组家族统一采用 `modid-版本号-mc版本号+build.构建数`**，完整规约与打包收尾任务见 [versioning.md](versioning.md)。

```properties
# gradle.properties
mod_version=0.0.1          # 只写版本号，不含 MC 版本
mod_build_number=0         # 不手工维护，由 buildAlpha 构建后同步为所用编号
```

```groovy
// build.gradle
def isAlphaBuild = gradle.startParameter.taskNames.any { it.endsWith('buildAlpha') }
// buildAlpha 用 +1 后的编号构建，成功后把同一编号写回 gradle.properties
def buildNumber = mod_build_number.toInteger() + (isAlphaBuild ? 1 : 0)
def baseVersion = "${mod_version}-${minecraft_version}"
version = "${baseVersion}+build.${buildNumber}"
```

## 构建脚本骨架

```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '<最新>'
    id 'idea'
}

// 版本号计算使用上节规约，包含 buildAlpha 的 +1 逻辑
group = mod_group_id

sourceSets.main.resources {
    srcDir('src/generated/resources')
}

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version
    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }
    runs { /* 见下 */ }
    mods {
        "${mod_id}" { sourceSet(sourceSets.main) }
    }
}
```

## 运行配置

标准运行配置为 `client1` / `client2` / `server` / `gameTestServer` / `data`：

```groovy
runs {
    client1 {
        client()
        systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        gameDirectory = project.file('./run/client1')
        programArguments.addAll("--username", "player1")
    }
    client2 {
        client()
        gameDirectory = project.file('./run/client2')
        programArguments.addAll("--username", "player2")
    }
    server {
        server()
        programArgument '--nogui'
    }
    gameTestServer {
        type = "gameTestServer"
    }
    data {
        data()
        programArguments.addAll '--mod', project.mod_id, '--all',
                '--output', file('src/generated/resources/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
    }
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        logLevel = org.slf4j.event.Level.DEBUG
    }
}
```

## 模组元数据展开

用 `generateModMetadata` 任务把 `gradle.properties` 展开进 `neoforge.mods.toml`（Forge 为 `mods.toml`）：

```groovy
var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            neo_version_range      : neo_version_range,
            loader_version_range   : loader_version_range,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : version,
            mod_authors            : mod_authors,
            mod_description        : mod_description,
            mod_credits            : mod_credits,
            mod_url                : mod_url,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from "src/main/templates"
    into "build/generated/sources/modMetadata"
}

sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata
```

模板文件放 `src/main/templates/META-INF/`。

## gradle.properties

**只强制两项**：`mod_license`、`mod_description`。其余按需。

```properties
mod_id=<modid>
mod_name=<ModName>
mod_license=MIT
mod_version=0.0.1
mod_group_id=io.zershyan
mod_authors=Zershyan
mod_description=Server Memo
mod_credits=
mod_url=
```

## Proguard（按需，默认不启用）

**用户未要求时不使用混淆**；要求使用时，套用下面的模板。

`gradle.properties` 开关：

```properties
enableProguard=true
```

`build.gradle` 模板（已去掉 modid 等具体信息）：

```groovy
// ========== proguard start ==========
import proguard.gradle.ProGuardTask
import java.nio.file.Files
import java.nio.file.StandardCopyOption

def enableProguard = project.hasProperty("enableProguard") && Boolean.parseBoolean(project.enableProguard)
def proguardOutputJar = layout.buildDirectory.file("libs/obfuscate/${mod_id}-${version}-obfuscated.jar").get().asFile
def javaToolchainHome = tasks.named('compileJava', JavaCompile).get().javaCompiler.get().metadata.installationPath.asFile
def javaToolchainLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}

// 修正混淆后 jar 内 zip 条目的编码
tasks.register('proguardUtf8Fix', JavaExec) {
    dependsOn('testClasses')
    onlyIf { enableProguard }
    javaLauncher = javaToolchainLauncher
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'ZipEntryEncodingFixer'
    args proguardOutputJar.absolutePath
    outputs.upToDateWhen { false }
}

tasks.register('proguard', ProGuardTask) {
    onlyIf { enableProguard }
    dependsOn('jar')
    configuration 'proguard-rules.pro'
    libraryjars new File(javaToolchainHome, 'jmods')
    configurations.compileClasspath.files.each { file ->
        libraryjars file.absolutePath
    }
    injars(tasks.jar.archiveFile.get().asFile)
    outjars(proguardOutputJar)
    inputs.files(tasks.jar.archiveFile)
    inputs.file(file('proguard-rules.pro'))
    inputs.file(file('obfuscate/dict.txt'))
    finalizedBy(proguardUtf8Fix)
}

afterEvaluate {
    if (enableProguard) {
        tasks.named('jar').configure { finalizedBy('proguard') }
    }
}

// 打包收尾：混淆产物拷成最终包，原包挪进 origin。
// 混淆产物只读不搬，proguard 任务的输出追踪才不会被自己人破坏。
// baseVersion / isAlphaBuild 由版本号规约定义，见 versioning.md
tasks.register('finalizeArtifacts') {
    group = 'build'
    def libsDir = layout.buildDirectory.dir('libs').get().asFile
    def obfuscateDir = new File(libsDir, 'obfuscate')
    def originDir = new File(libsDir, 'origin')
    def modId = mod_id
    def modVersion = version
    doLast {
        def obfuscatedJar = new File(obfuscateDir, "${modId}-${modVersion}-obfuscated.jar")
        def backupJar = new File(originDir, "${modId}-${modVersion}-origin.jar")
        def builtJar = new File(libsDir, "${modId}-${modVersion}.jar")
        def releaseVersion = isAlphaBuild ? modVersion : baseVersion
        def finalJar = new File(libsDir, "${modId}-${releaseVersion}.jar")
        if (obfuscatedJar.exists()) {
            boolean alreadyObfuscated = builtJar.exists()
                    && Files.mismatch(builtJar.toPath(), obfuscatedJar.toPath()) == -1L
            if (builtJar.exists() && !alreadyObfuscated) {
                backupJar.parentFile.mkdirs()
                Files.move(builtJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            Files.copy(obfuscatedJar.toPath(), finalJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

tasks.named('proguardUtf8Fix') { finalizedBy 'finalizeArtifacts' }
// ========== proguard end ==========
```

需要的配套文件：

- `proguard-rules.pro` —— 混淆规则。
- `obfuscate/dict.txt` —— 混淆字典。
- `ZipEntryEncodingFixer` —— 测试源集内的 zip 条目编码修正工具。

任务链：`jar → proguard → proguardUtf8Fix → finalizeArtifacts`。

启用混淆时，本节给出的 `finalizeArtifacts` **替换** [versioning.md](versioning.md) 中不启用混淆的那一版（它多了一步：把混淆产物拷成最终包）。版本号拼接部分不变。

**启用混淆后注册类名用 `Mod*` 前缀**（见 [naming-and-layout.md](naming-and-layout.md)）。

## 完成前检查

- 目标 MC 版本与 moddev / 加载器版本是否匹配？
- 是否使用了项目实际的 Java、Gradle、映射版本？
- `generateModMetadata` 的字段是否与模板中的占位符一一对应？
- 混淆仅在需要时启用，且注册类名已按 `Mod*` 前缀调整？