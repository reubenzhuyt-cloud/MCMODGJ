# 《天象之境》玩法设计愿景（Design Vision）

> **文档定位**：本文是**玩法设计愿景 / 设计基线（Design Vision）**，回答「这个模组想让玩家体验什么、未来长成什么样」。
> 它**不是实现现状文档**。每一项玩法都标注了当前落地状态与证据；实现层面的架构、类图与调用链见 `docs/ARCHITECTURE.md`（现状架构，由另一流程产出；若该文件暂不存在，以本仓库实际代码/资源为准）。
> **技术权威**：版本矩阵、代码规约、质量门禁、热重载边界一律以根目录 [`AGENTS.md`](../AGENTS.md) 为准；本文若与其冲突，以 `AGENTS.md` 为准。
> **命名空间**：全篇统一为 **`weather_realm`**。历史 GDD 通篇使用的 `examplemod` 为早期误用，已在本文修正；旧 GDD / 旧实施计划已随本次文档收敛清理，不再随仓库分发。
> **状态**：🟢 可玩纵切（入界 → 采集 → 传送门 → 群系探索 → 调谐）；终局内容多为愿景。
> **最后更新**：2026-09-30。

## 状态图例

| 标记 | 含义 |
| :- | :- |
| ✅ 已实现 | 代码 / 数据 / 资源齐备，可在游戏中触发 |
| 🟡 部分实现 | 骨架或单一环节已落地，闭环不完整 |
| ⬜ 未实现 | 仅存在于设计愿景，代码与资源中无对应实现 |

---

## 0. 状态总览（先看这张表）

| 玩法模块 | 状态 | 一句话说明 |
| :- | :- | :- |
| 自建维度 `weather_realm:crystal_realm` | ✅ | 噪声生成 + 三维度类型 + 三重群系源 |
| 三大天气群系（永冻 / 燃焰 / 风沙） | ✅ | 地表材质、矿脉、植被均按群系分流 |
| 永久暴风雪 + 群系级天气过渡 | ✅ | 仅永冻群系降雪，跨群系平滑淡入淡出 |
| 气候传送门（2×2 水池 + 12 框架 + 碎片点燃） | ✅ | 投掷 `climate_shard` 点燃，双向往返 |
| 天象祭坛核心 + 调谐基座（天象调控仪 UI） | ✅ | 右键核心打开三态天气调控界面 |
| 气象祭坛**自然生成** | 🟡 | 结构/模板/战利品俱全，但缺 `structure_set`，当前不会自然刷出 |
| 极域天象图（手持地图） | ✅ | 复用原版地图渲染，仅在天象之境可用 |
| 《古代天气研究手记》+ Patchouli 四篇 | ✅ | Patchouli 可选，缺失时聊天栏降级 |
| 坚冰木族 13 件套 + 冰晶装备/工具 | ✅ | 配方齐全 |
| 4 种冰原动物（羊/牛/猪/猫） | ✅ | 刷新、渲染、掉落齐备 |
| 异界地表结构（村庄 / 避难所 / 熔岩遗迹 / 烈火祭坛） | ✅ | 村庄经材质 + 生物替换处理器生成 |
| 燃焰 / 风沙木族与植被 | 🟡 | 原木/树叶/植被有，木板等衍生件与配方缺失 |
| 失温 / 霜冻值系统 | ⬜ | 无 `HypothermiaManager` / `frost_resistance` / `frostbite` |
| 动态风暴潮周期 | ⬜ | 无 `BlizzardCycleManager`，天气仅由 UI 手动切换 |
| 风暴能量（碎片 / 充能核 / 引风瓶 / 基座） | ⬜ | 代码中零命中 |
| 极寒气象观测所地牢（导流柱 / 结界） | ⬜ | 仅存在于 Lore 与旧 GDD |
| 终局 Boss（极寒机枢·泰坦） | ⬜ | 未实现 |
| 天气神器（暴风雪权杖 / 霜痕巨刃 / 天气掌控器） | ⬜ | 未实现 |

---

## 1. 核心概念

### 1.1 一句话前提

