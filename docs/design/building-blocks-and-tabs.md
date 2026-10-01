# 天象之境 · 建筑方块体系与创造标签页重构 — 设计文档

> **文档类型**：技术设计规范（Technical Design Spec）
> **日期**：2026-10-01
> **状态**：已批准（团队负责人）
> **基线 commit**：`09f7270`
> **平台**：Minecraft **1.21.1** + **NeoForge 21.1.252** · JDK 21 · ModDevGradle 2.0.147
> **命名空间**：`weather_realm`（中文名「天象之境」）
> **影响范围**：新增 2 个 Java 注册类（`ModBuildingBlocks`、`WeatherSpikeBlock`）、扩展现有 `ModItems`/`ModCreativeTabs`/`WeatherRealm`、扩展 Python 资源管线（`tools/gen_block_assets.py` + `tools/biome_data.py`）、新增校验脚本、新增约 95 件建筑方块 + 9 件生态小物。
> **预计新增方块数**：**建筑 95 件 + 生态小物 9 件 = 104 件**（与任务初稿「合计 101」「约 110」的差异及核算见 §5、§6、§10）。
> **权威约束**：技术骨架以仓库根目录 [`AGENTS.md`](../../AGENTS.md) 为准。
> **同目录先例**：[`docs/design/build-portal-command.md`](build-portal-command.md)。

---

## 目录

1. 背景与现状
2. 目标与非目标
3. 三个主题
4. 创造标签页重构（子项目 A）
5. 建筑方块清单（子项目 C）
6. 生态美术小物（子项目 C 第二部分 / D）
7. 资源生成管线扩展（子项目 B）
8. Java 侧设计（子项目 C）
9. 掉落表、配方与命名
10. 实施顺序与验收

---

## 1. 背景与现状

本节所有断言均为对 `09f7270` 工作区源码的**实测**，每条给出 `文件:行号`。行号以本文件编写时的源码为准。

### 1.1 三方块体系规模

| 事实 | 数值 | 依据 |
| :- | :- | :- |
| `ModBlocks` 注册方块的**具体字面声明** | 69 处（33 处 `registerSimpleBlock("...")` + 28 处 `registerBlock("...")` + 8 处 `BLOCKS.register("...")`） | `ModBlocks.java` 全文 |
| 另：「矿石族」模板声明 | 2 处，使用变量而非字面 id（`BLOCKS.registerSimpleBlock(shallowName,…)` / `(deepName,…)`），不计入上面 69 处 | `ModBlocks.java:372-373` |
| 矿石族实际生成方块 | 36 件（`registerOrePair` 调用 18 次 × 2） | `ModBlocks.java:364-388` |
| **方块总数** | **105** = `69 + 36` | 实测 `blockstates/` 目录文件数 = **105**（`src/main/resources/assets/weather_realm/blockstates/`），与注册名一一对应 |
| `ModItems` 物品注册项**字面声明** | 97 处（65 `registerSimpleBlockItem("...")` + 20 `ITEMS.register("...")` + 12 `registerSimpleItem("...")`） | `ModItems.java` 全文 |
| 生成矿石 BlockItem | 36 件 | `ModItems.java:221-227` |
| **物品总数** | **133**（97 + 36） | `ITEMS` 声明：`ModItems.java:51`；生成循环：`ModItems.java:221-227` |
| 创造标签页数量 | **1 个自定义页**（`weather_realm_tab`） | `ModCreativeTabs.java:24-27` |
| 该页展示条目数 | **133**（97 条显式 `output.accept` + 36 条 `GENERATED_ORE_ITEMS.forEach`） | `ModCreativeTabs.java:31-130`，显式 accept 行 32-128，`forEach` 行 `:129` |
| 向原版标签页的额外注入 | `BUILDING_BLOCKS` 注入 2 项（`permafrost`、`deep_permafrost`） | `ModCreativeTabs.java:137-142`；监听器注册 `WeatherRealm.java:36` |

> 核对说明：`ModBlocks` 字面 69 处中，有 2 处是矿石族模板（`ModBlocks.java:372-373`，在同一行内被 `registerOrePair` 多次调用），故具体方块 = 69 − 2 = 67，加矿石族实际 36 件 = **105**，与 `blockstates/` 目录 105 个文件、`loot_table/blocks/` 103 个文件（缺 `weather_portal`、`weather_pedestal`，见 §9）互相印证。

### 1.2 建筑方块缺口

| 事实 | 依据 |
| :- | :- |
| 坚冰木（`frost_*`）是本模组**唯一完整 13 件套**木族：`frost_log / frost_wood / stripped_frost_log / stripped_frost_wood / frost_planks / frost_stairs / frost_slab / frost_fence / frost_fence_gate / frost_door / frost_trapdoor / frost_pressure_plate / frost_button` | `ModBlocks.java:125,130-201`；`ModItems.java:63-78` |
| 焦木族（`scorched_*`）**只有** `scorched_log`、`scorched_wood`、`stripped_scorched_log`、`scorched_leaves`，**无 `stripped_scorched_wood`、无木板及任何衍生件** | `ModBlocks.java:391-395`；`ModItems.java:230-233` |
| 风化木族（`arid_*`）**只有** `arid_log`、`arid_wood`、`stripped_arid_log`、`arid_leaves`，**无 `stripped_arid_wood`、无木板及任何衍生件** | `ModBlocks.java:409-413`；`ModItems.java:236-239` |
| 三种岩石各只有「浅层 + 深层」两种纯方块：`permafrost`/`deep_permafrost`、`fire_stone`/`deep_fire_stone`、`weathered_sandstone`/`deep_weathered_sandstone`，**零形状变体**（无 polished/bricks/stairs/slab/wall/chiseled/pillar） | `ModBlocks.java:251,259,351-356` |
| **全模组没有任何灯具**：`lightLevel` 仅 2 处——祭坛 15、传送门 12；全仓库无 `LanternBlock` 引用 | `ModBlocks.java:210`（15）、`ModBlocks.java:229`（12）；`grep LanternBlock` = 0 命中 |
| 焦木/风化木族**无任何配方**；仅坚冰木有 11 条木族配方 | `data/weather_realm/recipe/frost_*.json`、`stripped_frost_wood.json` 存在；`scorched_*`/`arid_*` 配方目录内无文件 |

### 1.3 资源生成管线

| 事实 | 依据 |
| :- | :- |
| 入口脚本 `tools/gen_block_assets.py`，入口函数 `main()`、主流程 `run()` | `gen_block_assets.py:420`（`main`）、`:394`（`run`） |
| 数据表 `tools/biome_data.py`：`SURFACE_COVERS`、`THEMES` | `biome_data.py:39`（`SURFACE_COVERS`）、`:63`（`THEMES`） |
| `THEMES` **只声明 fire/wind 两主题** | `biome_data.py:63-188`（`fire` 在 `:64`，`wind` 在 `:126`） |
| 生成器**只支持 4 种模型类型**：`pillar`、`cross`、`leaves`、`cube_all` | `gen_block_assets.py:259`（pillar）、`:276`（cross）、`:283`（leaves）、`:290`（else→cube_all） |
| 楼梯/台阶/栅栏/栅栏门/门/活板门/按钮/压力板/墙/玻璃板/灯笼**全部没有模板** | 同上，`write_block_client` 只匹配上述 4 分支 |
| `frost_*` 那套形状变体的 blockstate/model 是**手写静态文件**、脚本从不触碰 | `biome_data.py` 的 `THEMES`/`SURFACE_COVERS` 不含任何 `frost_*`；`gen_block_assets.py:405-413` 只遍历 `covers` 与 `THEMES` |
| `permafrost` 整族、`blizzard_*`、`weather_*`、`frost_*` 木族 13 件套**不在表内**，人工维护 | 同上 |
| 管线写文件语义：`write_json` **整体覆盖** | `gen_block_assets.py:118-120` |
| `merge_tag` **去重追加、只增不删** | `gen_block_assets.py:123-132` |
| `write_lang` **合并**（生成器管的键覆盖，其余保留） | `gen_block_assets.py:373-390`（`data.update(table)` 在 `:389`） |

