# 《天象之境》实现现状架构（Architecture）

> **文档定位**：本文是**实现现状（as-built）**文档，回答「代码现在长什么样、谁调用谁」。
> 玩法意图、路线图与逐项实现状态见 [`docs/DESIGN.md`](DESIGN.md)；版本矩阵、代码规约、热重载边界、质量门禁以根目录 [`AGENTS.md`](../AGENTS.md) 为准。
> **最后更新**：2026-10-01（对齐 HEAD `3029c56`）。
> **命名空间**：`weather_realm` · **显示名**：天象之境 / Weather Realm · **modId**：`weather_realm`。
> **方法**：本文每条结构性断言均按最终代码逐条核对，标注文件路径与行号（行号为该文件总行数标注中的实际位置）。任何与代码不一致处，以代码为准。

---

## 1. 概述

| 项 | 值 | 来源 |
| :- | :- | :- |
| modId / 显示名 | `weather_realm` / `Weather Realm`（中文「天象之境」由 lang 提供） | `WeatherRealm.java:17`、`gradle.properties:29,31` |
| 版本 | `1.0.0` | `gradle.properties:35` |
| 自建维度 | `weather_realm:crystal_realm` | `ModDimensions.java:16`、`data/weather_realm/dimension/crystal_realm.json:2` |
| 群系（3 个） | `crystal_plains`（水晶平原）/ `blazing_plains`（烈焰平原）/ `arid_wasteland`（干旱荒原） | `data/weather_realm/worldgen/biome/*.json` |
| 主类 | `WeatherRealm`，**仅 42 行**，只做注册编排与配置注册 | `WeatherRealm.java:23-41` |

### 1.1 核心玩法一图流

```
主世界 / 创造模式取得 climate_shard（气候碎片）
        │
        ▼
搭建 4×4 气候池：py-2 承台 → py-1 的 2×2 源水 + 外环 12 格框架
        │  （12 格需 ∈ #weather_realm:climate_portal_frames：ice/abyss/sand/nether 四类）
        ▼
向水池投掷 climate_shard
        │  每 5 tick 扫描（ClimatePortalHandler.onEntityTick）
        │  校验 isPoolValid（四格水源）+ isRingValid（外环 12 格）
        ▼
ignite()：消耗 1 碎片 → 2×2 写为 weather_portal → 雷电/粒子演出
        │
        ▼
踏入 weather_portal（WeatherPortalBlock.entityInside）
        │  → getPortalDestination()：目标维度 = crystal_realm ⇄ overworld
        │  → 在 crystal_realm 内搜索最近 crystal_plains，落点铺 3×3 浮冰承台
        ▼
crystal_realm：三群系勘探（永冻挖 blizzard_crystal / 燃焰挖 blaze_crystal / 风沙挖 wind_crystal）
        │  三群系地表材质由 SurfaceSystemMixin → ModSurfaceRules 全柱替换
        ▼
（可选）weather_altar_core 置于 weather_pedestal 之上
        │  右键打开「天象调控仪」→ SetWeatherPayload → 服务端 setWeatherParameters 改写天气
        ▼
手持极域天象图（biome_map）查看已探索群系，右键切换 2/4 区块/像素
```

---

## 2. 模块结构树

根路径：`src/main/java/com/example/weather_realm/`。括号内为该文件**实测总行数**。

### 2.1 根包（注册与全局）