玩家循着一位**古代炼金术士**的研究轨迹逆行，走进被撕裂出的天气异维度 **天象之境**，在极端气候中求生、勘探，最终由「环境的受害者」蜕变为「气象之主」。

### 1.2 核心差异化：天灾即资源

- 风暴与严寒**既是致死威胁，也是唯一的高阶资源产地**。
- 每一种环境压力都对应一条生存/生产链，逼玩家主动走进危险，而不是躲进洞里。
- 数据驱动优先：数值、生成、掉落一律走 datapack（见 `AGENTS.md` §5.2）。

### 1.3 双轨驱动模型

| 轨道 | 内容 | 反馈周期 | 现状 |
| :- | :- | :- | :- |
| **A · 维度气象** | 失温生存 + 风暴潮周期 + 风暴能量捕集 | 分钟级 | ⬜（当前仅有手动天气调控 + 永久暴风雪） |
| **B · 区域试炼** | 观测所地牢 + 导流解密 + Boss 战 | 小时级 | ⬜ |

两轨本应互相供能：A 产出的充能筹码是 B 的通行证，B 的掉落反哺 A。**该闭环尚未实现**，是路线图的核心。

### 1.4 设计支柱

1. **天灾即资源**：威胁即生产资料。
2. **读环境即读关卡**：霜花、风沙、粒子与光色编码状态，零 UI 也能读懂。
3. **数据驱动优先**：数值/生成/掉落/配方全部走 datapack + DataGen。
4. **复用原版状态**：降水、光照、冰冻条、地图渲染优先复用，私状态只做最薄封装。
5. **垂直切片交付**：每个里程碑都要是端到端可玩闭环。

---

## 2. 世界观与维度

### 2.1 叙事设定 ✅（内容已入库）

- **时间线**：约三百年前的「大旱纪元」，主世界连年干旱，村庄北迁。
- **先哲（老炼金术士）**：为终结旱灾而研制初代天气晶石，建立调谐祭坛；超限催化引发**裂隙之灾**，祭坛与本人被吸入暴风雪异界，即今之天象之境。
- **四篇编年史**（已实现，见下）：天候纪元 34 年天旱与初遇 → 37 年神迹调谐台 → 39 年失控与碎裂 → 41 年裂隙之门（终章）。
- **Lore 中的后日谈**：先哲在异界建成**极寒气象观测所**，以永冻土构装体 + 反应炉锻造「机枢」替身。此段**仅为叙事愿景，没有对应的地牢/Boss 实现** ⬜。

> 证据：`src/main/resources/data/weather_realm/patchouli_books/weather_tome/book.json`、`assets/weather_realm/patchouli_books/weather_tome/{zh_cn,en_us}/entries/{prologue,ritual,fracture,dimension}.json`。

### 2.2 天象之境维度 `weather_realm:crystal_realm` ✅

| 项 | 值 | 证据 |
| :- | :- | :- |
| 维度类型 | 自然维度、有天空光、`fixed_time=6000`、`ambient_light=0.2`、`min_y=-64` / `height=384`、`effects=overworld` | `data/weather_realm/dimension_type/crystal_realm.json` |
| 生成器 | `minecraft:noise` + `settings=minecraft:overworld`（复用原版噪声设置） | `data/weather_realm/dimension/crystal_realm.json` |
| 群系源 | `multi_noise`：按温度切分三重群系（-1.0~-0.2 / -0.2~0.35 / 0.35~1.0） | 同上 |
| 高程一致性 | `min_y=-64` / `height=384` 与所引用噪声设置严格一致，防加载区块崩溃 | `dimension_type` 与 `AGENTS.md` §7 |

- 维度**100% 数据驱动**，无 Java `ChunkGenerator`；符合 `AGENTS.md` §7「新维度最小三件套」。
- 表面材质由 `ModSurfaceRules` 经 `@Mixin`（`SurfaceSystemMixin`）注入，按群系分流。
- 传送方式：**气候传送门**（见 §4）。旧 GDD 中的「祭坛裂隙仪式 + `weather_rift` 方块」**未实现** ⬜，现有替代是传送门 + 调试指令 `/build_portal`。