### 1.4 工具标签与手工标签现状

| 事实 | 依据 |
| :- | :- |
| `write_tags` 只收录：`_ore` 后缀 / 主题 base·deep·crystal 白名单 / 木族 3 件（log·wood·stripped_log）/ 地表覆盖 | `gen_block_assets.py:336-370`（pickaxe 判定 `:343`，axe 三件 `:345`） |
| 现有 `frost_planks … frost_button` 是**手工写进** `data/minecraft/tags/block/mineable/axe.json` | `data/minecraft/tags/block/mineable/axe.json:4-16`（源数据另含 scorched/arid 原木 `:17-22`） |
| 已存在的原版标签文件（本次扩展要写入的目标） | `data/minecraft/tags/`：`block/mineable/{pickaxe,axe,shovel,hoe}.json`、`block/{planks,logs,leaves,fence_gates,wooden_stairs,wooden_slabs,wooden_fences,wooden_doors,wooden_trapdoors,wooden_pressure_plates,wooden_buttons,small_flowers,flowers,tall_flowers,wool,needs_*_tool}.json`、`item/{planks,logs,wooden_*}.json` |
| `walls` 标签**当前不存在**，需新建 `data/minecraft/tags/block/walls.json` | `data/minecraft/tags/block/walls.json` 不存在（目录清单实测） |

### 1.5 客户端 jar 与 DataGen 现状

| 事实 | 依据 |
| :- | :- |
| 本机客户端 jar 存在：`C:\Users\31087\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar` | 实测 `Test-Path` = True |
| 脚本查找逻辑：`find_client_jar`，优先 `MC_CLIENT_JAR` 环境变量，再 glob 上述路径 | `gen_block_assets.py:37-49`（glob 在 `:43`） |
| 无 DataGen provider；`src/generated/` 目录不存在 | `Test-Path src/generated` = False |
| `build.gradle` 的 `runs.data` 配置存在但产出为空（`--output` 指向 `src/generated/resources/`） | `build.gradle:20-22`（`sourceSets.main.resources + srcDir`）、`:72-79`（`data { … }`，`--output` 在 `:78`） |

---

## 2. 目标与非目标

### 2.1 目标

1. 为**三个主题**（永冻/燃焰/风沙）各补齐一套建筑师可用的建材体系：岩石形状变体 + 木族补全 + 灯具 + 装饰。
2. **每主题 1 款灯笼**（共 3 款，常亮、零逻辑）。
3. **每主题 3 件生态美术小物**（共 9 件）。
4. 创造模式拆成**方块页 / 物品页**两页，并按材质·用途分类排序，使 133 个现有条目与新增条目都有稳定、可预测的位置。

### 2.2 非目标（明确不做）

- **不改任何 `.nbt` 结构与 `template_pool`**（不触碰 `FrostVillageStructure`/`FrostPoolElements` 引用的结构文件）。
- **不改现有方块的 id / 注册名 / 现有 JSON 路径**（`frost_*`、`permafrost`、`scorched_*`、`arid_*` 等一律保留）。
- **不引入 Java DataGen**（`src/generated/` 继续不存在；资源仍走 Python 管线 + 手写）。
- **不做可开关灯 / 红石灯**（灯笼常亮，无 `powered` 状态、无逻辑）。
- **不做真发光（emissive）**（需光影支持，超出范围）。
- **不动 worldgen**（不新增/修改群系、结构、features 阶段）。
- **不动天象图与传送门逻辑**（`BiomeMap*`、`ClimatePortalHandler`、`WeatherPortalBlock` 全部排除）。

---

## 3. 三个主题

数据来源：`src/main/resources/data/weather_realm/worldgen/biome/*.json`。

| 项 | `crystal_plains` 水晶平原（永冻） | `blazing_plains` 烈焰平原（燃焰） | `arid_wasteland` 干旱荒原（风沙） |
| :- | :- | :- | :- |
| 群系文件 | `crystal_plains.json` | `blazing_plains.json` | `arid_wasteland.json` |
| `has_precipitation` | `true`（`:2`） | `false`（`:2`） | `false`（`:2`） |
| `temperature` | `-0.5`（`:3`） | `2.0`（`:3`） | `1.5`（`:3`） |
| `downfall` | `0.9`（`:4`） | `0.0`（`:4`） | `0.0`（`:4`） |
| `sky_color` | `9088760` = `0x8AAEF8`（`:6`） | `11141120` = `0xAA0000`（`:6`） | `15784042` = `0xF0D86A`（`:6`） |
| `fog_color` | `12638456` = `0xC0D8F8`（`:7`） | `16724530` = `0xFF3232`（`:7`） | `14467710` = `0xDCC27E`（`:7`） |
| `water_color` | `4159204` = `0x3F76E4`（`:8`） | `16744230` = `0xFF7F26`（`:8`） | `4418698` = `0x436C8A`（`:8`） |
| `water_fog_color` | `328755` = `0x050433`（`:9`） | `11153408` = `0xAA3000`（`:9`） | `3426678` = `0x344976`（`:9`） |
| 现有方块代表 | `permafrost`、`deep_permafrost`、`blizzard_crystal_block` | `fire_stone`、`deep_fire_stone`、`blaze_crystal_block` | `weathered_sandstone`、`deep_weathered_sandstone`、`wind_crystal_block` |
| 木族名 | `frost_*`（坚冰木） | `scorched_*`（焦木） | `arid_*`（风化木） |
| 岩石名（浅层/深层） | `permafrost` / `deep_permafrost` | `fire_stone` / `deep_fire_stone` | `weathered_sandstone` / `deep_weathered_sandstone` |
| 主题代号（本文档排序键 `themeOrder`） | `0`（永冻） | `1`（燃焰） | `2`（风沙） |

> 说明：`crystal_plains` 另有 `grass_color`/`foliage_color`（`:10-11`）与 `music`（`:12-17`），与本设计无关。三群系 `features` 均为 11 个数组（`crystal_plains.json:49-83`、`blazing_plains.json:14-41`、`arid_wasteland.json:14-41`），本次不动。

---

## 4. 创造标签页重构（子项目 A）

### 4.1 两页注册

| 项 | 页 A：方块 | 页 B：物品 |
| :- | :- | :- |
| 注册 id | `weather_realm:building_blocks` | `weather_realm:items`（沿用/改造现有 `weather_realm_tab` 语义） |
| 变量名 | `BUILDING_TAB` | `ITEMS_TAB` |
| 翻译键 | `itemGroup.weather_realm.building_blocks` | `itemGroup.weather_realm.items` |
| 图标 | `ModItems.PERMAFROST_ITEM`（未调用前不展开注册表） | `ModItems.BLIZZARD_CRYSTAL` |
| 排序 | `.withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)` | `.withTabsAfter(ModCreativeTabs.BUILDING_TAB)`（即排在方块页之后） |

- 现有单页注册：`ModCreativeTabs.java:24-27`，`withTabsBefore(CreativeModeTabs.COMBAT)` 在 `:29`。重构后**保留原 `itemGroup.weather_realm` 键**作为物品页键，避免破坏既有翻译（`lang/zh_cn.json:2`、`lang/en_us.json:2`），方块页另加新键。
- 图标用 `DeferredItem.get()...`：`FROST_FLOWER_ITEM` 现被用作图标（`ModCreativeTabs.java:30`），重构后改到物品页或按上表。

### 4.2 页 A（方块）分类顺序（9 类）

