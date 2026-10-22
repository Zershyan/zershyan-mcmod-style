# 版本号规约

**整个模组家族统一采用这一套版本号，不允许各仓库自行变形。**

## 版本号格式

```
<modid>-<mod版本号>-<mc版本号>+build.<构建数>
```

例：`examplemod-0.0.1-1.21.1+build.2`

- `modid` 来自 `mod_id`（`archivesName = mod_id`）。
- `mod版本号` 来自 `mod_version`，**只写版本号本身，不含 MC 版本**。
- `mc版本号` 来自 `minecraft_version`，由构建脚本拼接，不手写进 `mod_version`。
- `构建数` 来自 `mod_build_number`，每次构建递增。
- **`build` 后必须有点**：`+build.2`，不是 `+build2`。

## gradle.properties

```properties
minecraft_version=1.21.1

mod_id=examplemod
mod_version=0.0.1
mod_build_number=0
```

- `mod_version` 只写版本号（如 `0.0.1`）。
- 换 MC 版本时只改 `minecraft_version`，`mod_version` 不动。
- `mod_build_number` **不手工维护**：初始写 `0`，之后由 `buildAlpha` 同步为构建所用的编号（见「构建号同步」）。

## build.gradle 版本段

```groovy
group = mod_group_id

// 包内版本号恒带 +build.<编号>，只有 buildAlpha 会把这个编号也写进 jar 文件名
def isAlphaBuild = gradle.startParameter.taskNames.any { it.endsWith('buildAlpha') }
// buildAlpha 用 +1 后的编号构建，成功后把同一编号写回 gradle.properties，
// 使产物里的编号与 properties 始终一致
def buildNumber = mod_build_number.toInteger() + (isAlphaBuild ? 1 : 0)
def baseVersion = "${mod_version}-${minecraft_version}"
version = "${baseVersion}+build.${buildNumber}"
```

## 双轨产物

包内版本号与 jar 文件名**不同轨**：

| 位置 | 普通 `build` | `buildAlpha` |
|---|---|---|
| 使用的编号 | `mod_build_number` | `mod_build_number + 1` |
| 包内 `neoforge.mods.toml` 的 version | `0.0.1-1.21.1+build.1` | `0.0.1-1.21.1+build.2` |
| jar 文件名 | `examplemod-0.0.1-1.21.1.jar` | `examplemod-0.0.1-1.21.1+build.2.jar` |
| 是否回写 `mod_build_number` | **否** | **是（同步为构建所用编号）** |

理由：**包内版本号必须恒带构建数**，便于从游戏内、日志和崩溃报告区分具体构建；而**发布用的 jar 名不带构建数**，便于下载站与用户识别正式版本。

- `buildAlpha` 是**发版**动作：消耗一个新编号，产物里的编号与 `gradle.properties` 里的编号**完全一致**。
- `build` 是**本地构建**动作：沿用上一次已发布的编号（即 `mod_build_number`），不回写，因此**重复构建不会消耗编号**。

例（`mod_build_number` 从 `0` 开始）：

| 执行 | 产物包内版本 | 执行后 `mod_build_number` |
|---|---|---|
| `build` | `+build.0` | `0`（不变） |
| `buildAlpha` | `+build.1` | `1` |
| `build` | `+build.1` | `1`（不变） |
| `buildAlpha` | `+build.2` | `2` |

## 构建号同步

`mod_build_number` **不人工维护**。`buildAlpha` 用 `+1` 后的编号构建，构建成功后把**同一个编号**写回 `gradle.properties`：

```groovy
// buildAlpha 用 +1 后的编号构建，成功后把同一编号写回 gradle.properties，
// 使产物里的编号与 properties 始终一致
def buildNumber = mod_build_number.toInteger() + (isAlphaBuild ? 1 : 0)
version = "${baseVersion}+build.${buildNumber}"
```