---

## 3. 三大天气群系

三重群系共用同一噪声地形，靠 `temperature` 参数分区，地表规则与地物各自分流。

### 3.1 水晶平原 `weather_realm:crystal_plains`（永冻）✅

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 中文名 | 水晶平原 | `lang/zh_cn.json` |
| 气候 | `temperature=-0.5`、`downfall=0.9`、有降水 | `worldgen/biome/crystal_plains.json` |
| 地表 | 顶层雪 / 浮冰 / 冰 / 细雪 / 砂砾，基底 `permafrost` → `deep_permafrost` | `world/ModSurfaceRules.java` |
| 生物 | 冰原羊 / 冰原牛 / 冰原猪 / 冰原猫 | 同上 `spawners` |
| 专属矿 | 冻土暴风雪结晶矿（浅/深）→ `blizzard_crystal` | `placed_feature/ore_permafrost_blizzard_crystal.json` |
| 植被 | 坚冰木、冰晶花、冰雪草、冰川兰、霜草幼芽、高霜草/高霜冻花、晶冰斑块 | `placed_feature/{frost_tree,frost_flower,...}.json` |
| 天气表现 | **永久暴风雪**（仅此群系），带 1.25 s 跨群系淡入淡出 | `client/ClientBlizzardEffects.java` |
| 专属音乐 | 「水晶平原」`unyielding_icy_wind` | `sounds.json` + `sounds/music/unyielding_icy_wind.ogg` |

### 3.2 烈焰平原 `weather_realm:blazing_plains`（燃焰）🟡

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 中文名 | 烈焰平原 | `lang/zh_cn.json` |
| 气候 | `temperature=2.0`、`downfall=0`、无降水 | `worldgen/biome/blazing_plains.json` |
| 地表 | `fire_stone` / `deep_fire_stone` | `world/ModSurfaceRules.java` |
| 专属矿 | 火石烈火晶石矿 → `blaze_crystal` | `placed_feature/ore_fire_stone_blaze_crystal.json` |
| 木族/植被 | 焦木（原木/木/去皮原木/树叶）、灰烬兰、烈焰草幼芽、火晶花 | `placed_feature/{scorched_tree,cinder_bloom,flame_sprout,fire_flower}.json` |
| 缺失 | **无生物刷新**（`spawners` 为空）、**无热害/灼烧机制**、木族无木板及配方 | 同上 JSON 与 `recipe/` 目录 |

> 设计愿景：烈焰平原应是「反向失温」——高温灼烧、岩浆裂隙与耐火装备链。目前只完成了**地形与资源层**。

### 3.3 干旱荒原 `weather_realm:arid_wasteland`（风沙）🟡

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 中文名 | 干旱荒原 | `lang/zh_cn.json` |
| 气候 | `temperature=1.5`、`downfall=0`、无降水 | `worldgen/biome/arid_wasteland.json` |
| 地表 | `weathered_sandstone` / `deep_weathered_sandstone` | `world/ModSurfaceRules.java` |
| 专属矿 | 风化砂石风沙晶石矿 → `wind_crystal` | `placed_feature/ore_weathered_sandstone_wind_crystal.json` |
| 木族/植被 | 风化木（原木/木/去皮原木/树叶）、沙丘花、风生草、风沙灌木 | `placed_feature/{arid_tree,dune_flower,wind_sprout,arid_bush}.json` |
| 缺失 | **无生物刷新**、**无沙暴/风蚀机制**、木族无木板及配方 | 同上 JSON 与 `recipe/` 目录 |

### 3.4 表层过渡方块 🟡

- `frost_moss`（耐寒苔藓）、`dry_turf`（干草坪）、`volcanic_ash`（火山灰）已注册并进创造标签与模型，证据：`lang/zh_cn.json`、`ModBlocks.java`。
- 世界生成期由 `world/ModSurfaceRules.java` 经 `SurfaceSystemMixin` **直接作为三群系顶层草皮铺设**（`ModSurfaceRules.java:139-191` 的 `abovePreliminarySurface` + `ON_FLOOR` 分支），不依赖 `placed_feature`，已确认生效。