同一类内排序规则见 §4.4。**现有**条目按类归位如下（共 104 个方块物品 = 105 方块 − `weather_portal` 无物品，`ModBlocks.java:232`）：

| # | 分类 | 现有条目（id，逐个列出） | 数量 |
| :-: | :- | :- | :-: |
| ① | 木板与木衍生 | `frost_log`、`frost_wood`、`stripped_frost_log`、`stripped_frost_wood`、`frost_planks`、`frost_stairs`、`frost_slab`、`frost_fence`、`frost_fence_gate`、`frost_door`、`frost_trapdoor`、`frost_pressure_plate`、`frost_button`、`scorched_log`、`scorched_wood`、`stripped_scorched_log`、`arid_log`、`arid_wood`、`stripped_arid_log` | 19 |
| ② | 石质建材 | （现有为空；新增 60 件，见 §5） | 0 |
| ③ | 灯具 | （现有为空；新增 3 件，见 §5） | 0 |
| ④ | 玻璃与透明 | （现有为空；新增 12 件，见 §5） | 0 |
| ⑤ | 装饰细部 | `frost_wool`（`ModItems.java:173`）（新增 9 件生态小物归此，见 §6） | 1 |
| ⑥ | 矿石与矿物块 | `permafrost_iron_ore`、`deep_permafrost_iron_ore`、`permafrost_coal_ore`、`deep_permafrost_coal_ore`、`permafrost_copper_ore`、`deep_permafrost_copper_ore`、`permafrost_gold_ore`、`deep_permafrost_gold_ore`、`permafrost_redstone_ore`、`deep_permafrost_redstone_ore`、`permafrost_emerald_ore`、`deep_permafrost_emerald_ore`、`permafrost_lapis_ore`、`deep_permafrost_lapis_ore`、`permafrost_diamond_ore`、`deep_permafrost_diamond_ore`、`permafrost_blizzard_crystal_ore`、`deep_permafrost_blizzard_crystal_ore`（18）＋ 36 个生成矿石 BlockItem（`deep/fire/weathered`×8 矿种×2 浅深、2 晶矿×2 浅深；`ModItems.java:221-227`）＋ `blizzard_crystal_block`、`blaze_crystal_block`、`wind_crystal_block`（3） | 57 |
| ⑦ | 天然地质 | `permafrost`、`deep_permafrost`、`fire_stone`、`deep_fire_stone`、`weathered_sandstone`、`deep_weathered_sandstone`、`frost_moss`、`dry_turf`、`volcanic_ash` | 9 |
| ⑧ | 植物与自然 | `frost_flower`、`frost_grass`、`glacier_bloom`、`frost_sprout`、`tall_frost_flower`、`tall_frost_grass`、`frost_sapling`、`frost_leaves`、`scorched_leaves`、`arid_leaves`、`cinder_bloom`、`flame_sprout`、`fire_flower`、`dune_flower`、`wind_sprout`、`arid_bush` | 16 |
| ⑨ | 功能方块 | `weather_altar_core`、`weather_pedestal` | 2 |
| | **合计** | | **104** |

核算：`19 + 0 + 0 + 0 + 1 + 57 + 9 + 16 + 2 = 104`。✔ 与「方块物品 = 105 − 1」一致。

### 4.3 页 B（物品）分类顺序（6 类）

共 29 个非方块物品（133 − 104 = 29）：

| # | 分类 | 条目（id，逐个列出） | 数量 |
| :-: | :- | :- | :-: |
| ① | 晶石与材料 | `blizzard_crystal`、`blaze_crystal`、`wind_crystal`、`climate_shard`、`frost_leather`、`frost_pelt` | 6 |
| ② | 工具 | `blizzard_crystal_sword`、`blizzard_crystal_pickaxe`、`blizzard_crystal_axe`、`blizzard_crystal_shovel`、`blizzard_crystal_hoe` | 5 |
| ③ | 护甲 | `blizzard_crystal_helmet`、`blizzard_crystal_chestplate`、`blizzard_crystal_leggings`、`blizzard_crystal_boots` | 4 |
| ④ | 食物与农产品 | `frost_mutton`、`cooked_frost_mutton`、`frost_beef`、`cooked_frost_beef`、`frost_porkchop`、`cooked_frost_porkchop`、`frost_raspberry`、`frost_milk_bucket` | 8 |
| ⑤ | 刷怪蛋 | `frost_sheep_spawn_egg`、`frost_cow_spawn_egg`、`frost_pig_spawn_egg`、`frost_cat_spawn_egg` | 4 |
| ⑥ | 功能道具 | `ancient_weather_tome`、`biome_map` | 2 |
| | **合计** | | **29** |

核算：`6 + 5 + 4 + 8 + 4 + 2 = 29`。✔ 与「物品总数 133 − 方块物品 104」一致。

> 天象图 = `biome_map`；古籍 = `ancient_weather_tome`；36 个生成矿石 BlockItem 全部归入页 A 的 ⑥。

### 4.4 分类内排序规则

1. **主题序**：`themeOrder`「永冻(0) → 燃焰(1) → 风沙(2)」。
2. **同族内派生顺序**（建筑师习惯）：`原木 → 木 → 去皮原木 → 去皮木 → 木板 → 楼梯 → 台阶 → 栅栏 → 栅栏门 → 门 → 活板门 → 压力板 → 按钮`。
3. 岩石族内部：`浅层 → 深层`；每层内 `基础/磨制 → 磨制楼梯 → 磨制台阶 → 磨制墙 → 砖 → 砖楼梯 → 砖台阶 → 砖墙 → 裂砖 → 雕纹 → 柱`。
4. 矿石族内部：`浅层X矿 → 深层X矿`，矿种按 `煤→铜→铁→金→红石→绿宝石→青金石→钻石→晶矿`。

排序键由数据结构提供：`ModBuildingBlocks.STONE_FAMILIES` / `WOOD_FAMILIES` 为**有序 `List`**，`displayItems` 直接遍历按序 `output.accept`（详见 §8）。

### 4.5 移除原版 `BUILDING_BLOCKS` 注入

- **现状**：`ModCreativeTabs.addCreative` 在 `event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS` 时 `accept` 了 `PERMAFROST_ITEM`、`DEEP_PERMAFROST_ITEM`（`ModCreativeTabs.java:137-142`），监听器由 `WeatherRealm.java:36` 挂到 Mod 总线。
- **决策**：**整体移除** `addCreative` 方法体与 `WeatherRealm.java:36` 的 `addListener(ModCreativeTabs::addCreative)` 调用，并删除 `BuildCreativeModeTabContentsEvent`/`CreativeModeTabs` 两个 import（`ModCreativeTabs.java:6,8` 现被使用）。
- **理由**：重构后的独立方块页已收纳这两个方块（§4.2 ⑦ 天然地质）；继续注入原版页会造成**同一物品出现在两页**，与「分类排序可预测」目标冲突。移除后不新增任何对原版页的注入。

---

## 5. 建筑方块清单（子项目 C）

### 5.1 命名规则（选定一种并全表一致）

**选定：`<族名>_<派生件>`（族名前置）**，例如浅层 `permafrost` → `permafrost_stairs`、`permafrost_bricks`、`permafrost_polished`。

理由：
- 与现有习惯对齐——`permafrost_coal_ore`（族名前置）、`fire_stone_*`、`frost_planks`、`frost_stairs`（`ModBlocks.java:130,139`；`ModBlocks.java:283`）均为族名前置。
- 深度族沿用现有 `deep_` 前缀：`deep_permafrost_*`（对齐 `ModBlocks.java:259`）。
- 生成器可按固定后缀拼 id，标签与检索稳定。

> 权衡：原版采用 `polished_granite` / `granite_bricks`，是「限定词前置」。本设计放弃该风格，换取与本模组已有 id 前缀的一致性。