| 文件（行数） | 职责 |
| :- | :- |
| `WeatherRealm.java` (42) | `@Mod` 入口（`:14`）；`MODID`/`LOGGER`；构造器按固定顺序调用各 `Mod*` 注册（`:25-33`），注册创造标签监听（`:36`）与 COMMON/CLIENT 配置（`:39-40`）。 |
| `ModBlocks.java` (444) | 方块 `DeferredRegister.Blocks`（`:51`）；坚冰木/焦木/风化木族、祭坛核心、传送门、地质与 54 个矿石方块、三群系植被；含 `GENERATED_ORES` 列表与 `register(IEventBus)`（`:441`）。 |
| `ModItems.java` (256) | 物品 `DeferredRegister.Items`（`:51`）；全部 BlockItem、晶石、手记、天象图、气候碎片、护甲/工具、刷怪蛋、食物、动物掉落；`ARMOR_MATERIALS`（`:121`）；`register`（`:249`）与 `registerArmorMaterials`（`:253`）。 |
| `ModCreativeTabs.java` (143) | 创造标签 `weather_realm_tab`（`:27`，图标为冰晶花）；`displayItems` 逐项登记；`addCreative` 把冻土建材加入原版「建筑方块」页（`:137`）。 |
| `ModBlockProperties.java` (105) | 方块属性工厂（`stoneLike` / 各矿石 / 木柱 / 植物），消除重复的属性配方。 |
| `ModStructureTypes.java` (38) | 结构类型 + 结构处理器注册：`frost_village`（`:27`）、`frost_wood_replace`（`:31`）。 |
| `ModSounds.java` (28) | 音效注册：`music.biome.crystal_plains`（`:20`）。 |
| `ModBlockEntities.java` (28) | 方块实体注册：`weather_altar_core`（`:21`）。 |
| `ModParticles.java` (26) | 粒子类型注册：`blizzard_snow`（`:20`）。 |
| `ModDimensions.java` (19) | 维度 `ResourceKey` 常量 `CRYSTAL_REALM`（`:16`）。 |
| `ModEntities.java` (47) | 实体类型注册：冰原羊/牛/猪/猫（`:24-46`）。 |
| `ModEntityEvents.java` (61) | 实体属性（`:31`）与刷怪放置（`:38`）；`frost_plantable_on` 或实心面上可刷（`:55-60`）。 |
| `ModTags.java` (33) | 标签常量：`FROST_PLANTABLE_ON`（`:20`）、`CLIMATE_PORTAL_FRAMES`（`:29`）。 |
| `ServerWeatherHandler.java` (45) | Game 总线：玩家登录/换维进入 `crystal_realm` 时推送 `START_RAINING` 等降水事件（`:38-43`）。 |

### 2.2 `block/`（方块与方块实体）

| 文件（行数） | 职责 |
| :- | :- |
| `WeatherPortalBlock.java` (163) | 传送门方块，`extends Block implements Portal`；空选框（`:62`）、不可冲毁（`:68`）、`entityInside`→`setAsInsidePortal`（`:79`）、`getPortalDestination`（`:87`）、粒子（`:148`）。 |
| `WeatherAltarCoreBlock.java` (36) | 祭坛核心 `BaseEntityBlock`（渲染形状 INVISIBLE，视觉由 BER 接管）。 |
| `WeatherAltarCoreBlockEntity.java` (22) | 核心方块实体（无数据，仅用于挂 BER）。 |
| `FrostPlantBlock.java` (44) | 冰系植物共享基类：仅可种在 `frost_plantable_on` 或满层雪上（`:28-43`）。 |
| `BiomePlantBlock.java` (58) | 燃焰/风沙植物共享基类，用 `Ground` 枚举选择 `mayPlaceOn`（`:48-57`）；合并了原 `FirePlantBlock`/`AridPlantBlock`。 |
| `FrostBonemeal.java` (67) | 骨粉扩散共享助手：`spreadArea`（`:27`）/`duplicateNearby`（`:45`）。 |
| `FrostFlowerBlock.java` (62) | 冰晶花；骨粉复制到邻近雪块，否则掉落自身（`:55-60`）。 |
| `FrostGrassBlock.java` (57) | 冰雪草；骨粉以 5×5 扩散冰雪草/冰晶花（`:51-56`）。 |
| `FrostSproutBlock.java` (33) | 霜草幼芽（剪刀采集）。 |
| `GlacierBloomBlock.java` (37) | 冰川兰；可种在草/土/冻土与 frost-plantable。 |
| `TallFrostFlowerBlock.java` (51) | 两格高霜冻花；骨粉掉落自身。 |
| `TallFrostGrassBlock.java` (30) | 两格高霜草。 |
| `FrostSaplingBlock.java` (116) | 坚冰木树苗；骨粉生成 6–8 格高锥形冷杉（`:56-83`）。 |
| `FrostLeavesBlock.java` (21) | 坚冰木树叶（原版落叶逻辑）。 |
| `FrostLogBlock.java` (21) | 坚冰木/焦木/风化木共用轴向柱方块。 |

### 2.3 `item/`

| 文件（行数） | 职责 |
| :- | :- |
| `BiomeMapItem.java` (181) | 极域天象图，`extends MapItem`；固定 `MapId(-1)`（`:50`）；档位 2/4 区块/像素（`:52-61`）；`getCustomMapData` 走客户端注入的 `Function`（`:110`），右键切档（`:123`）。 |
| `ClimateShardItem.java` (18) | 气候碎片，**刻意惰性**（点燃逻辑在 `portal/ClimatePortalHandler`）。 |
| `AncientWeatherTomeItem.java` (87) | 《古代天气研究手记》；反射调 Patchouli，缺失时聊天栏降级（`:48-86`）。 |
| `FrostMilkBucketItem.java` (16) | 冰牛奶桶（继承原版牛奶行为）。 |

### 2.4 `client/`（仅客户端；Common 禁引）