---

## 4. 天气系统与气候传送门

### 4.1 现有天气能力

| 能力 | 状态 | 说明与证据 |
| :- | :- | :- |
| 维度永久暴风雪 | ✅ | 客户端按群系驱动原版降水渲染 + 自定义 `blizzard_snow` 粒子；`client/ClientBlizzardEffects.java` |
| 登录/换维推送降水事件 | ✅ | `ServerWeatherHandler.java` 向进入 `crystal_realm` 的玩家推送 `START_RAINING` 等 |
| 天象调控仪（手动三态天气） | ✅ | 右键 `weather_altar_core`（下方为 `weather_pedestal`）打开 UI，按钮发 `set_weather` 载荷，服务端 `setWeatherParameters` 锁定 10 分钟；`client/gui/WeatherControlScreen.java`、`network/SetWeatherPayload.java` |
| 自定义天气粒子 | ✅ | `blizzard_snow`（`assets/weather_realm/particles/blizzard_snow.json`） |
| 动态风暴潮周期（CALM/RAMP_UP/SURGE/RAMP_DOWN） | ⬜ | 全仓无 `BlizzardCycleManager` 命中 |
| 失温 / 霜冻值 / `frost_resistance` / `frostbite` | ⬜ | 全仓零命中 |
| 风暴能量（`storm_shard` / 充能核 / 引风瓶 / 引风基座 / `storm_charge`） | ⬜ | 全仓零命中 |

> 结论：当前天气是**「静态永久暴风雪 + 手动开关」**，而非 GDD 设想的**动态周期 + 生存压力**。这是最大的愿景落差。

### 4.2 气候传送门 `weather_realm:weather_portal` ✅

**点燃配方**（已实现，可玩）：

```
投掷 climate_shard 落入 2×2 源水
  → ClimatePortalHandler 每 5 tick 扫描 ItemEntity
  → 校验四格均为水源 + 外环 12 格 ∈ #weather_realm:climate_portal_frames
  → 消耗 1 碎片，2×2 覆写为 weather_portal
  → 雷击 / 末影龙 / 电火花 / 末地烛 / 暴雪粒子演出
```

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 四类可平替框架 | 极寒之石（冰/浮冰/蓝冰/雪块/细雪）、晦暗之石（黑曜石/哭泣黑曜石）、荒漠之石（沙/红沙/砂岩/红砂岩/砂砾）、烈焰之石（岩浆块/下界砖/红下界砖/下界岩） | `data/weather_realm/tags/block/{portal_frames_*,climate_portal_frames}.json` |
| 双向传送 | 任意非 `crystal_realm` 维度 ↔ `crystal_realm`；目标维度未加载时 `getPortalDestination` 返回 `null`（无自定义聊天提示） | `block/WeatherPortalBlock.java:87-95` |
| 调试指令 | `/build_portal`（OP 2+）在脚下生成未激活底座 | `command/PortalCommands.java` |
| 视觉设计 | 水平面薄片、无碰撞、不可破坏、免疫流体冲毁 | `WeatherPortalBlock` 方块属性 |

> ⚠️ 与旧 GDD 的差异：GDD 设想的是「祭坛充能 + 手记右键 + 踏入裂隙传送」的**仪式状态机**；实际实现改成了**「投掷碎片点燃传送门」**这一更直接、数据驱动的方案。仪式状态机 ⬜。

---

## 5. 极域天象图（手持地图）✅

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 中文名 / ID | 极域天象图 / `weather_realm:biome_map` | `lang/zh_cn.json` |
| 交互 | 右键切换缩放档位（2 区块/像素 ↔ 4 区块/像素），带动作栏提示与翻页音效 | `item/BiomeMapItem.java` |
| 维度限制 | 仅在天象之境生效，其他维度提示「仅在天象之境能感应四象微鸣」 | `message.weather_realm.biome_map.wrong_dimension` |
| 渲染 | 复用原版 `MapItem` 第一人称举图姿势与像素管线；像素数据由客户端生成 | `item/BiomeMapItem.java`、`client/map/BiomeMapClientData.java`、`client/map/BiomeMapExplorationState.java` |
| 双端隔离 | Common 类不引用 `net.minecraft.client.*`，客户端提供者经 `Function` 注入 | `BiomeMapItem` 类注释与实现 |