### 5.2 浅层石族（每主题 11 件）

以 `X ∈ {permafrost, fire_stone, weathered_sandstone}` 为例：

| # | id 模板 | 说明 | 母材质/复用贴图 |
| :-: | :- | :- | :- |
| 1 | `X_polished` | 磨制基底 | 新贴图（母岩重着色） |
| 2 | `X_polished_stairs` | 磨制楼梯 | 复用 `X_polished` |
| 3 | `X_polished_slab` | 磨制台阶 | 复用 `X_polished` |
| 4 | `X_polished_wall` | 磨制墙 | 复用 `X_polished` |
| 5 | `X_bricks` | 砖 | 新贴图 |
| 6 | `X_brick_stairs` | 砖楼梯 | 复用 `X_bricks` |
| 7 | `X_brick_slab` | 砖台阶 | 复用 `X_bricks` |
| 8 | `X_brick_wall` | 砖墙 | 复用 `X_bricks` |
| 9 | `X_cracked_bricks` | 裂砖 | `X_bricks` + 裂纹重着色 |
| 10 | `X_chiseled` | 雕纹 | 新贴图 |
| 11 | `X_pillar` | 柱 | 新贴图（end + side） |

小计：`11 × 3 = 33`。

### 5.3 深层石族（每主题 9 件）

以 `D ∈ {deep_permafrost, deep_fire_stone, deep_weathered_sandstone}` 为例：

| # | id 模板 | 说明 | 母材质 |
| :-: | :- | :- | :- |
| 1 | `D_polished` | 磨制深层基底 | 新贴图 |
| 2 | `D_polished_stairs` | 磨制楼梯 | 复用 `D_polished` |
| 3 | `D_polished_slab` | 磨制台阶 | 复用 `D_polished` |
| 4 | `D_polished_wall` | 磨制墙 | 复用 `D_polished` |
| 5 | `D_bricks` | 深层砖 | 新贴图 |
| 6 | `D_brick_stairs` | 砖楼梯 | 复用 `D_bricks` |
| 7 | `D_brick_slab` | 砖台阶 | 复用 `D_bricks` |
| 8 | `D_brick_wall` | 砖墙 | 复用 `D_bricks` |
| 9 | `D_chiseled` | 雕纹深层 | 新贴图 |

小计：`9 × 3 = 27`。深层族无 pillar、无 cracked（按本设计）。

### 5.4 木族补全（焦木 + 风化木）

目标：把焦木、风化木补齐到与坚冰木同构的 **13 件套**。坚冰木 13 件清单见 §1.2。

**焦木（`scorched_*`）**：

| 状态 | 件 |
| :- | :- |
| 已存在 | `scorched_log`、`scorched_wood`、`stripped_scorched_log` |
| **本次新增（10）** | `stripped_scorched_wood`、`scorched_planks`、`scorched_stairs`、`scorched_slab`、`scorched_fence`、`scorched_fence_gate`、`scorched_door`、`scorched_trapdoor`、`scorched_pressure_plate`、`scorched_button` |

**风化木（`arid_*`）**：

| 状态 | 件 |
| :- | :- |
| 已存在 | `arid_log`、`arid_wood`、`stripped_arid_log` |
| **本次新增（10）** | `stripped_arid_wood`、`arid_planks`、`arid_stairs`、`arid_slab`、`arid_fence`、`arid_fence_gate`、`arid_door`、`arid_trapdoor`、`arid_pressure_plate`、`arid_button` |

小计：**新增 20 件**（每族 10），两族补全后各 13 件套。

> **与任务初稿的差异（重要）**：任务初稿写「木族补全（26 件）：焦木 13 + 风化木 13」。实测两族已各含 `log/wood/stripped_log` 3 件（`ModBlocks.java:391-393`、`:409-411`），故真正需要新增的是 **每族 10 件、合计 20 件**。若按「每族新增 13」计会与既有 id 重复注册。本设计以实测为准。

### 5.5 灯笼（3 件）

| id | 类 | 特性 |
| :- | :- | :- |
| `frost_lantern` | `LanternBlock` | `lightLevel = 15`，可悬挂/站立，`waterlogged`，常亮零逻辑 |
| `blaze_lantern` | `LanternBlock` | 同上 |
| `wind_lantern` | `LanternBlock` | 同上 |

小计 3。全模组当前**零灯具**（§1.2），本项为首次引入 `LanternBlock`。

### 5.6 装饰（12 件）

每主题：玻璃、玻璃板、格栅、锁链。

| id 模板（`T` = 主题前缀 `frost`/`blaze`/`wind`） | 类 | 说明 |
| :- | :- | :- |
| `T_glass` | `TransparentBlock` | 透明玻璃块，复用原版 `glass` 行为 |
| `T_glass_pane` | `IronBarsBlock`（原版玻璃板即用此类） | 玻璃板 |
| `T_grate` | `IronBarsBlock` | 「格栅」，贴图源 `iron_bars`，作装饰栅栏 |
| `T_chain` | `ChainBlock` | 锁链，可悬挂 |

小计：`4 × 3 = 12`。

> 命名说明：`frost_glass` 可能与坚冰木无关，但为空命名空间，无冲突；三主题以 `frost_/blaze_/wind_` 前缀区分（与 `blizzard_crystal` 的 `blizzard_` 不冲突，因本设计不用 `blizzard_` 作建筑前缀）。

### 5.7 建筑方块合计（自洽核算）

| 族 | 算式 | 件数 |
| :- | :- | :-: |
| 浅层石 | `11 × 3` | 33 |
| 深层石 | `9 × 3` | 27 |
| 木族补全 | `10 × 2` | 20 |
| 灯笼 | `1 × 3` | 3 |
| 装饰 | `4 × 3` | 12 |
| **建筑小计** | `33 + 27 + 20 + 3 + 12` | **95** |

### 5.8 可选扩展（列出但**未纳入本次**）

| 扩展项 | 每主题 | 说明 |
| :- | :-: | :- |
| 地砖族 `X_tiles` + 楼梯/台阶/墙 | 4 | 浅层/深层各 2 族 × 2 = 8/主题 |
| 雕纹砖 `X_chiseled_bricks` / `D_chiseled_bricks` | 2 | 浅深各 1 |
| 晶簇装饰块（可附着，见 §6） | 2 | 已在 §6 计入生态小物，此处仅指岩石晶簇 |
| 合计新增（全部主题） | | **约 +50** |

> 该扩展**不进入** §5.7 与 §6 的件数，仅在 backlog 记录，避免与本文档的验收清单混淆。

---

## 6. 生态美术小物（子项目 C 第二部分 / D）

每主题 3 件，共 9 件。**不属于** §5.7 的 95 件建筑总数。

| 主题 | ① 可附着晶簇 | ② 叠层覆盖物 | ③ 尖锥 |
| :- | :- | :- | :- |
| 永冻 | `frost_crystal_cluster` | `frost_snow_layer` | `frost_spike` |
| 燃焰 | `blaze_crystal_cluster` | `blaze_ash_layer` | `blaze_spike` |
| 风沙 | `wind_crystal_cluster` | `wind_sand_layer` | `wind_spike` |

### 6.1 实现选型（零自定义类 vs 新增 1 个自定义类）

| 小物 | 实现 | 自定义类 | 理由 |
| :- | :- | :- | :- |
| 可附着晶簇 | 复用原版 `AmethystClusterBlock`：`new AmethystClusterBlock(height, width, properties)` | **无** | 原版紫水晶簇已具备 `facing` + `waterlogged` 与生长面判定、正确掉落语义；本模组只需装饰，无需 regrow。 |
| 叠层覆盖物 | 复用原版 `SnowLayerBlock`：`new SnowLayerBlock(properties)` | **无** | 雪层的 `layers(1..8)` 与覆盖判定完全够用；三主题分别表现为雪层/灰烬层/沙层。 |
| 尖锥 | 新增 `WeatherSpikeBlock` | **1 个** | 需要 `thickness`(tip/frustum/middle/base) + `vertical_direction`(up/down) + `waterlogged` 的静态装饰形态，原版无零逻辑等价物。 |