| 文件（行数） | 职责 |
| :- | :- |
| `ClientSetup.java` (28) | 客户端初始化入口（`FMLClientSetupEvent`，`Dist.CLIENT`，`:18-27`）。 |
| `ClientBlizzardEffects.java` (102) | 常驻暴风雪：按群系平滑强度、驱动原版降水渲染、生成粒子（`:41-101`）。 |
| `BlizzardSnowParticle.java` (105) | 自定义雪花粒子 + `Provider`（`:92`）。 |
| `ClientParticleProviders.java` (24) | 注册 `blizzard_snow` 的 sprite provider（`:22`）。 |
| `ClientBlockEntityRenderers.java` (30) | 注册祭坛核心 BER + 加载两个附加模型（`:22-28`）。 |
| `WeatherAltarCoreRenderer.java` (94) | 祭坛核心半透明外壳 + 悬浮旋转内晶 + 光环粒子。 |
| `WeatherAltarInteractionHandler.java` (42) | 右键（核心下方为基座）打开调控界面（`:28-41`）。 |
| `gui/WeatherControlScreen.java` (61) | 「天象调控仪」：晴/雨/雷三键 → `SetWeatherPayload`（`:46-49`）。 |
| `ClientTooltipHandler.java` (96) | 为模组物品附加英文注解与 `.desc` 行。 |
| `ModEntityRenderers.java` (26) | 注册四种冰原动物渲染器。 |
| `FrostSheepRenderer.java` (28) / `FrostSheepFurLayer.java` (41) | 冰原羊本体与羊毛层。 |
| `FrostCowRenderer.java` (17) / `FrostPigRenderer.java` (17) / `FrostCatRenderer.java` (17) | 薄壳子类，仅换贴图。 |
| `FrostEntityTextures.java` (27) | 三个冰系生物贴图路径共享。 |
| `map/BiomeMapClientData.java` (249) | 天象图像素数据源：128×128 `MapItemSavedData`、探索迷雾、落图缓存、调色（`:94-248`）。 |
| `map/BiomeMapExplorationState.java` (58) | 客户端探索状态（内存 `LongSet`，登录时清空）。 |

### 2.5 `world/`（世界生成）

| 文件（行数） | 职责 |
| :- | :- |
| `ModSurfaceRules.java` (193) | 三群系全柱地表规则；`wrapSurfaceRules` 记忆化（`:95`）、`createModRules`（`:126`）、三群系规则（`:139`/`:159`/`:179`）。 |
| `FrostVillageStructure.java` (153) | 仿 `JigsawStructure`，把放置元素改写为带 `FrostWoodProcessor`（`:97-147`）。 |
| `FrostWoodProcessor.java` (79) | 把云杉族换为坚冰木族、把猪/猫实体换为冰原猪/冰原猫（`:31-33`、`:59-78`）。 |
| `FrostWoodMapping.java` (75) | 云杉 → 坚冰木的方块状态重映射（保留 facing/axis 等属性）。 |
| `FrostPoolElements.java` (59) | 给池元素烘焙额外处理器，使其在结构序列化后仍存活。 |

### 2.6 其余包

| 文件（行数） | 职责 |
| :- | :- |
| `portal/ClimatePortalHandler.java` (160) | `@EventBusSubscriber`（Game）：`onEntityTick`（`:40`）、`tryActivate`（`:65`）、`isPoolValid`（`:89`）、`isRingValid`（`:102`）、`ignite`（`:117`）。 |
| `network/ModNetwork.java` (48) | `@EventBusSubscriber`（Mod）：注册载荷与处理（`:30-47`）。 |
| `network/SetWeatherPayload.java` (27) | C2S 天气请求 record + `Type`/`StreamCodec`。 |
| `network/WeatherMode.java` (46) | 晴/雨/雷枚举 + codec + 消息键。 |
| `command/PortalCommands.java` (106) | `/build_portal`（OP 2+）：脚下铺未激活底座（`:34-105`）。 |
| `entity/FrostSheep.java` (91) / `FrostCow.java` (47) / `FrostPig.java` (38) / `FrostCat.java` (35) | 四种冰原动物（掉落/挤奶/剪毛/繁衍覆写）。 |
| `config/WeatherRealmConfig.java` (80) | COMMON + CLIENT 两个 `ModConfigSpec`（见 §6）。 |
| `mixin/SurfaceSystemMixin.java` (21) | 跳板模式：`@ModifyVariable` 在 `SurfaceSystem.buildSurface` HEAD 处委托 `ModSurfaceRules.wrapSurfaceRules`（`:12-19`）。 |

> Mixin 声明在 `src/main/resources/weather_realm.mixins.json`，经 `src/main/templates/META-INF/neoforge.mods.toml:48-49` 的 `[[mixins]]` 加载。

