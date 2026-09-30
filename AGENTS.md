# AGENTS.md — MC 1.21.1 + NeoForge Mod 开发约定

## 0. 这份文件是什么

- 这是 **本仓库(Game Jam MC mod 项目)的项目级权威约定**。后续接手本目录的 agent **必须遵守**。
- 若本文件与任何调研素材(见第 12 节)冲突,**以本文件为准**。
- 素材位置:`E:\UnityProjects\CA\` 下的 6 份报告(SETUP.md 环境清单 + 5 份 research-*.md)。
- 本文件自包含:读者可能**没有任何对话上下文**,只看到这份文件。
- 目标读者:即将开始写 MC 1.21.1 + NeoForge mod 的开发 agent。
- 最后更新时间:**2026-09-28**。
- ⚠️ 凡标注 **「⚠️需复核」** 的条目为**未本机实测**的推断,首次使用前自行验证,不要盲信。第 11 节集中列出。

---

## 1. 项目目标与范围

- **性质**:Game Jam 项目。目标是在有限时间(48h 量级)内产出一个**可玩**的 Minecraft mod。
- **本文件锁定什么**:技术骨架、版本、代码规约、工作方式、质量门禁。
- **本文件不锁定什么**:具体玩法创意(主题、机制由团队决定)。
- **成功标准**:能 `.\gradlew.bat build` 出 jar、能在 `runClient` 与 `runServer` 下稳定运行、GameTest 通过。

---

## 2. 硬性技术约束(不可自行更改)

| 组件 | 锁定值 | 说明 |
| :- | :- | :- |
| Minecraft | **1.21.1** | 长期支持、生态最成熟 |
| 加载器 | **NeoForge**(**不是** Forge) | Forge 自 1.20.2 起生态已被 NeoForge 取代;1.21+ 新项目一律 NeoForge |
| NeoForge 版本 | **21.1.252** | 1.21.1 对应版本段为 `21.1.x`;**勿**写成 `1.21.1-21.1.x` 旧 Forge 格式 |
| JDK | **21** | MC 1.20.5 起底层升级 Java 21,用 JDK 17 会在配置期报错 |
| Gradle | **8.10.2** | 由 `gradlew.bat` wrapper 管理,**勿手装** |
| 构建插件 | **ModDevGradle 2.0.x**,`net.neoforged.moddev` | 最新 2.0.147;ForgeGradle/NeoGradle 已弃用 |
| 映射 | **Parchment 2024.11.17** | 补充形参名与 Javadoc,显著提升可读性 |
| 模板仓库 | `https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle` | **务必用这个**;`MDK-1.21-ModDevGradle` 对应 MC **1.21.0**,不要用 |
| 操作系统 | Windows(本仓库约定) | 命令前缀用 `.\gradlew.bat` |

模板 `gradle.properties` 已核实为 `minecraft_version=1.21.1` / `neo_version=21.1.252`,配套 `parchment_mappings_version=2024.11.17`。

---

## 3. 环境准备

**一句话**:装 **JDK 21 + IntelliJ IDEA Community + Minecraft Development 插件**,克隆上面的 MDK 1.21.1 模板,配好国内网络,即可开工。完整下载清单与地址见 `E:\UnityProjects\CA\SETUP.md`。

最容易踩的 4 条:

| # | 要点 | 后果 |
| :- | :- | :- |
| 1 | JDK **必须是 21**(Temurin 21 或 JBR 21) | 版本不符报 `Unsupported class file major version` |
| 2 | IDEA 的 `Build and run using` / `Run tests using` 从 **Gradle** 改为 **IntelliJ IDEA** | 否则热重载(HotSwap)失效 |
| 3 | 国内网络需配 **Maven 镜像 / Gradle 代理**(首次构建下载 2.0~3.5 GB) | 首次 `runClient` 卡下载 |
| 4 | `runClient` 用 **`--username Dev`** 离线启动 | 开发期**不需要**正版账号 |

- JDK 方案 A(最省事):Adoptium Temurin 21 LTS,标准 HotSwap 可用,只能改方法体。
- JDK 方案 B(增强热重载):JetBrains Runtime 21 SDK(JBR),内置 DCEVM,配合 `-XX:+AllowEnhancedClassRedefinition` 可增删方法/字段。
- 磁盘预留 **15 GB** 以上;首次构建网络通畅约 **5~12 分钟**。

---

## 4. 代码约定

### 4.1 注册

- 用 `DeferredRegister.Blocks` / `DeferredRegister.Items` 等快捷子类。
- 返回类型是 **`DeferredHolder<R, T>`**(以及 `DeferredItem<T>` / `DeferredBlock<T>`),**不是** 旧 Forge 的 `RegistryObject`。

```java
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
public static final DeferredItem<Item> RUBY = ITEMS.registerSimpleItem("ruby", new Item.Properties());
```

### 4.2 物品自定义数据

- **禁止** ItemStack NBT:`ItemStack.getTag()` 在 1.20.5+ **已移除**。
- 用强类型 **`DataComponentType<T>`**,通过 `stack.set(COMPONENT.get(), val)` / `stack.get(COMPONENT.get())` 读写。

### 4.3 网络

- 用 `record MyPayload(...) implements CustomPacketPayload` + 声明 `Type` 与 `StreamCodec`。
- 在 Mod 总线监听 **`RegisterPayloadHandlersEvent`** 注册,用 **`PacketDistributor`** 派发。
- **禁止**用旧版 `SimpleChannel`(已废弃)。

### 4.4 Capabilities

- 在 Mod 总线监听 **`RegisterCapabilitiesEvent`** 直接挂载(方块的 `registerBlockEntity`、实体的 `registerEntity`、物品的 `registerItem`)。
- **不要**用旧的 `ICapabilityProvider` / `AttachCapabilitiesEvent`(20.4+ 已重构)。

### 4.5 事件总线

| 总线 | 字段 | 触发时机 | 挂什么 |
| :- | :- | :- | :- |
| Mod 事件总线 | `modEventBus` | 初始化/加载期 | `RegisterEvent`、`RegisterPayloadHandlersEvent`、`RegisterCapabilitiesEvent`、`GatherDataEvent`、客户端 `RegisterMenuScreensEvent` |
| Game 事件总线 | `NeoForge.EVENT_BUS` | 每帧/每 Tick | `PlayerInteractEvent`、`LivingDamageEvent`、`LevelEvent`、`ServerTickEvent` |

- Mod 入口经**构造器注入** `(IEventBus modEventBus, ModContainer modContainer)`;不要用已废弃的 `FMLJavaModLoadingContext.get()`。
- 声明式:`@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)` + `@SubscribeEvent public static void ...`。

### 4.6 资源路径

| 路径 | 用途 |
| :- | :- |
| `src/main/resources/assets/<modid>/` | 客户端:lang、models、textures、sounds |
| `src/main/resources/data/<modid>/` | 服务端:recipe、loot_table、tags、worldgen |
| `src/main/resources/META-INF/neoforge.mods.toml` | mod 元数据与依赖声明 |
| `src/generated/resources/` | **DataGen 输出**,提交进 Git |

- 在 `build.gradle` 挂 `sourceSets.main.resources { srcDir 'src/generated/resources' }`。
- **严禁**把 DataGen 输出指向 `src/main/resources`(会覆盖/清空手写资源)。

---

## 5. 架构规约(强制,违反会导致返工)

### 5.1 跳板模式(Trampoline)—— 最重要的一条

- **原因**:Mixin 在**类装载期**由 NeoForge 的 `TransformingClassLoader` 对原版类织入字节码。Mixin 类本身只是模板,运行时执行的是**注入后的原版类**。HotSwap 只替换模板类,不会重新触发类转换链、不会重新织入目标类。
- **规定**:`@Mixin` 类里**只写一行委托调用**,业务逻辑全部放到普通类里。

```java
// ❌ 业务写在 Mixin 里 → 改动必须重启
@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 20 行体力结算逻辑...
    }
}