### 6.2 可附着晶簇（零自定义类复用）

- **类**：`net.minecraft.world.level.block.AmethystClusterBlock`（原版）。
- **blockstate**：`variants`，键为 `facing`（`up/down/north/south/east/west`）× `waterlogged`（`true/false`），共 `6 × 2 = 12` 组；每组指向同一 `block/<name>` 模型并施加对应 `x`/`y` 旋转（与原版 `amethyst_cluster` blockstate 同构）。
- **贴图源**：`assets/minecraft/textures/block/amethyst_cluster.png`（实测存在），按主题 `profile` 重着色。
- **物品模型**：`item/generated` + 独立 `textures/item/<name>.png`（原版晶簇物品即扁平图）。

### 6.3 叠层覆盖物（零自定义类复用）

- **类**：`net.minecraft.world.level.block.SnowLayerBlock`（原版）。本模组已有先例：`FrostPlantBlock.java:9` 已 import 该类并在 `:33-34` 读取 `SnowLayerBlock.LAYERS`，说明该方块的 `layers` 状态可被植被判定复用。
- **blockstate**：`variants`，键为 `layers=1..8`，共 8 组；`layers=8` 时用完整方块高度模型，`1..7` 用递增高度模型（模板 `minecraft:block/snow_height2..height14` 系列，或直接复用 `snow` 的 8 个 `heightX` 模型）。
- **贴图源**：`assets/minecraft/textures/block/snow.png`（实测存在）。
- **物品模型**：`item/generated` + 独立 `textures/item/<name>.png`。

### 6.4 尖锥（新增 1 个自定义类 `WeatherSpikeBlock`）

要点：

1. **不继承** `PointedDripstoneBlock`——后者耦合滴水/流体与落地行为，本设计只要静态装饰。
2. 状态：`THICKNESS`（`tip/frustum/middle/base`，自定义枚举 `DripstoneThickness` 或自建 `SpikeThickness`）、`VERTICAL_DIRECTION`（`up/down`）、`WATERLOGGED`。
3. **blockstate**：`variants`，键为 `vertical_direction` × `thickness` × `waterlogged` = `2 × 4 × 2 = 16` 组；每组指向 `block/<name>_<thickness>` 模型（可按方向用 `x`/`y` 旋转复用）。
4. `canSurvive`：需要支撑判定——`up` 需下方为固体面/可附着方块，`down` 需上方为固体面。
5. `getShape`：用 `Block.box(...)` 做 4 段收窄碰撞（tip 最细、base 最粗）。
6. **不做**两段自动合并（不实现 `PointedDripstoneBlock` 的 tip 合并行为）。
7. 贴图源：**原版无单张 `pointed_dripstone.png` 方块贴图**（实测只有 `pointed_dripstone_up_tip` 等 10 张方向图 + `item/pointed_dripstone.png`），本设计**改用主题晶石/岩石重着色贴图**作尖锥贴图，避免逐方向切图。

### 6.5 分类归属

9 件生态小物统一归入 **页 A ⑤ 装饰细部**（不是「植物与自然」，因为它们是矿物/冰雪装饰而非活体植被；尖锥也无生长逻辑）。可选：叠层覆盖物因有地表覆盖属性可考虑 ⑦ 天然地质，但为保持同类聚合，统一放 ⑤ 并在文档中说明。

### 6.6 生态小物总数核算

`3 主题 × 3 件 = 9 件`。加上 §5.7 的 95 件建筑 = **104 件新增方块**。

---

## 7. 资源生成管线扩展（子项目 B）

### 7.1 新增形状模板清单

> **与任务初稿的差异**：初稿写「10 种」，但其后清单实际列出 **11 种**（stairs/slab/fence/fence_gate/door/trapdoor/button/pressure_plate/wall/glass_pane/lantern）。本设计按 **11 种** 实现。

每个模板在 `gen_block_assets.py` 的 `write_block_client` 中新增一个 `model` 分支（现只有 4 分支：`gen_block_assets.py:259,276,283,290`）。

| # | 形状 | 原版 `parent` 模板 | 方块模型文件数 | blockstate 类型 | 物品模型 |
| :-: | :- | :- | :-: | :- | :- |
| 1 | stairs | `block/stairs`、`block/inner_stairs`、`block/outer_stairs` | 3 | `variants`（facing 4 × half 2 × shape 5 = **40** 组） | `parent: <modid>:block/<name>` |
| 2 | slab | `block/slab`、`block/slab_top` | 2 | `variants`（type 3：bottom/top/double） | `parent: <modid>:block/<name>` |
| 3 | fence | `block/fence_post`、`block/fence_side`、`block/fence_inventory` | 3 | `multipart`（north/south/east/west + waterlogged） | `parent: <modid>:block/fence_inventory` |
| 4 | fence_gate | `block/template_fence_gate`、`_open`、`_wall`、`_wall_open` | 4 | `variants`（facing 4 × open 2 × in_wall 2 = 16 组） | `parent: <modid>:block/<name>` |
| 5 | door | `block/door_bottom_left`、`_open`、`door_bottom_right`、`_open`、`door_top_left`、`_open`、`door_top_right`、`_open` | 8 | `variants`（facing 4 × half 2 × hinge 2 × open 2 = 32 组） | `item/generated` + `textures/item/<name>.png` |
| 6 | trapdoor | `block/template_trapdoor_bottom`、`_top`、`_open` | 3 | `variants`（facing 4 × half 2 × open 2 × powered 2 = 32 组） | `parent: <modid>:block/<name>_bottom` |
| 7 | button | `block/button`、`block/button_pressed`、`block/button_inventory` | 3 | `multipart`（facing 4 × powered 2 + 薄板面） | `parent: <modid>:block/button_inventory` |
| 8 | pressure_plate | `block/pressure_plate_up`、`block/pressure_plate_down` | 2 | `variants`（powered 2） | `parent: <modid>:block/<name>` |
| 9 | wall | `block/template_wall_post`、`template_wall_side`、`template_wall_side_tall`、`block/wall_inventory` | 4 | `multipart`（up/north/south/east/west + waterlogged） | `parent: <modid>:block/wall_inventory` |
| 10 | glass_pane | `block/template_glass_pane_post`、`_side`、`_noside`、`_side_alt`、`_noside_alt` | 5 | `multipart`（north/south/east/west + waterlogged） | `item/generated` + `textures/item/<name>.png` |
| 11 | lantern | `block/template_lantern`、`block/template_hanging_lantern` | 2 | `variants`（hanging 2） | `item/generated` + `textures/item/<name>.png` |
| 12 | cluster（生态小物） | `block/amethyst_cluster` + 5 方向旋转 | 1 | `variants`（facing 6 × waterlogged 2 = 12） | `item/generated` |
| 13 | layer（生态小物） | `block/snow_height2..height14` 系列 | 8 | `variants`（layers 1..8） | `item/generated` |

> 第 12、13 项为 §6 生态小物的复用模板；尖锥（§6.4）含自定义类与 16 组 blockstate，**手写静态文件**（与 `frost_*` 同策略，见 §1.3），不进生成器。

### 7.2 族展开（在数据表声明一次族）

在 `biome_data.py` 新增两个元素，展开后复用**同一 `profile`（母材质一套重着色 profile）**：

- `stoneFamily(name, deepName, base_src, deep_src, stone_profile, deep_profile)` → 展开出 §5.2 + §5.3 的全部 20 件/主题。
- `woodFamily(prefix, plank_src, log_src, wood_profile)` → 展开出 §5.4 的 10 件衍生件/主题。