- 设计意图：把「读环境即读关卡」具象化为**一张会亮的区域图**，引导玩家找到三片群系与结构。
- 探索状态是**纯客户端内存态**（`LongOpenHashSet`），登出即清空，**不随存档持久化**。证据：`client/map/BiomeMapExplorationState.java:22,55`、`BiomeMapClientData.java:124-133`。

---

## 6. 天气祭坛与 Patchouli 手记

### 6.1 天象祭坛 ✅ / 自然生成 🟡

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 核心方块 | 天象祭坛核心 `weather_altar_core`（`BaseEntityBlock`，视觉由 BER 接管） | `block/WeatherAltarCoreBlock.java`、`client/WeatherAltarCoreRenderer.java` |
| 基座 | 调谐基座 `weather_pedestal` | `ModBlocks.java` |
| 交互 | 核心 + 下方基座 → 客户端打开「天象调控仪」 | `client/WeatherAltarInteractionHandler.java` |
| 战利品 | 雕像内宝箱必得《古代天气研究手记》 | `data/weather_realm/loot_table/chests/weather_altar.json` |
| 结构模板 | `weather_altar.nbt` 内含核心 / 基座 / 手记箱 | 已解压 NBT 校验，字符串含 `weather_altar_core` / `weather_pedestal` / `ancient_weather_tome` |
| **自然生成** | 🟡 存在 `worldgen/structure/weather_altar.json` 与 `template_pool/weather_altar.json`，但**没有对应的 `structure_set`**，`village/altar_pool.json` 亦无任何池引用它 | `data/weather_realm/worldgen/structure_set/` 仅含 4 个文件，无 `weather_altar` |

> 现状：祭坛**只能**通过 `/place structure weather_realm:weather_altar` 或创造模式搭建复现，村庄里不会自然出现。旧 GDD 声称「生成于平原/雪原村庄」**不成立**。修复见 §9 路线图。

### 6.2 《古代天气研究手记》与 Patchouli ✅

| 项 | 内容 | 证据 |
| :- | :- | :- |
| 物品 | `weather_realm:ancient_weather_tome`，右键开启 | `item/AncientWeatherTomeItem.java` |
| Patchouli 可选 | 反射调用 `PatchouliAPI.openBookGUI`；未安装时聊天栏输出四篇摘要，客户端永不崩溃 | 同上；`lang` 内 `message.weather_realm.tome.*` |
| 书籍 | `weather_realm:weather_tome`（青色书本贴图），分类「老炼金术士日记」 | `data/.../patchouli_books/weather_tome/book.json`、`assets/.../zh_cn/categories/lore.json` |
| 四篇条目 | 34 年·天旱与初遇 / 37 年·神迹调谐台 / 39 年·失控与碎裂 / 41 年·裂隙之门（终章） | 四个 entries JSON |
| 3D 门框图 | `dimension` 条目内嵌 `patchouli:multiblock`，以标签作材质，Visualize 自动轮播可替代方块 | `entries/dimension.json` |

> **注意**：旧 GDD 的物品 ID 为 `ancient_weather_notes`，实际为 **`ancient_weather_tome`**；书 ID 为 `weather_tome`。以实际为准。

---

## 7. 内容清单（按已落地资源）

### 7.1 地质与矿脉 ✅

- **冻土系**：`permafrost` / `deep_permafrost`（硬度为对应原版 1.5×）。
- **火石系**：`fire_stone` / `deep_fire_stone`。
- **风化砂石系**：`weathered_sandstone` / `deep_weathered_sandstone`。
- **原版平替矿**：煤/铜/铁/金/红石/绿宝石/青金石/钻石，每群系各含浅层 + 深层双变体（共 3×8×2 = 48 个矿石方块）。
- **专属矿**：`permafrost_blizzard_crystal_ore` → `blizzard_crystal`；`fire_stone_blaze_crystal_ore` → `blaze_crystal`；`weathered_sandstone_wind_crystal_ore` → `wind_crystal`。
- 证据：`ModBlocks.java` / `ModItems.java`、`worldgen/placed_feature/ore_*.json`、`recipe/*_from_{smelting,blasting}_*.json`。

