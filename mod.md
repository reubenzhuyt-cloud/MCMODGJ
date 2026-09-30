# 天气冒险维度模组 —— 架构文档（Weather Realm / 天象之境）

> **文档性质**：根目录架构总览 + 实现现状归档。描述**当前工程全貌**（以 `src/main/` 产物为准）。
> **命名空间**：全篇统一为 `weather_realm`。
> **设计权威**：完整设计规范见 [`docs/superpowers/specs/2026-09-29-weather-dimension-adventure-design.md`](docs/superpowers/specs/2026-09-29-weather-dimension-adventure-design.md)（GDD）。设计层冲突以 GDD 为准。
> **技术权威**：[`AGENTS.md`](AGENTS.md)（版本、架构、质量门禁）。技术层冲突以 `AGENTS.md` 为准。
> **命令速查**：[`TEST_COMMANDS.md`](TEST_COMMANDS.md)。
> **状态**：🟢 开发中；已具备「入界 → 生存 → 探索 → 调谐」完整可玩纵切。
> **最后更新**：2026-09-30

---

## 目录

1. [项目基础与核心约束](#1-项目基础与核心约束)
2. [天气祭坛与宏观调谐体系](#2-天气祭坛与宏观调谐体系)
3. [气候传送门与跨维度拓扑](#3-气候传送门与跨维度拓扑)
4. [维度地质、生态与生物体系](#4-维度地质生态与生物体系)
5. [异界地表地牢与村庄系统](#5-异界地表地牢与村庄系统)
6. [剧情手记与 Patchouli 3D 轮换投影](#6-剧情手记与-patchouli-3d-轮换投影)
7. [构建、部署与联调流水线](#7-构建部署与联调流水线)

---

## 1. 项目基础与核心约束

### 1.1 项目标识

| 维度 | 内容 |
| :- | :- |
| 项目代号 | **Weather Realm / 天象之境** |
| ModID（命名空间） | `weather_realm` |
| 主类 | `com.example.weather_realm.WeatherRealm`（`@Mod(WeatherRealm.MODID)`） |
| 显示名 | `Weather Realm`（en_us）/ `天象之境`（zh_cn） |
| mod 版本 | `1.0.0` |
| 类型 | Game Jam 天气主题**冒险维度**模组 |

### 1.2 硬性技术约束

| 组件 | 锁定值 | 说明 |
| :- | :- | :- |
| Minecraft | **1.21.1** | 长期支持版本 |
| 加载器 | **NeoForge `21.1.252`** | `21.1.x` 版本段；**勿**写成 `1.21.1-21.1.x` |
| JDK | **21** | MC 1.20.5 起底层为 Java 21 |
| Gradle | **8.10.2**（wrapper 管理） | 命令前缀 `.\gradlew.bat` |
| 构建插件 | **ModDevGradle `2.0.147`** | `net.neoforged.moddev` |
| 映射 | **Parchment `2024.11.17`** | 形参名 + Javadoc |
| 资源目录 | `assets/weather_realm/`、`data/weather_realm/` | 客户端 / 服务端资源 |
| DataGen 输出 | `src/generated/resources/` | 已挂入 `sourceSets.main.resources` |

### 1.3 架构原则

| 原则 | 落地方式 |
| :- | :- |
| **跳板模式（Trampoline）** | `@EventBusSubscriber` / 方块 / 物品只做**一行委托**，业务逻辑全部外置于普通类（如 `WeatherPortalManager`、`FrostWoodProcessor`、`ModNetwork`），便于 JBR 热替换；Mixin 注入点本身不可热更（当前工程尚未引入 Mixin）。 |
| **双端隔离** | 所有 `net.minecraft.client.*` 仅存在于 `com.example.weather_realm.client` 包；Common 代码只依赖 `ServerLevel` / `ServerPlayer`，`runServer` 可干净启动。 |
| **数据驱动优先** | 维度 / 群系 / 结构 / 矿石 / 掉落 / 配方 / 标签**一律**走 datapack JSON；数值不硬编码进 Java 字段，改数值只需 `/reload`（worldgen 除外，需重进存档）。 |
| **Patchouli 软依赖解耦** | `AncientWeatherTomeItem` 通过 `ModList.get().isLoaded("patchouli")` + 反射调用 `PatchouliAPI`；未安装时降级为聊天栏摘要，客户端**永不崩溃**。 |
| **事件总线分工** | Mod 总线：`RegisterPayloadHandlersEvent`、`EntityAttributeCreationEvent`、`RegisterSpawnPlacementsEvent`、`EntityRenderersEvent`、`ModelEvent`、`FMLCommonSetupEvent`；Game 总线：`ServerTickEvent.Post`、`PlayerEvent`、`PlayerInteractEvent`、`EntityTickEvent.Post`。 |

### 1.4 核心注册项清单

| 类别 | 关键 ID（均为 `weather_realm:` 命名空间） |
| :- | :- |
| **维度** | `crystal_realm` |
| **群系** | `crystal_plains` |
| **方块** | 坚冰木族：`frost_log` / `frost_wood` / `stripped_frost_log` / `stripped_frost_wood` / `frost_planks` / `frost_stairs` / `frost_slab` / `frost_fence` / `frost_fence_gate` / `frost_door` / `frost_trapdoor` / `frost_pressure_plate` / `frost_button`；霜植：`frost_flower` / `frost_grass` / `frost_sapling` / `frost_leaves` / `glacier_bloom`（冰川兰）/ `frost_sprout`（霜草幼芽）/ `tall_frost_flower`（高霜冻花）/ `tall_frost_grass`（高霜草）；地质：`permafrost` / `deep_permafrost`；矿石 8 种（浅/深双变体）：`permafrost_{coal,copper,iron,gold,redstone,emerald,lapis,diamond}_ore`；冰晶：`blizzard_crystal_block` / `permafrost_blizzard_crystal_ore` / `deep_permafrost_blizzard_crystal_ore`；祭坛与传送：`weather_altar_core` / `weather_pedestal` / `weather_portal`；掉落：`frost_wool` |
| **BlockEntity** | `weather_altar_core` |
| **物品（材料/食物）** | `blizzard_crystal` / `climate_shard` / `ancient_weather_tome` / `frost_mutton` / `cooked_frost_mutton` / `frost_beef` / `cooked_frost_beef` / `frost_leather` / `frost_porkchop` / `cooked_frost_porkchop` / `frost_pelt` / `frost_milk_bucket` / `frost_raspberry` |
| **物品（装备）** | 护甲 `blizzard_crystal_{helmet,chestplate,leggings,boots}`；工具 `blizzard_crystal_{sword,pickaxe,axe,shovel,hoe}` |
| **刷怪蛋** | `frost_{sheep,cow,pig,cat}_spawn_egg` |
| **实体** | `frost_sheep` / `frost_cow` / `frost_pig` / `frost_cat` |
| **护甲材料** | `blizzard_crystal`（datapack-backed `ARMOR_MATERIAL`） |
| **结构** | `crystal_village`（自定义类型 `frost_village`）/ `weather_altar` / `shelter` / `yanjiang` / `fire` |
| **结构处理器** | `frost_wood_replace`（`FrostWoodProcessor`） |
| **粒子** | `blizzard_snow` |
| **网络载荷** | `set_weather`（`SetWeatherPayload`，`playToServer`） |
| **创造标签** | `example_tab`（标题 key `itemGroup.weather_realm`，图标 `frost_flower`） |

---

## 2. 天气祭坛与宏观调谐体系

### 2.1 根节点拼装池代理（100% 必定生成）

为了让雪原 / 平原村庄**必然**使用模组定制的「坚冰木村庄」，工程直接**覆盖原版村庄结构入口**：

| 覆盖文件 | 覆盖内容 |
| :- | :- |
| `data/minecraft/worldgen/structure/village_plains.json` | `start_pool` 改写为 `weather_realm:village/plains/start` |
| `data/minecraft/worldgen/structure/village_snowy.json` | `start_pool` 改写为 `weather_realm:village/snowy/start` |

两个代理池 `data/weather_realm/worldgen/template_pool/village/{plains,snowy}/start.json` 均为**单元素、`weight=1`** 的 `single_pool_element`，直指 `weather_realm:village/{plains,snowy}/start.nbt`——因此村庄**根节点 100% 命定**为模组模板（无原版随机竞争）。其余构件仍由原版村庄拼装池接入，最后统一经 `FrostWoodProcessor` 做材质替换（见 §4.3）。

- 村庄逻辑由 `FrostVillageStructure`（`weather_realm:frost_village`）承载：镜像 `JigsawStructure`，但把每个 `PoolElementStructurePiece` 的池元素通过 `FrostPoolElements.withProcessors` 注入 `FrostWoodProcessor.INSTANCE`，从而**在结构生成期**把云杉族替换为坚冰木族。

### 2.2 BER 动态双层渲染核心

`weather_altar_core` 使用 `BaseEntityBlock` + **零数据** BlockEntity，仅作为渲染挂点。客户端 `WeatherAltarCoreRenderer`（视图距离 64）每帧绘制：

| 图层 | 模型 | 变换 |
| :- | :- | :- |
| **外壳** | `block/weather_altar_core_shell`（standalone，`RenderType.translucent`） | 以方块中心为轴**微缩放大 `1.15×`** |
| **内芯** | `block/weather_altar_core_inner`（standalone，`RenderType.translucent`，`FULL_BRIGHT`） | **浮空**（`sin` 上下浮动 0.08）+ **Y 轴自转**（`time × 2.2°`，约 163 s/圈）+ **X 轴摆动**（`sin(time×0.05)×14°`） |
| **暴雪光晕** | `weather_realm:blizzard_snow` 粒子 | 每 5 tick 在半径 0.75 的圆环上散布 |

- 模型经 `ClientBlockEntityRenderers.registerAdditionalModels`（`ModelEvent.RegisterAdditional`）注册为 standalone 附加烘焙模型。
- 方块本体 render shape 为 INVISIBLE，视觉完全由 BER 接管。

### 2.3 客户端调控 UI 与原版天气同步

```
右键 weather_altar_core（且其正下方为 weather_pedestal）
  → WeatherAltarInteractionHandler（client-only）打开 WeatherControlScreen
  → 三按钮各发一条 SetWeatherPayload
  → ModNetwork.handleSetWeather 调 setWeatherParameters(...)
  → 聊天栏回显
```

| 环节 | 实现 |
| :- | :- |
| 入口（客户端限定） | `client.WeatherAltarInteractionHandler`（`@EventBusSubscriber(value = Dist.CLIENT)`）监听 `PlayerInteractEvent.RightClickBlock`：核心方块 + 下方为 `weather_pedestal` → `setScreen(new WeatherControlScreen())` |
| UI | `client.gui.WeatherControlScreen`：三个 200×20 按钮（晴空 / 甘霖 / 狂雷），复用原版半透明渐变背景与按钮音效，`isPauseScreen=false` |
| 载荷 | `network.SetWeatherPayload(WeatherMode mode)`，`TYPE = weather_realm:set_weather`，`StreamCodec.composite` 编码 |
| 注册（Mod 总线） | `network.ModNetwork.onRegisterPayloads` → `registrar.playToServer(...)`，协议版本 `"1"` |
| 服务端接管 | `handleSetWeather` 在 `enqueueWork` 中依模式设置 `setWeatherParameters`：`CLEAR` → `clearTime=12000`；`RAIN`/`THUNDER` → `weatherTime=12000`，并广播对应聊天消息 |

- **12000 tick 语义**：`12000 = 12000 / 20 = 600 秒 = 10 分钟`，即一次 UI 操作将目标维度的原版天气状态锁定 10 分钟（与原版 `/weather <type> <duration>` 时长语义一致）。
- `WeatherMode` 枚举持有 `raining()` / `thundering()` / `messageKey()`，翻译键 `message.weather_realm.weather.*`。

---

## 3. 气候传送门与跨维度拓扑

### 3.1 传送门方块：末地门式水平面薄片

`WeatherPortalBlock`（`weather_realm:weather_portal`）是一个**水平面状、无碰撞的凝缩风暴薄片**：

| 属性 | 值 |
| :- | :- |
| 选框 | `Block.box(0, 0, 0, 16, 12, 16)` —— **Y = 12 像素**厚的满格薄片 |
| 碰撞 | `noCollission()` 无碰撞 |
| 破坏 | `strength(-1.0F, 3600000.0F)` + `noLootTable()` **不可破坏** |
| 流体防护 | 覆写 `canBeReplaced(state, fluid)` 恒返回 `false` —— **水流 / 岩浆不可冲毁** |
| 拾取 | `getCloneItemStack` 返回 `ItemStack.EMPTY`，禁止中键吸取 |
| 光照 | `lightLevel = 12`，`pushReaction = BLOCK` |
| 粒子 | `animateTick` 持续喷 `END_ROD` + 偶发 `blizzard_snow` |

### 3.2 传送：3 tick 瞬发 + 跨维往返

- 玩家嵌入传送门时，`entityInside` 委托 `WeatherPortalManager.onPortalDwell`。
- `DWELL_TICKS = 3`：连续驻留 **3 tick** 即触发。
- 冷却 `TRAVEL_COOLDOWN_TICKS = 120`，防止刚落地立即被原门弹回。
- 方向判定：源维度**非** `crystal_realm` → 目标 `crystal_realm`；否则目标 `overworld`。目标维度缺失时中文错误提示 `message.weather_realm.portal.no_dimension`。
- 落地后 `teleportTo(...)` 并播放末影传送音效。

### 3.3 点燃：2×2 水池判定 + 投掷碎片

```
投掷 climate_shard 落入 2×2 源水
  → ClimatePortalHandler（Game 总线，EntityTickEvent.Post，每 5 tick 节流扫描 ItemEntity）
  → WeatherPortalManager.tryActivate
  → isPoolValid：四格均为 source water
  → isRingValid：外环 12 格全部 ∈ #weather_realm:climate_portal_frames
  → ignite：消耗 1 碎片，2×2 覆写为 weather_portal，打雷音效 + 电火花 / 末地烛 / 暴雪粒子演出
```

- 碎片可在四格中任一格落点，`tryActivate` 依次尝试 4 个候选最小角。

### 3.4 四大气候平替标签

框架标签 `#weather_realm:climate_portal_frames` 由四个子标签组合（`climate_portal_frames.json` 内 `#replace:false`）：

| 子标签 | 主题 | 可平替材料 |
| :- | :- | :- |
| `#weather_realm:portal_frames_ice` | 极寒之石 | `ice` / `packed_ice` / `blue_ice` / `snow_block` / `powder_snow` |
| `#weather_realm:portal_frames_abyss` | 晦暗之石 | `obsidian` / `crying_obsidian` |
| `#weather_realm:portal_frames_sand` | 荒漠之石 | `sand` / `red_sand` / `sandstone` / `red_sandstone` / `gravel` |
| `#weather_realm:portal_frames_nether` | 烈焰之石 | `magma_block` / `nether_bricks` / `red_nether_bricks` / `netherrack` |

> 设计意图：「凡同类材料皆可任意平替」，玩家无需苛求稀有蓝冰或原版黑曜石。

### 3.5 落点与停机坪

`findOrCreateGate`：先沿目标列邻域扫描既有门（`findGateNear` + `normalizeGate` 归位到 2×2 最小角，免 BlockEntity、重启安全）；若无，则：

1. 取 2×2 范围内四方 `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES` 的最大高度作为**安全地表面**；
2. 收敛到 `[minBuildHeight+3, maxBuildHeight-4]`；
3. `buildLanding` 铺 **5×5 停机坪**：内 3×3 `packed_ice` 地板 + 外环 `blue_ice` 边框，并清空上方 4×4×3 空间；
4. `placeGate` 放置 2×2 `weather_portal`。

---

## 4. 维度地质、生态与生物体系

### 4.1 维度与群系

| 文件 | 关键值 |
| :- | :- |
| `dimension/crystal_realm.json` | `noise` 生成器，`settings = weather_realm:crystal_realm`，`biome_source = fixed → weather_realm:crystal_plains` |
| `dimension_type/crystal_realm.json` | `natural=true`、`has_skylight=true`、`ambient_light=0.2`、`fixed_time=6000`、`min_y=-64`、`height=384`、`logical_height=384`、`effects=minecraft:overworld` |
| `worldgen/noise_settings/crystal_realm.json` | `default_block = weather_realm:permafrost`；垂直梯度：基岩 → `deep_permafrost`（y≤0，8 格过渡）→ 地表规则（顶层 `snow_block` / `packed_ice` / `ice` / `powder_snow` / `gravel`） |
| `worldgen/biome/crystal_plains.json` | 温度 `-0.5`、降水 `0.9`；生物刷新：`frost_sheep`(12) / `frost_cow`(10) / `frost_pig`(10) / `frost_cat`(3) |

- 高程 `min_y=-64` / `height=384` 与 `dimension_type` **严格一致**（防加载区块崩溃）。

### 4.2 冻土地质与 8 种冻土矿石

| 方块 | 硬度 | 抗爆 | 说明 |
| :- | :- | :- | :- |
| `permafrost` | 2.25F | 6.0F | 浅层基材（stone 1.5 × 1.5） |
| `deep_permafrost` | 4.5F | 6.0F | 深层基材（deepslate 1.5 × 3.0） |

- **1.5× 硬度规则**：冻土系硬度 = 对应原版 × 1.5，抗爆统一 6.0F。
- 8 种原版平替矿石（每种含浅层 `permafrost_*` / 深层 `deep_permafrost_*`）：

| 矿石 | 掉落 | 数量 | 附魔 |
| :- | :- | :- | :- |
| 煤 | `minecraft:coal` | 1 | `ore_drops` |
| 铜 | `minecraft:raw_copper` | 2–5 | `ore_drops` |
| 铁 | `minecraft:raw_iron` | 1 | `ore_drops` |
| 金 | `minecraft:raw_gold` | 1 | `ore_drops` |
| 红石 | `minecraft:redstone` | 4–5 | `uniform_bonus_count` |
| 绿宝石 | `minecraft:emerald` | 1 | `ore_drops` |
| 青金石 | `minecraft:lapis_lazuli` | 4–9 | `ore_drops` |
| 钻石 | `minecraft:diamond` | 1 | `ore_drops` |

- 另有极寒专属第 9 类：`permafrost_blizzard_crystal_ore` / `deep_permafrost_blizzard_crystal_ore` → `blizzard_crystal`。
- 世界生成：统一注册于 `crystal_plains.json` **features step 6（UNDERGROUND_ORES）**；精准采集走 `alternatives + match_tool`；双倍烧炼（`smelting=400` / `blasting=200`）。

### 4.3 坚冰木 13 件全套家族

| # | 注册 ID | 中文名 | 类型 |
| :- | :- | :- | :- |
| 1 | `frost_log` | 坚冰原木 | `FrostLogBlock`（轴向原木） |
| 2 | `frost_wood` | 坚冰木 | `FrostLogBlock`（四面树皮木干） |
| 3 | `stripped_frost_log` | 去皮坚冰原木 | `FrostLogBlock` |
| 4 | `stripped_frost_wood` | 去皮坚冰木 | `FrostLogBlock` |
| 5 | `frost_planks` | 坚冰木板 | 简单方块 |
| 6 | `frost_stairs` | 坚冰木楼梯 | `StairBlock` |
| 7 | `frost_slab` | 坚冰木台阶 | `SlabBlock` |
| 8 | `frost_fence` | 坚冰木栅栏 | `FenceBlock` |
| 9 | `frost_fence_gate` | 坚冰木栅栏门 | `FenceGateBlock`（`FROST_WOOD_TYPE`） |
| 10 | `frost_door` | 坚冰木门 | `DoorBlock`（`FROST_BLOCK_SET_TYPE`） |
| 11 | `frost_trapdoor` | 坚冰木活板门 | `TrapDoorBlock` |
| 12 | `frost_pressure_plate` | 坚冰木压力板 | `PressurePlateBlock` |
| 13 | `frost_button` | 坚冰木按钮 | `ButtonBlock` |

- 四个原木/木干方块（1–4）**共用同一属性工厂** `frostWoodPillarProperties()`：`mapColor=ICE`、`instrument=BASS`、`strength(2.0F)`、`sound=WOOD`、`ignitedByLava()`，确保外观与属性完全对齐。
- 材质组：`FROST_BLOCK_SET_TYPE = new BlockSetType("frost")` + `FROST_WOOD_TYPE = new WoodType("frost", FROST_BLOCK_SET_TYPE)`。
- 标签：`#weather_realm:frost_logs`（1–4）、`#minecraft:logs` / `#minecraft:leaves`（原木 / 树叶）。
- 配方：环绕 / 阶梯 / 台阶 / 栅栏 / 门 / 活板门 / 压力板 / 按钮 / 去皮等全套。

**`FrostWoodMapping` 映射**（云杉族 → 坚冰木族）：结构生成期保留全部匹配 `Property`（facing / half / open / axis / waterlogged…）。

| 原版 | 坚冰木 |
| :- | :- |
| `minecraft:spruce_log` | `weather_realm:frost_log` |
| `minecraft:spruce_wood` | `weather_realm:frost_wood` |
| `minecraft:stripped_spruce_log` | `weather_realm:stripped_frost_log` |
| `minecraft:stripped_spruce_wood` | `weather_realm:stripped_frost_wood` |
| `minecraft:spruce_planks` | `weather_realm:frost_planks` |
| `minecraft:spruce_stairs` | `weather_realm:frost_stairs` |
| `minecraft:spruce_slab` | `weather_realm:frost_slab` |
| `minecraft:spruce_fence` | `weather_realm:frost_fence` |
| `minecraft:spruce_fence_gate` | `weather_realm:frost_fence_gate` |
| `minecraft:spruce_door` | `weather_realm:frost_door` |
| `minecraft:spruce_trapdoor` | `weather_realm:frost_trapdoor` |
| `minecraft:spruce_pressure_plate` | `weather_realm:frost_pressure_plate` |
| `minecraft:spruce_button` | `weather_realm:frost_button` |

### 4.4 四大冰原特化生物

| 实体 | 中文名 | 基类 | 产物 / 特性 |
| :- | :- | :- | :- |
| `frost_sheep` | 冰原羊 | `Sheep` | 剪毛得 `frost_wool`（1–3）；掉落 `frost_mutton`（带 `furnace_smelt`）；三套 loot（普通 / 剪毛 / 幼体） |
| `frost_cow` | 冰原牛 | `Cow` | 空桶挤奶得 `frost_milk_bucket`；掉落 `frost_beef` / `frost_leather` |
| `frost_pig` | 冰原猪 | `Pig` | 掉落 `frost_porkchop` + `frost_pelt` |
| `frost_cat` | 冰原猫 | `Cat` | 雪原猫变体；掉落对应 loot |

- 属性：`EntityAttributeCreationEvent` 直接复用原版 `Sheep/Cow/Pig/Cat.createAttributes()`。
- 刷新：`RegisterSpawnPlacementsEvent` 以 `Operation.REPLACE` 注册 `ON_GROUND` + `MOTION_BLOCKING_NO_LEAVES`，规则允许站在冻土可种植方块或任意实心方块上。
- 渲染：`client.ModEntityRenderers` 注册 4 个实体渲染器（羊含 `FrostSheepFurLayer`）。
- 食物：`frost_mutton` / `frost_beef` / `frost_porkchop` 及其 `cooked_*`；`FrostMilkBucketItem` 继承原版牛奶桶行为（32 tick 饮用动画、清除效果、返还空桶）。
- 新增方块 `frost_wool`（雪色、羊毛音效、`ignitedByLava`），供御寒套与合成使用。

### 4.5 雪原村庄动物拦截替换

`FrostWoodProcessor.processEntity` 在结构生成期改写 NBT 中的 `id`：

| 原版实体 | 替换为 |
| :- | :- |
| `minecraft:pig` | `weather_realm:frost_pig` |
| `minecraft:cat` | `weather_realm:frost_cat` |

- 保留其余 NBT（位置、旋转等），仅换 `id`，故村庄内**不会**再生成原版猪 / 猫。

---

## 5. 异界地表地牢与村庄系统

### 5.1 结构总表

| 结构 ID | 中文名 | 类型 | 生成群系 | start_pool | 间距 / 分离 / salt |
| :- | :- | :- | :- | :- | :- |
| `weather_realm:crystal_village` | 坚冰木雪原村庄 | 自定义 `frost_village` | `weather_realm:crystal_plains` | `minecraft:village/snowy/town_centers` | `spacing=34` / `separation=12` / `10387312` |
| `weather_realm:shelter` | 避难所 | `minecraft:jigsaw` | `weather_realm:crystal_plains` | `weather_realm:shelter` | `spacing=28` / `separation=14` / `19024503` |
| `weather_realm:yanjiang` | 熔岩遗迹 | `minecraft:jigsaw` | `weather_realm:crystal_plains` | `weather_realm:yanjiang` | `spacing=24` / `separation=12` / `19024502` |
| `weather_realm:fire` | 烈火祭坛 | `minecraft:jigsaw` | `weather_realm:crystal_plains` | `weather_realm:fire` | `spacing=20` / `separation=10` / `19024501` |
| `weather_realm:weather_altar` | 村庄气象祭坛 | `minecraft:jigsaw` | `plains` / `snowy_plains` / `sunflower_plains` | `weather_realm:weather_altar` | 表现为独立小品（`size=1`） |

- 三座异界地表地牢（shelter / yanjiang / fire）均为 `size=1`、`project_start_to_heightmap=WORLD_SURFACE_WG`、`start_height.absolute=1`、`terrain_adaptation=beard_thin`，各自对应 `data/weather_realm/structure/*.nbt` 模板 + `worldgen/template_pool/*.json` 单元素池。
- 战利品：`data/weather_realm/loot_table/chests/shelter.json`、`chests/weather_altar.json`。
- `weather_altar` 的箱子掉落 `chests/weather_altar.json`：必得 `ancient_weather_tome`，另有雪块 / 铜锭 / 浮冰 / 蓝冰 / `blizzard_crystal`。

### 5.2 坚冰木雪原村庄（`FrostVillageStructure`）

- 结构类型 `weather_realm:frost_village`：完整镜像原版 `JigsawStructure` 的 CODEC（`start_pool` / `size` / `start_height` / `use_expansion_hack` / `pool_aliases` / `dimension_padding` / `liquid_settings` 等）。
- `findGenerationPoint` 先跑标准 `JigsawPlacement.addPieces`，再 `applyFrostWood`：把每个 `PoolElementStructurePiece` 的池元素经 `FrostPoolElements.withProcessors(..., FrostWoodProcessor.INSTANCE)` 重建，**把处理器烘焙进元素**，保证结构序列化后仍生效。
- `processEntity` 同时完成 §4.5 的动物替换——一个处理器同时负责「材质替换 + 生物替换」。

---

## 6. 剧情手记与 Patchouli 3D 轮换投影

### 6.1 《古代天气研究手记》与 4 篇日记

- 物品 `weather_realm:ancient_weather_tome`（`stacksTo(1)`），右键触发：
  - Patchouli 已安装 → 反射 `PatchouliAPI.openBookGUI(ServerPlayer, ResourceLocation)` 打开书 `weather_realm:weather_tome`；
  - 未安装 → 聊天栏逐条输出 4 篇摘要（`message.weather_realm.tome.*`）。
- 书结构：`book.json`（`model = weather_realm:ancient_weather_tome`、青色书本贴图）+ 分类 `lore`（「老炼金术士日记」）+ 4 篇条目：

| 条目 | 中文标题 | 阶段 |
| :- | :- | :- |
| `prologue` | 天候纪元 34 年 · 天旱 | 老炼金术士初凝晶核（天旱） |
| `ritual` | 天候纪元 37 年 · 神迹调谐台 | 核心落于调谐基座（调谐） |
| `fracture` | 天候纪元 39 年 · 失控与碎裂 | 雷霆震裂核心，剥离气候碎片（碎裂） |
| `dimension` | 天候纪元 41 年 · 裂隙之门（终章） | 十二基石环抱 2×2 水池，投入碎片开门（裂隙） |

- 本地化：`assets/weather_realm/patchouli_books/weather_tome/{zh_cn,en_us}/...` 双语。

### 6.2 3D 轮换多方块模型规范

`dimension.json` 内嵌 `patchouli:multiblock` 页，`pattern` 为一层 4×4 矩阵，`mapping` 直接把四个**标签**作为材质，实现「同类平替」可视化：

```
pattern: 4×4
  o i s n
  i 0 w s
  s w w i
  n i s o

mapping:
  i → #weather_realm:portal_frames_ice     (极寒之石)
  o → #weather_realm:portal_frames_abyss   (晦暗之石)
  s → #weather_realm:portal_frames_sand    (荒漠之石)
  n → #weather_realm:portal_frames_nether  (烈焰之石)
  w / 0 → minecraft:water                  (2×2 水源)
```

- `enable_visualize: true` + `symmetrical: false`。
- 点击「Visualize」后，Patchouli 会**每秒自动轮播**该标签下所有可替代方块（冰 / 浮冰 / 蓝冰 / 雪块 / 黑曜石…），玩家无需记忆具体方块。
- 该映射与 §3.4 的四个数据标签**同源**，文档 / 游戏内 / 实际判定三者一致。

---

## 7. 构建、部署与联调流水线

### 7.1 常用 Gradle 命令

| 命令 | 用途 |
| :- | :- |
| `.\gradlew.bat build` | 构建 jar 到 `build/libs/` |
| `.\gradlew.bat runClient` | 开发客户端（集成服务端），IDE HotSwap 首选 |
| `.\gradlew.bat runServer` | 独立 Dedicated Server，验证双端隔离 / 权限 |
| `.\gradlew.bat runData` | DataGen 导出到 `src/generated/resources/` |
| `.\gradlew.bat runGameTestServer` | 无头 GameTest（CI 门禁） |

### 7.2 `deploy.ps1`：一键构建 + PCL 客户端热注入

```powershell
.\deploy.ps1                                        # 默认注入 PCL 客户端 mods 目录
.\deploy.ps1 -TargetModsDir "D:\...\versions\X\mods"  # 自定义注入目录
```

流程（`deploy.bat` 为等价 CMD 薄封装：`powershell -NoProfile -ExecutionPolicy Bypass -File deploy.ps1`）：

1. **校验 / 切换 JDK 21**：先探测 `JAVA_HOME`，否则回退 `C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot`；均无效则 `exit 1`。
2. **构建**：`.\gradlew.bat build`，非零退出码立即中止并透传。
3. **挑选 jar**：`build\libs\` 下最新 `weather_realm-*.jar`（排除 `-sources` / `-javadoc` / `-dev`），无则回退任意可部署 jar。
4. **清理旧 jar**：删除目标目录内的模板遗留 jar 与旧版 `weather_realm-*.jar`。
5. **覆盖注入**：复制到 `C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\`（不存在自动创建），打印 DEPLOY OK。

> 适用时机：PCL 启动器游玩联调、多客户端联机、光影 / 渲染等 `runClient` 覆盖不到的场景。注入后需重启客户端；**新增注册项不可热更**（见 `AGENTS.md` §6）。

### 7.3 常用游戏内定位与调试指令

```mcfunction
# --- 维度与传送 ---------------------------------------------------------
/execute in weather_realm:crystal_realm run tp @s 0 100 0
/execute in weather_realm:crystal_realm run spreadplayers 0 0 10 50 under 256 false @s   # 安全地表落点
/execute in minecraft:overworld run tp @s 0 100 0

# --- 定位与放置 ---------------------------------------------------------
/locate biome weather_realm:crystal_plains
/locate structure weather_realm:crystal_village
/locate structure weather_realm:weather_altar
/locate structure weather_realm:shelter
/locate structure weather_realm:yanjiang
/locate structure weather_realm:fire
/place structure weather_realm:weather_altar

# --- 祭坛 / 传送门联调 --------------------------------------------------
/setblock ~ ~ ~ weather_realm:weather_pedestal
/setblock ~ ~1 ~ weather_realm:weather_altar_core           # 右键核心打开调控 UI
/give @s weather_realm:climate_shard                        # 投入 2×2 水池点燃传送门
/give @s weather_realm:ancient_weather_tome                 # 右键阅读手记

# --- 实体 / 环境 --------------------------------------------------------
/summon weather_realm:frost_sheep ~ ~ ~ {NoAI:1b}
/gamerule doWeatherCycle false
/gamerule keepInventory true
```

> ⚠️ 新增维度 / 群系 / 结构属**动态注册表**，`/reload` **无效**，需退出到主菜单重进存档。
> ⚠️ 配方 / 标签 / 战利品表 / 函数改动走 `/reload`（~1 秒）；贴图 / 模型 / 语言需 IDE `Ctrl+F9` 后游戏内 `F3+T`。

---

> **备注**：本文档为架构总览与现状归档，随实现推进修订；设计层冲突以 GDD 为准，技术层冲突以 `AGENTS.md` 为准。