**共享贴图规则**（原版做法，减少贴图数）：
- 楼梯/台阶/墙/栅栏的方块模型直接引用**母方块**贴图键，不额外生成 `*_stairs.png` 等。
- 焦木/风化木的木板贴图独立生成（母材质），其余木衍生件复用木板贴图键；门/活板门有独立 `_top` 变体可用同族重着色。

### 7.3 `write_tags` 改造

`write_tags`（`gen_block_assets.py:336`）改为读取每个 spec 的 `tool`（`pickaxe`/`axe`/`shovel`/`hoe`）与 `needs`（`stone`/`iron`/`diamond`）字段。新方块入标签策略：

| 方块 | `mineable/*` | `needs_*_tool` | 其它原版标签 |
| :- | :- | :- | :- |
| 石质形状变体（浅/深、磨制/砖/裂砖/雕纹/柱） | `pickaxe` | 沿用母岩（浅层 `stone` 级，深层 `iron` 级，可按母岩现有分类） | `walls`（仅 `_wall`） |
| 木族衍生件 | `axe` | 无 | `planks`、`logs`、`wooden_stairs`、`wooden_slabs`、`wooden_fences`、`wooden_fence_gates`/`fence_gates`、`wooden_doors`、`wooden_trapdoors`、`wooden_pressure_plates`、`wooden_buttons` |
| 木原木/木干/去皮 | `axe` | 无 | `logs` |
| 灯笼 | `pickaxe` | 无 | — |
| 玻璃/玻璃板 | 无（原版玻璃不定 mineable） | 无 | — |
| 格栅/锁链 | `pickaxe` | 无 | — |
| 生态小物 | 按材质（晶簇 `pickaxe`；叠层无；尖锥 `pickaxe`） | 无 | — |

- `walls` 标签**当前不存在**，需新建 `data/minecraft/tags/block/walls.json`（§1.4）。
- 木族标签写入 `block` 与 `item` 两套 `wooden_*`（现有文件已存在，`data/minecraft/tags/item/wooden_*.json`）。

### 7.4 新增校验脚本 `tools/verify_building_assets.py`

**双向断言**：

1. **表 → Java**：数据表中每个建材 id，都能在 Java 源码里找到注册字符串。
2. **Java → 表**：Java 中每个建材 id 都有配套 `blockstates/<id>.json`、`models/block/…`、`models/item/…`、`loot_table/blocks/<id>.json`。

**解析方式**（正则匹配三类注册调用）：

- `registerSimpleBlock\("([a-z0-9_]+)"`（`ModBlocks`）；
- `registerBlock\("([a-z0-9_]+)"`（`ModBlocks`）；
- `register\("([a-z0-9_]+)"`（`ModBlocks`/`ModItems` 的 `BLOCKS.register`、`ITEMS.register`）。

脚本对 `ModBlocks.java`、`ModItems.java` 提取 id 集合，与数据表展开集合求**对称差**；不一致时 **stderr 列出差异 + `sys.exit(1)` 非零退出**。仅校验本设计新增/补全的建材集合（以 `ModBuildingBlocks` 生成 id 为准），不误伤人工维护的 `frost_*`。

### 7.5 幂等与覆盖

| 文件类别 | 语义 | 重跑影响 |
| :- | :- | :- |
| `textures/block/**.png`、`models/block/**`、`models/item/**`、`blockstates/**`、`loot_table/blocks/*.json` | `write_json` 整体覆盖（`gen_block_assets.py:118`） | 只覆盖生成器自己的产物，**不会**触碰 `frost_*` 手写文件（不在表中） |
| `data/minecraft/tags/**` | `merge_tag` 去重追加、只增不删（`gen_block_assets.py:123`） | 重复重跑不产生重复项；但**删除表项不会从标签移除**（只增不删） |
| `lang/*.json` | 合并（生成器键覆盖，其余保留，`gen_block_assets.py:389`） | 人工新增的键保留 |

**重跑前自检建议**：

1. `git status` 干净、已提交；重跑后 `git diff --stat` 复核，确认没有删除既有条目。
2. 先备份/记下 `data/minecraft/tags/block/mineable/axe.json`（含手工 `frost_*` 行 `:4-16`）。
3. 运行 `python tools/gen_block_assets.py` 后立即 `python tools/verify_building_assets.py`，非零退出即停止。
4. 检查 `lang/zh_cn.json`/`en_us.json` 未被缩减（行数应只增不减，当前各 168 行）。

### 7.6 输出目录决策

生成物**继续写入** `src/main/resources/...`（现为 `gen_block_assets.py:246,308,325,337,374` 的固定路径）。

理由：

1. 现有 105 个方块的资源已在该目录，拆目录会割裂同一资源族。
2. `lang`／`tags` 必须与**手写内容合并**（`frost_*` 手工标签、人工 lang 键），分目录会互相覆盖。

**与 `AGENTS.md` §4.6「禁止把 DataGen 输出指向 `src/main/resources`」不冲突**：该条针对 Gradle `runData` 的 DataGen 产物（`build.gradle:72-79` 的 `--output src/generated/resources/`）；本管线是**独立的 Python 工具**，具备 `merge_tag`/`write_lang` 的合并语义（§7.5），且不通过 Gradle DataGen。

### 7.7 贴图源映射表

| 新族/装饰 | 源贴图（`assets/minecraft/textures/...`） | 备注 |
| :- | :- | :- |
| 浅层 stone 族 | 主题现有母岩贴图（`permafrost`/`fire_stone`/`weathered_sandstone` 的 `textures/block/<base>.png`） | 由母材质 `profile` 重着色 |
| 深层 stone 族 | 主题现有深层母岩贴图（`deep_*.png`） | 同上 |
| 磨制/砖/雕纹/柱 | 参考原版 `polished_deepslate.png`、`deepslate_bricks.png`、`chiseled_deepslate.png`、`deepslate_tiles.png` 的构图 | 实测原版有这些源图；以母岩重着色 |
| 楼梯/台阶/墙/栅栏/栅栏门/门/活板门/压力板/按钮 | **不单独取图**，复用母方块贴图键 | — |
| 灯笼 | `block/lantern.png`（实测存在，且带 `lantern.png.mcmeta` 动画） | **决策见下** |
| 玻璃 | `block/glass.png`（实测存在） | 重着色 |
| 玻璃板 | `block/glass.png` + `block/glass_pane_top.png`（实测存在） | — |
| 格栅 | `block/iron_bars.png`（实测存在） | 重着色 |
| 锁链 | `block/chain.png`（实测存在）；物品用 `item/chain.png` | — |
| 可附着晶簇 | `block/amethyst_cluster.png`（实测存在） | 按主题重着色 |
| 叠层覆盖物 | `block/snow.png`（实测存在） | 按主题重着色 |
| 尖锥 | 改用主题晶石/岩石重着色贴图 | 原版无单张方块 `pointed_dripstone.png` |

**不存在的源贴图（实测，必须复用其它图）**：

- 原版**没有** `*_wall.png`（`textures/block/*_wall.png` 匹配数 = 0）→ 墙复用母方块贴图。
- 原版**没有** `*_carpet.png`（匹配数 = 0）→ 叠层覆盖物改用 `snow.png`。
- 原版**没有** `oak_fence.png`、`oak_stairs.png`（只有 `oak_planks.png`、`oak_log.png`）→ 栅栏/楼梯复用母方块贴图。
- 原版**没有**单张方块 `pointed_dripstone.png`（只有 10 张方向图 + `item/pointed_dripstone.png`）→ 尖锥改用主题贴图。