### 7.2 晶体系 ✅

| 晶石 | 中文名 | 衍生 |
| :- | :- | :- |
| `blizzard_crystal` | 暴风雪结晶 | 结晶块、**整套护甲**（头/胸/腿/靴）、**五工具**（剑/镐/斧/锹/锄） |
| `blaze_crystal` | 烈火晶石 | 烈火晶石块 |
| `wind_crystal` | 风沙晶石 | 风沙晶石块 |

- 证据：`lang/zh_cn.json`、`ModItems.java`、`recipe/blizzard_crystal_*.json`。

### 7.3 木族

| 木族 | 中文前缀 | 已注册件 | 状态 |
| :- | :- | :- | :- |
| `frost_*` | 坚冰木 | 原木/木/去皮原木/去皮木/木板/楼梯/台阶/栅栏/栅栏门/门/活板门/压力板/按钮 + 树叶/树苗 | ✅ 13 件套 + 配方 |
| `scorched_*` | 焦木 | 原木/木/去皮原木/树叶 | 🟡 缺木板及衍生件、缺配方 |
| `arid_*` | 风化木 | 原木/木/去皮原木/树叶 | 🟡 缺木板及衍生件、缺配方 |

- 结构生成期由 `FrostWoodProcessor` 把原版**云杉族 → 坚冰木族**、**猪/猫 → 冰原猪/冰原猫**替换，证据：`world/FrostWoodProcessor.java`、`world/FrostVillageStructure.java`。

### 7.4 植被 ✅

- 永冻：冰晶花、冰雪草、冰川兰、霜草幼芽、高霜冻花、高霜草、坚冰木树苗。
- 燃焰：灰烬兰、烈焰草幼芽、火晶花。
- 风沙：沙丘花、风生草、风沙灌木。
- 证据：`lang/zh_cn.json`、`placed_feature/` 下对应 JSON。

### 7.5 生物

| 分类 | 内容 | 状态 |
| :- | :- | :- |
| 被动/家畜 | 冰原羊（剪毛得冰原羊毛、掉冰原羊肉）、冰原牛（挤冰牛奶桶、掉冰原牛肉/冰原皮革）、冰原猪（掉冰原猪肉/霜原皮毛）、冰原猫 | ✅ `ModEntities.java`、`entity/Frost*.java`、`loot_table/entities/*` |
| 食物链 | 生/熟冰原羊肉、牛肉、猪肉，冰树莓，冰牛奶桶 | ✅ |
| 敌对生物 | 霜风怨灵、霜纹铁卫、风暴工蜂 | ⬜ 全仓零命中 |
| Boss | 极寒机枢·泰坦（Tempest Core Golem） | ⬜ 未实现 |
| 召唤物 | 充能球护盾 | ⬜ 未实现 |

### 7.6 结构

| 结构 ID | 中文 | 生成群系 | 状态 |
| :- | :- | :- | :- |
| `weather_realm:crystal_village` | 坚冰木雪原村庄（自定义 `frost_village`） | `crystal_plains` | ✅ |
| `weather_realm:shelter` | 避难所 | `crystal_plains` | ✅ 外壳 + 专属战利品 |
| `weather_realm:yanjiang` | 熔岩遗迹 | `crystal_plains` | ✅ 外壳（无专属战利品） |
| `weather_realm:fire` | 烈火祭坛 | `crystal_plains` | ✅ 外壳（无专属战利品） |
| `weather_realm:weather_altar` | 村庄气象祭坛 | 设计为平原/雪原村庄 | 🟡 缺 `structure_set`，不自然生成 |
| 原版村庄覆盖 | `village_plains` / `village_snowy` 起始池改为模组坚冰木村庄 | 主世界 | ✅ `data/minecraft/worldgen/structure/village_*.json` |

