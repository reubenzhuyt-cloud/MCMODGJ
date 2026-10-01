# 建筑方块体系与创造标签页重构 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已批准的设计文档 [`docs/design/building-blocks-and-tabs.md`](../building-blocks-and-tabs.md)，把创造页拆成「建筑方块 / 物品」两页并补全 95 件建筑方块 + 9 件生态小物（共 104 件新方块）及其资源、标签、掉落与配方。

**Architecture:** 分四阶段独立交付：A（纯 Java 创造页重构，零新注册）→ B（Python 资源生成管线扩展，空表幂等）→ C（`ModBuildingBlocks` + `ModItems` 批量注册 + 95 件建筑资源）→ D（`WeatherSpikeBlock` + 9 件生态小物）。阶段 A 的标签页排序以「纯数据 id 表」为权威（`ModCreativeTabs.BUILDING_CATEGORIES` / `ITEM_CATEGORIES`，可由校验脚本正则解析）；族对象（`ModBuildingBlocks.STONE_FAMILIES` 等）只负责方块对象的注册，二者共享同一命名规则，保证顺序一致。

**Tech Stack:** MC 1.21.1 / NeoForge 21.1.252 / ModDevGradle 2.0.147 / JDK 21 / Python 3 + Pillow

## Global Constraints

- 项目根 `C:\Users\31087\Desktop\mc\ModDevelopGamejam`；modId `weather_realm`；MC **1.21.1**；NeoForge **21.1.252**；JDK **21**；命令前缀 `.\gradlew.bat`。
- **验收门禁**：`.\gradlew.bat build` 必须 `BUILD SUCCESSFUL`；新增/改动的资源与 Java 注册必须通过 `python tools\verify_building_assets.py`（本计划 Phase B 新建）。**禁止跑 `runClient`/`runServer`/`runGameTestServer`**（团队规则）；`src/test` 不存在、GameTest 0 用例，**不新增测试基建**。
- 收尾规则（AGENTS.md §8.1）：每阶段结束 **build 通过 → `.\deploy.ps1` 部署**（部署前客户端须完全退出）。
- **不改**任何现有方块/item 的注册 id 与现有资源文件路径；**不改** `.nbt` 结构、`template_pool`、worldgen、天象图、传送门逻辑；**不引入** Java DataGen；`Common` 代码**禁止**引用 `net.minecraft.client.*`。
- **禁止 `git add -A` / `git add .`**；不提交 `.opencode/`、`tools/__pycache__/`；不 push/amend/rebase/切分支。提交信息 `type(scope): summary`。
- 新增注册项**必须重启客户端**才生效（AGENTS.md §6/§10）。
- 生成物写入 `src\main\resources\...`（沿用现有 105 方块先例；理由见设计文档 §7.6）。
- 统一在本计划约定的解释器下运行脚本：`python`（Windows，Python 3.11，已装 Pillow）。

## 阶段总览

| 阶段 | 内容 | Task 数 | 可独立验收点 | 提交 scope |
| :- | :- | :-: | :- | :- |
| A | 创造页拆两页 + 133 现有 item 归位 + 移除原版建筑方块注入 | 5 | `verify_tab_coverage.py` 退出 0；`build` 通过 | `feat(creativetab)` |
| B | 11 形状模板 + 族展开结构 + `write_tags` 改造 + 双向校验脚本 | 6 | 重跑生成器 `git diff` 无删减；`verify_building_assets.py` 空集退出 0 | `feat(assets-pipeline)` |
| C | `ModBuildingBlocks` + 95 建筑方块 + 资源/掉落/配方 | 8 | `build` + 两个校验脚本退出 0 | `feat(building-blocks)` |
| D | `WeatherSpikeBlock` + 9 生态小物 | 5 | 尖锥 16 组 blockstate 校验；`build` + 校验退出 0 | `feat(eco-decor)` |

---

# 阶段 A — 创造标签页重构

> 设计文档对照：§4 全部；§10.1 A 行。

### Task A1: 双标签页注册与分类数据表（`ModCreativeTabs.java` 全量重写）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModCreativeTabs.java`（全量替换，1-143 行）
- Test: 无（用 `.\gradlew.bat build` 与 `verify_tab_coverage.py` 验收）

**Interfaces:**
- Consumes: `WeatherRealm.MODID`、`ModItems.PERMAFROST_ITEM`、`ModItems.BLIZZARD_CRYSTAL`（既有）。
- Produces:
  - `record ModCreativeTabs.TabCategory(String key, List<String> itemIds)`
  - `public static final List<TabCategory> ModCreativeTabs.BUILDING_CATEGORIES`（9 类，顺序=显示顺序）
  - `public static final List<TabCategory> ModCreativeTabs.ITEM_CATEGORIES`（6 类）
  - `ResourceKey<CreativeModeTab> ModCreativeTabs` 内部键 `building_blocks`；`DeferredHolder<CreativeModeTab,CreativeModeTab> BUILDING_TAB` / `ITEMS_TAB`
  - `public static void ModCreativeTabs.register(IEventBus)`

**决策（来自设计文档 §4.1）**：页 A 注册 id = `building_blocks`，翻译键 `itemGroup.weather_realm.building_blocks`，变量 `BUILDING_TAB`，图标 `PERMAFROST_ITEM`；页 B 注册 id = `items`（**替换**旧 `weather_realm_tab`），**沿用**翻译键 `itemGroup.weather_realm`，变量 `ITEMS_TAB`，图标 `BLIZZARD_CRYSTAL`。旧 id `weather_realm_tab` 与 `addCreative` 一并删除。

- [ ] **Step 1: 用下列完整内容覆盖 `ModCreativeTabs.java`**

```java
package com.example.weather_realm;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页注册 / Creative mode tab registration.
 *
 * <p>Two tabs replace the former single {@code weather_realm_tab}: a building-blocks page
 * ({@code building_blocks}) and an items page ({@code items}, keeping the original
 * {@code itemGroup.weather_realm} translation key). Each page is ordered by a pure-data
 * {@link TabCategory} table whose {@code itemIds} are namespace-less item ids; the verifier
 * {@code tools/verify_tab_coverage.py} regex-parses those lists and asserts every registered
 * item appears exactly once.</p>
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WeatherRealm.MODID);

    /** 一个创造页分类：分类键（仅校验脚本使用）+ 按显示次序排列的 item id（不含命名空间）。 */
    public record TabCategory(String key, List<String> itemIds) {
    }

    // ---- 页 A：建筑方块（9 类，列表次序即显示次序）--------------------------------------------
    public static final List<TabCategory> BUILDING_CATEGORIES = List.of(
            new TabCategory("wood", List.of(
                    "frost_log", "frost_wood", "stripped_frost_log", "stripped_frost_wood",
                    "frost_planks", "frost_stairs", "frost_slab", "frost_fence", "frost_fence_gate",
                    "frost_door", "frost_trapdoor", "frost_pressure_plate", "frost_button",
                    "scorched_log", "scorched_wood", "stripped_scorched_log",
                    "arid_log", "arid_wood", "stripped_arid_log")),
            new TabCategory("stone", List.of()),
            new TabCategory("lighting", List.of()),
            new TabCategory("glass", List.of()),
            new TabCategory("decoration", List.of("frost_wool")),
            new TabCategory("ores", List.of(
                    "permafrost_iron_ore", "deep_permafrost_iron_ore",
                    "permafrost_coal_ore", "deep_permafrost_coal_ore",
                    "permafrost_copper_ore", "deep_permafrost_copper_ore",
                    "permafrost_gold_ore", "deep_permafrost_gold_ore",
                    "permafrost_redstone_ore", "deep_permafrost_redstone_ore",
                    "permafrost_emerald_ore", "deep_permafrost_emerald_ore",
                    "permafrost_lapis_ore", "deep_permafrost_lapis_ore",
                    "permafrost_diamond_ore", "deep_permafrost_diamond_ore",
                    "permafrost_blizzard_crystal_ore", "deep_permafrost_blizzard_crystal_ore",
                    "fire_stone_coal_ore", "deep_fire_stone_coal_ore",
                    "weathered_sandstone_coal_ore", "deep_weathered_sandstone_coal_ore",
                    "fire_stone_copper_ore", "deep_fire_stone_copper_ore",
                    "weathered_sandstone_copper_ore", "deep_weathered_sandstone_copper_ore",
                    "fire_stone_iron_ore", "deep_fire_stone_iron_ore",
                    "weathered_sandstone_iron_ore", "deep_weathered_sandstone_iron_ore",
                    "fire_stone_gold_ore", "deep_fire_stone_gold_ore",
                    "weathered_sandstone_gold_ore", "deep_weathered_sandstone_gold_ore",
                    "fire_stone_redstone_ore", "deep_fire_stone_redstone_ore",
                    "weathered_sandstone_redstone_ore", "deep_weathered_sandstone_redstone_ore",
                    "fire_stone_emerald_ore", "deep_fire_stone_emerald_ore",
                    "weathered_sandstone_emerald_ore", "deep_weathered_sandstone_emerald_ore",
                    "fire_stone_lapis_ore", "deep_fire_stone_lapis_ore",
                    "weathered_sandstone_lapis_ore", "deep_weathered_sandstone_lapis_ore",
                    "fire_stone_diamond_ore", "deep_fire_stone_diamond_ore",
                    "weathered_sandstone_diamond_ore", "deep_weathered_sandstone_diamond_ore",
                    "fire_stone_blaze_crystal_ore", "deep_fire_stone_blaze_crystal_ore",
                    "weathered_sandstone_wind_crystal_ore", "deep_weathered_sandstone_wind_crystal_ore",
                    "blizzard_crystal_block", "blaze_crystal_block", "wind_crystal_block")),
            new TabCategory("geology", List.of(
                    "permafrost", "deep_permafrost",
                    "fire_stone", "deep_fire_stone",
                    "weathered_sandstone", "deep_weathered_sandstone",
                    "frost_moss", "dry_turf", "volcanic_ash")),
            new TabCategory("plants", List.of(
                    "frost_flower", "frost_grass", "glacier_bloom", "frost_sprout",
                    "tall_frost_flower", "tall_frost_grass", "frost_sapling", "frost_leaves",
                    "scorched_leaves", "arid_leaves",
                    "cinder_bloom", "flame_sprout", "fire_flower",
                    "dune_flower", "wind_sprout", "arid_bush")),
            new TabCategory("functional", List.of("weather_altar_core", "weather_pedestal")));

    // ---- 页 B：物品（6 类）--------------------------------------------------------------------
    public static final List<TabCategory> ITEM_CATEGORIES = List.of(
            new TabCategory("crystals", List.of(
                    "blizzard_crystal", "blaze_crystal", "wind_crystal",
                    "climate_shard", "frost_leather", "frost_pelt")),
            new TabCategory("tools", List.of(
                    "blizzard_crystal_sword", "blizzard_crystal_pickaxe", "blizzard_crystal_axe",
                    "blizzard_crystal_shovel", "blizzard_crystal_hoe")),
            new TabCategory("armor", List.of(
                    "blizzard_crystal_helmet", "blizzard_crystal_chestplate",
                    "blizzard_crystal_leggings", "blizzard_crystal_boots")),
            new TabCategory("food", List.of(
                    "frost_mutton", "cooked_frost_mutton", "frost_beef", "cooked_frost_beef",
                    "frost_porkchop", "cooked_frost_porkchop", "frost_raspberry", "frost_milk_bucket")),
            new TabCategory("spawn_eggs", List.of(
                    "frost_sheep_spawn_egg", "frost_cow_spawn_egg",
                    "frost_pig_spawn_egg", "frost_cat_spawn_egg")),
            new TabCategory("misc", List.of("ancient_weather_tome", "biome_map")));

    private static final ResourceKey<CreativeModeTab> BUILDING_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "building_blocks"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BUILDING_TAB =
            CREATIVE_MODE_TABS.register("building_blocks", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.weather_realm.building_blocks"))
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .icon(() -> ModItems.PERMAFROST_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptCategories(output, BUILDING_CATEGORIES))
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS_TAB =
            CREATIVE_MODE_TABS.register("items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.weather_realm"))
                    .withTabsAfter(BUILDING_TAB_KEY)
                    .icon(() -> ModItems.BLIZZARD_CRYSTAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptCategories(output, ITEM_CATEGORIES))
                    .build());

    private static void acceptCategories(CreativeModeTab.Output output, List<TabCategory> categories) {
        for (TabCategory category : categories) {
            for (String id : category.itemIds()) {
                output.accept(BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, id)));
            }
        }
    }

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
```

- [ ] **Step 2: 确认重写后不再包含 `addCreative` 与旧 id**

Run: `Select-String -Path src\main\java\com\example\weather_realm\ModCreativeTabs.java -Pattern "addCreative|weather_realm_tab|FROST_FLOWER_ITEM"`
Expected: 无任何输出（三个模式均 0 命中）。

- [ ] **Step 3: 编译**

Run: `.\gradlew.bat build`
Expected: 末行 `BUILD SUCCESSFUL`。（本步只验证 Phase A 的 Java 改动；此时 `addCreative` 仍被 `WeatherRealm` 引用会先失败——**先完成 A2 再回到本步**。）

- [ ] **Step 4: 提交**

```powershell
git add src/main/java/com/example/weather_realm/ModCreativeTabs.java
git commit -m "feat(creativetab): 拆分建筑方块/物品两页并建立纯数据分类表"
```

**分类数据表（现有 133 item 逐一归位，即 `BUILDING_CATEGORIES`/`ITEM_CATEGORIES` 的内容）**

页 A（104 个方块物品；`weather_portal` 无物品，不计）：

| 类别 | 次序 | item id |
| :- | :-: | :- |
| ① 木板与木衍生 | 1 | frost_log |
| ① | 2 | frost_wood |
| ① | 3 | stripped_frost_log |
| ① | 4 | stripped_frost_wood |
| ① | 5 | frost_planks |
| ① | 6 | frost_stairs |
| ① | 7 | frost_slab |
| ① | 8 | frost_fence |
| ① | 9 | frost_fence_gate |
| ① | 10 | frost_door |
| ① | 11 | frost_trapdoor |
| ① | 12 | frost_pressure_plate |
| ① | 13 | frost_button |
| ① | 14 | scorched_log |
| ① | 15 | scorched_wood |
| ① | 16 | stripped_scorched_log |
| ① | 17 | arid_log |
| ① | 18 | arid_wood |
| ① | 19 | stripped_arid_log |
| ② 石质建材 | — | （现有为空；Phase C 补 60） |
| ③ 灯具 | — | （现有为空；Phase C 补 3） |
| ④ 玻璃与透明 | — | （现有为空；Phase C 补 12） |
| ⑤ 装饰细部 | 1 | frost_wool（Phase D 再补 9 件生态小物） |
| ⑥ 矿石与矿物块 | 1-18 | permafrost_iron_ore, deep_permafrost_iron_ore, permafrost_coal_ore, deep_permafrost_coal_ore, permafrost_copper_ore, deep_permafrost_copper_ore, permafrost_gold_ore, deep_permafrost_gold_ore, permafrost_redstone_ore, deep_permafrost_redstone_ore, permafrost_emerald_ore, deep_permafrost_emerald_ore, permafrost_lapis_ore, deep_permafrost_lapis_ore, permafrost_diamond_ore, deep_permafrost_diamond_ore, permafrost_blizzard_crystal_ore, deep_permafrost_blizzard_crystal_ore |
| ⑥ | 19-36 | fire_stone_coal_ore, deep_fire_stone_coal_ore, weathered_sandstone_coal_ore, deep_weathered_sandstone_coal_ore, fire_stone_copper_ore, deep_fire_stone_copper_ore, weathered_sandstone_copper_ore, deep_weathered_sandstone_copper_ore, fire_stone_iron_ore, deep_fire_stone_iron_ore, weathered_sandstone_iron_ore, deep_weathered_sandstone_iron_ore, fire_stone_gold_ore, deep_fire_stone_gold_ore, weathered_sandstone_gold_ore, deep_weathered_sandstone_gold_ore, fire_stone_redstone_ore, deep_fire_stone_redstone_ore |
| ⑥ | 37-54 | weathered_sandstone_redstone_ore, deep_weathered_sandstone_redstone_ore, fire_stone_emerald_ore, deep_fire_stone_emerald_ore, weathered_sandstone_emerald_ore, deep_weathered_sandstone_emerald_ore, fire_stone_lapis_ore, deep_fire_stone_lapis_ore, weathered_sandstone_lapis_ore, deep_weathered_sandstone_lapis_ore, fire_stone_diamond_ore, deep_fire_stone_diamond_ore, weathered_sandstone_diamond_ore, deep_weathered_sandstone_diamond_ore, fire_stone_blaze_crystal_ore, deep_fire_stone_blaze_crystal_ore, weathered_sandstone_wind_crystal_ore, deep_weathered_sandstone_wind_crystal_ore |
| ⑥ | 55-57 | blizzard_crystal_block, blaze_crystal_block, wind_crystal_block |
| ⑦ 天然地质 | 1-9 | permafrost, deep_permafrost, fire_stone, deep_fire_stone, weathered_sandstone, deep_weathered_sandstone, frost_moss, dry_turf, volcanic_ash |
| ⑧ 植物与自然 | 1-8 | frost_flower, frost_grass, glacier_bloom, frost_sprout, tall_frost_flower, tall_frost_grass, frost_sapling, frost_leaves |
| ⑧ | 9-12 | scorched_leaves, arid_leaves, cinder_bloom, flame_sprout |
| ⑧ | 13-16 | fire_flower, dune_flower, wind_sprout, arid_bush |
| ⑨ 功能方块 | 1-2 | weather_altar_core, weather_pedestal |

> 说明：⑥ 的第 19-54 段即设计文档 §4.2 的「36 个生成矿石」，此处按 `ModBlocks.GENERATED_ORES` 的声明次序（每矿种：fire 浅/深、weathered 浅/深）展开为字面 id，便于校验脚本解析。现有 18 个冻土矿沿用 §4.2 枚举次序（铁先行），生成矿石沿用声明次序——这是对 §4.2 与 §4.4 排序规则冲突的显式取舍，见文末「自检」。

页 B（29 个非方块物品）：