**灯笼决策**：`block/lantern.png` 带 `.mcmeta` 动画，而重着色管线 `recolor`（`gen_block_assets.py:58`）只处理**单帧** → **放弃闪烁动画**，静态重着色 + 提亮灯芯像素。且灯笼物品**必须**有独立 `textures/item/<name>.png` + `item/generated` 模型（与原版灯笼一致），**不能** parent 方块模型。

---

## 8. Java 侧设计（子项目 C）

### 8.1 新增 `ModBuildingBlocks.java`

放在 `com.example.weather_realm` 包内，持有两个有序族列表与工厂方法：

```java
public final class ModBuildingBlocks {
    private ModBuildingBlocks() {}

    public record StoneFamily(String name, String deepName, String themeKey,
                              DeferredBlock<Block> base, DeferredBlock<Block> deep,
                              DeferredBlock<Block> polished, DeferredBlock<StairBlock> polishedStairs,
                              DeferredBlock<SlabBlock> polishedSlab, DeferredBlock<WallBlock> polishedWall,
                              DeferredBlock<Block> bricks, DeferredBlock<StairBlock> brickStairs,
                              DeferredBlock<SlabBlock> brickSlab, DeferredBlock<WallBlock> brickWall,
                              DeferredBlock<Block> crackedBricks, DeferredBlock<Block> chiseled,
                              DeferredBlock<Block> pillar, BlockBehaviour.Properties props) {}

    public record WoodFamily(String prefix, String themeKey, BlockSetType setType, WoodType woodType,
                             DeferredBlock<Block> planks, DeferredBlock<StairBlock> stairs,
                             DeferredBlock<SlabBlock> slab, DeferredBlock<FenceBlock> fence,
                             DeferredBlock<FenceGateBlock> fenceGate, DeferredBlock<DoorBlock> door,
                             DeferredBlock<TrapDoorBlock> trapdoor,
                             DeferredBlock<PressurePlateBlock> pressurePlate,
                             DeferredBlock<ButtonBlock> button,
                             BlockBehaviour.Properties planksProps) {}

    public static final List<StoneFamily> STONE_FAMILIES = new ArrayList<>();
    public static final List<WoodFamily> WOOD_FAMILIES = new ArrayList<>();

    private static StoneFamily stoneFamily(String name, MapColor color, BlockBehaviour.Properties props) { /* 注册 11 浅 + 9 深… */ }
    private static WoodFamily woodFamily(String prefix, BlockSetType setType, WoodType woodType,
                                         BlockBehaviour.Properties planksProps) { /* 注册 10 衍生件… */ }

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
```

- **`BlockSetType`/`WoodType` 注册方式**：参照现有 `ModBlocks.FROST_BLOCK_SET_TYPE`（`ModBlocks.java:54`）与 `FROST_WOOD_TYPE`（`ModBlocks.java:55`）：`BlockSetType.register(new BlockSetType("scorched"))`、`WoodType.register(new WoodType("scorched", SCORCHED_BLOCK_SET_TYPE))`。焦木、风化木各注册一套；坚冰木沿用现有 `FROST_BLOCK_SET_TYPE`/`FROST_WOOD_TYPE`。
- `StoneFamily`/`WoodFamily` 记录中的字段顺序即创建顺序，`STONE_FAMILIES`/`WOOD_FAMILIES` 的插入顺序即创造页排序依据。

### 8.2 `ModItems` 扩展

- **BlockItem 批量注册**：仿现有 `GENERATED_ORE_ITEMS` 循环（`ModItems.java:221-227`），遍历 `ModBuildingBlocks.STONE_FAMILIES`/`WOOD_FAMILIES`/装饰表批量 `ITEMS.registerSimpleBlockItem(id, block)`。
- **门**：用 `DoubleHighBlockItem`（现例：`frost_door` 在 `ModItems.java:71-72`，高花草在 `:58-61`）。
- **列表顺序必须确定**：用于创造页输出的批量 `List<DeferredItem<BlockItem>>` 必须与 `STONE_FAMILIES` 的迭代顺序一致（保持确定性；不要用 `HashMap`/无序 `Set`）。
- 尖锥/晶簇/叠层生态小物同样注册 BlockItem（晶簇物品用 `item/generated`）。

### 8.3 `ModCreativeTabs` 扩展

- 两页的 `displayItems` 改为**遍历族/表**输出，不再逐条手写（替代 `ModCreativeTabs.java:32-129`）。
- 排序数据结构：`ModBuildingBlocks.STONE_FAMILIES`（有序 `List<StoneFamily>`） + `WOOD_FAMILIES` + 装饰 `List` + 灯具 `List`；矿石族沿用 `ModItems.GENERATED_ORE_ITEMS`。
- 页 A：先木族（①）、再石族（②）、灯具（③）、玻璃（④）、装饰与生态小物（⑤）、矿石与矿物块（⑥，含 `GENERATED_ORE_ITEMS`）、天然地质（⑦）、植物（⑧）、功能（⑨）。
- 页 B：按 §4.3 六类，遍历既有 `DeferredItem` 常量。

### 8.4 注册接线与顺序约束

在 `WeatherRealm.java` 的构造器中插入 `ModBuildingBlocks.register(modEventBus);`：

- **插入位置**：`WeatherRealm.java:25`（`ModBlocks.register`）**之后**、`:26`（`ModItems.register`）**之前**。
- **顺序约束**：必须早于 `ModItems`，因为 `ModItems` 的批量循环会在类初始化期引用 `ModBuildingBlocks` 的方块 holder（BlockItem 引用方块）。`ModBuildingBlocks` 内部先 `BLOCKS.register(bus)`，其 `DeferredBlock` 由延迟注册在冻结前解析。

### 8.5 重启提示

新增方块/物品属于**静态注册项**；新增 `BlockSetType`/`WoodType`、新增 `@SubscribeEvent` 之外的注册项**不可热替换**。首次加入后必须**重启客户端**（引用 `AGENTS.md` 第 6 节决策表「新增方块/物品/GUI 类型 → 需重启」与第 10 节「`Registry is already frozen`」）。

---

## 9. 掉落表、配方与命名

### 9.1 掉落表

- 每个新方块都要 `data/weather_realm/loot_table/blocks/<id>.json`，默认**自掉落**（`biome_data.self_loot`，`biome_data.py:286`）。
- 玻璃/玻璃板用原版语义（无精准采集则不掉落，可用 `glass` 的 loot 结构）。
- **既有反例**：`weather_pedestal` **缺掉落表**——实测 `loot_table/blocks/weather_pedestal.json` 不存在（方块 105 个，掉落表仅 103 个；缺 `weather_portal`[`ModBlocks.java:232` 声明 `noLootTable`] 与 `weather_pedestal`）。本设计**顺手补齐 `weather_pedestal` 掉落表**（自掉落）。
- 本次新增掉落表数量估算：建筑 95 + 生态小物 9 + 补 `weather_pedestal` 1 = **105**。

### 9.2 配方

生成器新增配方写出（或独立脚本），配方类型与数量估算：

| 配方 | 规则 | 估算条数 |
| :- | :- | :-: |
| 木板 ← 原木 | `scorched_planks`、`arid_planks` | 2 |
| 木干 ← 原木（4 合 3 或 2×2） | `scorched_wood`…（补齐族内一致） | 2 |
| 楼梯 ← 母方块（6 合 4） | 浅层磨制/砖 ×2 + 深层磨制/砖 ×2 = 4/主题 ×3 = 12；木 2 | 14 |
| 台阶 ← 母方块（3 合 6） | 同楼梯口径 12 + 木 2 | 14 |
| 墙 ← 母方块（6 合 6） | 石 4/主题 ×3 = 12 | 12 |
| 磨制 ← 母岩 | 浅深各 1 ×3 = 6 | 6 |
| 砖 ← 母岩（4 合 4） | 浅深各 1 ×3 = 6 | 6 |
| 雕纹 ← 母岩（2 合 1） | 浅深各 1 ×3 = 6 | 6 |
| 柱 ← 母岩（2 合 2） | 浅层 1 ×3 = 3 | 3 |
| 裂砖 ← 砖（熔炉/高炉） | 浅层 1 ×3 = 3 | 3 |
| 木栅栏/栅栏门/门/活板门/压力板/按钮/去皮木 | 每族各 1 ×2 = 14 | 14 |
| 玻璃/玻璃板 | 各 1 ×3 = 6 | 6 |
| 灯笼 | 3 | 3 |
| **合计（估算）** | | **约 91** |