- 三座异界地表结构均为 `size=1`、`WORLD_SURFACE_WG`、`beard_thin`；战利品表见 `loot_table/chests/{shelter,weather_altar}.json`。
- 旧 GDD 的 `frost_observatory`（极寒气象观测所）多房间 Jigsaw 地牢 ⬜，当前**没有**对应实现。

### 7.7 表现层（音乐 / 粒子）

- 群系音乐：`music.weather_realm.crystal_plains` → `unyielding_icy_wind.ogg` ✅。
- 粒子：`blizzard_snow` ✅；GDD 另有 `frost_vortex` / `conduit_beam` / `reactor_ray` 等 ⬜。
- 物品提示描述行（`item.*.desc`）由 `client/ClientTooltipHandler.java` 渲染 ✅。

---

## 8. 玩法循环

### 8.1 当前可玩闭环（✅ 已落地）

```
主世界搜集线索（手记提示 4×4 门框配方）
        │
        ▼
取得 climate_shard（祭坛宝箱 / 创造）＋ 备好 12 格四类框架与 2×2 水池
        │
        ▼
投掷碎片点燃气候传送门 ──▶ 进入 weather_realm:crystal_realm
        │
        ▼
按群系勘探：永冻挖 blizzard_crystal ／ 燃焰挖 blaze_crystal ／ 风沙挖 wind_crystal
        │
        ▼
搜集坚冰木、矿石、冰原动物产物，返回主世界建造
        │
        ▼
（可选）搭建 weather_altar_core + weather_pedestal，用「天象调控仪」手动改写天气
```

### 8.2 目标终局循环（⬜ 愿景，见 §9）

```
入界 → 失温生存（装备梯队）→ 观测所地牢（导流柱 ×4 + 结界）
     → 机枢泰坦 Boss 战 → 天气神器 / 天气掌控器 → 自由改写全维度天气
```

---

## 9. 待办与路线图

### 9.1 近期可做（补齐现有闭环的缺口）

| # | 事项 | 说明 | 涉及 |
| :- | :- | :- | :- |
| R1 | **给气象祭坛接上 `structure_set`** | 目前祭坛不自然生成，是最大「资源白做」问题；注意村庄内注入需 `BiomeModifier`/池接线，勿覆盖原版村庄 | `worldgen/structure_set/weather_altar.json`（新建）、`village/altar_pool.json` 接线 |
| R2 | 焦木 / 风化木补齐木板、楼梯、门等衍生件 + 配方 | 与坚冰木族对齐 | `ModBlocks.java` / `ModItems.java`、`recipe/` |
| R3 | 燃焰 / 风沙群系补生物或环境机制 | 至少让两片群系「有威胁、有回报」，避免空跑 | `worldgen/biome/*.json` |
| R4 | 确认 `frost_moss` / `dry_turf` / `volcanic_ash` 是否参与世界生成 | 若否，补地物或改为合成获得 | `placed_feature/` |
| R5 | ~~清理模板残留 `Config.java`~~ **已完成** | 模板 `Config.java` 已删除，替换为 `config/WeatherRealmConfig.java`（COMMON + CLIENT 两个 `ModConfigSpec`） | 无 |
| R6 | 给 shelter / yanjiang / fire 补差异化战利品与叙事 | 目前基本是空壳 | `loot_table/chests/` |

### 9.2 Jam 后（终局愿景，按依赖排序）

| # | 事项 | 依赖 |
| :- | :- | :- |
| L1 | **失温系统**：`frost_resistance` 属性 + 霜冻值分层 + 热源结算 + `frostbite` 伤害类型 + HUD | 跳板模式，业务外置普通类 |
| L2 | **御寒装备梯队**：T1 霜绒套（`frost_wool` + `frost_wool_*`）→ T2 冰晶套 | L1 |
| L3 | **动态风暴潮周期**：`BlizzardCycleManager` 四相位 + 雾效 + 动态风力 | L1 |
| L4 | **风暴能量链**：`storm_shard` → `dormant_storm_core` → `charged_storm_core`，配 `weather_condenser` / `storm_relay` | L3 |
| L5 | **敌对生物**：霜风怨灵（飞行冲刺）、霜纹铁卫（重锤） | L3/L4 |
| L6 | **极寒气象观测所地牢**：多房间 Jigsaw + `conduit_pylon` 四态 + `glacier_barrier` 四重封印 | L4/L5 |
| L7 | **终局 Boss**：极寒机枢·泰坦两阶段 + 风暴工蜂 + 充能球护盾 | L6 |
| L8 | **天气神器**：暴风雪权杖、霜痕巨刃、天气掌控器（全维度天气切换） | L7 |
| L9 | 失温/伤害/充能的**双端同步载荷**（`FrostStatePayload` 等） | L1/L4 |
| L10 | GameTest 覆盖核心状态机；补 `advancement` 引导线 | 全 |