---

## 3. 注册表分工

### 3.1 每个 `Mod*` 类管什么

| 类 | 注册表 | 入口 |
| :- | :- | :- |
| `ModBlocks` | `BLOCKS`（方块 + BlockSetType/WoodType） | `ModBlocks.register(bus)` |
| `ModItems` | `ITEMS`；额外 `ARMOR_MATERIALS` | `ModItems.register(bus)` / `registerArmorMaterials(bus)` |
| `ModCreativeTabs` | `CREATIVE_MODE_TABS` | `ModCreativeTabs.register(bus)` + `modEventBus.addListener(ModCreativeTabs::addCreative)` |
| `ModParticles` | `PARTICLE_TYPES` | `ModParticles.register(bus)` |
| `ModSounds` | `SOUND_EVENTS` | `ModSounds.register(bus)` |
| `ModEntities` | `ENTITY_TYPES` | `ModEntities.ENTITY_TYPES.register(bus)` |
| `ModStructureTypes` | `STRUCTURE_TYPES` + `STRUCTURE_PROCESSORS` | `ModStructureTypes.register(bus)` |
| `ModBlockEntities` | `BLOCK_ENTITY_TYPE` | `ModBlockEntities.register(bus)` |

### 3.2 `WeatherRealm` 构造器调用顺序（以代码为准）

`WeatherRealm.java:25-40` 顺序固定为：

```
ModBlocks.register        // BLOCKS
ModItems.register         // ITEMS
ModCreativeTabs.register  // CREATIVE_MODE_TABS
ModParticles.register     // PARTICLE_TYPES
ModSounds.register        // SOUND_EVENTS
ModItems.registerArmorMaterials // ARMOR_MATERIALS
ModEntities.ENTITY_TYPES.register // ENTITY_TYPES
ModStructureTypes.register // STRUCTURE_TYPES + STRUCTURE_PROCESSORS
ModBlockEntities.register  // BLOCK_ENTITY_TYPE
modEventBus.addListener(ModCreativeTabs::addCreative)
modContainer.registerConfig(COMMON, WeatherRealmConfig.COMMON_SPEC)
modContainer.registerConfig(CLIENT, WeatherRealmConfig.CLIENT_SPEC)
```

该顺序保留了拆分前单体类的声明顺序，注释见 `WeatherRealm.java:24`。

### 3.3 新增一个方块 / 物品，要改哪几个文件

1. `ModBlocks.java`：`BLOCKS.registerSimpleBlock("id", props)` 或 `BLOCKS.registerBlock(...)`；复用 `ModBlockProperties` 的属性工厂。
2. `ModItems.java`：`ITEMS.registerSimpleBlockItem("id", ModBlocks.X)`（两格高/门用 `DoubleHighBlockItem`）。
3. `ModCreativeTabs.java`：在 `displayItems` 里 `output.accept(...)`（若要在本模组页显示）。
4. 资源：`assets/weather_realm/blockstates/<id>.json`、`models/block|item/<id>.json`、`textures/block|item/<id>.png`、`lang/zh_cn.json` + `lang/en_us.json`。
5. 可选数据：`data/weather_realm/loot_table/blocks/<id>.json`、`recipe/<id>.json`、`data/minecraft/tags/block/...`（可挖性/木族/花等）。
6. **必须重启客户端**（静态注册表在启动期冻结，见 `AGENTS.md` §6.1 与 §10 `Registry is already frozen`）。

---

## 4. 双端隔离规约

- **Common 侧禁止引用 `net.minecraft.client.*`**（`AGENTS.md` §5.3）。当前 `WeatherRealm.java` 对 `net.minecraft.client` **0 命中**。
- 客户端初始化入口是 `client/ClientSetup.java`（`@EventBusSubscriber(..., value = Dist.CLIENT)`），原先嵌在主类的 `ClientModEvents` 已迁出。
- 天象图的 Common→Client 桥接：`BiomeMapItem`（Common）只持有一个 `Function<Level, MapItemSavedData>` 静态字段（`BiomeMapItem.java:69`），由客户端类 `BiomeMapClientData` 的静态初始化器安装（`BiomeMapClientData.java:83-88`）。因此专用服务端加载 `BiomeMapItem` 时不会触碰客户端类。
- 其余客户端专属逻辑（渲染、粒子、GUI、地图）全部位于 `client/**`，并以 `Dist.CLIENT` 或 `value = Dist.CLIENT` 的 `@EventBusSubscriber` 绑定。
- 验证手段：`.\gradlew.bat runServer` 必须干净启动（`AGENTS.md` §9.2）。

---

## 5. 关键数据流