| 类别 | 次序 | item id |
| :- | :-: | :- |
| ① 晶石与材料 | 1-6 | blizzard_crystal, blaze_crystal, wind_crystal, climate_shard, frost_leather, frost_pelt |
| ② 工具 | 1-5 | blizzard_crystal_sword, blizzard_crystal_pickaxe, blizzard_crystal_axe, blizzard_crystal_shovel, blizzard_crystal_hoe |
| ③ 护甲 | 1-4 | blizzard_crystal_helmet, blizzard_crystal_chestplate, blizzard_crystal_leggings, blizzard_crystal_boots |
| ④ 食物与农产品 | 1-8 | frost_mutton, cooked_frost_mutton, frost_beef, cooked_frost_beef, frost_porkchop, cooked_frost_porkchop, frost_raspberry, frost_milk_bucket |
| ⑤ 刷怪蛋 | 1-4 | frost_sheep_spawn_egg, frost_cow_spawn_egg, frost_pig_spawn_egg, frost_cat_spawn_egg |
| ⑥ 功能道具 | 1-2 | ancient_weather_tome, biome_map |

### Task A2: 移除原版 `BUILDING_BLOCKS` 注入（`WeatherRealm.java`）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\WeatherRealm.java:35-36`
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModCreativeTabs.java`（已由 A1 删除 `addCreative` 与其 import）

**Interfaces:**
- Consumes: `WeatherRealm` 构造器与既有 `ModXxx.register(IEventBus)` 调用。
- Produces: 无新增；仅删除 `modEventBus.addListener(ModCreativeTabs::addCreative);`。

- [ ] **Step 1: 删除旧的监听器注册**

现有代码（`WeatherRealm.java:35-36`）：

```java
        // Register the item to a creative tab
        modEventBus.addListener(ModCreativeTabs::addCreative);
```

替换为空（两行整体删除，保留其上下空行结构）：

```java