// ✅ 业务外置 → 普通类可被 JBR 热替换
@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerHookManager.handlePlayerTick((Player)(Object)this);
    }
}
```

- **收益**:`PlayerHookManager.java` 等普通类里的任意改动、新增函数、调整参数都能热替换;只有 Mixin **注入点本身**(`@At` 改动、增减 Mixin 类)才必须重启。

### 5.2 数据驱动优先

- 数值、配方、掉落、标签**一律**走 datapack/JSON + DataGen。
- **严禁硬编码进 Java 字段**。改数值只需游戏内 `/reload`。

### 5.3 客户端/服务端隔离

- **Common 代码禁止引用 `net.minecraft.client.*`** 或客户端限定逻辑。
- 合并前必须能干净跑起 `runServer`(否则在独立服务端会抛 `ClassNotFoundException`)。

---

## 6. 热重载能力边界与迭代 SOP(核心工作流)

### 6.1 改动类型 → 重载动作决策表

| 改动类型 | 涉及路径 | 推荐重载动作 | 需重启客户端 |
| :- | :- | :- | :-: |
| 已有方法内部逻辑、计算公式 | `src/main/java/**`(普通类) | `Ctrl+Shift+F9` → JBR HotSwap | **否** |
| 新增辅助方法/字段/Lambda | 普通类 | `Ctrl+Shift+F9` → JBR HotSwap | **否** |
| Mixin 注入逻辑(跳板外置) | `*Handler.java` / `*Helper.java` / `Mod*` 注册类(本项目无 `*Manager`) | `Ctrl+Shift+F9` → JBR HotSwap | **否** |
| Mixin 注入点本身(`@At` 改动、增减 Mixin 类) | `**/mixin/*.java` | 无法热更 | **是** |
| 新增方块/物品/实体/GUI 类型 | `DeferredRegister` 注册项 | 静态注册表已冻结 | **是** |
| 新增 `@SubscribeEvent` 监听 | Mod Event Bus 监听方法 | 总线只在启动扫一次 | **是** |
| 贴图 PNG | `assets/<modid>/textures/...` | `Ctrl+F9` → `F3+T` | **否** |
| 模型 / blockstates | `assets/<modid>/models/`, `blockstates/` | `Ctrl+F9` → `F3+T` | **否** |
| 语言文件 | `assets/<modid>/lang/*.json` | `Ctrl+F9` → `F3+T` | **否** |
| 配方 / 标签 / 战利品表 / 函数 | `data/<modid>/recipe\|tags\|loot_table` | 游戏内 `/reload` | **否** |
| 附魔 / 伤害类型 / 画 / 唱片机歌曲 | `data/<modid>/enchantment\|damage_type...` | 退出到主界面 → 重进存档 | **否** |
| 生物群系 / 结构 / 维度 | `data/<modid>/worldgen/...` | 退出到主界面 → 重进存档 | **否** |
| DataGen 生成器逻辑 | `src/main/java/**/datagen/*.java` | `.\gradlew.bat runData` → 按产物重载 | **否** |

### 6.2 最优迭代 SOP(5 步)

```text
[开始修改]
  ├── 1. 普通 Java 业务逻辑(含新增方法/字段)
  │      → Ctrl+Shift+F9 → "Reload Changed Classes" → 切回游戏验证 (1~2 秒)
  ├── 2. 贴图 / 模型 JSON / 语言文件
  │      → Ctrl+F9 → 游戏内 F3+T (2~5 秒)
  ├── 3. 数据包(配方、标签、战利品、mcfunction)
  │      → 保存 → 游戏内 /reload (1 秒)
  ├── 4. 动态注册表(附魔、结构、生物群系)
  │      → Esc → 保存并退出到标题 → 重进世界 (3~5 秒)
  └── 5. 新增 Item/Block 实例,或改 Mixin 注入点
         → 不可热更,直接 Rerun 重启 (冷启动约 15 秒)
```

### 6.3 何时直接重启更划算

- 遇到 **`@SubscribeEvent` 绑定异常**、改了**类继承体系或构造函数**:不要排查热替换为何失效,**直接重启**。
- 新增方块/物品:**批量一次性声明完,只重启一次**。

### 6.4 环境前置(3 条,否则热重载不生效)

1. **JBR 21**(JetBrains Runtime 21)。
2. IDEA 用 **IntelliJ 编译**(`Settings → Build Tools → Gradle` 改为 IntelliJ IDEA)。
3. 用 **Debug 模式**启动 `runClient`。

---

## 7. Worldgen 约定(群系 / 维度)

- **关键事实**:1.21.1 的 worldgen 是 **100% 数据驱动**;加维度**不需要**写 Java 类、**不需要**注册 `ChunkGenerator`。
- **新维度最小三件套**(全部在 `data/<modid>/` 下):

| 路径 | 作用 | 必须 |
| :- | :- | :- |
| `dimension_type/<dim>.json` | 高度、光照、天候、世界规则 | **必须** |
| `dimension/<dim>.json` | 维度入口:生成器与群系源 | **必须** |
| `worldgen/biome/<biome>.json` | 维度基底群系 | **必须** |

- **最快路径**:复用原版 `minecraft:overworld` 的噪声设置(`"settings": "minecraft:overworld"`)。
- **高程必须一致**(否则加载区块立即崩):复用 overworld 噪声时 `dimension_type` 必须是 **`min_y: -64` / `height: 384`**。
- **限制**:`BiomeModifier` **不能**向主世界**注入新群系**,只能修改**已有群系**的内容(地物、刷怪)。想把新群系放进主世界只有:覆盖 `overworld.json`(有冲突)/ TerraBlender / 放进自定义维度(**Jam 推荐**)。
- **Jam 禁令清单**(三条无底洞,**禁止**):
  - ❌ 自研 `NoiseGeneratorSettings` / `density_function`;
  - ❌ 下界式传送门方块 + POI(状态机 + 网络同步,极易暴雷);
  - ❌ 3 个以上群系的互嵌 `multi_noise`。
- **工时参考**:

| 目标 | 方案 | 预估工时 |
| :- | :- | :- |
| 极速 MVP 维度 | 纯 JSON + 复用原版噪声 + 单群系 + 道具传送 | 1.5 ~ 2.5 h |
| 主世界注入群系 | 覆盖 `overworld.json` 噪声设置 | 3 ~ 5 h |
| DataGen 完整流 | Provider 生成 Biome/Feature/Dimension | 4 ~ 6 h |
| 自研噪声维度 | 手写 DensityFunction + 传送门 POI | 16 h+ ❌ |

- **传送方式**:`/execute in <modid>:<dim> run spreadplayers ... under 256 false @s`(指令,0 工时)、**右键道具传送(推荐)**;传送门(**禁**)。
- **道具传送**:物品 `use` 里 `player.teleportTo(targetLevel, x, y, z, yRot, xRot)`;落点要算安全地表,别固定 y=0 或 y=300。
- **首次使用任何 worldgen JSON 前必须**用 **Misode 1.21.1 生成器**校验:https://misode.github.io/
- **`features` 数组阶段数(本工程实测)**:三个群系 JSON 的 `features` 均为 **11 个数组**,对应生成阶段索引 **0..10**(空阶段写 `[]`)。素材中"长度恰好为 10"的说法与本仓库不符;改 worldgen 前仍建议用 Misode 1.21.1 生成器校验。

---

## 8. 构建与运行命令

Windows 一律用 `.\gradlew.bat` 前缀。

| 命令 | 用途 | 何时用 |
| :- | :- | :- |
| `.\gradlew.bat runClient` | 启动开发客户端(内置集成服务端) | 逻辑开发、GUI/渲染调试、日常验证 |
| `.\gradlew.bat runServer` | 启动独立 Dedicated Server | 验证 C-S 同步包、权限指令、双端隔离 |
| `.\gradlew.bat runData` | 执行 DataGen | 导出配方、模型、标签、语言包等 JSON |
| `.\gradlew.bat runGameTestServer` | 无头 GameTest 服务器 | 跑集成逻辑用例,CI/本地自动化门禁 |
| `.\gradlew.bat build` | 构建 jar 到 `build/libs/` | 封版、产出上传包 |
| `.\deploy.ps1`(或 `.\deploy.bat`) | 一键构建并自动热注入 jar 到 PCL 客户端版本 mods 目录 | 用 PCL 启动器联调、多客户端 / 光影测试 |
| `.\gradlew.bat --refresh-dependencies` / `genSources` | 刷新依赖 / 生成源码 | IDE 无法解析形参名时 |

- ⚠️需复核:`runClientData`(仅客户端数据生成任务)在 ModDevGradle v2 中**是否存在**未证实。
- 加速:`gradle.properties` 开 `org.gradle.daemon/parallel/caching`;依赖齐后用 `--offline`。
- 双客户端联机(示意,⚠️需复核 DSL):
  `--quickPlayMultiplayer localhost:25565`、`--quickPlaySingleplayer <存档>` 可跳过主菜单。

### 8.1 一键构建与客户端注入工作流 (PCL 联调)

- **【收尾标准工作流 · 由团队负责人指定】** 任何代码改动完成后,标准收尾 = **先 `.\gradlew.bat build` 通过 → 再 `.\deploy.ps1` 部署**到 PCL 客户端 mods 目录(部署前必须**完全退出** Minecraft 客户端)。**只 build 不部署视为未完成**;部署完成后需在启动器内重启客户端。
- 命令:`.\deploy.ps1`(PowerShell)或 `.\deploy.bat`(cmd 用户的薄封装,等价于 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File deploy.ps1`)。
- 流程:校验/设置 `JAVA_HOME` 为 JDK 21 → 执行 `.\gradlew.bat build`(非零退出码立即中止并透传错误码)→ 取 `build\libs\` 下最新的可部署 jar(`weather_realm-1.21.1-1.0.0.jar`,规则为 `<mod_id>-<minecraft_version>-<mod_version>.jar`;已排除 `-sources`/`-javadoc`/`-dev` jar)→ **覆盖**复制到注入目标目录。
- 注入目标:`C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\`(不存在时脚本自动创建)。
- 适用时机:用 **PCL 启动器**游玩联调、**多客户端**联机、**光影 / 渲染**等无法用 `runClient` 覆盖的场景。
- ⚠️ **部署前必须完全退出 Minecraft 客户端**:脚本会检测目标实例是否仍有客户端进程在运行,若在运行则**拒绝部署**(防热覆盖 jar 触发 `NoClassDefFoundError`),需先完全退出后重跑;确需强制时可显式 `.\deploy.ps1 -Force`(不建议)。
- 注意:注入后需在启动器内**重启客户端**;新增注册项不可热更(见第 6 节)。纯 Java 逻辑迭代仍优先走 IDE HotSwap。

---

## 9. 质量门禁

提交/合并前**必须**全部通过:

1. `.\gradlew.bat build` 通过。
2. `runServer` 能干净启动(**无客户端类泄漏**)。
3. GameTest 全绿(`.\gradlew.bat runGameTestServer`)。
4. DataGen 产物(`src/generated/resources/`)**已提交**。
5. 未把开发期专属配置带进产物;`neoforge.mods.toml` 的 `version`/`license`/`modId`/依赖区间正确。

- 可选:`spotlessApply` 格式化(团队自行决定是否引入)。
- 注:单元测试(`src/test/java`,JUnit 5)**不加载** MC 引擎与注册表,只能测纯算法;任何依赖 `net.minecraft.*` 的代码只能用 GameTest。

---

## 10. 已知的坑(清单)

| 现象 | 原因 | 排查 |
| :- | :- | :- |
| `Unsupported class file major version` / `Target JVM 21 cannot be compiled by Java 17` | JDK 版本不符 | 查 `Project Structure → SDK` 与 `Gradle JVM` 是否都是 21 |
| 热重载失效(`Ctrl+Shift+F9` 不生效) | IDEA 用 Gradle 编译 / 非 Debug 启动 / 未装 JBR | 改 `Build and run using` 为 IntelliJ IDEA;Debug 启动 |
| `Could not find net.neoforged:neoforge:x.x.x` | `neo_version` 格式写错 | 应为 `21.1.x`(用 `21.1.252`),**不要**写成 `1.21.1-21.1.x` |
| 首次 `runClient` 卡住 | 停在 `:downloadAssets` / `:createMinecraftArtifacts` | 确认代理覆盖 JVM 流量;`--info` 看卡在哪个 URL |
| 数据包解析失败 | `features` 数组阶段数不对(见 7 节冲突) | 用 Misode 1.21.1 校验;空阶段补 `[]` |
| 加载区块即崩 | `dimension_type` 的 `min_y`/`height` 与所引用 `noise_settings` 不一致 | 复用 overworld 时必须是 `-64` / `384` |
| BiomeModifier 无效 | 误用它往主世界注入**新群系** | BiomeModifier 只能改已有群系内容;换自定义维度 |
| 资源改了不生效 | 忘了 IDE 增量构建(`Ctrl+F9`)或忘了 `F3+T` | 运行中的客户端读 `build/resources/main`,必须先构建再重载 |
| 语言文件改了不生效 | 语言文件不自动监视 | **必须按 `F3+T`**(或切换一次语言) |
| 引用原版资源找不到 | 漏写 `minecraft:` 前缀 | 如 `"settings": "minecraft:overworld"` 必须显式命名空间 |
| `Registry is already frozen` | 运行时新增静态注册项 | 新增 Block/Item/EntityType 必须重启 |
| 命名空间冲突 / 双端崩溃 | Common 代码引用了 `net.minecraft.client.*` | 跑 `runServer` 复现 |

---

## 11. 尚未验证的结论(诚实清单)

> ⚠️ **置顶提示**:以下结论来自调研推断,**未经本机实测**。首次使用请自行验证,不要视为既定事实。

| 结论 | 状态 |
| :- | :- |
| `-Dmixin.hotSwap=true` 在 NeoForge/ModLauncher 下是否可用 | **两份调研结论互相矛盾**,未实测。已核实 `-XX:+AllowEnhancedClassRedefinition` 参数本身真实存在且 JBR 21 内置 DCEVM,但 **Mixin 热替换的实际生效范围仍未实测** |
| JBR 21 增强类重定义能覆盖到什么程度(涉及 Mixin 与注册表) | ⚠️需复核 |
| `mc_devlogin` 这个 Gradle 属性是否真实 | ⚠️需复核,未获官方证实 |
| `runClientData` 任务名是否存在 | ⚠️需复核 |
| NeoForge 是否仍认 `forge.logging.*` 系统属性 | ⚠️需复核(旧 Forge 时代做法) |
| worldgen 的 JSON 字段全集与 `features` 阶段枚举 | ✅ 已实测:本工程三群系 `features` 为 **11 段(0..10)**;字段全集仍建议用 Misode 校验 |
| `maven.neoforged.net` 在国内各地网络的实际速度 | ⚠️需复核,开工前实测 |
| `neoForge.runs.*` DSL 字段名(`gameDirectory`、`parchment { }` 等) | ⚠️需复核,以 ModDevGradle 官方文档为准 |

---

## 12. 参考文档索引

| 文档 | 路径 / URL | 用途 |
| :- | :- | :- |
| 技术栈与 API 调研 | `E:\UnityProjects\CA\research-minecraft-forge-stack.md` | 版本矩阵、注册/网络/Capabilities API、DataGen |
| 热重载能力边界调研 | `E:\UnityProjects\CA\research-mc-hotswap.md` | 重载决策表、迭代 SOP、跳板模式 |
| 调试工作流调研 | `E:\UnityProjects\CA\research-mc-debug-workflow.md` | 构建任务、断点、GameTest、质量门禁 |
| Worldgen 调研 | `E:\UnityProjects\CA\research-mc-worldgen.md` | 群系/维度 JSON、BiomeModifier、工时表 |
| 环境开工清单 | `E:\UnityProjects\CA\SETUP.md` | 下载清单、版本号、国内网络、常见坑 |
| 游戏横向选型(仅背景) | `E:\UnityProjects\CA\research-game-mod-support.md` | 备选游戏对比,不必展开 |
| NeoForge 官方文档 | https://docs.neoforged.net/ | 权威 API 参考 |
| ModDevGradle | https://github.com/neoforged/ModDevGradle | 构建插件与 run DSL |
| Parchment 映射 | https://parchmentmc.org/ | 形参名与 Javadoc 映射 |
| Misode 数据包生成器 | https://misode.github.io/ | worldgen / datapack JSON 校验(1.21.1) |
| Blockbench | https://www.blockbench.net/ | 方块/物品/实体建模与动画 |
| Modrinth | https://modrinth.com/ | mod 发布平台 |

---

> 本文件为团队约定,任何修改需团队确认。冲突时以本文件为准,第 11 节的未验证项除外——那些以你自己实测为准。