### 5.1 传送门激活（投掷气候碎片）

入口：`portal/ClimatePortalHandler.java`

```
EntityTickEvent.Post
  └─ onEntityTick (:40)  仅处理 ItemEntity；扫描节流 level.getGameTime() % PORTAL_SCAN_INTERVAL_TICKS (:49-52)
       └─ 必须是 climate_shard (:53-56)
            └─ tryActivate (:65)
                 ├─ 当前格必须是水 (:70)
                 ├─ 以该格四个可能的最小角逐一尝试 (:77-84)
                 ├─ isPoolValid (:89)   2×2 四格均须为水源
                 └─ isRingValid (:102)  外环 4×4 去中心 2×2 = 12 格均 ∈ CLIMATE_PORTAL_FRAMES
                      └─ ignite (:117)
                           ├─ 消耗 1 碎片、空则 discard (:119-124)
                           ├─ 2×2 写为 weather_portal (:126-131)
                           └─ 雷声/龙吟 + 电火花/末地烛/暴雪粒子 (:137-158)
```

### 5.2 玩家传送（维度往返）

入口：`block/WeatherPortalBlock.java`

```
entityInside (:79)  canUsePortal(false) → entity.setAsInsidePortal(this, pos) (:80-82)
  └─ 引擎 Portal 管线调用 getPortalDestination (:87)
       ├─ 目标维度：当前是 crystal_realm → overworld，否则 → crystal_realm (:89-92)
       ├─ 目标是 overworld：用 targetLevel.getSharedSpawnPos() 作落点 (:98-100)
       └─ 目标是 crystal_realm (:101-135)：
            ├─ findClosestBiome3d 搜索最近 crystal_plains (:110-116)
            │    半径 / 水平步长 / 垂直步长来自 WeatherRealmConfig (:107-109)
            ├─ targetLevel.getHeight(MOTION_BLOCKING, x, z) 取地表 (:120-121)
            └─ 3×3 承台：可替换处铺 PACKED_ICE，上方两格清空 (:123-133)
       └─ 返回 DimensionTransition(目标level, 目标Vec3, ZERO, yRot, xRot,
              PLAY_PORTAL_SOUND.then(PLACE_PORTAL_TICKET)) (:137-144)
```

> 未找到维度时 `getPortalDestination` 返回 `null`（`:93-95`）——**没有**自定义“门缺失”聊天提示（旧设计稿/旧 lang 键 `message.weather_realm.portal.no_dimension` 已删除）。

### 5.3 天气系统

- **服务端**：`ServerWeatherHandler.syncWeather`（`:38-43`）只对进入 `crystal_realm` 的玩家推送 `START_RAINING` + `RAIN_LEVEL_CHANGE(1.0)` + `THUNDER_LEVEL_CHANGE(1.0)`，保证原版雪渲染立即工作；**不再每 tick 全局锁定维度天气**（避免覆盖燃焰/风沙群系）。
- **客户端**：`ClientBlizzardEffects.onClientTick`（`client/ClientBlizzardEffects.java:41`）在 `crystal_realm` 内按当前群系（仅 `crystal_plains`）把暴风雪强度平滑逼近目标（`:62-65`），驱动 `level.setRainLevel`（`:68`）并生成 `blizzard_snow` 粒子（`:83-100`）。
- **手动天气**：`gui/WeatherControlScreen.selectMode`（`:46`）→ `PacketDistributor.sendToServer(new SetWeatherPayload(mode))` → `ModNetwork.handleSetWeather`（`network/ModNetwork.java:35`）→ `ServerLevel.setWeatherParameters`，时长取 `WeatherRealmConfig.WEATHER_CLEAR_TIME` / `WEATHER_RAIN_TIME`（`ModNetwork.java:42-44`）。
- 配置时长见 §6。

### 5.4 天象图（手持地图）

入口：`item/BiomeMapItem.java`（Common）与 `client/map/BiomeMapClientData.java`（Client）