> **范围纪律**：`AGENTS.md` §7 明令禁止自研噪声/密度函数、下界式传送门 POI、3 个以上群系互嵌。上述愿景均在此红线内落地（维度已用原版噪声、传送用数据驱动的门方块 + 指令/道具）。

---

## 10. 与旧 GDD 的关键差异（供复核）

| 主题 | 旧 GDD 写法 | 当前实际 |
| :- | :- | :- |
| 命名空间 | `examplemod` | **`weather_realm`** |
| 维度 / 群系 | 单群系 `crystal_plains`（fixed 源） | **三群系 multi_noise**（crystal_plains / blazing_plains / arid_wasteland） |
| 进入维度 | 祭坛重构仪式 + 裂隙方块 | **投掷 `climate_shard` 点燃 2×2 水池 + 12 格框架** |
| 祭坛 UI | 未提 | **天象调控仪**（晴/雨/雷三态，`weather_altar_core` + `weather_pedestal`） |
| 手记物品 ID | `ancient_weather_notes` | **`ancient_weather_tome`**（书 `weather_tome`） |
| 祭坛生成 | 声称生成于平原/雪原村庄 | 🟡 **缺 `structure_set`，当前不自然生成** |
| 地图 | 未在正文实现层出现 | **极域天象图 `biome_map`**（手持地图） |
| 失温 / 风暴 / 地牢 / Boss / 神器 | 作为「已定稿实现规范」 | **全部 ⬜ 未实现**，仅属愿景 |

---

## 11. 不确定条目与已复核项（需人工复核）

**仍需复核：**

1. **`weather_altar` 是否真的不生成**：依据是「无 `structure_set` + `village/altar_pool.json` 无引用链」。已复核 `worldgen/structure_set/` 仅含 `crystal_village`/`fire`/`shelter`/`yanjiang` 四个文件，结论成立；若后续补资源需修正。
2. **`mod_group_id` 仍为 `com.example.examplemod`**（`gradle.properties:39`），而源码包名为 `com.example.weather_realm`；是否属有意遗留待团队确认（当前不影响本地运行）。

**已复核（本轮据代码确认，从原「不确定」移出）：**

3. **`frost_moss` / `dry_turf` / `volcanic_ash` 参与世界生成**：✅ 已确认。由 `world/ModSurfaceRules.java` 经 `SurfaceSystemMixin` 作为三群系顶层草皮铺设（不依赖 `placed_feature`）。
4. **极域天象图探索进度是否持久化**：✅ 已确认**不持久化**。`BiomeMapExplorationState` 是客户端内存 `LongSet`，登出时 `clear()`。
5. **`weather_altar_core` 的 BER 双层模型是否启用**：✅ 已确认启用。`client/ClientBlockEntityRenderers.java:22` 注册 BER，`:26-28` 通过 `ModelEvent.RegisterAdditional` 加载 shell/inner 两个独立模型。
6. **模板残留 `Config.java`**：✅ 已删除，替换为 `config/WeatherRealmConfig.java`；其 COMMON/CLIENT 项分别被 `ModNetwork`、`WeatherPortalBlock`、`ClimatePortalHandler` 与 `ClientBlizzardEffects` 读取。

---

> 本文是设计基线，随实现推进修订；设计层冲突以本文为准，技术层冲突以 [`AGENTS.md`](../AGENTS.md) 为准。现状实现细节见 [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)。