```groovy
// 与 build 完全相同，只是编号 +1 并把该编号同步回 gradle.properties
tasks.register('buildAlpha') {
    group = 'build'
    description = '打成 alpha 包，产物版本号带 +build.<编号>，构建成功后把该编号同步回 gradle.properties'
    dependsOn 'build'
    def propsFile = layout.projectDirectory.file('gradle.properties').asFile
    def buildNumberBefore = mod_build_number
    def buildNumberUsed = buildNumber
    doLast {
        def text = propsFile.getText('UTF-8')
        def matcher = (text =~ '(?m)^mod_build_number=\\s*(\\d+)\\s*$')
        if (!matcher.find()) {
            logger.warn('mod_build_number not found in gradle.properties, skipped sync')
            return
        }
        propsFile.setText(
                text.replaceFirst('(?m)^mod_build_number=\\s*\\d+\\s*$', "mod_build_number=${buildNumberUsed}"),
                'UTF-8')
        logger.lifecycle("mod_build_number synced: ${buildNumberBefore} -> ${buildNumberUsed}")
    }
}
```

行为：

- **编号在内存里算好，不预改 `gradle.properties`**：构建时版本号直接是 `mod_build_number + 1`，产物与随后的 properties 值一致。
- **回写发生在构建成功之后**（`doLast`），因此构建失败不会消耗编号。
- **只有 `buildAlpha` 回写**，普通 `build` 不动编号。
- 日志用英文，避免 Windows 控制台中文乱码。

## 打包收尾任务

### finalizeArtifacts

jar 默认名带构建数（因为 `version` 含它），需要改名成发布名，并把原包挪进 `origin/`：

```groovy
// 打包收尾：最终包改名到 libs 顶层，原包（带编号）挪进 libs/origin
import java.nio.file.Files
import java.nio.file.StandardCopyOption

tasks.register('finalizeArtifacts') {
    group = 'build'
    description = '整理 build/libs：最终包改名，原包挪进 origin 子目录'
    def libsDir = layout.buildDirectory.dir('libs').get().asFile
    def modId = mod_id
    def modVersion = version
    def releaseVersion = isAlphaBuild ? version : baseVersion
    doLast {
        def builtJar = new File(libsDir, "${modId}-${modVersion}.jar")
        def backupJar = new File(new File(libsDir, 'origin'), "${modId}-${modVersion}-origin.jar")
        if (releaseVersion != modVersion) {
            // 最终包与原包内容一致，只是名字不带编号；原包上一轮可能已搬进 origin，就从那里取
            def source = builtJar.exists() ? builtJar : backupJar
            if (source.exists()) {
                Files.copy(source.toPath(), new File(libsDir, "${modId}-${releaseVersion}.jar").toPath(), StandardCopyOption.REPLACE_EXISTING)
                if (source == builtJar) {
                    backupJar.parentFile.mkdirs()
                    Files.move(builtJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } else if (!builtJar.exists() && backupJar.exists()) {
            // buildAlpha 的最终包与原包同名，把上一轮搬走的原包取回来
            Files.copy(backupJar.toPath(), builtJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

tasks.named('build') { finalizedBy 'finalizeArtifacts' }
```

`finalizeArtifacts` 必须**恒定挂到 `build` 上**，不要只在启用混淆时挂 —— 否则不启用混淆的项目不会执行改名，双轨就失效了。

`isAlphaBuild` 通过检测任务名判定，因此任务名**必须是 `buildAlpha`**，不能改成别的名字。

## 与 Proguard 的关系

启用 Proguard 时，`finalizeArtifacts` 要额外把混淆产物拷成最终包（混淆产物输出在 `libs/obfuscate/`）。此时以 [build.md](build.md) 的 Proguard 段为准，那一段给出了合并后的写法；版本号拼接部分不变。

## 完成前检查

- `mod_version` 是否只含版本号，没有把 MC 版本写进去？
- `build` 后是否有点（`+build.2` 而非 `+build2`）？
- 包内 `mods.toml` 的 version 是否恒带 `+build.<编号>`？
- 普通 `build` 的 jar 名是否**不带**构建数？`buildAlpha` 的是否**带**？
- `buildAlpha` 产物的包内编号是否与其后 `gradle.properties` 里的编号**完全一致**？
- 跑过 `build` 后编号是否**没有**变化？
- 构建编号是否在内存里算好后使用，而不是先改 `gradle.properties` 再构建？
- `finalizeArtifacts` 是否挂到了 `build` 上（而非只在混淆时挂）？