```
BiomeMapItem (extends vanilla MapItem)
  ├─ defaultProperties(): stacksTo(1)、DataComponents.MAP_ID = MapId(-1) (:76-80)
  ├─ use() (:123): 仅 crystal_realm 可切换，cycleZoom() 换档并提示 (:143-152)
  └─ getCustomMapData(stack, level) (:110)
       Common 侧只读静态 Function provider (:117-118)

BiomeMapClientData（@EventBusSubscriber Dist.CLIENT）
  ├─ static {} (:83) 安装 provider = BiomeMapClientData::instance
  ├─ instance(level) (:94) 创建/复用 128×128 MapItemSavedData（维度变化时重建）
  ├─ onClientTick (:108) → updatePlayerArrow + refreshIfNeeded
  │    ├─ updatePlayerArrow: addDecoration(PLAYER, 0,0, yRot) (:140-143)
  │    └─ refreshIfNeeded (:145): 仅手持时，位移/换档/空闲满 100 tick 才重绘
  │         ├─ BiomeMapExplorationState.updateExploration(半径) (:172)
  │         └─ rebuildColors (:194) 逐像素：未探索=fog / 已探索未加载=缓存 / 已加载=按群系调色
  │              → changed 时 gameRenderer.getMapRenderer().update(MAP_ID, saved) (:176)
  ├─ explorationRadiusChunks (:187) = clamp(max(renderDistance+8, halfMapChunks),16,256)
  ├─ colorFor (:234): crystal_plains→ICE / blazing_plains→FIRE / arid_wasteland→SAND / 其它→STONE
  └─ onLoggingOut (:124) 清空数据、缓存与探索状态

BiomeMapExplorationState（内存态）
  ├─ EXPLORED_CHUNKS: LongOpenHashSet (:22)
  ├─ updateExploration (:31) 标记半径内已加载区块
  ├─ isExplored (:50)
  └─ clear (:55) 登出时调用
```

> 关键事实：探索进度**不持久化**，是纯客户端内存态，登出即清空（`BiomeMapClientData.java:124-133`、`BiomeMapExplorationState.java:22`）。

### 5.5 世界生成与地表规则

- Mixin（跳板）：`mixin/SurfaceSystemMixin.java` 在 `SurfaceSystem.buildSurface` 参数 `ordinal=0` 上 `@ModifyVariable`，单行委托 `ModSurfaceRules.wrapSurfaceRules(original)`（`:18-19`）。业务全部在普通类里，可热替换。
- `ModSurfaceRules`：
  - `wrapSurfaceRules`（`:95`）用 `WRAPPED_RULES`（`ConcurrentHashMap`，`:81`）按输入规则源身份**记忆化**，避免每列重建规则树；`original==null` 时直接返回 `modRules()`。
  - 三群系规则组 `createModRules`（`:126`）→ `crystal_plains`/`blazing_plains`/`arid_wasteland`（`:139`/`:159`/`:179`）。
  - 每群系：`ABOVE_BEDROCK`（`aboveBottom(5)`，保留原版基岩层）→ 群系判定 → 序列：
    1. 顶层草皮（`FROST_MOSS` / `VOLCANIC_ASH` / `DRY_TURF`）**仅包裹在 `abovePreliminarySurface()` + `ON_FLOOR`**，避免把洞口/山体内部地板铺成草皮；
    2. `ABOVE_ZERO` → 浅层岩（`permafrost` / `fire_stone` / `weathered_sandstone`）；
    3. `BELOW_ZERO` → 深层岩（`deep_*`）。
  - 浅/深层岩分支**故意不做 `abovePreliminarySurface` 包裹**，实现整柱替换，从而让自定义 `ore_replaceables` 标签下的全套矿石生成。
  - 群系 `ResourceKey` 常量在 `:47-59`。

---

## 6. 配置（`config/WeatherRealmConfig.java`，80 行）

两个 spec 均在构造器注册（`WeatherRealm.java:39-40`）。

### 6.1 COMMON（`COMMON_SPEC`，`:55`）

| Java 字段 | 配置键 | 默认值 | 范围 | 行 |
| :- | :- | :- | :- | :- |
| `WEATHER_CLEAR_TIME` | `weatherClearTime` | 12000 | 0..MAX | `:26` |
| `WEATHER_RAIN_TIME` | `weatherRainTime` | 12000 | 0..MAX | `:31` |
| `PORTAL_SCAN_INTERVAL_TICKS` | `portalScanIntervalTicks` | 5 | 1..MAX | `:36` |
| `PORTAL_SEARCH_RADIUS` | `portalBiomeSearchRadius` | 6400 | 1..MAX | `:41` |
| `PORTAL_SEARCH_HORIZONTAL_STEP` | `portalBiomeSearchHorizontalStep` | 32 | 1..MAX | `:46` |
| `PORTAL_SEARCH_VERTICAL_STEP` | `portalBiomeSearchVerticalStep` | 64 | 1..MAX | `:51` |

### 6.2 CLIENT（`CLIENT_SPEC`，`:79`）

| Java 字段 | 配置键 | 默认值 | 范围 | 行 |
| :- | :- | :- | :- | :- |
| `BLIZZARD_FLAKES_PER_TICK` | `blizzardFlakesPerTick` | 40 | 0..MAX | `:60` |
| `BLIZZARD_RADIUS` | `blizzardRadius` | 16.0 | 0..MAX | `:65` |
| `BLIZZARD_HEIGHT` | `blizzardHeight` | 12.0 | 0..MAX | `:70` |
| `BLIZZARD_TRANSITION_STEP` | `blizzardTransitionStep` | 0.04 | 0..1 | `:75` |