> 具体形状与计数由生成器在实施时按母方块精确展开；上表为数量级估算。

### 9.3 命名规范

- 键格式：`block.weather_realm.<id>` / `item.weather_realm.<id>`，中英双语。现有风格样例：`lang/zh_cn.json:16`（`block.weather_realm.frost_planks`）、`:17`（`frost_stairs`）、`:28`（`permafrost`）、`:29`（`deep_permafrost`）；`lang/en_us.json` 同键。
- **中文名逐条给出**（示例，完整清单由生成器 `write_lang` 按族模板产出；实施时以本表词根拼装）：

| id | English | 中文 |
| :- | :- | :- |
| `permafrost_polished` | Polished Permafrost | 磨制冻土 |
| `permafrost_stairs` | Permafrost Stairs | 冻土楼梯 |
| `permafrost_brick_wall` | Permafrost Brick Wall | 冻土砖墙 |
| `deep_fire_stone_chiseled` | Chiseled Deep Fire Stone | 雕纹深层火石 |
| `weathered_sandstone_pillar` | Weathered Sandstone Pillar | 风化砂石柱 |
| `scorched_planks` | Scorched Planks | 焦木木板 |
| `arid_fence_gate` | Arid Fence Gate | 风化木栅栏门 |
| `frost_lantern` | Frost Lantern | 坚冰灯笼 |
| `blaze_lantern` | Blaze Lantern | 燃焰灯笼 |
| `wind_lantern` | Wind Lantern | 风沙灯笼 |
| `frost_crystal_cluster` | Frost Crystal Cluster | 坚冰晶簇 |
| `blaze_ash_layer` | Volcanic Ash Layer | 火山灰层 |
| `wind_spike` | Wind Spike | 风沙尖锥 |

- **词根约定**：
  - 永冻 = 坚冰/霜/冻土 → `frost_*` / `permafrost_*`
  - 燃焰 = 火/焦/灰烬 → `blaze_*` / `fire_stone_*` / `scorched_*`
  - 风沙 = 风化/沙丘/风沙 → `wind_*` / `weathered_sandstone_*` / `arid_*`
- 若个别中文名无法一一定稿，由生成器输出占位「<英文名>」并在实施 PR 中由团队统一润色；本设计给出上表 13 条样例作为风格基线，全部名目均已成文，无待补项。

---

## 10. 实施顺序与验收

### 10.1 子项目顺序

**A（创造标签页重构） → B（资源管线扩展） → C（建筑方块） → D（生态小物）**。

| 阶段 | 内容 | 可独立验收点 |
| :- | :- | :- |
| A | 拆两页、分类排序、移除 `BUILDING_BLOCKS` 注入 | 现有 133 条目在两页中位置正确、无重复、无丢失；`.\gradlew.bat build` 通过。**A 可以最先独立交付**：只动 `ModCreativeTabs`/`WeatherRealm`，不新增注册项、不依赖新资源；即使 B/C/D 全部回退，A 仍成立。 |
| B | 11 形状模板 + 族展开 + `write_tags` 改造 + `verify_building_assets.py` | 跑脚本后 `verify` 双向断言通过、标签只增不删、`lang` 行数不减。 |
| C | `ModBuildingBlocks` + 95 件建筑注册 + 资源 | `build` 通过、`verify` 通过。 |
| D | 9 件生态小物 + `WeatherSpikeBlock` | `build` 通过；尖锥 16 组 blockstate 校验。 |

### 10.2 验收标准

1. `.\gradlew.bat build` 通过（产物 `build/libs/weather_realm-1.21.1-1.0.0.jar`）。
2. `.\deploy.ps1` 部署成功——**部署前客户端必须完全退出**（脚本会检测并拒绝热覆盖）。
3. 游戏内目视清单（部署并**完全重启客户端**后）：
   - 创造模式出现**两个**「天象之境」标签页（方块页、物品页），顺序与 §4 一致；
   - 方块页可见 9 类、物品页可见 6 类，逐类抽查 1~2 项位置；
   - 三主题的磨制/砖/楼梯/台阶/墙/雕纹/柱 全部可放、可挖、正常掉落；
   - 焦木与风化木各有完整 13 件套（含木板、楼梯、门、活板门…）；
   - 三款灯笼可悬挂、可站立、常亮 `lightLevel=15`（F3 查看光照）；
   - 玻璃/玻璃板/格栅/锁链可放置且渲染正确；
   - 9 件生态小物可放置、尖锥四面朝向与收窄碰撞正确；
   - 原版「建筑方块」页**不再**出现 `permafrost`。
4. `python tools/verify_building_assets.py` 退出码为 0。

### 10.3 测试方式说明（明确不跑的）

- 按团队规则**不跑** `runClient` / `runServer` / `runGameTestServer`；收尾 = `.\gradlew.bat build` + `.\deploy.ps1`（`AGENTS.md` §8.1）。
- `runGameTestServer` **当前也无用例**（仓库 GameTest 数量实测 = 0），本设计不新增 GameTest。

### 10.4 风险与回滚

| # | 风险 | 缓解 / 回滚 |
| :-: | :- | :- |
| 1 | 新增约 104 个方块的注册表/资源体积（贴图、模型、blockstate、loot、lang） | 分批提交（A → B → C → D）；`build` 失败即回退该批 commit；生成物可整目录 `git checkout` 还原 |
| 2 | 重跑生成器对已纳入表的文件**整体覆盖** | 重跑前提交干净工作区；`git diff --stat` 复核；先备份 `mineable/axe.json` 手工行 |
| 3 | `frost_*` 手写资源与生成器「族」概念并存 → 双轨维护 | **本次不**把 `frost_*` 迁入生成器：其资源已稳定、迁入需重写并校验 13×4 组文件，收益低于风险。后续可选统一路径：单独提交一次「迁移 PR」，将 `frost_*` 纳入 `woodFamily` 并跑 `verify` 双向断言 |
| 4 | 新注册项不可热更 | 按 `AGENTS.md` §6/§10：首次加入后必须**重启客户端**；批量一次性声明，只重启一次 |
| 5 | 标签只增不删导致废弃条目残留 | 若删除方块，需手工清理 `data/minecraft/tags/**` 中的对应行 |

---

## 附：本次实测与任务初稿的差异汇总

| 项 | 任务初稿 | 实测/本设计 | 依据 |
| :- | :- | :- | :- |
| 建筑合计件数 | 101（含「61 石 + 26 木」） | **95**（60 石 + 20 木 + 3 灯 + 12 装饰） | 浅层 `11×3=33`、深层 `9×3=27` → 石 60；木族已有 log/wood/stripped_log，仅需每族补 10 → 20 |
| 木族补全 | 26 件 | **20 件** | `ModBlocks.java:391-393,409-411` 已有各 3 件 |
| 新增方块总计 | 约 110 | **104**（95 建筑 + 9 生态） | §5.7 + §6.6 |
| 形状模板数 | 10 种 | **11 种** | 初稿清单实列 11 项（见 §7.1） |
| 生态小物分类 | 未定 | 页 A ⑤ 装饰细部 | §6.5 |
| 命名风格 | 待选 | 族名前置 `<族>_<派生>` | §5.1 |