```

- [ ] **Step 2: 确认全仓库不再引用 `addCreative`**

Run: `Select-String -Path src\main\java -Pattern "addCreative" -Recurse`
Expected: 无任何输出（0 命中）。

- [ ] **Step 3: 编译（此时整棵 Java 树应干净通过）**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 提交**

```powershell
git add src/main/java/com/example/weather_realm/WeatherRealm.java src/main/java/com/example/weather_realm/ModCreativeTabs.java
git commit -m "refactor(creativetab): 移除原版建筑方块标签页注入"
```

### Task A3: 新增标签页翻译键（`lang` 两文件）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\resources\assets\weather_realm\lang\zh_cn.json:2`
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\resources\assets\weather_realm\lang\en_us.json:2`

**Interfaces:**
- Consumes: A1 使用的键 `itemGroup.weather_realm.building_blocks`（页 A）与 `itemGroup.weather_realm`（页 B，已存在）。
- Produces: 新增 `itemGroup.weather_realm.building_blocks` 双语值。

- [ ] **Step 1: 在 `zh_cn.json` 第 2 行后插入方块页键**

现有第 1-3 行：

```json
{
  "itemGroup.weather_realm": "天象之境",
  "biome.weather_realm.crystal_plains": "水晶平原",
```

改为：

```json
{
  "itemGroup.weather_realm": "天象之境",
  "itemGroup.weather_realm.building_blocks": "天象之境·建筑方块",
  "biome.weather_realm.crystal_plains": "水晶平原",
```

- [ ] **Step 2: 在 `en_us.json` 第 2 行后插入方块页键**

现有第 1-3 行：

```json
{
  "itemGroup.weather_realm": "Weather Realm",
  "biome.weather_realm.crystal_plains": "Crystal Plains",
```

改为：

```json
{
  "itemGroup.weather_realm": "Weather Realm",
  "itemGroup.weather_realm.building_blocks": "Weather Realm Building Blocks",
  "biome.weather_realm.crystal_plains": "Crystal Plains",
```

- [ ] **Step 3: 校验两文件是合法 JSON 且旧键未被删**

Run: `python -c "import json;d=json.load(open(r'src/main/resources/assets/weather_realm/lang/zh_cn.json',encoding='utf-8'));e=json.load(open(r'src/main/resources/assets/weather_realm/lang/en_us.json',encoding='utf-8'));print(d['itemGroup.weather_realm'],d['itemGroup.weather_realm.building_blocks'],e['itemGroup.weather_realm.building_blocks'],len(d),len(e))"`
Expected: `天象之境 天象之境·建筑方块 Weather Realm Building Blocks 169 169`。

- [ ] **Step 4: 提交**

```powershell
git add src/main/resources/assets/weather_realm/lang/zh_cn.json src/main/resources/assets/weather_realm/lang/en_us.json
git commit -m "feat(creativetab): 新增建筑方块页翻译键"
```

### Task A4: 标签页覆盖校验脚本 `tools/verify_tab_coverage.py`

**Files:**
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\verify_tab_coverage.py`

**Interfaces:**
- Consumes: `ModItems.java`（字面注册）、`biome_data`（生成矿石 id）、`ModCreativeTabs.java`（`TabCategory` 字面表）。
- Produces: 可执行脚本 `python tools/verify_tab_coverage.py`，退出码 0/1。若 `biome_data.all_building_block_ids()` 存在（Phase C 后）则一并纳入注册全集。

**实现思路**：注册 item 全集 = ① `ModItems.java` 中 `registerSimpleBlockItem("id"` / `registerSimpleItem("id"` / `ITEMS.register("id"` 三类字面 id；② `biome_data` 计算的 36 个生成矿石 id（fire/wind × 8 矿种 × 2 浅深 + 2 晶矿 × 2）；③ 若 `biome_data.all_building_block_ids()` 存在，并入 95 个新建材 id。标签页 id 全集 = 用正则 `new TabCategory\("(\w+)",\s*List\.of\(([^)]*)\)` 抓取 `ModCreativeTabs.java` 的 `BUILDING_CATEGORIES` 与 `ITEM_CATEGORIES`，再对每段 `findall('"([a-z0-9_]+)"')` 取 id。断言：注册全集 ⊆ 标签页全集（不缺），标签页全集 ⊆ 注册全集（不多），且每个 id 恰好出现一次。

- [ ] **Step 1: 创建脚本**

```python
#!/usr/bin/env python3
"""Verify the creative-tab coverage of every registered Weather Realm item.

Registered set = ModItems literals + generated ore ids (computed from biome_data)
+ building-block ids (biome_data.all_building_block_ids, once Phase C exists).
Tab set = the id literals inside ModCreativeTabs.BUILDING_CATEGORIES / ITEM_CATEGORIES.

Usage (from the repo root):
    python tools/verify_tab_coverage.py
Exits 0 when both directions match and every id appears exactly once, else 1.
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402

ITEM_LITERAL_RE = re.compile(r'(?:registerSimpleBlockItem|registerSimpleItem|ITEMS\.register)\(\s*"([a-z0-9_]+)"')
CATEGORY_RE = re.compile(r'new\s+TabCategory\(\s*"(\w+)"\s*,\s*List\.of\((.*?)\)\)', re.DOTALL)
QUOTED_ID_RE = re.compile(r'"([a-z0-9_]+)"')


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def registered_item_ids(root: Path) -> set:
    java = root / "src/main/java/com/example/weather_realm/ModItems.java"
    ids = set(ITEM_LITERAL_RE.findall(_read(java)))
    for theme in bd.THEMES:
        for ore in bd.ORE_ORDER:
            ids.add(bd.shallow_ore(theme, ore))
            ids.add(bd.deep_ore(theme, ore))
        ids.add(bd.shallow_crystal_ore(theme))
        ids.add(bd.deep_crystal_ore(theme))
    all_building = getattr(bd, "all_building_block_ids", None)
    if all_building is not None:
        ids |= set(all_building())
    return ids


def tab_item_ids(root: Path) -> list:
    java = root / "src/main/java/com/example/weather_realm/ModCreativeTabs.java"
    text = _read(java)
    found = []
    for _key, body in CATEGORY_RE.findall(text):
        found.extend(QUOTED_ID_RE.findall(body))
    return found


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    args = ap.parse_args()
    root = Path(args.root).resolve()

    registered = registered_item_ids(root)
    tab_ids = tab_item_ids(root)
    counts = Counter(tab_ids)

    print(f"[tab] registered items : {len(registered)}")
    print(f"[tab] tab entries      : {len(tab_ids)} (distinct {len(counts)})")

    problems = []
    duplicates = sorted(i for i, c in counts.items() if c > 1)
    if duplicates:
        problems.append(f"duplicated in tabs: {duplicates}")
    missing = sorted(registered - set(tab_ids))
    if missing:
        problems.append(f"registered but not in any tab: {missing}")
    extra = sorted(set(tab_ids) - registered)
    if extra:
        problems.append(f"in a tab but not registered: {extra}")

    if problems:
        print("TAB COVERAGE FAILED", file=sys.stderr)
        for p in problems:
            print("  -", p, file=sys.stderr)
        sys.exit(1)
    print("TAB COVERAGE OK")
    sys.exit(0)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行校验**

Run: `python tools/verify_tab_coverage.py`
Expected: 打印 `[tab] registered items : 133`、`[tab] tab entries      : 133 (distinct 133)`、末行 `TAB COVERAGE OK`，退出码 0。

- [ ] **Step 3: 确认退出码为 0**

Run: `python tools/verify_tab_coverage.py; echo "exit=$LASTEXITCODE"`
Expected: 含 `TAB COVERAGE OK` 与 `exit=0`。

- [ ] **Step 4: 提交**

```powershell
git add tools/verify_tab_coverage.py
git commit -m "test(creativetab): 新增标签页覆盖率双向校验脚本"
```

### Task A5: 阶段 A 收尾（构建 + 部署 + 提交）

**Files:** 无新增/改动（仅执行命令）。

**Interfaces:**
- Consumes: A1-A4 全部产物。
- Produces: 阶段 A 可运行产物；PCL 客户端 mods 内 jar。

- [ ] **Step 1: 最终构建**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`，`build\libs\weather_realm-1.21.1-1.0.0.jar` 存在。

- [ ] **Step 2: 确认工作区已提交、无残留改动**

Run: `git status --short`
Expected: 仅剩未跟踪的 `?? .opencode/` 与 `?? tools/__pycache__/`。

- [ ] **Step 3: 部署（先完全退出 Minecraft 客户端）**

Run: `.\deploy.ps1`
Expected: 脚本输出构建成功并将 `weather_realm-1.21.1-1.0.0.jar` 复制到 `C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\`；若检测到客户端运行则中止并提示。

- [ ] **Step 4: 在启动器内重启客户端目视验收（人工，非 `runClient`）**

Expected: 创造模式出现两个「天象之境」页：物体页标题 `天象之境·建筑方块` 在方块页集合中、物品页标题 `天象之境`；原版「建筑方块」页不再出现 `permafrost`；两页条目总数为 133 且无重复。

---

# 阶段 B — 资源生成管线扩展

> 设计文档对照：§7 全部；§10.1 B 行。
> **数据/注册归属（本计划约定）**：Phase B 只搭「引擎」——形状模板、族声明结构、展开函数、`write_tags`/`write_lang` 改造、校验脚本；**不填充任何新族数据**，因此 `BUILDING_FAMILIES` 为空。新族的实际数据与 95 件资源在 Phase C 填充/生成。这保证 Phase B 校验脚本在「空集」下退出 0（任务硬性要求），并让 B 的验收聚焦「重跑不删旧产物」。

### Task B1: 11 种形状模板（`tools/gen_block_assets.py`）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py:1-433`（新增 helpers 与改写 `write_block_client`）

**Interfaces:**
- Consumes: `ZIP`（vanilla client jar 句柄）、`read_png`、`recolor`、`write_json`、`MODID`。
- Produces:
  - `def read_text(zf, rel: str) -> str`
  - `def write_template_blockstate(assets: Path, name: str, template_rel: str, replacements: list) -> None`
  - 扩展后的 `def write_block_client(root: Path, spec) -> None`，支持 `spec["model"] ∈ {pillar, cross, leaves, cube_all, glass_block, stairs, slab, wall, fence, fence_gate, door, trapdoor, button, pressure_plate, lantern, pane, chain}`。

**形状模板规格（逐种）**

| # | `model` | 原版 `parent` 模板 | 生成的 `models/block` 文件 | blockstate 来源（从 vanilla jar 读取后改名） | 物品模型 |
| :-: | :- | :- | :- | :- | :- |
| 1 | `stairs` | `block/stairs`、`inner_stairs`、`outer_stairs` | `<name>.json` / `<name>_inner.json` / `<name>_outer.json` | `blockstates/oak_stairs.json` → `variants`（facing×half×shape=40 组） | `parent: <modid>:block/<name>` |
| 2 | `slab` | `block/slab`、`slab_top` | `<name>.json` / `<name>_top.json` | `blockstates/oak_slab.json` → `variants`（type 3） | `parent: <modid>:block/<name>` |
| 3 | `wall` | `block/template_wall_post`、`template_wall_side`、`template_wall_side_tall`、`block/wall_inventory` | `<name>_post.json` / `<name>_side.json` / `<name>_side_tall.json` / `<name>_inventory.json` | `blockstates/cobblestone_wall.json` → `multipart`（up + 4 向 low/tall） | `parent: <modid>:block/<name>_inventory` |
| 4 | `fence` | `block/fence_post`、`fence_side`、`fence_inventory` | `<name>_post.json` / `<name>_side.json` / `<name>_inventory.json` | `blockstates/oak_fence.json` → `multipart`（4 向） | `parent: <modid>:block/<name>_inventory` |
| 5 | `fence_gate` | `block/template_fence_gate`、`_open`、`_wall`、`_wall_open` | `<name>.json` / `_open` / `_wall` / `_wall_open` | `blockstates/oak_fence_gate.json` → `variants`（16） | `parent: <modid>:block/<name>` |
| 6 | `door` | `block/door_bottom_left`、`_open`、`door_bottom_right`、`_open`、`door_top_left`、`_open`、`door_top_right`、`_open` | 8 个 `<name>_bottom_left.json` … `<name>_top_right_open.json` | `blockstates/oak_door.json` → `variants`（32） | `item/generated` + `textures/item/<name>.png` |
| 7 | `trapdoor` | `block/template_trapdoor_bottom`、`_top`、`_open` | `<name>_bottom.json` / `_top` / `_open` | `blockstates/oak_trapdoor.json` → `variants`（32） | `parent: <modid>:block/<name>_bottom` |
| 8 | `button` | `block/button`、`button_pressed`、`button_inventory` | `<name>.json` / `<name>_pressed.json` / `<name>_inventory.json` | `blockstates/oak_button.json` → `variants`（face×facing×powered） | `parent: <modid>:block/<name>_inventory` |
| 9 | `pressure_plate` | `block/pressure_plate_up`、`pressure_plate_down` | `<name>.json` / `<name>_down.json` | `blockstates/oak_pressure_plate.json` → `variants`（powered 2） | `parent: <modid>:block/<name>` |
| 10 | `lantern` | `block/template_lantern`、`template_hanging_lantern` | `<name>.json` / `<name>_hanging.json` | `blockstates/lantern.json` → `variants`（hanging 2） | `item/generated` + `textures/item/<name>.png` |
| 11 | `pane` | `block/template_glass_pane_post`、`_side`、`_noside`、`_side_alt`、`_noside_alt` | `<name>_post.json` / `_side` / `_side_alt` / `_noside` / `_noside_alt` | `blockstates/glass_pane.json` → `multipart`（4 向 side/side_alt + noside/noside_alt） | `item/generated`，`layer0 = <modid>:block/<pane_tex>` |

> 另加两个非形状分支：`glass_block`（`cube_all` + `render_type: translucent`，blockstate `""` 单变体）与 `chain`（`parent: minecraft:block/chain`，blockstate 复用 `axis` 三变体并内联旋转）。

> **blockstate 采用「读取 vanilla blockstate 再改名」策略**：脚本已从同一 jar 读取贴图，故直接从 `assets/minecraft/blockstates/<template>.json` 读文本、按**长名优先**做字符串替换（如先把 `minecraft:block/oak_trapdoor_bottom` 换成 `<modid>:block/<name>_bottom`，再处理更短的名），写回 `blockstates/<name>.json`。这样 multipart 的完整 `when/apply` 结构 100% 与 vanilla 一致，杜绝手写 40/32 组变体出错。

- [ ] **Step 1: 在 `gen_block_assets.py` 顶部（`read_png` 之后）新增两个 helper**

```python
def read_text(zf: zipfile.ZipFile, rel: str) -> str:
    with zf.open(f"assets/minecraft/{rel}") as fh:
        return fh.read().decode("utf-8")


def write_template_blockstate(assets: Path, name: str, template_rel: str,
                              replacements) -> None:
    """Copy a vanilla blockstate and rename its model references (longest first)."""
    text = read_text(ZIP, f"blockstates/{template_rel}.json")
    for old, new in sorted(replacements, key=lambda r: len(r[0]), reverse=True):
        text = text.replace(old, new)
    path = assets / "blockstates" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
```

- [ ] **Step 2: 用下列完整实现替换 `write_block_client`（第 245-304 行原函数）**

```python
def _tx(spec, key):
    return f"{MODID}:block/{spec['tex'][key]}"


def write_block_client(root: Path, spec) -> None:
    assets = root / "src/main/resources/assets" / MODID
    name = spec["name"]
    for sub in ("textures/block", "textures/item", "models/block", "models/item", "blockstates"):
        (assets / sub).mkdir(parents=True, exist_ok=True)
    composite = spec.get("composite")
    for out_tex, src_tex, profile in spec.get("textures", []):
        if composite is not None:
            background = Image.open(
                assets / "textures/block" / f"{composite['background']}.png").convert("RGBA")
            img = compose_ore(read_png(ZIP, src_tex), read_png(ZIP, composite["vanilla_base"]),
                              profile, background)
        else:
            img = recolor(read_png(ZIP, src_tex), profile)
        img.save(assets / "textures/block" / f"{out_tex}.png")
    for out_tex, src_tex, profile in spec.get("item_textures", []):
        recolor(read_png(ZIP, src_tex), profile).save(assets / "textures/item" / f"{out_tex}.png")

    model = spec["model"]
    if model == "pillar":
        side, top = spec["pillar_side"], spec["pillar_top"]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"}})
        write_json(assets / "models/block" / f"{name}_horizontal.json", {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {
            "axis=x": {"model": f"{MODID}:block/{name}_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": f"{MODID}:block/{name}"},
            "axis=z": {"model": f"{MODID}:block/{name}_horizontal", "x": 90}}})
    elif model == "cross":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MODID}:block/{texture}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "leaves":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
            "textures": {"all": f"{MODID}:block/{texture}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "cube_all":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{texture}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "glass_block":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
            "textures": {"all": _tx(spec, "all")}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "chain":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/chain",
            "textures": {"all": _tx(spec, "all"), "particle": _tx(spec, "all")}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {
            "axis=x": {"model": f"{MODID}:block/{name}", "x": 90, "y": 90},
            "axis=y": {"model": f"{MODID}:block/{name}"},
            "axis=z": {"model": f"{MODID}:block/{name}", "x": 90}}})
    elif model == "stairs":
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}",
                "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                             "top": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_stairs", [
            ("minecraft:block/oak_stairs_inner", f"{MODID}:block/{name}_inner"),
            ("minecraft:block/oak_stairs_outer", f"{MODID}:block/{name}_outer"),
            ("minecraft:block/oak_stairs", f"{MODID}:block/{name}")])
    elif model == "slab":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/slab",
            "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                         "top": _tx(spec, "parent")}})
        write_json(assets / "models/block" / f"{name}_top.json", {
            "parent": "minecraft:block/slab_top",
            "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                         "top": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_slab", [
            ("minecraft:block/oak_slab_top", f"{MODID}:block/{name}_top"),
            ("minecraft:block/oak_planks", _tx(spec, "double")),
            ("minecraft:block/oak_slab", f"{MODID}:block/{name}")])
    elif model == "wall":
        for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"),
                               ("_side_tall", "template_wall_side_tall"),
                               ("_inventory", "wall_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"wall": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "cobblestone_wall", [
            ("minecraft:block/cobblestone_wall_side_tall", f"{MODID}:block/{name}_side_tall"),
            ("minecraft:block/cobblestone_wall_side", f"{MODID}:block/{name}_side"),
            ("minecraft:block/cobblestone_wall_post", f"{MODID}:block/{name}_post")])
    elif model == "fence":
        for suffix, parent in (("_post", "fence_post"), ("_side", "fence_side"),
                               ("_inventory", "fence_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_fence", [
            ("minecraft:block/oak_fence_post", f"{MODID}:block/{name}_post"),
            ("minecraft:block/oak_fence_side", f"{MODID}:block/{name}_side")])
    elif model == "fence_gate":
        for suffix, parent in (("", "template_fence_gate"), ("_open", "template_fence_gate_open"),
                               ("_wall", "template_fence_gate_wall"),
                               ("_wall_open", "template_fence_gate_wall_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_fence_gate", [
            ("minecraft:block/oak_fence_gate_wall_open", f"{MODID}:block/{name}_wall_open"),
            ("minecraft:block/oak_fence_gate_wall", f"{MODID}:block/{name}_wall"),
            ("minecraft:block/oak_fence_gate_open", f"{MODID}:block/{name}_open"),
            ("minecraft:block/oak_fence_gate", f"{MODID}:block/{name}")])
    elif model == "door":
        tex = {"bottom": _tx(spec, "parent"), "top": _tx(spec, "parent")}
        for suffix, parent in (("_bottom_left", "door_bottom_left"),
                               ("_bottom_left_open", "door_bottom_left_open"),
                               ("_bottom_right", "door_bottom_right"),
                               ("_bottom_right_open", "door_bottom_right_open"),
                               ("_top_left", "door_top_left"),
                               ("_top_left_open", "door_top_left_open"),
                               ("_top_right", "door_top_right"),
                               ("_top_right_open", "door_top_right_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": tex})
        write_template_blockstate(assets, name, "oak_door", [
            (f"minecraft:block/oak_door_{part}", f"{MODID}:block/{name}_{part}")
            for part in ("bottom_left_open", "bottom_left", "bottom_right_open", "bottom_right",
                         "top_left_open", "top_left", "top_right_open", "top_right")])
    elif model == "trapdoor":
        for suffix, parent in (("_bottom", "template_trapdoor_bottom"),
                               ("_top", "template_trapdoor_top"), ("_open", "template_trapdoor_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_trapdoor", [
            ("minecraft:block/oak_trapdoor_bottom", f"{MODID}:block/{name}_bottom"),
            ("minecraft:block/oak_trapdoor_top", f"{MODID}:block/{name}_top"),
            ("minecraft:block/oak_trapdoor_open", f"{MODID}:block/{name}_open")])
    elif model == "button":
        for suffix, parent in (("", "button"), ("_pressed", "button_pressed"),
                               ("_inventory", "button_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_button", [
            ("minecraft:block/oak_button_pressed", f"{MODID}:block/{name}_pressed"),
            ("minecraft:block/oak_button", f"{MODID}:block/{name}")])
    elif model == "pressure_plate":
        for suffix, parent in (("", "pressure_plate_up"), ("_down", "pressure_plate_down")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_pressure_plate", [
            ("minecraft:block/oak_pressure_plate_down", f"{MODID}:block/{name}_down"),
            ("minecraft:block/oak_pressure_plate", f"{MODID}:block/{name}")])
    elif model == "lantern":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/template_lantern", "textures": {"lantern": _tx(spec, "lantern")}})
        write_json(assets / "models/block" / f"{name}_hanging.json", {
            "parent": "minecraft:block/template_hanging_lantern",
            "textures": {"lantern": _tx(spec, "lantern")}})
        write_template_blockstate(assets, name, "lantern", [
            ("minecraft:block/lantern_hanging", f"{MODID}:block/{name}_hanging"),
            ("minecraft:block/lantern", f"{MODID}:block/{name}")])
    elif model == "pane":
        for suffix, parent in (("_post", "template_glass_pane_post"), ("_side", "template_glass_pane_side"),
                               ("_side_alt", "template_glass_pane_side_alt"),
                               ("_noside", "template_glass_pane_noside"),
                               ("_noside_alt", "template_glass_pane_noside_alt")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}",
                "textures": {"pane": _tx(spec, "pane"), "edge": _tx(spec, "edge")}})
        write_template_blockstate(assets, name, "glass_pane", [
            ("minecraft:block/glass_pane_side_alt", f"{MODID}:block/{name}_side_alt"),
            ("minecraft:block/glass_pane_noside_alt", f"{MODID}:block/{name}_noside_alt"),
            ("minecraft:block/glass_pane_side", f"{MODID}:block/{name}_side"),
            ("minecraft:block/glass_pane_noside", f"{MODID}:block/{name}_noside"),
            ("minecraft:block/glass_pane_post", f"{MODID}:block/{name}_post")])
    else:
        raise ValueError(f"unknown model: {model}")

    kind, ref = spec.get("item", (None, None))
    if kind is None:
        if model == "cross":
            kind, ref = "generated", f"{MODID}:block/{spec['textures'][0][0]}"
        else:
            kind, ref = "parent", name
    if kind == "generated":
        write_json(assets / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": ref}})
    elif kind == "none":
        pass
    else:
        write_json(assets / "models/item" / f"{name}.json", {"parent": f"{MODID}:block/{ref}"})
```

> 兼容性说明：`kind is None` 的默认分支精确复刻原函数——`cross` 用 `item/generated` + `layer0 = block/<texture>`，其余用 `parent: block/<name>`。现有 59 方块的规格均无 `item` 字段，重写后产物字节不变；新规格一律显式给出 `item=(...)`。

- [ ] **Step 3: 语法检查**

Run: `python -c "import ast;ast.parse(open(r'tools/gen_block_assets.py',encoding='utf-8').read());print('SYNTAX OK')"`
Expected: `SYNTAX OK`。

- [ ] **Step 4: 重跑生成器确认现有产物零差异（幂等基线）**

Run: `python tools/gen_block_assets.py`
Expected: 末行形如 `[gen] wrote textures + resources for 59 blocks (118 block textures) ...`。

- [ ] **Step 5: 检查未删减任何既有资源**

Run: `git status --short; git diff --stat`
Expected: `git status` 无 `src/` 下的改动（现有 59 方块的生成物与提交一致）；若出现差异，逐一核对是否为 `cross` 规格缺 `item` 字段导致，修正后再重跑。

- [ ] **Step 6: 提交**

```powershell
git add tools/gen_block_assets.py
git commit -m "feat(assets-pipeline): 新增 11 种建筑形状模板与 blockstate 复用"
```

### Task B2: 族数据结构与展开函数（`tools/biome_data.py`）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\biome_data.py`（在文件末尾追加；不改动 1-354 行既有内容）

**Interfaces:**
- Consumes: 既有 `THEMES`、`MODID`。
- Produces:
  - `def build_stone_families() -> list`（元素字段：`theme, name, deep, en, zh, deep_en, deep_zh, stone_profile, deep_profile`）
  - `def build_wood_families() -> list`（元素字段：`theme, prefix, planks_src, profile, log_name, en, zh, ...`）
  - `BUILDING_FAMILIES = []`（Phase B 为空；Phase C 填充）
  - `def stone_specs(family) -> list`、`def wood_specs(family) -> list`
  - `def all_building_block_ids() -> list`

**族声明结构与展开签名（Phase B 落定，Phase C 使用）**

- 石族 `build_stone_families()` 元素（dict）字段：
  - `theme`（`"frost"|"fire"|"wind"`）、`name`（浅层名，如 `permafrost`）、`deep`（深层名，如 `deep_permafrost`）
  - `en`/`zh`/`deep_en`/`deep_zh`（显示词根）、`stone_profile`/`deep_profile`（重着色 profile dict）
- 木族 `build_wood_families()` 元素（dict）字段：
  - `theme`、`prefix`（`scorched|arid`）、`planks_src`（如 `block/oak_planks`）、`profile`（木族重着色）
  - `en`/`zh`（如 `"Scorched"`/`"焦木"`）
- `stone_specs(family)` 展开：浅层 11 件 + 深层 9 件，返回 spec 列表；衍生件与母方块**共用贴图键**（`tex["parent"]` 指向 `X_polished`/`X_bricks`，不生成 `_stairs.png`）。
- `wood_specs(family)` 展开：10 件，`parent` 指向 `<prefix>_planks`（stripped wood 指向 `stripped_<prefix>_log`）。
- `all_building_block_ids()`：返回 `stone_specs*3 + wood_specs*2 + decoration + lantern` 的全部 id，供校验脚本/创造页表使用。

- [ ] **Step 1: 追加数据与展开函数（完整代码）**

```python
# ============================================================================
# 建筑方块族（子项目 C）/ Building-block families
# ============================================================================
THEME_ORDER = ["frost", "fire", "wind"]

FROST_STONE_PROFILE = dict(target_hue=0.55, sat_floor=0.10, sat_mul=0.55, val_mul=1.02,
                           protect_sat=None, speck_hue_shift=0.0)
FROST_DEEP_PROFILE = dict(target_hue=0.55, sat_floor=0.10, sat_mul=0.50, val_mul=0.62,
                          protect_sat=None, speck_hue_shift=0.0)

STONE_BASES = [
    dict(theme="frost", name="permafrost", deep="deep_permafrost",
         en="Permafrost", zh="冻土", deep_en="Deep Permafrost", deep_zh="深层冻土",
         stone_profile=FROST_STONE_PROFILE, deep_profile=FROST_DEEP_PROFILE),
    dict(theme="fire", name="fire_stone", deep="deep_fire_stone",
         en="Fire Stone", zh="火石", deep_en="Deep Fire Stone", deep_zh="深层火石",
         stone_profile=THEMES[0]["stone_profile"], deep_profile=THEMES[0]["deep_profile"]),
    dict(theme="wind", name="weathered_sandstone", deep="deep_weathered_sandstone",
         en="Weathered Sandstone", zh="风化砂石",
         deep_en="Deep Weathered Sandstone", deep_zh="深层风化砂石",
         stone_profile=THEMES[1]["stone_profile"], deep_profile=THEMES[1]["deep_profile"]),
]

WOOD_BASES = [
    dict(theme="fire", prefix="scorched", en="Scorched", zh="焦木",
         planks_src="block/oak_planks", profile=THEMES[0]["wood_profile"],
         log="scorched_log", stripped_log="stripped_scorched_log"),
    dict(theme="wind", prefix="arid", en="Arid", zh="风化木",
         planks_src="block/oak_planks", profile=THEMES[1]["wood_profile"],
         log="arid_log", stripped_log="stripped_arid_log"),
]

# Phase B 为空；Phase C 用真实族填充。
BUILDING_FAMILIES = []


def build_stone_families():
    return list(STONE_BASES)


def build_wood_families():
    return list(WOOD_BASES)


# (suffix, en_suffix, zh_suffix, model, needs_parent_model)
_SHALLOW_DERIVED = [
    ("polished", "Polished", "磨制", "cube_all", False),
    ("polished_stairs", "Polished Stairs", "磨制楼梯", "stairs", True),
    ("polished_slab", "Polished Slab", "磨制台阶", "slab", True),
    ("polished_wall", "Polished Wall", "磨制墙", "wall", True),
    ("bricks", "Bricks", "砖", "cube_all", False),
    ("brick_stairs", "Brick Stairs", "砖楼梯", "stairs", True),
    ("brick_slab", "Brick Slab", "砖台阶", "slab", True),
    ("brick_wall", "Brick Wall", "砖墙", "wall", True),
    ("cracked_bricks", "Cracked Bricks", "裂纹砖", "cube_all", False),
    ("chiseled", "Chiseled", "雕纹", "cube_all", False),
    ("pillar", "Pillar", "柱", "pillar", True),
]
_DEEP_DERIVED = [d for d in _SHALLOW_DERIVED if d[0] not in ("cracked_bricks", "pillar")]


def _stone_display(base_en, base_zh, suffix_en, suffix_zh):
    # 磨制/雕纹 为限定词前置，其余为族名前置，与设计文档 §9.3 样例一致。
    if suffix_en in ("Polished", "Chiseled"):
        return f"{suffix_en} {base_en}", f"{suffix_zh}{base_zh}"
    return f"{base_en} {suffix_en}", f"{base_zh}{suffix_zh}"


def _is_deep(name):
    return name.startswith("deep_")


def stone_specs(family):
    specs = []
    for base_name, base_en, base_zh, profile, is_deep in (
            (family["name"], family["en"], family["zh"], family["stone_profile"], False),
            (family["deep"], family["deep_en"], family["deep_zh"], family["deep_profile"], True)):
        derived = _DEEP_DERIVED if is_deep else _SHALLOW_DERIVED
        for suffix, sen, szh, model, has_parent in derived:
            name = f"{base_name}_{suffix}"
            en, zh = _stone_display(base_en, base_zh, sen, szh)
            spec = dict(name=name, model=model, en=en, zh=zh, loot=("self",),
                        tool="pickaxe", needs="iron" if is_deep else "stone",
                        textures=[], textures_src=[], item=("parent", name), tags=[])
            if has_parent:
                spec["tex"] = {"parent": f"{base_name}_{_parent_of(suffix)}"}
            if model == "slab":
                spec["tex"] = {"parent": f"{base_name}_{_parent_of(suffix)}",
                               "double": f"{base_name}_{_parent_of(suffix)}"}
            if model == "wall":
                spec["tags"] = ["walls"]
            if not has_parent:  # polished / bricks / cracked / chiseled -> 新贴图
                src = {"polished": "block/polished_deepslate", "bricks": "block/deepslate_bricks",
                       "cracked_bricks": "block/cracked_deepslate_bricks",
                       "chiseled": "block/chiseled_deepslate"}[suffix]
                spec["textures"] = [(name, src, profile)]
                spec["tex"] = {"parent": name}
            if model == "pillar":
                spec["textures"] = [(f"{name}", "block/deepslate", profile),
                                    (f"{name}_top", "block/deepslate_top", profile)]
                spec["pillar_side"] = name
                spec["pillar_top"] = f"{name}_top"
            specs.append(spec)
    return specs


def _parent_of(suffix):
    if suffix.startswith("polished"):
        return "polished"
    if suffix.startswith("brick") or suffix in ("bricks",):
        return "bricks"
    return suffix


_WOOD_DERIVED = [
    ("planks", "Planks", "木板", "cube_all"),
    ("stairs", "Stairs", "楼梯", "stairs"),
    ("slab", "Slab", "台阶", "slab"),
    ("fence", "Fence", "栅栏", "fence"),
    ("fence_gate", "Fence Gate", "栅栏门", "fence_gate"),
    ("door", "Door", "门", "door"),
    ("trapdoor", "Trapdoor", "活板门", "trapdoor"),
    ("pressure_plate", "Pressure Plate", "压力板", "pressure_plate"),
    ("button", "Button", "按钮", "button"),
]


def wood_specs(family):
    specs = [dict(name=f"stripped_{family['prefix']}_wood", model="pillar",
                  en=f"Stripped {family['en']} Wood", zh=f"去皮{family['zh']}",
                  loot=("self",), tool="axe", needs=None, tags=["logs"],
                  textures=[], tex={"parent": family["stripped_log"]},
                  pillar_side=family["stripped_log"], pillar_top=family["stripped_log"])]
    for suffix, sen, szh, model in _WOOD_DERIVED:
        name = f"{family['prefix']}_{suffix}"
        spec = dict(name=name, model=model, en=f"{family['en']} {sen}", zh=f"{family['zh']}{szh}",
                    loot=("self",), tool="axe", needs=None, tags=[], tex={"parent": f"{family['prefix']}_planks"},
                    textures=[], item=("parent", name))
        if suffix == "planks":
            spec["textures"] = [(name, family["planks_src"], family["profile"])]
            spec["tex"] = {"parent": name}
            spec["tags"] = ["planks"]
        elif suffix == "stairs":
            spec["tags"] = ["wooden_stairs"]
        elif suffix == "slab":
            spec["tags"] = ["wooden_slabs"]
            spec["tex"] = {"parent": f"{family['prefix']}_planks", "double": f"{family['prefix']}_planks"}
        elif suffix == "fence":
            spec["tags"] = ["wooden_fences"]
        elif suffix == "fence_gate":
            spec["tags"] = ["fence_gates"]
        elif suffix == "door":
            spec["tags"] = ["wooden_doors"]
            spec["item"] = ("generated", "weather_realm:item/" + name)
            spec["item_textures"] = [(name, "item/oak_door", family["profile"])]
        elif suffix == "trapdoor":
            spec["tags"] = ["wooden_trapdoors"]
            spec["item"] = ("parent", name + "_bottom")
        elif suffix == "pressure_plate":
            spec["tags"] = ["wooden_pressure_plates"]
        elif suffix == "button":
            spec["tags"] = ["wooden_buttons"]
            spec["item"] = ("parent", name + "_inventory")
        if model == "fence":
            spec["item"] = ("parent", name + "_inventory")
        if model == "wall":
            spec["item"] = ("parent", name + "_inventory")
        specs.append(spec)
    return specs


def all_building_block_ids():
    ids = []
    for fam in BUILDING_FAMILIES:
        if fam.get("kind") == "stone":
            ids += [s["name"] for s in stone_specs(fam)]
        elif fam.get("kind") == "wood":
            ids += [s["name"] for s in wood_specs(fam)]
        elif fam.get("kind") == "lantern":
            ids.append(fam["name"])
        elif fam.get("kind") == "decoration":
            ids += [f"{fam['prefix']}_{s}" for s in ("glass", "glass_pane", "grate", "chain")]
    return ids
```

> **注意**：Phase B 只提交以上「引擎」，`BUILDING_FAMILIES` 保持空；`all_building_block_ids()` 返回 `[]`，故校验脚本空集通过。`decoration`/`lantern` 的 spec 生成函数在 Phase C/D 追加（见 C4/D3）。

- [ ] **Step 2: 语法检查**

Run: `python -c "import sys;sys.path.insert(0,r'tools');import biome_data as bd;print(bd.all_building_block_ids(), len(bd.build_stone_families()), len(bd.build_wood_families()))"`
Expected: `[] 3 2`。

- [ ] **Step 3: 提交**

```powershell
git add tools/biome_data.py
git commit -m "feat(assets-pipeline): 新增建筑方块族声明结构与展开函数"
```

### Task B3: `write_tags` 改造与 `walls` 标签新建

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py:336-370`（`write_tags` 全量替换）
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py:394-417`（`run` 传参）

**Interfaces:**
- Consumes: B2 的 spec 字段 `tool` / `needs` / `tags`。
- Produces: `def write_tags(root, covers, specs, themes, building_specs) -> None`；新建 `data/minecraft/tags/block/walls.json`。

**新方块标签策略（设计文档 §7.3）**：木族衍生件 → `axe` + `planks`/`logs`/`wooden_stairs`/…；石质形状变体 → `pickaxe` + 母岩 `needs_*`（浅层 `stone`、深层 `iron`）+（仅 `_wall`）`walls`；灯笼 → `pickaxe`；玻璃/玻璃板 → 不设 `mineable`；格栅/锁链 → `pickaxe`；生态小物 → 晶簇/尖锥 `pickaxe`、叠层不设。

- [ ] **Step 1: 用下列完整实现替换 `write_tags`（第 336-370 行）**

```python
def write_tags(root: Path, covers, specs, themes, building_specs) -> None:
    mc_tags = root / "src/main/resources/data/minecraft/tags"

    pickaxe, axe, shovel, logs, leaves, small_flowers, flowers = [], [], [], [], [], [], []
    needs_stone, needs_iron, needs_diamond = [], [], []
    planks, wooden_stairs, wooden_slabs, wooden_fences, wooden_fence_gates = [], [], [], [], []
    wooden_doors, wooden_trapdoors, wooden_pressure_plates, wooden_buttons = [], [], [], []
    walls = []

    for theme in themes:
        names = [s["name"] for s in specs[theme["key"]]]
        pickaxe += [n for n in names
                    if n.endswith("_ore") or n in (theme["base"], theme["deep"], theme["crystal_block"])]
        wn = bd.wood_block_names(theme)
        axe += [wn["log"], wn["wood"], wn["stripped_log"]]
        logs += [wn["log"], wn["wood"], wn["stripped_log"]]
        leaves.append(wn["leaves"])
        for plant in theme["plants"]:
            if plant["flower"]:
                small_flowers.append(plant["name"])
                flowers.append(plant["name"])
        for ore in ("copper", "lapis"):
            needs_stone += [bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)]
        for ore in ("gold", "redstone", "emerald", "diamond"):
            needs_iron += [bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)]
        needs_diamond += [bd.shallow_crystal_ore(theme), bd.deep_crystal_ore(theme)]
    shovel += [c["name"] for c in covers]

    tag_targets = {"planks": planks, "logs": logs, "wooden_stairs": wooden_stairs,
                   "wooden_slabs": wooden_slabs, "wooden_fences": wooden_fences,
                   "fence_gates": wooden_fence_gates, "wooden_doors": wooden_doors,
                   "wooden_trapdoors": wooden_trapdoors,
                   "wooden_pressure_plates": wooden_pressure_plates,
                   "wooden_buttons": wooden_buttons, "walls": walls}
    for spec in building_specs:
        name = spec["name"]
        tool = spec.get("tool")
        if tool == "pickaxe":
            pickaxe.append(name)
        elif tool == "axe":
            axe.append(name)
        elif tool == "shovel":
            shovel.append(name)
        needs = spec.get("needs")
        if needs == "stone":
            needs_stone.append(name)
        elif needs == "iron":
            needs_iron.append(name)
        elif needs == "diamond":
            needs_diamond.append(name)
        for tag in spec.get("tags", []):
            tag_targets[tag].append(name)

    def mod(items):
        return [f"{MODID}:{n}" for n in items]

    merge_tag(mc_tags / "block/mineable/pickaxe.json", mod(pickaxe))
    merge_tag(mc_tags / "block/mineable/axe.json", mod(axe))
    merge_tag(mc_tags / "block/mineable/shovel.json", mod(shovel))
    merge_tag(mc_tags / "block/logs.json", mod(logs))
    merge_tag(mc_tags / "block/leaves.json", mod(leaves))
    merge_tag(mc_tags / "block/small_flowers.json", mod(small_flowers))
    merge_tag(mc_tags / "block/flowers.json", mod(flowers))
    merge_tag(mc_tags / "block/needs_stone_tool.json", mod(needs_stone))
    merge_tag(mc_tags / "block/needs_iron_tool.json", mod(needs_iron))
    merge_tag(mc_tags / "block/needs_diamond_tool.json", mod(needs_diamond))
    merge_tag(mc_tags / "block/walls.json", mod(walls))
    for fname in ("planks", "wooden_stairs", "wooden_slabs", "wooden_fences",
                  "fence_gates", "wooden_doors", "wooden_trapdoors",
                  "wooden_pressure_plates", "wooden_buttons"):
        merge_tag(mc_tags / "block" / f"{fname}.json", mod(tag_targets[fname]))
        merge_tag(mc_tags / "item" / f"{fname}.json", mod(tag_targets[fname]))
    merge_tag(mc_tags / "item/logs.json", mod(logs))
```

- [ ] **Step 2: 更新 `run()` 调用（第 413 行附近）**

将：

```python
    write_tags(root, covers, specs, bd.THEMES)
    write_lang(root, covers, specs, bd.THEMES)
```

改为：

```python
    building_specs = []
    for fam in bd.BUILDING_FAMILIES:
        if fam["kind"] == "stone":
            building_specs += bd.stone_specs(fam)
        elif fam["kind"] == "wood":
            building_specs += bd.wood_specs(fam)
    for spec in building_specs:
        write_block_client(root, spec)
        write_block_loot(root, spec)
        total += 1
    write_tags(root, covers, specs, bd.THEMES, building_specs)
    write_lang(root, covers, specs, bd.THEMES)
```

- [ ] **Step 3: 重跑生成器（空建筑表）**

Run: `python tools/gen_block_assets.py`
Expected: 末行 `... for 59 blocks ...`（因 `BUILDING_FAMILIES` 为空，数量不变）。

- [ ] **Step 4: 确认标签只增不删、且新建了 `walls.json`**

Run: `git status --short; Test-Path src\main\resources\data\minecraft\tags\block\walls.json`
Expected: `src/` 下仅可能新增 `data/minecraft/tags/block/walls.json`（内容 `{"replace": false, "values": []}`）；`git diff` 中既有标签文件的既有行**不得减少**。

- [ ] **Step 5: 提交**

```powershell
git add tools/gen_block_assets.py src/main/resources/data/minecraft/tags/block/walls.json
git commit -m "feat(assets-pipeline): write_tags 支持 tool/needs/tags 并新建 walls 标签"
```

### Task B4: `write_lang` 扩展

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py:373-390`（`write_lang` 全量替换）

**Interfaces:**
- Consumes: B2 spec 的 `en` / `zh` 字段。
- Produces: `def write_lang(root, covers, specs, themes, building_specs) -> None`；合并语义不变（生成器键覆盖、其余键保留）。

**中文名来源**：spec 的 `zh` 字段（由 B2 `_stone_display`/`wood_specs` 按设计文档 §9.3 词根规则拼装）；键格式 `block.<modid>.<id>`，与现有风格一致。

- [ ] **Step 1: 用下列完整实现替换 `write_lang`（第 373-390 行）**

```python
def write_lang(root: Path, covers, specs, themes, building_specs) -> None:
    lang_dir = root / "src/main/resources/assets" / MODID / "lang"
    en, zh = {}, {}
    for spec in covers:
        en[f"block.{MODID}.{spec['name']}"] = spec["en"]
        zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]
    for theme in themes:
        for spec in specs[theme["key"]]:
            en[f"block.{MODID}.{spec['name']}"] = spec["en"]
            zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]
        en[f"item.{MODID}.{theme['crystal']}"] = theme["crystal_en"]
        zh[f"item.{MODID}.{theme['crystal']}"] = theme["crystal_zh"]
    for spec in building_specs:
        en[f"block.{MODID}.{spec['name']}"] = spec["en"]
        zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]

    for filename, table in (("en_us.json", en), ("zh_cn.json", zh)):
        path = lang_dir / filename
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        data.update(table)
        write_json(path, data)
```

- [ ] **Step 2: 更新 `run()` 调用**

将 `write_lang(root, covers, specs, bd.THEMES)` 改为 `write_lang(root, covers, specs, bd.THEMES, building_specs)`。

- [ ] **Step 3: 语法检查 + 重跑 + 行数不减**

Run: `python -c "import ast;ast.parse(open(r'tools/gen_block_assets.py',encoding='utf-8').read());print('SYNTAX OK')"; python tools/gen_block_assets.py; python -c "import json;print(len(json.load(open(r'src/main/resources/assets/weather_realm/lang/zh_cn.json',encoding='utf-8'))))"`
Expected: `SYNTAX OK`；生成器末行 59 方块；lang 键数 `169`（Phase A 后为 169，不减少）。

- [ ] **Step 4: 提交**

```powershell
git add tools/gen_block_assets.py
git commit -m "feat(assets-pipeline): write_lang 合并建筑方块中英文名"
```

### Task B5: 双向校验脚本 `tools/verify_building_assets.py`

**Files:**
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\verify_building_assets.py`

**Interfaces:**
- Consumes: `biome_data.all_building_block_ids()`、Java 字面注册（`ModBlocks.java`/`ModItems.java`）、`ModCreativeTabs.java` 分类表、资源目录。
- Produces: 可执行 `python tools/verify_building_assets.py`，退出码 0/1。

**双向断言**：
1. **表 → Java**：`all_building_block_ids()` 每个 id 必须出现在 `ModBlocks.java`/`ModItems.java` 的字面注册中，或出现在 `ModCreativeTabs.java` 的标签页 id 表中（新建材在 `ModBuildingBlocks` 中按变量名注册，id 不字面出现，故以标签页字面表为准）。
2. **Java → 表 / 资源**：`all_building_block_ids()` 每个 id 必须有 `blockstates/<id>.json`、`models/item/<id>.json`、`loot_table/blocks/<id>.json` 与至少一个 `models/block/<id>*.json`；且 `ModBlocks.java`/`ModItems.java` 中凡匹配「新建材命名模式」的字面 id 必须在表内。

**空集成立**：`all_building_block_ids()` 在 Phase B 返回 `[]` → 两个方向均为空 → 退出 0。

- [ ] **Step 1: 创建脚本**

```python
#!/usr/bin/env python3
"""Two-way assertion for the new building-block family (design §7.4).

Direction 1 (table -> Java): every id in biome_data.all_building_block_ids() must be
registered literally in ModBlocks/ModItems, or placed as a literal in ModCreativeTabs.
Direction 2 (Java -> table/resources): every table id must have blockstate / block model /
item model / loot table; and any literal ModBlocks/ModItems id matching a building naming
pattern must be present in the table.

Usage (from the repo root):
    python tools/verify_building_assets.py
Exits 0 when both directions have an empty symmetric difference, else 1.
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402

MODID = bd.MODID
JAVA_DIR = Path("src/main/java/com/example/weather_realm")

REGISTER_RES = [
    re.compile(r'registerSimpleBlock\(\s*"([a-z0-9_]+)"'),
    re.compile(r'registerBlock\(\s*"([a-z0-9_]+)"'),
    re.compile(r'registerSimpleBlockItem\(\s*"([a-z0-9_]+)"'),
    re.compile(r'(?:ITEMS|BLOCKS)\.register\(\s*"([a-z0-9_]+)"'),
]
TAB_ID_RE = re.compile(r'new\s+TabCategory\(\s*"(\w+)"\s*,\s*List\.of\((.*?)\)\)', re.DOTALL)
QUOTED_RE = re.compile(r'"([a-z0-9_]+)"')

# 新建材命名模式（用于识别“Java 里有、表里没有”的多余建材）
BUILDING_PATTERNS = [
    r'^(?:permafrost|deep_permafrost|fire_stone|deep_fire_stone|weathered_sandstone|deep_weathered_sandstone)_'
    r'(?:polished|polished_stairs|polished_slab|polished_wall|bricks|brick_stairs|brick_slab|brick_wall|cracked_bricks|chiseled|pillar)$',
    r'^(?:scorched|arid)_(?:planks|stairs|slab|fence|fence_gate|door|trapdoor|pressure_plate|button)$',
    r'^stripped_(?:scorched|arid)_wood$',
    r'^(?:frost|blaze|wind)_(?:lantern|glass|glass_pane|grate|chain)$',
    r'^(?:frost|blaze|wind)_(?:crystal_cluster|snow_layer|ash_layer|sand_layer|spike)$',
]
BUILDING_RE = [re.compile(p) for p in BUILDING_PATTERNS]


def is_building_id(name: str) -> bool:
    return any(r.match(name) for r in BUILDING_RE)


def java_literal_ids(root: Path) -> set:
    ids = set()
    for fname in ("ModBlocks.java", "ModItems.java"):
        text = (root / JAVA_DIR / fname).read_text(encoding="utf-8")
        for regex in REGISTER_RES:
            ids.update(regex.findall(text))
    return ids


def tab_ids(root: Path) -> set:
    text = (root / JAVA_DIR / "ModCreativeTabs.java").read_text(encoding="utf-8")
    ids = set()
    for _key, body in TAB_ID_RE.findall(text):
        ids.update(QUOTED_RE.findall(body))
    return ids


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    args = ap.parse_args()
    root = Path(args.root).resolve()
    assets = root / "src/main/resources/assets" / MODID
    data = root / "src/main/resources/data" / MODID

    expected = sorted(bd.all_building_block_ids())
    java_ids = java_literal_ids(root) | tab_ids(root)

    problems = []
    missing_java = [i for i in expected if i not in java_ids]
    if missing_java:
        problems.append(f"table ids missing from Java/tab: {missing_java}")

    extra_java = sorted(i for i in java_ids if is_building_id(i) and i not in set(expected))
    if extra_java:
        problems.append(f"Java building ids missing from table: {extra_java}")

    for i in expected:
        need = [
            assets / "blockstates" / f"{i}.json",
            assets / "models/item" / f"{i}.json",
            data / "loot_table/blocks" / f"{i}.json",
        ]
        for path in need:
            if not path.is_file():
                problems.append(f"missing resource: {path.relative_to(root)}")
        if not list((assets / "models/block").glob(f"{i}*.json")):
            problems.append(f"missing block model: {i}")

    print(f"[build] table ids   : {len(expected)}")
    print(f"[build] java/tab ids: {len(java_ids)}")
    if problems:
        print("BUILDING ASSETS FAILED", file=sys.stderr)
        for p in problems[:50]:
            print("  -", p, file=sys.stderr)
        sys.exit(1)
    print("BUILDING ASSETS OK (0 differences)")
    sys.exit(0)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行（Phase B 空集）**

Run: `python tools/verify_building_assets.py; echo "exit=$LASTEXITCODE"`
Expected: `[build] table ids   : 0`、`[build] java/tab ids: <非零>`（现有字面 id 数）、`BUILDING ASSETS OK (0 differences)`、`exit=0`。

- [ ] **Step 3: 静态命名模式自检（确认不误伤既有 id）**

Run: `python -c "import sys;sys.path.insert(0,r'tools');import verify_building_assets as v;import pathlib;print([i for i in v.java_literal_ids(pathlib.Path('.')) if v.is_building_id(i)])"`
Expected: `[]`（现有 105 方块无命中任何新建材模式）。

- [ ] **Step 4: 提交**

```powershell
git add tools/verify_building_assets.py
git commit -m "test(assets-pipeline): 新增建材双向断言校验脚本"
```

### Task B6: 阶段 B 收尾（构建 + 部署）

**Files:** 无。

**Interfaces:** Consumes B1-B5。Produces 阶段 B 可运行产物。

- [ ] **Step 1: 构建**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 2: 两个校验脚本均退出 0**

Run: `python tools/verify_tab_coverage.py; python tools/verify_building_assets.py`
Expected: 分别 `TAB COVERAGE OK` 与 `BUILDING ASSETS OK (0 differences)`。

- [ ] **Step 3: 工作区检查（只应留运行时目录）**

Run: `git status --short`
Expected: 仅 `?? .opencode/`、`?? tools/__pycache__/`。

- [ ] **Step 4: 部署**

Run: `.\deploy.ps1`
Expected: jar 复制到 PCL mods 目录（客户端须已退出）。

---

# 阶段 C — 建筑方块内容（95 件）

> 设计文档对照：§5、§8、§9、§6（生态小物在 D）。
> **顺序**：C1（Java 注册骨架）→ C5（配方）→ C4（族数据 + 资源生成）→ C7（创造页表）→ 收尾。C2/C3/C6 与相邻任务并行推进；每个 Task 独立提交。

### Task C1: 新增 `ModBuildingBlocks.java`

**Files:**
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModBuildingBlocks.java`

**Interfaces:**
- Consumes: `ModBlocks.PERMAFROST/DEEP_PERMAFROST/FIRE_STONE/DEEP_FIRE_STONE/WEATHERED_SANDSTONE/DEEP_WEATHERED_SANDSTONE`、`ModBlockProperties`、`com.example.weather_realm.block.FrostLogBlock`。
- Produces（供 C2/C7/D2 使用）：
  - `record StoneLayer(String name, DeferredBlock<Block> base, DeferredBlock<Block> polished, DeferredBlock<StairBlock> polishedStairs, DeferredBlock<SlabBlock> polishedSlab, DeferredBlock<WallBlock> polishedWall, DeferredBlock<Block> bricks, DeferredBlock<StairBlock> brickStairs, DeferredBlock<SlabBlock> brickSlab, DeferredBlock<WallBlock> brickWall, DeferredBlock<Block> crackedBricks, DeferredBlock<Block> chiseled, DeferredBlock<RotatedPillarBlock> pillar)`
  - `record StoneFamily(String themeKey, StoneLayer shallow, StoneLayer deep)`
  - `record WoodFamily(String prefix, String themeKey, BlockSetType setType, WoodType woodType, DeferredBlock<FrostLogBlock> strippedWood, DeferredBlock<Block> planks, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab, DeferredBlock<FenceBlock> fence, DeferredBlock<FenceGateBlock> fenceGate, DeferredBlock<DoorBlock> door, DeferredBlock<TrapDoorBlock> trapdoor, DeferredBlock<PressurePlateBlock> pressurePlate, DeferredBlock<ButtonBlock> button)`
  - `record LanternSet(String prefix, DeferredBlock<LanternBlock> lantern)`
  - `record DecorationSet(String prefix, DeferredBlock<TransparentBlock> glass, DeferredBlock<IronBarsBlock> glassPane, DeferredBlock<IronBarsBlock> grate, DeferredBlock<ChainBlock> chain)`
  - `public static final List<StoneFamily> STONE_FAMILIES`、`List<WoodFamily> WOOD_FAMILIES`、`List<LanternSet> LANTERNS`、`List<DecorationSet> DECORATIONS`
  - `public static void register(IEventBus)`；`static` 初始化块

- [ ] **Step 1: 创建文件（完整代码）**

```java
package com.example.weather_realm;

import java.util.ArrayList;
import java.util.List;

import com.example.weather_realm.block.FrostLogBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 建筑方块注册 / Building-block registration (design §8).
 *
 * <p>Holds the ordered {@link StoneFamily} / {@link WoodFamily} / {@link LanternSet} /
 * {@link DecorationSet} lists whose insertion order is the family order used by recipes and
 * the creative page. {@link ModItems} turns every holder here into a BlockItem.</p>
 */
public final class ModBuildingBlocks {
    private ModBuildingBlocks() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WeatherRealm.MODID);

    public static final BlockSetType SCORCHED_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType("scorched"));
    public static final WoodType SCORCHED_WOOD_TYPE = WoodType.register(new WoodType("scorched", SCORCHED_BLOCK_SET_TYPE));
    public static final BlockSetType ARID_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType("arid"));
    public static final WoodType ARID_WOOD_TYPE = WoodType.register(new WoodType("arid", ARID_BLOCK_SET_TYPE));

    public record StoneLayer(String name, DeferredBlock<Block> base,
                             DeferredBlock<Block> polished, DeferredBlock<StairBlock> polishedStairs,
                             DeferredBlock<SlabBlock> polishedSlab, DeferredBlock<WallBlock> polishedWall,
                             DeferredBlock<Block> bricks, DeferredBlock<StairBlock> brickStairs,
                             DeferredBlock<SlabBlock> brickSlab, DeferredBlock<WallBlock> brickWall,
                             DeferredBlock<Block> crackedBricks, DeferredBlock<Block> chiseled,
                             DeferredBlock<RotatedPillarBlock> pillar) {
    }

    public record StoneFamily(String themeKey, StoneLayer shallow, StoneLayer deep) {
    }

    public record WoodFamily(String prefix, String themeKey, BlockSetType setType, WoodType woodType,
                             DeferredBlock<FrostLogBlock> strippedWood, DeferredBlock<Block> planks,
                             DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                             DeferredBlock<FenceBlock> fence, DeferredBlock<FenceGateBlock> fenceGate,
                             DeferredBlock<DoorBlock> door, DeferredBlock<TrapDoorBlock> trapdoor,
                             DeferredBlock<PressurePlateBlock> pressurePlate, DeferredBlock<ButtonBlock> button) {
    }

    public record LanternSet(String prefix, DeferredBlock<LanternBlock> lantern) {
    }

    public record DecorationSet(String prefix, DeferredBlock<TransparentBlock> glass,
                                DeferredBlock<IronBarsBlock> glassPane, DeferredBlock<IronBarsBlock> grate,
                                DeferredBlock<ChainBlock> chain) {
    }

    public static final List<StoneFamily> STONE_FAMILIES = new ArrayList<>();
    public static final List<WoodFamily> WOOD_FAMILIES = new ArrayList<>();
    public static final List<LanternSet> LANTERNS = new ArrayList<>();
    public static final List<DecorationSet> DECORATIONS = new ArrayList<>();

    private static StoneLayer layer(String name, DeferredBlock<Block> base, BlockBehaviour.Properties props,
                                    boolean deep) {
        DeferredBlock<Block> polished = BLOCKS.registerSimpleBlock(name + "_polished", props);
        DeferredBlock<StairBlock> polishedStairs = BLOCKS.register(name + "_polished_stairs",
                () -> new StairBlock(polished.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(polished.get())));
        DeferredBlock<SlabBlock> polishedSlab = BLOCKS.register(name + "_polished_slab",
                () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(polished.get())));
        DeferredBlock<WallBlock> polishedWall = BLOCKS.register(name + "_polished_wall",
                () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(polished.get()).forceSolidOn()));
        DeferredBlock<Block> bricks = BLOCKS.registerSimpleBlock(name + "_bricks", props);
        DeferredBlock<StairBlock> brickStairs = BLOCKS.register(name + "_brick_stairs",
                () -> new StairBlock(bricks.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(bricks.get())));
        DeferredBlock<SlabBlock> brickSlab = BLOCKS.register(name + "_brick_slab",
                () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(bricks.get())));
        DeferredBlock<WallBlock> brickWall = BLOCKS.register(name + "_brick_wall",
                () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(bricks.get()).forceSolidOn()));
        DeferredBlock<Block> cracked = deep ? null : BLOCKS.registerSimpleBlock(name + "_cracked_bricks", props);
        DeferredBlock<Block> chiseled = BLOCKS.registerSimpleBlock(name + "_chiseled", props);
        DeferredBlock<RotatedPillarBlock> pillar = deep ? null : BLOCKS.register(name + "_pillar",
                () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(bricks.get())));
        return new StoneLayer(name, base, polished, polishedStairs, polishedSlab, polishedWall,
                bricks, brickStairs, brickSlab, brickWall, cracked, chiseled, pillar);
    }

    private static WoodFamily woodFamily(String prefix, String themeKey, BlockSetType setType, WoodType woodType,
                                         MapColor mapColor) {
        DeferredBlock<FrostLogBlock> strippedWood = BLOCKS.registerBlock("stripped_" + prefix + "_wood",
                FrostLogBlock::new, ModBlockProperties.woodPillar(mapColor));
        DeferredBlock<Block> planks = BLOCKS.registerSimpleBlock(prefix + "_planks",
                BlockBehaviour.Properties.of()
                        .mapColor(mapColor)
                        .instrument(NoteBlockInstrument.BASS)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .ignitedByLava());
        DeferredBlock<StairBlock> stairs = BLOCKS.register(prefix + "_stairs",
                () -> new StairBlock(planks.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(planks.get())));
        DeferredBlock<SlabBlock> slab = BLOCKS.register(prefix + "_slab",
                () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(planks.get())));
        DeferredBlock<FenceBlock> fence = BLOCKS.register(prefix + "_fence",
                () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(planks.get())));
        DeferredBlock<FenceGateBlock> fenceGate = BLOCKS.register(prefix + "_fence_gate",
                () -> new FenceGateBlock(woodType, BlockBehaviour.Properties.ofFullCopy(planks.get()).forceSolidOn()));
        DeferredBlock<DoorBlock> door = BLOCKS.register(prefix + "_door",
                () -> new DoorBlock(setType, BlockBehaviour.Properties.of()
                        .mapColor(mapColor).instrument(NoteBlockInstrument.BASS).strength(3.0F)
                        .noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)));
        DeferredBlock<TrapDoorBlock> trapdoor = BLOCKS.register(prefix + "_trapdoor",
                () -> new TrapDoorBlock(setType, BlockBehaviour.Properties.of()
                        .mapColor(mapColor).instrument(NoteBlockInstrument.BASS).strength(3.0F)
                        .noOcclusion().isValidSpawn((state, level, pos, type) -> false).ignitedByLava()));
        DeferredBlock<PressurePlateBlock> pressurePlate = BLOCKS.register(prefix + "_pressure_plate",
                () -> new PressurePlateBlock(setType, BlockBehaviour.Properties.of()
                        .mapColor(mapColor).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                        .noCollission().strength(0.5F).ignitedByLava().pushReaction(PushReaction.DESTROY)));
        DeferredBlock<ButtonBlock> button = BLOCKS.register(prefix + "_button",
                () -> new ButtonBlock(setType, 30, BlockBehaviour.Properties.of()
                        .noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
        return new WoodFamily(prefix, themeKey, setType, woodType, strippedWood, planks, stairs, slab,
                fence, fenceGate, door, trapdoor, pressurePlate, button);
    }

    private static LanternSet lanternSet(String prefix, MapColor color) {
        DeferredBlock<LanternBlock> lantern = BLOCKS.register(prefix + "_lantern",
                () -> new LanternBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).strength(3.5F).sound(SoundType.LANTERN)
                        .lightLevel(state -> 15).noOcclusion().pushReaction(PushReaction.DESTROY)));
        return new LanternSet(prefix, lantern);
    }

    private static DecorationSet decorationSet(String prefix, MapColor color) {
        DeferredBlock<TransparentBlock> glass = BLOCKS.register(prefix + "_glass",
                () -> new TransparentBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                        .isValidSpawn((state, level, pos, type) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .isViewBlocking((state, level, pos) -> false)));
        DeferredBlock<IronBarsBlock> glassPane = BLOCKS.register(prefix + "_glass_pane",
                () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                        .isValidSpawn((state, level, pos, type) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .isViewBlocking((state, level, pos) -> false)));
        DeferredBlock<IronBarsBlock> grate = BLOCKS.register(prefix + "_grate",
                () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).strength(5.0F, 6.0F).sound(SoundType.METAL)
                        .requiresCorrectToolForDrops().noOcclusion()));
        DeferredBlock<ChainBlock> chain = BLOCKS.register(prefix + "_chain",
                () -> new ChainBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).strength(5.0F, 6.0F).sound(SoundType.CHAIN)
                        .requiresCorrectToolForDrops().noOcclusion()));
        return new DecorationSet(prefix, glass, glassPane, grate, chain);
    }

    private static void stoneFamily(String themeKey, String shallowName, DeferredBlock<Block> shallowBase,
                                    String deepName, DeferredBlock<Block> deepBase,
                                    BlockBehaviour.Properties shallowProps, BlockBehaviour.Properties deepProps) {
        STONE_FAMILIES.add(new StoneFamily(themeKey,
                layer(shallowName, shallowBase, shallowProps, false),
                layer(deepName, deepBase, deepProps, true)));
    }

    static {
        stoneFamily("frost", "permafrost", ModBlocks.PERMAFROST,
                "deep_permafrost", ModBlocks.DEEP_PERMAFROST,
                ModBlockProperties.stoneLike(MapColor.STONE, 2.25F, 6.0F, SoundType.DEEPSLATE),
                ModBlockProperties.stoneLike(MapColor.DEEPSLATE, 4.5F, 6.0F, SoundType.DEEPSLATE));
        stoneFamily("fire", "fire_stone", ModBlocks.FIRE_STONE,
                "deep_fire_stone", ModBlocks.DEEP_FIRE_STONE,
                ModBlockProperties.fireStone(), ModBlockProperties.deepFireStone());
        stoneFamily("wind", "weathered_sandstone", ModBlocks.WEATHERED_SANDSTONE,
                "deep_weathered_sandstone", ModBlocks.DEEP_WEATHERED_SANDSTONE,
                ModBlockProperties.weatheredSandstone(), ModBlockProperties.deepWeatheredSandstone());

        WOOD_FAMILIES.add(woodFamily("scorched", "fire", SCORCHED_BLOCK_SET_TYPE, SCORCHED_WOOD_TYPE,
                MapColor.COLOR_BROWN));
        WOOD_FAMILIES.add(woodFamily("arid", "wind", ARID_BLOCK_SET_TYPE, ARID_WOOD_TYPE, MapColor.SAND));

        LANTERNS.add(lanternSet("frost", MapColor.ICE));
        LANTERNS.add(lanternSet("blaze", MapColor.COLOR_ORANGE));
        LANTERNS.add(lanternSet("wind", MapColor.SAND));

        DECORATIONS.add(decorationSet("frost", MapColor.ICE));
        DECORATIONS.add(decorationSet("blaze", MapColor.COLOR_ORANGE));
        DECORATIONS.add(decorationSet("wind", MapColor.SAND));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
```

- [ ] **Step 2: 语法/编译（此时 `ModItems` 尚未引用，单类也应能编译）**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`（若因 `ModBlockProperties.stoneLike` 可见性报错，确认其为 `public static`，现已是）。

- [ ] **Step 3: 提交**

```powershell
git add src/main/java/com/example/weather_realm/ModBuildingBlocks.java
git commit -m "feat(building-blocks): 新增建筑方块族注册类 ModBuildingBlocks"
```

### Task C2: `ModItems` 批量 BlockItem 注册

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModItems.java:249-251`（在 `register` 方法前插入批量注册字段与 `static` 块）

**Interfaces:**
- Consumes: C1 的 `STONE_FAMILIES`/`WOOD_FAMILIES`/`LANTERNS`/`DECORATIONS` 与各 `DeferredBlock` 字段。
- Produces: `public static final List<DeferredItem<BlockItem>> ModItems.BUILDING_BLOCK_ITEMS`（顺序=石→木→灯→装饰，供参考；创造页顺序由 C7 分类表决定）。

- [ ] **Step 1: 在 `ModItems.java` 的 `register(IEventBus)` 方法之前插入**

```java
    // ============================================================================================
    // 建筑方块 BlockItem 批量注册 / Building-block items (design §8.2)
    // ============================================================================================
    public static final List<DeferredItem<BlockItem>> BUILDING_BLOCK_ITEMS = new ArrayList<>();

    private static DeferredItem<BlockItem> buildingItem(String name, DeferredBlock<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(name, block);
        BUILDING_BLOCK_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<BlockItem> buildingDoorItem(String name, DeferredBlock<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.register(name,
                () -> new DoubleHighBlockItem(block.get(), new Item.Properties()));
        BUILDING_BLOCK_ITEMS.add(item);
        return item;
    }

    static {
        for (ModBuildingBlocks.StoneFamily family : ModBuildingBlocks.STONE_FAMILIES) {
            for (ModBuildingBlocks.StoneLayer layer : new ModBuildingBlocks.StoneLayer[]{family.shallow(), family.deep()}) {
                buildingItem(layer.name() + "_polished", layer.polished());
                buildingItem(layer.name() + "_polished_stairs", layer.polishedStairs());
                buildingItem(layer.name() + "_polished_slab", layer.polishedSlab());
                buildingItem(layer.name() + "_polished_wall", layer.polishedWall());
                buildingItem(layer.name() + "_bricks", layer.bricks());
                buildingItem(layer.name() + "_brick_stairs", layer.brickStairs());
                buildingItem(layer.name() + "_brick_slab", layer.brickSlab());
                buildingItem(layer.name() + "_brick_wall", layer.brickWall());
                if (layer.crackedBricks() != null) {
                    buildingItem(layer.name() + "_cracked_bricks", layer.crackedBricks());
                }
                buildingItem(layer.name() + "_chiseled", layer.chiseled());
                if (layer.pillar() != null) {
                    buildingItem(layer.name() + "_pillar", layer.pillar());
                }
            }
        }
        for (ModBuildingBlocks.WoodFamily family : ModBuildingBlocks.WOOD_FAMILIES) {
            String p = family.prefix();
            buildingItem("stripped_" + p + "_wood", family.strippedWood());
            buildingItem(p + "_planks", family.planks());
            buildingItem(p + "_stairs", family.stairs());
            buildingItem(p + "_slab", family.slab());
            buildingItem(p + "_fence", family.fence());
            buildingItem(p + "_fence_gate", family.fenceGate());
            buildingDoorItem(p + "_door", family.door());
            buildingItem(p + "_trapdoor", family.trapdoor());
            buildingItem(p + "_pressure_plate", family.pressurePlate());
            buildingItem(p + "_button", family.button());
        }
        for (ModBuildingBlocks.LanternSet set : ModBuildingBlocks.LANTERNS) {
            buildingItem(set.prefix() + "_lantern", set.lantern());
        }
        for (ModBuildingBlocks.DecorationSet set : ModBuildingBlocks.DECORATIONS) {
            buildingItem(set.prefix() + "_glass", set.glass());
            buildingItem(set.prefix() + "_glass_pane", set.glassPane());
            buildingItem(set.prefix() + "_grate", set.grate());
            buildingItem(set.prefix() + "_chain", set.chain());
        }
    }
```

> 需要的 import（`ModItems.java` 顶部）已在文件中存在：`Block`、`BlockItem`、`DoubleHighBlockItem`、`Item`、`ArrayList`、`List`、`DeferredBlock`、`DeferredItem`。

- [ ] **Step 2: 编译**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add src/main/java/com/example/weather_realm/ModItems.java
git commit -m "feat(building-blocks): ModItems 批量注册建筑方块物品"
```

### Task C3: 接线 `WeatherRealm`（注册顺序）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\WeatherRealm.java:25-26`

**Interfaces:**
- Consumes: `ModBuildingBlocks.register(IEventBus)`。
- Produces: 保证 `ModBuildingBlocks` 在 `ModItems` 之前初始化（设计 §8.4）。

- [ ] **Step 1: 在 `ModBlocks.register` 与 `ModItems.register` 之间插入一行**

```java
        ModBlocks.register(modEventBus);          // BLOCKS
        ModBuildingBlocks.register(modEventBus);  // BUILDING BLOCKS (must init before ModItems)
        ModItems.register(modEventBus);           // ITEMS
```

- [ ] **Step 2: 编译**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add src/main/java/com/example/weather_realm/WeatherRealm.java
git commit -m "feat(building-blocks): 接线 ModBuildingBlocks 注册顺序"
```

### Task C4: 填充族数据并生成 95 件建筑资源

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\biome_data.py`（填充 `BUILDING_FAMILIES`、追加 `decoration_specs`/`lantern_specs`）
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py`（`run()` 纳入 lantern/decoration specs）
- Generate: `src/main/resources/assets/weather_realm/{textures,models,blockstates}/...`、`src/main/resources/data/weather_realm/loot_table/blocks/*.json`

**Interfaces:**
- Consumes: B2 的 `stone_specs`/`wood_specs`；B1 的 `write_block_client`；B4 的 `write_lang`；B3 的 `write_tags`。
- Produces: 95 个 spec（60 石 + 20 木 + 3 灯 + 12 装饰）。

**命名（设计 §5.1，照抄）**：族名前置 `<族名>_<派生件>`；深层族继承 `deep_` 前缀。完整 95 新 id 与中英文名见本 Task 末尾表格。

- [ ] **Step 1: 在 `biome_data.py` 末尾追加灯具/装饰 spec 生成函数**

```python
_LANTERN_BASES = [
    ("frost", "坚冰", "Frost", "block/lantern", FROST_STONE_PROFILE),
    ("blaze", "燃焰", "Blaze", "block/lantern", THEMES[0]["stone_profile"]),
    ("wind", "风沙", "Wind", "block/lantern", THEMES[1]["stone_profile"]),
]
_DECORATION_BASES = [
    ("frost", "坚冰", "Frost", FROST_STONE_PROFILE),
    ("blaze", "燃焰", "Blaze", THEMES[0]["stone_profile"]),
    ("wind", "风沙", "Wind", THEMES[1]["stone_profile"]),
]


def lantern_specs():
    specs = []
    for prefix, zh, en, src, profile in _LANTERN_BASES:
        name = f"{prefix}_lantern"
        specs.append(dict(name=name, model="lantern", en=f"{en} Lantern", zh=f"{zh}灯笼",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(name, src, profile)], tex={"lantern": name},
                          item=("generated", f"{MODID}:item/{name}"),
                          item_textures=[(name, "item/lantern", profile)]))
    return specs


def decoration_specs():
    specs = []
    for prefix, zh, en, profile in _DECORATION_BASES:
        glass = f"{prefix}_glass"
        pane = f"{prefix}_glass_pane"
        grate = f"{prefix}_grate"
        chain = f"{prefix}_chain"
        specs.append(dict(name=glass, model="glass_block", en=f"{en} Glass", zh=f"{zh}玻璃",
                          loot=("glass",), tool=None, needs=None, tags=[],
                          textures=[(glass, "block/glass", profile)], tex={"all": glass},
                          item=("parent", glass)))
        specs.append(dict(name=pane, model="pane", en=f"{en} Glass Pane", zh=f"{zh}玻璃板",
                          loot=("glass",), tool=None, needs=None, tags=[],
                          textures=[(pane, "block/glass", profile),
                                    (f"{pane}_top", "block/glass_pane_top", profile)],
                          tex={"pane": pane, "edge": f"{pane}_top"},
                          item=("generated", f"{MODID}:block/{pane}")))
        specs.append(dict(name=grate, model="pane", en=f"{en} Grate", zh=f"{zh}格栅",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(grate, "block/iron_bars", profile)],
                          tex={"pane": grate, "edge": grate},
                          item=("generated", f"{MODID}:block/{grate}")))
        specs.append(dict(name=chain, model="chain", en=f"{en} Chain", zh=f"{zh}锁链",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(chain, "block/chain", profile)], tex={"all": chain},
                          item=("generated", f"{MODID}:item/{chain}"),
                          item_textures=[(chain, "item/chain", profile)]))
    return specs
```

- [ ] **Step 2: 填充 `BUILDING_FAMILIES`（替换 B2 末尾的空列表）**

```python
BUILDING_FAMILIES = (
    [dict(kind="stone", **f) for f in STONE_BASES]
    + [dict(kind="wood", **f) for f in WOOD_BASES]
    + [dict(kind="lantern", name=f"{p}_lantern") for p, *_ in _LANTERN_BASES]
    + [dict(kind="decoration", prefix=p) for p, *_ in _DECORATION_BASES]
)
```

- [ ] **Step 3: 更新 `gen_block_assets.py` 的 `run()`，纳入灯具与装饰**

将 B3 Step 2 中的 `building_specs` 构段替换为：

```python
    building_specs = []
    for fam in bd.BUILDING_FAMILIES:
        if fam["kind"] == "stone":
            building_specs += bd.stone_specs(fam)
        elif fam["kind"] == "wood":
            building_specs += bd.wood_specs(fam)
    building_specs += bd.lantern_specs()
    building_specs += bd.decoration_specs()
    for spec in building_specs:
        write_block_client(root, spec)
        write_block_loot(root, spec)
        total += 1
    write_tags(root, covers, specs, bd.THEMES, building_specs)
    write_lang(root, covers, specs, bd.THEMES, building_specs)
```

- [ ] **Step 4: 增加 `glass` 掉落语义**

在 `gen_block_assets.py` 的 `write_block_loot`（第 307-321 行）分支里，`else` 之前加入：

```python
    elif when[0] == "glass":
        table = bd.glass_loot(name)
```

并在 `biome_data.py` 末尾追加：

```python
def glass_loot(block):
    """Vanilla glass semantics: only silk touch drops the block itself."""
    return {
        "type": "minecraft:block",
        "pools": [{
            "bonus_rolls": 0.0,
            "rolls": 1.0,
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MODID}:{block}",
                "conditions": [
                    {"condition": "minecraft:match_tool",
                     "predicate": {"predicates": {"minecraft:enchantments": [
                         {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}},
                    {"condition": "minecraft:survives_explosion"},
                ],
            }],
        }],
        "random_sequence": f"{MODID}:blocks/{block}",
    }
```

- [ ] **Step 5: 语法检查 + 生成**

Run: `python -c "import ast;ast.parse(open(r'tools/gen_block_assets.py',encoding='utf-8').read());print('SYNTAX OK')"; python tools/gen_block_assets.py`
Expected: `SYNTAX OK`；末行 `[gen] wrote textures + resources for 154 blocks ...`（59 现有 + 95 新）。

- [ ] **Step 6: 双向校验（此时表与 Java 均已填充）**

Run: `python tools/verify_building_assets.py; echo "exit=$LASTEXITCODE"`
Expected: `[build] table ids   : 95`、`BUILDING ASSETS OK (0 differences)`、`exit=0`。

- [ ] **Step 7: 检查 blockstates 与掉落表数量**

Run: `(Get-ChildItem src\main\resources\assets\weather_realm\blockstates -File).Count; (Get-ChildItem src\main\resources\data\weather_realm\loot_table\blocks -File).Count`
Expected: blockstates = `105 + 95 = 200`；loot = `103 + 95 = 198`（`weather_pedestal` 在 C6 补齐后为 199，`weather_portal` 永无掉落表）。

- [ ] **Step 8: 提交**

```powershell
git add tools/biome_data.py tools/gen_block_assets.py src/main/resources/assets/weather_realm src/main/resources/data/weather_realm/loot_table/blocks
git commit -m "feat(building-blocks): 生成 95 件建筑方块的贴图/模型/掉落表"
```

**95 个新 id 与中英文名（生成规则：浅层/深层石族按上表词根；木族按 `<族>_<派生>`）**

浅层石族（33）：

| id | English | 中文 |
| :- | :- | :- |
| permafrost_polished | Polished Permafrost | 磨制冻土 |
| permafrost_polished_stairs | Polished Permafrost Stairs | 磨制冻土楼梯 |
| permafrost_polished_slab | Polished Permafrost Slab | 磨制冻土台阶 |
| permafrost_polished_wall | Polished Permafrost Wall | 磨制冻土墙 |
| permafrost_bricks | Permafrost Bricks | 冻土砖 |
| permafrost_brick_stairs | Permafrost Brick Stairs | 冻土砖楼梯 |
| permafrost_brick_slab | Permafrost Brick Slab | 冻土砖台阶 |
| permafrost_brick_wall | Permafrost Brick Wall | 冻土砖墙 |
| permafrost_cracked_bricks | Permafrost Cracked Bricks | 冻土裂纹砖 |
| permafrost_chiseled | Chiseled Permafrost | 雕纹冻土 |
| permafrost_pillar | Permafrost Pillar | 冻土柱 |
| fire_stone_polished | Polished Fire Stone | 磨制火石 |
| fire_stone_polished_stairs | Polished Fire Stone Stairs | 磨制火石楼梯 |
| fire_stone_polished_slab | Polished Fire Stone Slab | 磨制火石台阶 |
| fire_stone_polished_wall | Polished Fire Stone Wall | 磨制火石墙 |
| fire_stone_bricks | Fire Stone Bricks | 火石砖 |
| fire_stone_brick_stairs | Fire Stone Brick Stairs | 火石砖楼梯 |
| fire_stone_brick_slab | Fire Stone Brick Slab | 火石砖台阶 |
| fire_stone_brick_wall | Fire Stone Brick Wall | 火石砖墙 |
| fire_stone_cracked_bricks | Fire Stone Cracked Bricks | 火石裂纹砖 |
| fire_stone_chiseled | Chiseled Fire Stone | 雕纹火石 |
| fire_stone_pillar | Fire Stone Pillar | 火石柱 |
| weathered_sandstone_polished | Polished Weathered Sandstone | 磨制风化砂石 |
| weathered_sandstone_polished_stairs | Polished Weathered Sandstone Stairs | 磨制风化砂石楼梯 |
| weathered_sandstone_polished_slab | Polished Weathered Sandstone Slab | 磨制风化砂石台阶 |
| weathered_sandstone_polished_wall | Polished Weathered Sandstone Wall | 磨制风化砂石墙 |
| weathered_sandstone_bricks | Weathered Sandstone Bricks | 风化砂石砖 |
| weathered_sandstone_brick_stairs | Weathered Sandstone Brick Stairs | 风化砂石砖楼梯 |
| weathered_sandstone_brick_slab | Weathered Sandstone Brick Slab | 风化砂石砖台阶 |
| weathered_sandstone_brick_wall | Weathered Sandstone Brick Wall | 风化砂石砖墙 |
| weathered_sandstone_cracked_bricks | Weathered Sandstone Cracked Bricks | 风化砂石裂纹砖 |
| weathered_sandstone_chiseled | Chiseled Weathered Sandstone | 雕纹风化砂石 |
| weathered_sandstone_pillar | Weathered Sandstone Pillar | 风化砂石柱 |

深层石族（27）：

| id | English | 中文 |
| :- | :- | :- |
| deep_permafrost_polished | Polished Deep Permafrost | 磨制深层冻土 |
| deep_permafrost_polished_stairs | Polished Deep Permafrost Stairs | 磨制深层冻土楼梯 |
| deep_permafrost_polished_slab | Polished Deep Permafrost Slab | 磨制深层冻土台阶 |
| deep_permafrost_polished_wall | Polished Deep Permafrost Wall | 磨制深层冻土墙 |
| deep_permafrost_bricks | Deep Permafrost Bricks | 深层冻土砖 |
| deep_permafrost_brick_stairs | Deep Permafrost Brick Stairs | 深层冻土砖楼梯 |
| deep_permafrost_brick_slab | Deep Permafrost Brick Slab | 深层冻土砖台阶 |
| deep_permafrost_brick_wall | Deep Permafrost Brick Wall | 深层冻土砖墙 |
| deep_permafrost_chiseled | Chiseled Deep Permafrost | 雕纹深层冻土 |
| deep_fire_stone_polished | Polished Deep Fire Stone | 磨制深层火石 |
| deep_fire_stone_polished_stairs | Polished Deep Fire Stone Stairs | 磨制深层火石楼梯 |
| deep_fire_stone_polished_slab | Polished Deep Fire Stone Slab | 磨制深层火石台阶 |
| deep_fire_stone_polished_wall | Polished Deep Fire Stone Wall | 磨制深层火石墙 |
| deep_fire_stone_bricks | Deep Fire Stone Bricks | 深层火石砖 |
| deep_fire_stone_brick_stairs | Deep Fire Stone Brick Stairs | 深层火石砖楼梯 |
| deep_fire_stone_brick_slab | Deep Fire Stone Brick Slab | 深层火石砖台阶 |
| deep_fire_stone_brick_wall | Deep Fire Stone Brick Wall | 深层火石砖墙 |
| deep_fire_stone_chiseled | Chiseled Deep Fire Stone | 雕纹深层火石 |
| deep_weathered_sandstone_polished | Polished Deep Weathered Sandstone | 磨制深层风化砂石 |
| deep_weathered_sandstone_polished_stairs | Polished Deep Weathered Sandstone Stairs | 磨制深层风化砂石楼梯 |
| deep_weathered_sandstone_polished_slab | Polished Deep Weathered Sandstone Slab | 磨制深层风化砂石台阶 |
| deep_weathered_sandstone_polished_wall | Polished Deep Weathered Sandstone Wall | 磨制深层风化砂石墙 |
| deep_weathered_sandstone_bricks | Deep Weathered Sandstone Bricks | 深层风化砂石砖 |
| deep_weathered_sandstone_brick_stairs | Deep Weathered Sandstone Brick Stairs | 深层风化砂石砖楼梯 |
| deep_weathered_sandstone_brick_slab | Deep Weathered Sandstone Brick Slab | 深层风化砂石砖台阶 |
| deep_weathered_sandstone_brick_wall | Deep Weathered Sandstone Brick Wall | 深层风化砂石砖墙 |
| deep_weathered_sandstone_chiseled | Chiseled Deep Weathered Sandstone | 雕纹深层风化砂石 |

木族补全（20）：

| id | English | 中文 |
| :- | :- | :- |
| stripped_scorched_wood | Stripped Scorched Wood | 去皮焦木 |
| scorched_planks | Scorched Planks | 焦木木板 |
| scorched_stairs | Scorched Stairs | 焦木楼梯 |
| scorched_slab | Scorched Slab | 焦木台阶 |
| scorched_fence | Scorched Fence | 焦木栅栏 |
| scorched_fence_gate | Scorched Fence Gate | 焦木栅栏门 |
| scorched_door | Scorched Door | 焦木门 |
| scorched_trapdoor | Scorched Trapdoor | 焦木活板门 |
| scorched_pressure_plate | Scorched Pressure Plate | 焦木压力板 |
| scorched_button | Scorched Button | 焦木按钮 |
| stripped_arid_wood | Stripped Arid Wood | 去皮风化木 |
| arid_planks | Arid Planks | 风化木木板 |
| arid_stairs | Arid Stairs | 风化木楼梯 |
| arid_slab | Arid Slab | 风化木台阶 |
| arid_fence | Arid Fence | 风化木栅栏 |
| arid_fence_gate | Arid Fence Gate | 风化木栅栏门 |
| arid_door | Arid Door | 风化木门 |
| arid_trapdoor | Arid Trapdoor | 风化木活板门 |
| arid_pressure_plate | Arid Pressure Plate | 风化木压力板 |
| arid_button | Arid Button | 风化木按钮 |

灯笼（3）与装饰（12）：

| id | English | 中文 |
| :- | :- | :- |
| frost_lantern | Frost Lantern | 坚冰灯笼 |
| blaze_lantern | Blaze Lantern | 燃焰灯笼 |
| wind_lantern | Wind Lantern | 风沙灯笼 |
| frost_glass | Frost Glass | 坚冰玻璃 |
| frost_glass_pane | Frost Glass Pane | 坚冰玻璃板 |
| frost_grate | Frost Grate | 坚冰格栅 |
| frost_chain | Frost Chain | 坚冰锁链 |
| blaze_glass | Blaze Glass | 燃焰玻璃 |
| blaze_glass_pane | Blaze Glass Pane | 燃焰玻璃板 |
| blaze_grate | Blaze Grate | 燃焰格栅 |
| blaze_chain | Blaze Chain | 燃焰锁链 |
| wind_glass | Wind Glass | 风沙玻璃 |
| wind_glass_pane | Wind Glass Pane | 风沙玻璃板 |
| wind_grate | Wind Grate | 风沙格栅 |
| wind_chain | Wind Chain | 风沙锁链 |

**贴图源映射**：浅/深层石族 `polished`←`block/polished_deepslate`、`bricks`←`block/deepslate_bricks`、`cracked_bricks`←`block/cracked_deepslate_bricks`、`chiseled`←`block/chiseled_deepslate`、`pillar` side←`block/deepslate` / end←`block/deepslate_top`（各以族 profile 重着色）；楼梯/台阶/墙/栅栏/栅栏门/门/活板门/压力板/按钮**不单独取图**，复用母方块贴图键；木 `planks`←`block/oak_planks`；门物品←`item/oak_door`；灯笼块←`block/lantern`、物品←`item/lantern`；玻璃←`block/glass`；玻璃板 edge←`block/glass_pane_top`；格栅←`block/iron_bars`；锁链←`block/chain` + `item/chain`。原版无 `*_wall.png`、无 `*_stairs.png`、无 `*_carpet.png`，全部按上表复用/重着色。

### Task C5: 配方生成（约 91 条）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\biome_data.py`（追加 `building_recipes()` 与配方序列化 helper）
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py`（`run()` 调用 `write_recipes`）

**Interfaces:**
- Consumes: C4 族数据。
- Produces: `def building_recipes() -> dict`、`def write_recipes(root, recipes) -> None`、`data/weather_realm/recipe/*.json`。

- [ ] **Step 1: 在 `biome_data.py` 末尾追加配方 helper 与生成器**

```python
def _shaped(pattern, key, result, count):
    return {"type": "minecraft:crafting_shaped", "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()},
            "result": {"id": result, "count": count}}


def _shapeless(ingredients, result, count):
    return {"type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": i} for i in ingredients],
            "result": {"id": result, "count": count}}


def _smelting(ingredient, result):
    return {"type": "minecraft:smelting", "ingredient": {"item": ingredient},
            "result": {"id": result}, "experience": 0.1, "cookingtime": 200}


def building_recipes():
    r = {}
    # --- 石族：磨制/砖/雕纹/柱/裂砖 + 楼梯/台阶/墙（每主题 20 条 ×3 = 60）---
    for fam in STONE_BASES:
        for layer_name, base in ((fam["name"], fam["name"]), (fam["deep"], fam["deep"])):
            pol, bricks = f"{layer_name}_polished", f"{layer_name}_bricks"
            b = f"{MODID}:{base}"
            r[pol] = _shaped(["##", "##"], {"#": b}, f"{MODID}:{pol}", 4)
            r[bricks] = _shaped(["##", "##"], {"#": b}, f"{MODID}:{bricks}", 4)
            r[f"{layer_name}_chiseled"] = _shaped(["#", "#"], {"#": b},
                                                  f"{MODID}:{layer_name}_chiseled", 1)
            r[f"{pol}_stairs"] = _shaped(["#  ", "## ", "###"], {"#": f"{MODID}:{pol}"},
                                         f"{MODID}:{pol}_stairs", 4)
            r[f"{pol}_slab"] = _shaped(["###"], {"#": f"{MODID}:{pol}"}, f"{MODID}:{pol}_slab", 6)
            r[f"{pol}_wall"] = _shaped(["###", "###"], {"#": f"{MODID}:{pol}"},
                                       f"{MODID}:{pol}_wall", 6)
            r[f"{bricks}_stairs"] = _shaped(["#  ", "## ", "###"], {"#": f"{MODID}:{bricks}"},
                                            f"{MODID}:{bricks}_stairs", 4)
            r[f"{bricks}_slab"] = _shaped(["###"], {"#": f"{MODID}:{bricks}"}, f"{MODID}:{bricks}_slab", 6)
            r[f"{bricks}_wall"] = _shaped(["###", "###"], {"#": f"{MODID}:{bricks}"},
                                          f"{MODID}:{bricks}_wall", 6)
            if layer_name == fam["name"]:
                r[f"{layer_name}_pillar"] = _shaped(["#", "#"], {"#": b},
                                                    f"{MODID}:{layer_name}_pillar", 2)
                r[f"{layer_name}_cracked_bricks"] = _smelting(f"{MODID}:{bricks}",
                                                              f"{MODID}:{layer_name}_cracked_bricks")
    # --- 木族：木板/木干/去皮木 + 楼梯/台阶/栅栏/栅栏门/门/活板门/压力板/按钮（每族 11 ×2 = 22）---
    for fam in WOOD_BASES:
        p = fam["prefix"]
        log, slog = f"{MODID}:{fam['log']}", f"{MODID}:{fam['stripped_log']}"
        planks = f"{MODID}:{p}_planks"
        r[f"{p}_planks"] = _shapeless([log], planks, 4)
        r[f"{p}_wood"] = _shaped(["##", "##"], {"#": log}, f"{MODID}:{p}_wood", 3)
        r[f"stripped_{p}_wood"] = _shaped(["##", "##"], {"#": slog}, f"{MODID}:stripped_{p}_wood", 3)
        r[f"{p}_stairs"] = _shaped(["#  ", "## ", "###"], {"#": planks}, f"{MODID}:{p}_stairs", 4)
        r[f"{p}_slab"] = _shaped(["###"], {"#": planks}, f"{MODID}:{p}_slab", 6)
        r[f"{p}_fence"] = _shaped(["#S#", "#S#"], {"#": planks, "S": "minecraft:stick"},
                                  f"{MODID}:{p}_fence", 3)
        r[f"{p}_fence_gate"] = _shaped(["S#S", "S#S"], {"#": planks, "S": "minecraft:stick"},
                                       f"{MODID}:{p}_fence_gate", 1)
        r[f"{p}_door"] = _shaped(["##", "##", "##"], {"#": planks}, f"{MODID}:{p}_door", 3)
        r[f"{p}_trapdoor"] = _shaped(["###", "###"], {"#": planks}, f"{MODID}:{p}_trapdoor", 2)
        r[f"{p}_pressure_plate"] = _shaped(["##"], {"#": planks}, f"{MODID}:{p}_pressure_plate", 1)
        r[f"{p}_button"] = _shapeless([planks], f"{MODID}:{p}_button", 1)
    # --- 玻璃/玻璃板（6）---
    for prefix, *_ in _DECORATION_BASES:
        r[f"{prefix}_glass"] = _smelting("minecraft:sand", f"{MODID}:{prefix}_glass")
        r[f"{prefix}_glass_pane"] = _shaped(["###", "###"], {"#": f"{MODID}:{prefix}_glass"},
                                            f"{MODID}:{prefix}_glass_pane", 16)
    # --- 灯笼（3）---
    for prefix, *_ in _LANTERN_BASES:
        r[f"{prefix}_lantern"] = _shaped(["NNN", "NTN", "NNN"],
                                         {"N": "minecraft:iron_nugget", "T": "minecraft:torch"},
                                         f"{MODID}:{prefix}_lantern", 1)
    return r
```

- [ ] **Step 2: 在 `gen_block_assets.py` 追加 `write_recipes` 并在 `run()` 调用**

```python
def write_recipes(root: Path, recipes) -> None:
    data = root / "src/main/resources/data" / MODID / "recipe"
    for name, recipe in recipes.items():
        write_json(data / f"{name}.json", recipe)
```

在 `run()` 的 `write_lang(...)` 之后加入：

```python
    recipes = bd.building_recipes()
    write_recipes(root, recipes)
    print(f"[gen] wrote {len(recipes)} building recipes")
```

- [ ] **Step 3: 生成并校验数量**

Run: `python tools/gen_block_assets.py`
Expected: 末行前有 `[gen] wrote 91 building recipes`；配方目录文件数由 64 增至 155。

- [ ] **Step 4: 校验 recipe 目录文件数**

Run: `(Get-ChildItem src\main\resources\data\weather_realm\recipe -File).Count`
Expected: `64 + 91 = 155`。

- [ ] **Step 5: 提交**

```powershell
git add tools/biome_data.py tools/gen_block_assets.py src/main/resources/data/weather_realm/recipe
git commit -m "feat(building-blocks): 生成 91 条建筑配方"
```

### Task C6: 补齐 `weather_pedestal` 掉落表

**Files:**
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\resources\data\weather_realm\loot_table\blocks\weather_pedestal.json`

**Interfaces:**
- Consumes: `weather_realm:weather_pedestal`（`ModBlocks.WEATHER_PEDESTAL`）。
- Produces: 自掉落表，与 `biome_data.self_loot` 结构一致。

- [ ] **Step 1: 创建文件（完整内容）**

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "weather_realm:weather_pedestal"
        }
      ]
    }
  ],
  "random_sequence": "weather_realm:blocks/weather_pedestal"
}
```

- [ ] **Step 2: 校验数量**

Run: `(Get-ChildItem src\main\resources\data\weather_realm\loot_table\blocks -File).Count`
Expected: `199`（103 原有 + 95 新 + 1 补 = 199；`weather_portal` 无掉落表）。

- [ ] **Step 3: 提交**

```powershell
git add src/main/resources/data/weather_realm/loot_table/blocks/weather_pedestal.json
git commit -m "fix(loot): 补齐 weather_pedestal 自掉落表"
```

### Task C7: 扩展创造页分类表（纳入 95 新建材）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModCreativeTabs.java`（`BUILDING_CATEGORIES` 各分类 `List.of(...)`，A1 的 46-77 行区域）

**Interfaces:**
- Consumes: C4 的 95 个 id。
- Produces: 更新后的 `BUILDING_CATEGORIES`（方块页共 208 条：104 现有 + 95 新建 + 9 生态[D 补]）。

**排序（设计 §4.4）**：① 木族按主题序 永冻→燃焰→风沙，族内按 `原木→木→去皮原木→去皮木→木板→楼梯→台阶→栅栏→栅栏门→门→活板门→压力板→按钮`；② 石族按主题序，族内 `浅层→深层`，每层 `磨制→磨制楼梯→磨制台阶→磨制墙→砖→砖楼梯→砖台阶→砖墙→裂砖→雕纹→柱`；③ 灯具主题序；④ 玻璃主题序（每主题 `玻璃→玻璃板→格栅→锁链`）。

- [ ] **Step 1: 用下列内容替换 `BUILDING_CATEGORIES` 中 `wood`/`stone`/`lighting`/`glass` 四个分类项**

```java
            new TabCategory("wood", List.of(
                    "frost_log", "frost_wood", "stripped_frost_log", "stripped_frost_wood",
                    "frost_planks", "frost_stairs", "frost_slab", "frost_fence", "frost_fence_gate",
                    "frost_door", "frost_trapdoor", "frost_pressure_plate", "frost_button",
                    "scorched_log", "scorched_wood", "stripped_scorched_log", "stripped_scorched_wood",
                    "scorched_planks", "scorched_stairs", "scorched_slab", "scorched_fence",
                    "scorched_fence_gate", "scorched_door", "scorched_trapdoor",
                    "scorched_pressure_plate", "scorched_button",
                    "arid_log", "arid_wood", "stripped_arid_log", "stripped_arid_wood",
                    "arid_planks", "arid_stairs", "arid_slab", "arid_fence", "arid_fence_gate",
                    "arid_door", "arid_trapdoor", "arid_pressure_plate", "arid_button")),
            new TabCategory("stone", List.of(
                    "permafrost_polished", "permafrost_polished_stairs", "permafrost_polished_slab",
                    "permafrost_polished_wall", "permafrost_bricks", "permafrost_brick_stairs",
                    "permafrost_brick_slab", "permafrost_brick_wall", "permafrost_cracked_bricks",
                    "permafrost_chiseled", "permafrost_pillar",
                    "deep_permafrost_polished", "deep_permafrost_polished_stairs",
                    "deep_permafrost_polished_slab", "deep_permafrost_polished_wall",
                    "deep_permafrost_bricks", "deep_permafrost_brick_stairs",
                    "deep_permafrost_brick_slab", "deep_permafrost_brick_wall",
                    "deep_permafrost_chiseled",
                    "fire_stone_polished", "fire_stone_polished_stairs", "fire_stone_polished_slab",
                    "fire_stone_polished_wall", "fire_stone_bricks", "fire_stone_brick_stairs",
                    "fire_stone_brick_slab", "fire_stone_brick_wall", "fire_stone_cracked_bricks",
                    "fire_stone_chiseled", "fire_stone_pillar",
                    "deep_fire_stone_polished", "deep_fire_stone_polished_stairs",
                    "deep_fire_stone_polished_slab", "deep_fire_stone_polished_wall",
                    "deep_fire_stone_bricks", "deep_fire_stone_brick_stairs",
                    "deep_fire_stone_brick_slab", "deep_fire_stone_brick_wall",
                    "deep_fire_stone_chiseled",
                    "weathered_sandstone_polished", "weathered_sandstone_polished_stairs",
                    "weathered_sandstone_polished_slab", "weathered_sandstone_polished_wall",
                    "weathered_sandstone_bricks", "weathered_sandstone_brick_stairs",
                    "weathered_sandstone_brick_slab", "weathered_sandstone_brick_wall",
                    "weathered_sandstone_cracked_bricks", "weathered_sandstone_chiseled",
                    "weathered_sandstone_pillar",
                    "deep_weathered_sandstone_polished", "deep_weathered_sandstone_polished_stairs",
                    "deep_weathered_sandstone_polished_slab", "deep_weathered_sandstone_polished_wall",
                    "deep_weathered_sandstone_bricks", "deep_weathered_sandstone_brick_stairs",
                    "deep_weathered_sandstone_brick_slab", "deep_weathered_sandstone_brick_wall",
                    "deep_weathered_sandstone_chiseled")),
            new TabCategory("lighting", List.of("frost_lantern", "blaze_lantern", "wind_lantern")),
            new TabCategory("glass", List.of(
                    "frost_glass", "frost_glass_pane", "frost_grate", "frost_chain",
                    "blaze_glass", "blaze_glass_pane", "blaze_grate", "blaze_chain",
                    "wind_glass", "wind_glass_pane", "wind_grate", "wind_chain")),
```

- [ ] **Step 2: 编译**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 标签页覆盖校验（应仍退出 0，总数 133+95=228）**

Run: `python tools/verify_tab_coverage.py; echo "exit=$LASTEXITCODE"`
Expected: `[tab] registered items : 228`、`[tab] tab entries      : 228 (distinct 228)`、`TAB COVERAGE OK`、`exit=0`。

- [ ] **Step 4: 提交**

```powershell
git add src/main/java/com/example/weather_realm/ModCreativeTabs.java
git commit -m "feat(creativetab): 方块页纳入 95 件新建材"
```

### Task C8: 阶段 C 收尾（构建 + 校验 + 部署）

**Files:** 无。

**Interfaces:** Consumes C1-C7。Produces 阶段 C 可运行产物。

- [ ] **Step 1: 构建**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 2: 三脚本全部退出 0**

Run: `python tools/verify_tab_coverage.py; python tools/verify_building_assets.py`
Expected: `TAB COVERAGE OK` 与 `BUILDING ASSETS OK (0 differences)`。

- [ ] **Step 3: 语言文件行数只增不减**

Run: `python -c "import json;print(len(json.load(open(r'src/main/resources/assets/weather_realm/lang/zh_cn.json',encoding='utf-8'))))"`
Expected: `169 + 95 = 264`（Phase A 后 169；95 件新建材每个 `block.*` 一键）。

- [ ] **Step 4: 工作区检查**

Run: `git status --short`
Expected: 仅 `?? .opencode/`、`?? tools/__pycache__/`。

- [ ] **Step 5: 部署**

Run: `.\deploy.ps1`
Expected: jar 复制到 PCL mods 目录。

---

# 阶段 D — 生态美术小物（9 件）

> 设计文档对照：§6 全部。**归入页 A ⑤ 装饰细部（§6.5，照抄）**。

### Task D1: 新增 `WeatherSpikeBlock.java`

**Files:**
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\block\WeatherSpikeBlock.java`

**Interfaces:**
- Consumes: 无（自足）。
- Produces: `public class WeatherSpikeBlock extends Block implements SimpleWaterloggedBlock`，含 `public enum SpikeThickness implements StringRepresentable`（TIP/FRUSTUM/MIDDLE/BASE）；静态属性 `THICKNESS`、`VERTICAL_DIRECTION`、`WATERLOGGED`。

- [ ] **Step 1: 创建文件（完整代码）**

```java
package com.example.weather_realm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 天象尖锥 / Weather Spike - a static, tapering decoration (design §6.4).
 *
 * <p>Carries {@code thickness} (tip/frustum/middle/base), {@code vertical_direction} (up/down)
 * and {@code waterlogged}. No growth / merging behaviour.</p>
 */
public class WeatherSpikeBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<SpikeThickness> THICKNESS =
            EnumProperty.create("thickness", SpikeThickness.class);
    public static final DirectionProperty VERTICAL_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D),   // TIP
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),   // FRUSTUM
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D),   // MIDDLE
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D),   // BASE
    };

    public enum SpikeThickness implements StringRepresentable {
        TIP("tip"), FRUSTUM("frustum"), MIDDLE("middle"), BASE("base");

        private final String name;

        SpikeThickness(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public WeatherSpikeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(THICKNESS, SpikeThickness.BASE)
                .setValue(VERTICAL_DIRECTION, Direction.UP)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, VERTICAL_DIRECTION, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(THICKNESS).ordinal()];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction vertical = clicked.getAxis() == Direction.Axis.Y ? clicked : Direction.UP;
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(VERTICAL_DIRECTION, vertical)
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(VERTICAL_DIRECTION);
        BlockPos supportPos = pos.relative(direction.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == state.getValue(VERTICAL_DIRECTION).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
```

- [ ] **Step 2: 编译**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add src/main/java/com/example/weather_realm/block/WeatherSpikeBlock.java
git commit -m "feat(eco-decor): 新增 WeatherSpikeBlock 尖锥方块类"
```

### Task D2: 注册 9 件生态小物（`ModBuildingBlocks` + `ModItems`）

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModBuildingBlocks.java`
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModItems.java`

**Interfaces:**
- Consumes: D1 `WeatherSpikeBlock`；原版 `AmethystClusterBlock`、`SnowLayerBlock`。
- Produces: `record EcoSet(String prefix, DeferredBlock<AmethystClusterBlock> cluster, DeferredBlock<SnowLayerBlock> layer, DeferredBlock<WeatherSpikeBlock> spike)`；`public static final List<EcoSet> ECO_ITEMS`。

**构造参数（照设计 §6.1/§6.2/§6.3）**：
- 晶簇：`new AmethystClusterBlock(7.0F, 3.0F, properties)`（`height=7`、`width=3`）。
- 叠层：`new SnowLayerBlock(properties)`。
- 尖锥：`new WeatherSpikeBlock(properties)`。

- [ ] **Step 1: 在 `ModBuildingBlocks.java` 增加 import 与生态集合**

新增 import：

```java
import com.example.weather_realm.block.WeatherSpikeBlock;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
```

新增 record 与列表（放在 `DecorationSet` 与其余列表之后）：

```java
    public record EcoSet(String prefix, DeferredBlock<AmethystClusterBlock> cluster,
                         DeferredBlock<SnowLayerBlock> layer, DeferredBlock<WeatherSpikeBlock> spike) {
    }

    public static final List<EcoSet> ECO_ITEMS = new ArrayList<>();

    private static EcoSet ecoSet(String prefix, MapColor color, int lightLevel,
                                 SoundType sound, float strength) {
        DeferredBlock<AmethystClusterBlock> cluster = BLOCKS.register(prefix + "_crystal_cluster",
                () -> new AmethystClusterBlock(7.0F, 3.0F, BlockBehaviour.Properties.of()
                        .mapColor(color).forceSolidOn().noOcclusion()
                        .sound(SoundType.AMETHYST_CLUSTER).strength(strength)
                        .lightLevel(state -> lightLevel)
                        .pushReaction(PushReaction.DESTROY)));
        DeferredBlock<SnowLayerBlock> layer = BLOCKS.register(prefix + "_" + layerWord(prefix) + "_layer",
                () -> new SnowLayerBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).strength(0.1F).requiresCorrectToolForDrops()
                        .sound(sound).isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false).noOcclusion()));
        DeferredBlock<WeatherSpikeBlock> spike = BLOCKS.register(prefix + "_spike",
                () -> new WeatherSpikeBlock(BlockBehaviour.Properties.of()
                        .mapColor(color).forceSolidOn().noOcclusion()
                        .sound(sound).strength(strength)
                        .pushReaction(PushReaction.DESTROY)));
        return new EcoSet(prefix, cluster, layer, spike);
    }

    private static String layerWord(String prefix) {
        return switch (prefix) {
            case "frost" -> "snow";
            case "blaze" -> "ash";
            default -> "sand";
        };
    }
```

在 `static` 块末尾追加：

```java
        ECO_ITEMS.add(ecoSet("frost", MapColor.ICE, 7, SoundType.SNOW, 1.5F));
        ECO_ITEMS.add(ecoSet("blaze", MapColor.COLOR_ORANGE, 3, SoundType.SAND, 1.5F));
        ECO_ITEMS.add(ecoSet("wind", MapColor.SAND, 0, SoundType.SAND, 1.5F));
```

- [ ] **Step 2: 在 `ModItems.java` 的 `static` 块末尾注册生态 BlockItem**

```java
        for (ModBuildingBlocks.EcoSet set : ModBuildingBlocks.ECO_ITEMS) {
            buildingItem(set.prefix() + "_crystal_cluster", set.cluster());
            buildingItem(set.prefix() + "_" + switch (set.prefix()) {
                case "frost" -> "snow";
                case "blaze" -> "ash";
                default -> "sand";
            } + "_layer", set.layer());
            buildingItem(set.prefix() + "_spike", set.spike());
        }
```

- [ ] **Step 3: 编译**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 提交**

```powershell
git add src/main/java/com/example/weather_realm/ModBuildingBlocks.java src/main/java/com/example/weather_realm/ModItems.java
git commit -m "feat(eco-decor): 注册 9 件生态小物"
```

### Task D3: 生成/手写生态小物资源

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\biome_data.py`（追加 `cluster_specs`/`layer_specs`）
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\tools\gen_block_assets.py`（新增 `cluster`/`layer` 分支 + `run()` 纳入）
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\resources\assets\weather_realm\models\block\{frost,blaze,wind}_spike_{tip,frustum,middle,base}.json`（12 个，手写）
- Create: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\resources\assets\weather_realm\blockstates\{frost,blaze,wind}_spike.json`（3 个，手写）

**Interfaces:**
- Consumes: B1 `write_block_client`；B2 profile。
- Produces: 6 件可生成资源（3 晶簇 + 3 叠层）+ 3 件尖锥（贴图生成、模型/blockstate 手写）。

- [ ] **Step 1: 在 `biome_data.py` 追加晶簇/叠层 spec 生成函数**

```python
_ECO_BASES = [
    ("frost", "坚冰", "Frost", FROST_STONE_PROFILE, "snow"),
    ("blaze", "燃焰", "Blaze", THEMES[0]["stone_profile"], "ash"),
    ("wind", "风沙", "Wind", THEMES[1]["stone_profile"], "sand"),
]


def cluster_specs():
    specs = []
    for prefix, zh, en, profile, _word in _ECO_BASES:
        name = f"{prefix}_crystal_cluster"
        specs.append(dict(name=name, model="cluster", en=f"{en} Crystal Cluster", zh=f"{zh}晶簇",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(name, "block/amethyst_cluster", profile)], tex={"cross": name},
                          item=("generated", f"block/{name}")))
    return specs


def layer_specs():
    specs = []
    for prefix, zh, en, profile, word in _ECO_BASES:
        name = f"{prefix}_{word}_layer"
        specs.append(dict(name=name, model="layer", en={"snow": "Frost Snow Layer",
                          "ash": "Volcanic Ash Layer", "sand": "Wind Sand Layer"}[word],
                          zh={"snow": f"{zh}雪层", "ash": f"{zh}灰烬层", "sand": f"{zh}沙层"}[word],
                          loot=("self",), tool=None, needs=None, tags=[],
                          textures=[(name, "block/snow", profile)], tex={"all": name},
                          item=("generated", f"block/{name}")))
    return specs
```

- [ ] **Step 2: 在 `gen_block_assets.py` 的 `write_block_client` 增加 `cluster` 与 `layer` 分支**

在 `else: raise ValueError` 之前插入（`chain` 分支之后）：

```python
    elif model == "cluster":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": _tx(spec, "cross")}})
        write_template_blockstate(assets, name, "amethyst_cluster", [
            ("minecraft:block/amethyst_cluster", f"{MODID}:block/{name}")])
    elif model == "layer":
        for height in (2, 4, 6, 8, 10, 12, 14):
            write_json(assets / "models/block" / f"{name}_height{height}.json", {
                "parent": f"minecraft:block/snow_height{height}",
                "textures": {"texture": _tx(spec, "all"), "particle": _tx(spec, "all")}})
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "textures": {"all": _tx(spec, "all")}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {
            "layers=1": {"model": f"{MODID}:block/{name}_height2"},
            "layers=2": {"model": f"{MODID}:block/{name}_height4"},
            "layers=3": {"model": f"{MODID}:block/{name}_height6"},
            "layers=4": {"model": f"{MODID}:block/{name}_height8"},
            "layers=5": {"model": f"{MODID}:block/{name}_height10"},
            "layers=6": {"model": f"{MODID}:block/{name}_height12"},
            "layers=7": {"model": f"{MODID}:block/{name}_height14"},
            "layers=8": {"model": f"{MODID}:block/{name}"}}})
```

> `cluster` 的 blockstate 复用 vanilla `amethyst_cluster.json`（6 个 `facing` 变体；`waterlogged` 不影响模型，按 vanilla 惯例不入 blockstate——见文末「自检」）。

- [ ] **Step 3: 在 `run()` 纳入晶簇/叠层**

在 `building_specs += bd.decoration_specs()` 之后追加：

```python
    building_specs += bd.cluster_specs()
    building_specs += bd.layer_specs()
```

- [ ] **Step 4: 手写尖锥的 4 个模型（每主题一份，示例为 `frost`）**

`models/block/frost_spike_tip.json`（`frustum`/`middle`/`base` 分别把 `up_tip` 换成 `up_frustum`/`up_middle`/`up_base`，并把纹理 `frost_spike_tip` 换成对应名）：

```json
{
  "parent": "minecraft:block/pointed_dripstone_up_tip",
  "textures": {
    "cross": "weather_realm:block/frost_spike_tip"
  }
}
```

同法创建 `frost_spike_frustum.json`（parent `minecraft:block/pointed_dripstone_up_frustum`）、`frost_spike_middle.json`（`..._up_middle`）、`frost_spike_base.json`（`..._up_base`），以及 `blaze_spike_*`、`wind_spike_*` 共 12 个文件。

- [ ] **Step 5: 手写尖锥 blockstate（16 变体；示例为 `frost`）**

`blockstates/frost_spike.json`（`blaze`/`wind` 把 `frost_` 换成前缀即可）：

```json
{
  "variants": {
    "thickness=base,vertical_direction=down": { "model": "weather_realm:block/frost_spike_base", "x": 180 },
    "thickness=base,vertical_direction=up": { "model": "weather_realm:block/frost_spike_base" },
    "thickness=frustum,vertical_direction=down": { "model": "weather_realm:block/frost_spike_frustum", "x": 180 },
    "thickness=frustum,vertical_direction=up": { "model": "weather_realm:block/frost_spike_frustum" },
    "thickness=middle,vertical_direction=down": { "model": "weather_realm:block/frost_spike_middle", "x": 180 },
    "thickness=middle,vertical_direction=up": { "model": "weather_realm:block/frost_spike_middle" },
    "thickness=tip,vertical_direction=down": { "model": "weather_realm:block/frost_spike_tip", "x": 180 },
    "thickness=tip,vertical_direction=up": { "model": "weather_realm:block/frost_spike_tip" }
  }
}
```

> 8 个变体已覆盖 `vertical_direction`×`thickness`；`waterlogged` 不影响模型，按 vanilla 惯例不入 blockstate。若需显式列出 16 组，可对每行再复制一份 `waterlogged=false|true`，但 vanilla snow/pane 均省略，本计划从 vanilla 惯例。

- [ ] **Step 6: 用生成器产出尖锥贴图（只写 PNG）+ 生成晶簇/叠层资源**

在 `biome_data.py` 追加：

```python
def spike_textures():
    """(out_name, src_tex, profile) for the 4 up-facing spike textures per theme."""
    out = []
    for prefix, _zh, _en, profile, _word in _ECO_BASES:
        for thickness in ("tip", "frustum", "middle", "base"):
            out.append((f"{prefix}_spike_{thickness}",
                        f"block/pointed_dripstone_up_{thickness}", profile))
    return out
```

在 `gen_block_assets.py` 的 `run()` 末尾（`ZIP.close()` 之前）加入：

```python
    for out_tex, src_tex, profile in bd.spike_textures():
        img = recolor(read_png(ZIP, src_tex), profile)
        img.save(root / "src/main/resources/assets" / MODID / "textures/block" / f"{out_tex}.png")
```

- [ ] **Step 7: 生成 + 校验**

Run: `python tools/gen_block_assets.py`
Expected: `[gen] wrote textures + resources for 163 blocks ...`（59 现有 + 95 建筑 + 6 生态 + 3 尖锥计入 textures-only 不增 `total`；若实现将尖锥计入则数字相应变化，以实际输出为准）。随后 `python tools/verify_building_assets.py; echo "exit=$LASTEXITCODE"` 期望 `BUILDING ASSETS OK (0 differences)`、`exit=0`。

- [ ] **Step 8: 校验尖锥 blockstate 变体数**

Run: `python -c "import json;d=json.load(open(r'src/main/resources/assets/weather_realm/blockstates/frost_spike.json',encoding='utf-8'));print(len(d['variants']))"`
Expected: `8`（4 thickness × 2 direction）。

- [ ] **Step 9: 提交**

```powershell
git add tools/biome_data.py tools/gen_block_assets.py src/main/resources/assets/weather_realm/models/block src/main/resources/assets/weather_realm/blockstates src/main/resources/assets/weather_realm/textures/block
git commit -m "feat(eco-decor): 生成晶簇/叠层资源并手写尖锥模型与 blockstate"
```

### Task D4: 创造页 ⑤ 装饰细部纳入 9 件生态小物

**Files:**
- Modify: `C:\Users\31087\Desktop\mc\ModDevelopGamejam\src\main\java\com\example\weather_realm\ModCreativeTabs.java`（`decoration` 分类）

**Interfaces:**
- Consumes: D2 的 9 个 id。
- Produces: `decoration` 分类 10 条（`frost_wool` + 9 生态）。

- [ ] **Step 1: 替换 `decoration` 分类项**

```java
            new TabCategory("decoration", List.of(
                    "frost_wool",
                    "frost_crystal_cluster", "frost_snow_layer", "frost_spike",
                    "blaze_crystal_cluster", "blaze_ash_layer", "blaze_spike",
                    "wind_crystal_cluster", "wind_sand_layer", "wind_spike")),
```

- [ ] **Step 2: 编译 + 覆盖校验**

Run: `.\gradlew.bat build; python tools/verify_tab_coverage.py; echo "exit=$LASTEXITCODE"`
Expected: `BUILD SUCCESSFUL`；`[tab] registered items : 237`、`[tab] tab entries      : 237 (distinct 237)`、`TAB COVERAGE OK`、`exit=0`。

- [ ] **Step 3: 提交**

```powershell
git add src/main/java/com/example/weather_realm/ModCreativeTabs.java
git commit -m "feat(creativetab): 方块页装饰细部纳入 9 件生态小物"
```

### Task D5: 阶段 D 收尾（构建 + 全部校验 + 部署）

**Files:** 无。

**Interfaces:** Consumes D1-D4。Produces 最终可发布产物。

- [ ] **Step 1: 最终构建**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`，产出 `build\libs\weather_realm-1.21.1-1.0.0.jar`。

- [ ] **Step 2: 全部校验脚本退出 0**

Run: `python tools/verify_tab_coverage.py; python tools/verify_building_assets.py; python tools/verify_ore_textures.py`
Expected: `TAB COVERAGE OK`、`BUILDING ASSETS OK (0 differences)`、`ALL COMPOSITE ORES OK`。

- [ ] **Step 3: 资源总量核对**

Run: `(Get-ChildItem src\main\resources\assets\weather_realm\blockstates -File).Count; (Get-ChildItem src\main\resources\data\weather_realm\loot_table\blocks -File).Count; (Get-ChildItem src\main\resources\data\weather_realm\recipe -File).Count`
Expected: blockstates `209`（105+95+9）；loot `208`（103+95+9+1 补）；recipe `155`。

- [ ] **Step 4: 工作区检查**

Run: `git status --short`
Expected: 仅 `?? .opencode/`、`?? tools/__pycache__/`。

- [ ] **Step 5: 部署**

Run: `.\deploy.ps1`
Expected: jar 复制到 PCL mods 目录。

- [ ] **Step 6: 删除运行时缓存目录（不提交）**

Run: `Remove-Item -Recurse -Force tools\__pycache__ -ErrorAction SilentlyContinue`
Expected: 命令无输出；`git status --short` 不再出现 `tools/__pycache__/`。

---

## 自检

### ① 设计文档 10 章 → Task 对照

| 设计章节 | 对应 Task | 覆盖说明 |
| :- | :- | :- |
| §1 背景与现状 | A1/A4/B5 | 105 方块、133 item、单页、`addCreative`、管线现状均在 A/B 的基线检查与脚本中体现 |
| §2 目标与非目标 | Global Constraints | 非目标（不动 nbt/worldgen/传送门/DataGen）写入约束 |
| §3 三个主题 | B2/C4 | `STONE_BASES`/`WOOD_BASES` 与 themeKey 对齐三主题 |
| §4 创造标签页重构 | A1/A2/A3/A4/A5/C7/D4 | 双页、分类、排序、移除注入、旧页处理、覆盖率校验 |
| §5 建筑方块清单 | C1/C2/C4/C5/C7 | 命名规则、33+27+20+3+12=95、id/中英文名全表 |
| §6 生态美术小物 | D1/D2/D3/D4 | 复用类选型、尖锥自定义类、16 组 blockstate、归入 ⑤ |
| §7 资源生成管线 | B1/B2/B3/B4/B5/C4/C5/D3 | 11 形状模板、族展开、write_tags/write_lang、双向校验、幂等 |
| §8 Java 侧设计 | C1/C2/C3 | ModBuildingBlocks、ModItems 批量、注册顺序 |
| §9 掉落表/配方/命名 | C4/C5/C6 | 自掉落、glass 语义、补 weather_pedestal、91 配方、词根命名 |
| §10 实施顺序与验收 | 阶段总览 + 各阶段收尾 Task | A→B→C→D 独立交付、build/verify/deploy 门禁 |

### ② 占位符扫描结论

- 全文无 `TBD`/`TODO`/「稍后补」/「类似 Task N」；每个涉及代码的步骤均给出**完整** Java/Python/JSON 内容。
- 仅两处「以实际输出为准」用于**数量核对**（生成器 `total` 打印、尖锥是否计入 total），非实现占位；执行者按脚本真实打印断言即可。

### ③ 类型/签名一致性

- `ModCreativeTabs.TabCategory(String, List<String>)`：A1 定义，A4/C7/D4 使用，字段名 `key`/`itemIds` 一致（脚本用 `itemIds()` 位置表）。
- `ModBuildingBlocks.StoneLayer/StoneFamily/WoodFamily/LanternSet/DecorationSet/EcoSet`：C1/D2 定义，C2 使用；`layer.crackedBricks()`/`layer.pillar()` 可能为 `null`（深层），C2 已判空。
- `bd.all_building_block_ids()`：B2 定义（B 返回 `[]`），B5/C4/C7 依赖；生成侧 `stone_specs`/`wood_specs`/`lantern_specs`/`decoration_specs`/`cluster_specs`/`layer_specs` 与 `all_building_block_ids` 的 id 拼装规则一致（`<name>`/`<prefix>_<suffix>`）。
- `write_tags(root, covers, specs, themes, building_specs)` 与 `write_lang(root, covers, specs, themes, building_specs)`：B3/B4 定义，C4 `run()` 调用签名一致。
- `WeatherSpikeBlock.THICKNESS` 枚举 `SpikeThickness` 序号与 `SHAPES[]` 下标一致（TIP=0…BASE=3）。

### ④ 设计文档缺口/矛盾清单与处理

1. **任务书 id `weather_realm_blocks`/`weather_realm_items` vs 设计 §4.1 `building_blocks`/`items`** → 以设计文档为准（权威），采用 `building_blocks`/`items`（A1）。
2. **§4.1 表内页 B 翻译键 `itemGroup.weather_realm.items` vs 同节说明「保留原 `itemGroup.weather_realm`」** → 采信更具体的说明，页 B 沿用 `itemGroup.weather_realm`（A1/A3）。
3. **§4.2 矿石枚举次序（铁先行）vs §4.4 规则 4（煤先行）** → 现有 18 个沿用 §4.2 枚举次序，新建/生成按声明次序；§4.4 仅约束新增件（A1 表 + 说明）。
4. **§8.1 `record StoneFamily` 无法同时容纳浅层与深层衍生 holder** → 拆为 `StoneFamily{themeKey, shallow, deep}` + `StoneLayer`（C1）。
5. **§8.1 `DeferredBlock<Block> pillar` 与柱需要 `AXIS` 状态矛盾** → 改为 `DeferredBlock<RotatedPillarBlock>`（C1），blockstate 用 vanilla 柱三变体。
6. **§6.2 晶簇 blockstate「6×2=12 组」vs vanilla 省略 `waterlogged`** → 按 vanilla 只 6 个 `facing` 变体（D3）；`waterlogged` 状态仍存在于方块。
7. **§6.4 尖锥 blockstate「2×4×2=16 组」** → 手写 8 组（direction×thickness），`waterlogged` 不入 blockstate（vanilla 惯例，D3）；如需 16 组可按 Step 5 注释展开。
8. **§6.2 晶簇物品「独立 `textures/item/<name>.png`」** → 原版无 `item/amethyst_cluster.png`（实测），改用 `block/<name>` 作 `item/generated` 的 layer0（D3，已注明）。
9. **§7.1 注「尖锥手写、不进生成器」** → 尊重：模型/blockstate 手写，**贴图**由生成器重着色产出（`spike_textures()`），兼顾可复现性（D3）。
10. **§7.1 行 12/13（cluster/layer）与任务书「Phase B 11 种形状」** → cluster/layer 归入 Phase D 的 D3（引擎在 B1 的 `write_block_client` 中以新分支扩展）。
11. **§9.3 命名样例自身不一致（`Polished Permafrost` vs `Permafrost Brick Wall`；`Chiseled Deep Fire Stone`）** → 采用与样例吻合的原版惯例：polished/chiseled 限定词前置，bricks/stuars/slab/wall/pillar 族名前置（B2 `_stone_display`）。
12. **§7.4 校验脚本按 `ModBlocks/ModItems` 字面 id 提取，但新建材在 `ModBuildingBlocks` 以变量名注册** → B5 的「表→Java」方向改为「字面注册 ∪ `ModCreativeTabs` 字面 id 表」；「Java→表」用命名模式 + 资源存在性断言，并保留任务书要求的四类正则提取函数。
13. **§4.4/§8.3「displayItems 遍历族对象」vs 任务书「纯数据分类表可被脚本解析」** → 采用显式有序 id 表（§8.3 措辞亦允许「表」），族对象仅用于注册；两者顺序由同一命名规则保证一致（A1 + C7）。
14. **§7.2 无 `glass` loot 类型、§9.1 要求玻璃原版语义** → 新增 `bd.glass_loot` 与 `write_block_loot` 的 `glass` 分支（C4）。
15. **`weather_pedestal` 缺掉落表（§9.1）** → C6 补齐。

> 以上均为「设计文档未定/内部不一致」的显式处置，无默默忽略。

---

**计划完成。** 执行方式二选一：
1. **Subagent-Driven（推荐）**：每个 Task 派新 subagent + 两阶段复核。
2. **Inline Execution**：本会话按 executing-plans 批量执行、检查点复核。