> 所有项都带下界，防止误配 `0` 造成 tick 取模除零或死循环（类注释 `:12-14`）。CLIENT 项只在客户端代码读取（`ClientBlizzardEffects`）。

---

## 7. 网络协议（`network/`）

协议版本 `"1"`（`ModNetwork.java:24`）。

| Payload | 方向 | Codec | 处理 | 位置 |
| :- | :- | :- | :- | :- |
| `SetWeatherPayload(WeatherMode)` | **Client → Server**（`playToServer`，`:32`） | `StreamCodec.composite(WeatherMode.STREAM_CODEC, SetWeatherPayload::mode, SetWeatherPayload::new)` | `ModNetwork.handleSetWeather`：`enqueueWork` 内校验 `ServerPlayer`，取 config 时长调 `setWeatherParameters`，并向该玩家发聊天反馈 | `SetWeatherPayload.java:16-21`、`ModNetwork.java:35-46` |

- `WeatherMode` 枚举（`CLEAR`/`RAIN`/`THUNDER`）自带 `StreamCodec`（`WeatherMode.java:20-22`），纯数据、无客户端引用。
- **没有 S2C 载荷**；服务端→客户端的天气同步复用原版 `ClientboundGameEventPacket`（`ServerWeatherHandler.java:40-42`）。

---

## 8. 构建与部署

命令前缀 Win 一律 `.\gradlew.bat`（`AGENTS.md` §8）。

| 命令 | 用途 | 产物/说明 |
| :- | :- | :- |
| `.\gradlew.bat build` | 构建 jar | `build/libs/weather_realm-1.21.1-1.0.0.jar`（`deploy.ps1:14-18`、`gradle.properties:29,35`） |
| `.\gradlew.bat runClient` | 开发客户端（内置集成服务端） | 建议 Debug 启动以启用 HotSwap |
| `.\gradlew.bat runServer` | 独立 Dedicated Server | 验证双端隔离 |
| `.\gradlew.bat runGameTestServer` | 无头 GameTest 服务器 | 质量门禁 |
| `.\gradlew.bat runData` | DataGen | 输出 `src/generated/resources/`（当前目录为空，无 DataGen provider） |
| `.\deploy.ps1` / `.\deploy.bat` | 一键构建 + 注入 PCL mods | 见下 |

### 8.1 `deploy.ps1` 工作流（当前实现）

1. 校验 `JAVA_HOME` 为 JDK 21，否则回退 `C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot`（`deploy.ps1:45-64`）。
2. 执行 `.\gradlew.bat build`，非零退出码立即中止（`:66-73`）。
3. **运行中客户端检测**：扫描 `javaw/java` 命令行是否匹配目标实例，若疑似客户端在运行则**拒绝部署**并 `exit 1`（`:75-153`）；`-Force` 显式跳过（`:121-123`）。
4. 取 `build\libs\` 下最新 `weather_realm-*.jar`（排除 `-sources`/`-javadoc`/`-dev`），清理旧 `weather_realm-*.jar` 与遗留 `examplemod-*.jar`（`:155-196`）。
5. 覆盖复制到目标 `mods\`（默认 `C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\`，`:36`），并提示**必须完全重启客户端**（`:203-211`）。

---

## 9. 热重载边界（摘要，详见 `AGENTS.md` §6）

- **可热更（JBR HotSwap，无需重启）**：已有方法内部逻辑、新增辅助方法/字段/Lambda、跳板外置的业务类改动。
- **需 IDE 构建 + `F3+T`**：贴图 / 模型 / blockstates / 语言文件。
- **游戏内 `/reload`**：配方 / 标签 / 战利品表 / 函数。
- **退出到主界面重进存档**：附魔 / 伤害类型 / 生物群系 / 结构 / 维度等动态注册表。
- **必须重启客户端**：新增 Block/Item/Entity/GUI 类型、新增 `@SubscribeEvent`、改动 Mixin 注入点。

> 本工程实际存在的“业务外置类”命名是 `*Handler.java`（`ClimatePortalHandler`、`ServerWeatherHandler`、`ClientTooltipHandler`、`WeatherAltarInteractionHandler`）、`*Helper.java`（本项目暂无）与各 `Mod*` 注册类；**没有** `*Manager.java`。跳板示例：`SurfaceSystemMixin` → `world/ModSurfaceRules`。

---

## 10. 扩展点 / How-to

### 10.1 新增一个方块（+ 物品）
见 §3.3。注册项改动**需重启**。

### 10.2 新增一个群系
1. `data/weather_realm/worldgen/biome/<id>.json`：`has_precipitation`/`temperature`/`downfall`/`effects`/`spawners`/`features`（**必须是 11 个数组，索引 0..10**，空阶段写 `[]`）。
2. `data/weather_realm/dimension/crystal_realm.json` 的 `multi_noise.biomes` 增加参数区间（temperature 等）。
3. `world/ModSurfaceRules.java`：加 `ResourceKey<Biome>` 常量 + 一个 `createXxxRules()` 并加入 `createModRules()`。
4. `client/map/BiomeMapClientData.java`：加 `ResourceLocation` 常量与 `colorFor` 分支。
5. `lang/zh_cn.json` / `en_us.json`：加 `biome.weather_realm.<id>`。
6. 结构 / 地物若要生成，补 `worldgen/configured_feature`、`placed_feature`、`structure`、`structure_set`。
7. **退出到主界面 → 重进存档**（动态注册表，`/reload` 无效）。
8. 注意红线：`BiomeModifier` **不能**向主世界注入新群系（`AGENTS.md` §7）；不要自研 noise/density function。

### 10.3 新增一个网络包
1. 新建 `record XxxPayload(...) implements CustomPacketPayload`，声明 `Type`（`ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "...")`）与 `StreamCodec`（参考 `SetWeatherPayload.java`）。
2. 在 `network/ModNetwork.onRegisterPayloads` 用同一个 `PayloadRegistrar` 注册（`playToServer`/`playToClient`/`playBidirectional`）并写处理函数。
3. 客户端派发用 `PacketDistributor`（参考 `WeatherControlScreen.java:47`）。
4. **需重启**（`RegisterPayloadHandlersEvent` 只在启动扫一次）。

### 10.4 新增一个配置项
1. 在 `WeatherRealmConfig` 的 `COMMON_BUILDER` 或 `CLIENT_BUILDER` 上 `defineInRange(...)`（带安全下界）。
2. 在对应端代码读取（CLIENT 项只能在客户端读）。
3. **需重启**（spec 在构造器注册）；改值后重启或按 FML 配置重载流程生效。

---

## 11. 已知坑与未验证项

### 11.1 与本工程直接相关（摘自 `AGENTS.md` §10）

| 现象 | 原因 | 处理 |
| :- | :- | :- |
| `Registry is already frozen` | 运行时新增静态注册项 | 新增 Block/Item/Entity 必须重启 |
| 双端崩溃 / 命名空间冲突 | Common 引用了 `net.minecraft.client.*` | 跑 `runServer` 复现 |
| 资源改了不生效 | 未 IDE 增量构建 / 未 `F3+T` | 先 `Ctrl+F9` 再 `F3+T`；客户端读 `build/resources/main` |
| 语言文件改了不生效 | 语言文件不自动监视 | 必须 `F3+T`（或切换一次语言） |
| 加载区块即崩 | `dimension_type` 的 `min_y`/`height` 与所引 noise settings 不一致 | 复用 overworld 时必须 `-64` / `384`（本工程已是） |
| 数据包解析失败 | `features` 数组阶段数不对 | 本工程三群系实测均为 **11 段（0..10）**；仍建议用 Misode 1.21.1 校验 |

### 11.2 诚实清单（本轮无法/未在本机跑起来证实）

1. **未执行 `./gradlew build` / `runClient` / `runServer` / `runGameTestServer`**：本文件全部结论来自静态代码与资源核对，未经运行验证。
2. **`src/test/java` 不存在**：`TEST_COMMANDS.md` 与 `AGENTS.md` §9 提到的 JUnit 边界目前无实际用例；`runGameTestServer` 是否有非空用例集未核实。
3. **`weather_altar` 自然生成**：`worldgen/structure/weather_altar.json` 存在，但 `worldgen/structure_set/` 只有 `crystal_village`/`fire`/`shelter`/`yanjiang` 四个文件，**无 `weather_altar`**，故当前**不会自然生成**（与 `docs/DESIGN.md` 一致）。
4. **`src/generated/resources/` 目前无文件**：`runData` 尚无 DataGen provider，产物清单未验证。
5. **`gradle.properties:39` 的 `mod_group_id=com.example.examplemod`** 与源码包名 `com.example.weather_realm` 不一致；未确认是否影响发布（当前不影响本地运行）。
6. **`features` 第 6 段（索引 6）与第 10 段（索引 10）的语义**：本工程按 vanilla 阶段顺序填写（索引 6 放矿脉、索引 10 放 `freeze_top_layer`），已通过 JSON 计数确认段数，但未运行时验证世界实际生成结果。
