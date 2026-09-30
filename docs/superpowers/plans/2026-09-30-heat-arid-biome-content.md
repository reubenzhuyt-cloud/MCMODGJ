# 天象之境：炎热与风沙群系内容 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为天象之境的自建维度 `crystal_realm` 补齐两套群系内容——炎热群系 `blazing_plains`（火石 / 焦火木 / 炎火晶石）与风沙群系 `arid_wasteland`（风化砂石 / 风化木 / 风沙晶石），并把这些方块注入地表规则、矿产与植被地物，同时调低全部群系的树木生成概率（极寒降频，炎热 / 风沙更低更稀疏）。

**Architecture:** 三层落地。**Java 层**只在 `WeatherRealm` 里用 `DeferredRegister` 一次性声明新方块 / 物品，并新增 4 个极薄的方块类（两种群系植被基类 + 两个可催熟树苗）与 1 个共享的 `ModTreeBuilder` 树形工具；**地表层**改 `ModSurfaceRules` 的 `blazing_plains` / `arid_wasteland` 两条 `SurfaceRules` 分支；**资源 / 数据层**用一个 Python 脚本从原版 1.21.1 客户端 jar 里**换色**生成 60 个方块的 16×16 贴图、blockstates、block/item 模型、loot_table、tags、中英文 lang，再用第二个脚本生成树木 / 植物 / 矿石的 `configured_feature` 与 `placed_feature`。管线数据集中在 `tools/biome_data.py`，两个脚本共享，避免手写数百个 JSON。

**Tech Stack:** Minecraft 1.21.1 · NeoForge 21.1.252 · JDK 21 · ModDevGradle 2.0.147 · Parchment 2024.11.17 · Brigadier/DeferredRegister · Python 3.11 + Pillow 11（仅开发期离线生成资源，不进 jar）· PowerShell（`deploy.ps1`）。

## Global Constraints

以下为项目级硬约束，**每个 Task 都隐含遵守**（摘自仓库根 `AGENTS.md`）：

- Minecraft **1.21.1**；加载器 **NeoForge 21.1.252**（`neo_version` 写作 `21.1.252`，**不得**写成 `1.21.1-21.1.252`）。
- JDK **21**；Gradle 由 `.\gradlew.bat` wrapper 管理；**Windows 命令一律以 `.\gradlew.bat` 为前缀**。
- 构建插件 **ModDevGradle 2.0.147**（`net.neoforged.moddev`）；映射 **Parchment 2024.11.17**。
- 命名空间固定为 `weather_realm`，Mod ID 常量 `WeatherRealm.MODID`。
- 注册一律用 `DeferredRegister.Blocks` / `.Items` 等快捷子类；返回类型是 `DeferredBlock<T>` / `DeferredItem<T>`（**不是** `RegistryObject`）。
- **Common 代码禁止引用 `net.minecraft.client.*`**；合并前必须能干净跑起 `.\gradlew.bat runServer`。
- **禁止** ItemStack NBT（`ItemStack.getTag()` 在 1.20.5+ 已移除）；需要数据用强类型 `DataComponentType<T>`。
- 资源路径：客户端 `src/main/resources/assets/weather_realm/`，服务端 `src/main/resources/data/weather_realm/`；**不要**写 `src/generated/resources/`（本工程为手写 datapack，无 DataGen Provider）。
- 新增方块 / 物品 / 事件监听属**启动期冻结项**：运行时新增会 `Registry is already frozen`，**必须重启客户端**；worldgen（地物 / 群系）改动需**退出到主菜单重进存档**，`/reload` 无效。
- 数据驱动优先：数值 / 生成 / 掉落 / 标签全部走 datapack JSON；本次内容全部为 JSON + 极薄 Java。
- 跳板模式：业务外置于普通类（本计划的 `ModTreeBuilder`），便于 JBR HotSwap。
- Python 脚本仅开发期使用，产物（PNG/JSON）提交进 Git；脚本放在 `tools/`，不影响打包。

---

## File Structure

| 文件 | 操作 | 职责 |
| :- | :- | :- |
| `src/main/java/com/example/weather_realm/WeatherRealm.java` | **Modify** | 注册火石 / 风化砂石地质、18 对矿石、2 晶石物品 + 2 晶石块、2 套木质家族、4 种植被；补创造标签 |
| `src/main/java/com/example/weather_realm/block/BlazePlantBlock.java` | **Create** | 炎热群系植被基类，只能种在火石 / 深层火石上 |
| `src/main/java/com/example/weather_realm/block/WeatheredPlantBlock.java` | **Create** | 风沙群系植被基类，只能种在风化砂石 / 深层风化砂石上 |
| `src/main/java/com/example/weather_realm/world/ModTreeBuilder.java` | **Create** | 共享的锥形树生成工具（普通类，可 HotSwap） |
| `src/main/java/com/example/weather_realm/block/ScorchwoodSaplingBlock.java` | **Create** | 焦火树苗，骨粉催熟为焦火木 |
| `src/main/java/com/example/weather_realm/block/WeatheredSaplingBlock.java` | **Create** | 风化树苗，骨粉催熟为风化木 |
| `src/main/java/com/example/weather_realm/world/ModSurfaceRules.java` | **Modify** | `blazing_plains` 地表 → 火石 / 深层火石；`arid_wasteland` 地表 → 风化砂石 / 深层风化砂石 |
| `tools/biome_data.py` | **Create** | 两个生成脚本共享的方块 / 矿石 / 配色数据表 |
| `tools/gen_block_assets.py` | **Create** | 换色生成贴图 + blockstates + 模型 + loot_table + tags + lang |
| `tools/gen_worldgen_features.py` | **Create** | 生成树 / 植物 / 矿石的 configured + placed feature JSON |
| `src/main/resources/data/weather_realm/worldgen/placed_feature/frost_tree.json` | **Modify** | `count: 3` → `count: 1`（极寒树木降频） |
| `src/main/resources/data/weather_realm/worldgen/biome/blazing_plains.json` | **Modify** | 注入炎热矿石 + 焦火木 + 火绒草 / 烈焰花 |
| `src/main/resources/data/weather_realm/worldgen/biome/arid_wasteland.json` | **Modify** | 注入风沙矿石 + 风化木 + 风沙草 / 砂兰 |

**生成产物（脚本自动写入，不手改）：**

| 目录 | 内容 |
| :- | :- |
| `assets/weather_realm/textures/block/*.png` | 60 张 16×16 换色方块贴图 |
| `assets/weather_realm/textures/item/{ember_crystal,sand_crystal}.png` | 2 张晶石物品贴图 |
| `assets/weather_realm/blockstates/*.json` | 60 个方块状态 |
| `assets/weather_realm/models/block/*.json` | 60 个方块模型 + 8 个 `_horizontal` 柱状模型 |
| `assets/weather_realm/models/item/*.json` | 62 个物品模型 |
| `assets/weather_realm/lang/{en_us,zh_cn}.json` | 幂等追加 62 条本地化键 |
| `data/weather_realm/loot_table/blocks/*.json` | 60 张战利品表 |
| `data/weather_realm/tags/block/*_ore_replaceables.json` | 4 个矿石替换标签 |
| `data/minecraft/tags/block/*.json` | 幂等合并进 `mineable/pickaxe`、`mineable/axe`、`logs`、`leaves`、`flowers`、`small_flowers`、`needs_*_tool` |
| `data/weather_realm/worldgen/configured_feature/*.json` | 24 张（树 2 + 植物 4 + 矿 18） |
| `data/weather_realm/worldgen/placed_feature/*.json` | 24 张 |

**设计说明（为什么自动门禁是编译 + JSON 校验 + 字节码核查）：** 新增的是**静态注册项**，GameTest 无法在无头环境验证「方块能否在世界里被挖到 / 贴图是否正确」。因此自动门禁 = `compileJava` 通过 + 所有生成 JSON 可解析 + 部署 jar 含新类与资源 + `runServer` 干净启动；外观与生成由 Task 5 的游戏内清单人工验收。换色贴图在 Task 3 用视觉抽查（拼图）确认。

---

## 参考：本计划引入的全部新 ID（命名空间 `weather_realm:`）

**地质 / 矿石（每群系 1+1 基材 + 8×2 原版矿 + 1×2 专属矿 + 1 晶石块）：**

| 群系 | 基材 | 深层基材 | 专属矿 → 掉落 | 晶石块 |
| :- | :- | :- | :- | :- |
| 炎热 | `blaze_stone`（火石） | `deep_blaze_stone`（深层火石） | `blaze_stone_ember_crystal_ore` / `deep_...` → `ember_crystal`（炎火晶石） | `ember_crystal_block` |
| 风沙 | `weathered_sandstone`（风化砂石） | `deep_weathered_sandstone`（深层风化砂石） | `weathered_sandstone_sand_crystal_ore` / `deep_...` → `sand_crystal`（风沙晶石） | `sand_crystal_block` |

原版矿石平替（8 种 × 浅/深）：`blaze_stone_{coal,copper,iron,gold,redstone,emerald,lapis,diamond}_ore` / `deep_blaze_stone_..._ore`；`weathered_sandstone_..._ore` / `deep_weathered_sandstone_..._ore`。

**木质家族（7 件 ×2）：** 焦火木 `scorchwood_{log,wood,planks,leaves,sapling}` + `stripped_scorchwood_{log,wood}`；风化木 `weathered_{log,wood,planks,leaves,sapling}` + `stripped_weathered_{log,wood}`。

**植被（4 种）：** `ember_grass`（火绒草）、`flame_flower`（烈焰花）、`desert_grass`（风沙草）、`desert_bloom`（砂兰）。

**矿石降级规则（与 `permafrost_*` 对齐）：** 铜 / 青金石 → `#minecraft:needs_stone_tool`；金 / 红石 / 绿宝石 / 钻石 → `#minecraft:needs_iron_tool`；专属晶石矿 → `#minecraft:needs_diamond_tool`。

**矿石地物 ID：** `ore_blaze_stone_{ore}` / `ore_weathered_sandstone_{ore}`，专属矿为 `ore_blaze_stone_ember_crystal` / `ore_weathered_sandstone_sand_crystal`。

**树 / 植物地物 ID：** `scorchwood_tree`、`weathered_tree`、`ember_grass`、`flame_flower`、`desert_grass`、`desert_bloom`。

---

## Task 1: 在 `WeatherRealm.java` 注册两套群系内容

**Files:**
- Create: `src/main/java/com/example/weather_realm/block/BlazePlantBlock.java`
- Create: `src/main/java/com/example/weather_realm/block/WeatheredPlantBlock.java`
- Create: `src/main/java/com/example/weather_realm/world/ModTreeBuilder.java`
- Create: `src/main/java/com/example/weather_realm/block/ScorchwoodSaplingBlock.java`
- Create: `src/main/java/com/example/weather_realm/block/WeatheredSaplingBlock.java`
- Modify: `src/main/java/com/example/weather_realm/WeatherRealm.java`

**Interfaces:**
- Consumes:
  - `WeatherRealm.BLOCKS`（`DeferredRegister.Blocks`）、`WeatherRealm.ITEMS`（`DeferredRegister.Items`）。
  - 既有类型 `com.example.weather_realm.block.FrostLogBlock`（`RotatedPillarBlock`）、`FrostLeavesBlock`（`LeavesBlock`）。
  - MC `BushBlock`、`BonemealableBlock`、`BlockBehaviour.Properties`、`MapColor`、`SoundType`、`NoteBlockInstrument`、`PushReaction`。
- Produces（后续 Task / 地表规则 / 地物依赖的**精确符号名**）：
  - 静态字段：`BLAZE_STONE`、`DEEP_BLAZE_STONE`、`WEATHERED_SANDSTONE`、`DEEP_WEATHERED_SANDSTONE`（均 `DeferredBlock<Block>`）。
  - `EMBER_CRYSTAL` / `SAND_CRYSTAL`（`DeferredItem<Item>`）；`EMBER_CRYSTAL_BLOCK` / `SAND_CRYSTAL_BLOCK`（`DeferredBlock<Block>`）。
  - 木质：`SCORCHWOOD_LOG/WOOD`、`STRIPPED_SCORCHWOOD_LOG/WOOD`、`SCORCHWOOD_PLANKS/LEAVES/SAPLING`；`WEATHERED_LOG/WOOD`、`STRIPPED_WEATHERED_LOG/WOOD`、`WEATHERED_PLANKS/LEAVES/SAPLING`。
  - 植被：`EMBER_GRASS`、`FLAME_FLOWER`、`DESERT_GRASS`、`DESERT_BLOOM`。
  - 生成的 18 对矿石由静态块注册，物品汇入 `List<DeferredItem<BlockItem>> GENERATED_ORE_ITEMS`（创造标签遍历用）。
  - `public final class ModTreeBuilder`：`public static boolean grow(ServerLevel, BlockPos, RandomSource, Supplier<Block> trunk, Supplier<Block> leaves)`。

- [ ] **Step 1: 创建炎热群系植被基类 `BlazePlantBlock.java`**

Create `src/main/java/com/example/weather_realm/block/BlazePlantBlock.java` with exactly:

```java
package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 炎热群系植被基类 / Base for blaze-biome plants.
 *
 * <p>Unlike {@link FrostPlantBlock}, plants only grow on the blazing geology
 * ({@code blaze_stone} / {@code deep_blaze_stone}).</p>
 */
public class BlazePlantBlock extends BushBlock {
    public static final MapCodec<BlazePlantBlock> CODEC = simpleCodec(BlazePlantBlock::new);

    public BlazePlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BlazePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(WeatherRealm.BLAZE_STONE.get())
                || state.is(WeatherRealm.DEEP_BLAZE_STONE.get());
    }
}
```

- [ ] **Step 2: 创建风沙群系植被基类 `WeatheredPlantBlock.java`**

Create `src/main/java/com/example/weather_realm/block/WeatheredPlantBlock.java` with exactly:

```java
package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风沙群系植被基类 / Base for arid-biome plants.
 *
 * <p>Plants only grow on the weathered sandstone geology
 * ({@code weathered_sandstone} / {@code deep_weathered_sandstone}).</p>
 */
public class WeatheredPlantBlock extends BushBlock {
    public static final MapCodec<WeatheredPlantBlock> CODEC = simpleCodec(WeatheredPlantBlock::new);

    public WeatheredPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends WeatheredPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(WeatherRealm.WEATHERED_SANDSTONE.get())
                || state.is(WeatherRealm.DEEP_WEATHERED_SANDSTONE.get());
    }
}
```

- [ ] **Step 3: 创建共享树形工具 `ModTreeBuilder.java`**

Create `src/main/java/com/example/weather_realm/world/ModTreeBuilder.java` with exactly:

```java
package com.example.weather_realm.world;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 锥形树生成工具 / Shared conical tree builder for the new biome saplings.
 *
 * <p>Business logic lives in this plain class (trampoline-friendly, JBR hot-swappable).
 * Mirrors {@code FrostSaplingBlock}'s silhouette so both new woods match the mod's look.</p>
 */
public final class ModTreeBuilder {
    private ModTreeBuilder() {
    }

    /** @return {@code true} when a tree was placed, {@code false} when blocked overhead. */
    public static boolean grow(ServerLevel level, BlockPos pos, RandomSource random,
                               Supplier<Block> trunk, Supplier<Block> leaves) {
        int height = 6 + random.nextInt(3); // 6, 7 or 8 logs tall

        for (int y = 1; y <= height + 2; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (!above.isAir() && !above.canBeReplaced()) {
                return false;
            }
        }

        BlockState trunkState = trunk.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            level.setBlock(pos.above(y), trunkState, 3);
        }

        BlockState leafState = leaves.get().defaultBlockState();
        BlockPos top = pos.above(height);
        place(level, top.above(), leafState);
        cross(level, top, 1, leafState);
        square(level, top.below(), 1, true, leafState);
        square(level, top.below(2), 2, true, leafState);
        cross(level, top.below(3), 1, leafState);
        square(level, top.below(4), 2, true, leafState);
        if (height >= 7) {
            cross(level, top.below(5), 2, leafState);
        }
        return true;
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState leafState) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, leafState, 3);
        }
    }

    private static void square(ServerLevel level, BlockPos center, int radius, boolean trimCorners, BlockState leafState) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                place(level, center.offset(dx, 0, dz), leafState);
            }
        }
    }

    private static void cross(ServerLevel level, BlockPos center, int radius, BlockState leafState) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius) {
                    continue;
                }
                place(level, center.offset(dx, 0, dz), leafState);
            }
        }
    }
}
```

- [ ] **Step 4: 创建焦火树苗 `ScorchwoodSaplingBlock.java`**

Create `src/main/java/com/example/weather_realm/block/ScorchwoodSaplingBlock.java` with exactly:

```java
package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.world.ModTreeBuilder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 焦火树苗 / Scorchwood Sapling - grows a conical scorchwood tree when bonemealed.
 */
public class ScorchwoodSaplingBlock extends BlazePlantBlock implements BonemealableBlock {
    public static final MapCodec<ScorchwoodSaplingBlock> CODEC = simpleCodec(ScorchwoodSaplingBlock::new);

    public ScorchwoodSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ScorchwoodSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        ModTreeBuilder.grow(level, pos, random, WeatherRealm.SCORCHWOOD_LOG::get, WeatherRealm.SCORCHWOOD_LEAVES::get);
    }
}
```

- [ ] **Step 5: 创建风化树苗 `WeatheredSaplingBlock.java`**

Create `src/main/java/com/example/weather_realm/block/WeatheredSaplingBlock.java` with exactly:

```java
package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.world.ModTreeBuilder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风化树苗 / Weathered Sapling - grows a conical weathered tree when bonemealed.
 */
public class WeatheredSaplingBlock extends WeatheredPlantBlock implements BonemealableBlock {
    public static final MapCodec<WeatheredSaplingBlock> CODEC = simpleCodec(WeatheredSaplingBlock::new);

    public WeatheredSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<WeatheredSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        ModTreeBuilder.grow(level, pos, random, WeatherRealm.WEATHERED_LOG::get, WeatherRealm.WEATHERED_LEAVES::get);
    }
}
```

- [ ] **Step 6: 为 `WeatherRealm.java` 补 import**

在 `WeatherRealm.java` 顶部 import 区，把下面 4 行加进 `com.example.weather_realm.block.*` 分组，并把 `import java.util.ArrayList;` / `import java.util.List;` 加进 `java.*` 分组（`List` 当前已存在，可跳过重复）：

```java
import java.util.ArrayList;

import com.example.weather_realm.block.BlazePlantBlock;
import com.example.weather_realm.block.ScorchwoodSaplingBlock;
import com.example.weather_realm.block.WeatheredPlantBlock;
import com.example.weather_realm.block.WeatheredSaplingBlock;
```

- [ ] **Step 7: 插入新内容注册块**

在 `WeatherRealm.java` 中，定位到 `FROST_MILK_BUCKET` 声明之后、`// --- Particles` 注释之前（当前约为 565 行），**插入**以下完整代码块：

```java
    // ============================================================================================
    // 炎热 / 风沙 群系内容 / Blazing & arid biome content
    // ============================================================================================

    private static BlockBehaviour.Properties blazeStoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(2.25F, 6.0F)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties deepBlazeStoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .strength(4.5F, 6.0F)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties weatheredSandstoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(2.25F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties deepWeatheredSandstoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_ORANGE)
                .strength(4.5F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties crystalBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(4.5F, 6.0F)
                .sound(SoundType.AMETHYST)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties blazeWoodPillarProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties weatheredWoodPillarProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties blazePlantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties weatheredPlantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    // 火石 / Blaze stone geology
    public static final DeferredBlock<Block> BLAZE_STONE = BLOCKS.registerSimpleBlock("blaze_stone", blazeStoneProperties());
    public static final DeferredItem<BlockItem> BLAZE_STONE_ITEM = ITEMS.registerSimpleBlockItem("blaze_stone", BLAZE_STONE);
    public static final DeferredBlock<Block> DEEP_BLAZE_STONE = BLOCKS.registerSimpleBlock("deep_blaze_stone", deepBlazeStoneProperties());
    public static final DeferredItem<BlockItem> DEEP_BLAZE_STONE_ITEM = ITEMS.registerSimpleBlockItem("deep_blaze_stone", DEEP_BLAZE_STONE);

    // 风化砂石 / Weathered sandstone geology
    public static final DeferredBlock<Block> WEATHERED_SANDSTONE = BLOCKS.registerSimpleBlock("weathered_sandstone", weatheredSandstoneProperties());
    public static final DeferredItem<BlockItem> WEATHERED_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem("weathered_sandstone", WEATHERED_SANDSTONE);
    public static final DeferredBlock<Block> DEEP_WEATHERED_SANDSTONE = BLOCKS.registerSimpleBlock("deep_weathered_sandstone", deepWeatheredSandstoneProperties());
    public static final DeferredItem<BlockItem> DEEP_WEATHERED_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem("deep_weathered_sandstone", DEEP_WEATHERED_SANDSTONE);

    // 炎火晶石 / 风沙晶石 — items + decorative blocks
    public static final DeferredItem<Item> EMBER_CRYSTAL = ITEMS.registerSimpleItem("ember_crystal");
    public static final DeferredBlock<Block> EMBER_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("ember_crystal_block", crystalBlockProperties());
    public static final DeferredItem<BlockItem> EMBER_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ember_crystal_block", EMBER_CRYSTAL_BLOCK);
    public static final DeferredItem<Item> SAND_CRYSTAL = ITEMS.registerSimpleItem("sand_crystal");
    public static final DeferredBlock<Block> SAND_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("sand_crystal_block", crystalBlockProperties());
    public static final DeferredItem<BlockItem> SAND_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("sand_crystal_block", SAND_CRYSTAL_BLOCK);

    // 矿石批量注册 / Generated ore family (shallow + deep), items collected for the creative tab.
    private static final List<DeferredItem<BlockItem>> GENERATED_ORE_ITEMS = new ArrayList<>();

    private static void registerOrePair(String shallowName, String deepName,
                                        BlockBehaviour.Properties shallowProps,
                                        BlockBehaviour.Properties deepProps) {
        DeferredBlock<Block> shallow = BLOCKS.registerSimpleBlock(shallowName, shallowProps);
        GENERATED_ORE_ITEMS.add(ITEMS.registerSimpleBlockItem(shallowName, shallow));
        DeferredBlock<Block> deep = BLOCKS.registerSimpleBlock(deepName, deepProps);
        GENERATED_ORE_ITEMS.add(ITEMS.registerSimpleBlockItem(deepName, deep));
    }

    static {
        String[] vanillaOres = {"coal", "copper", "iron", "gold", "redstone", "emerald", "lapis", "diamond"};
        for (String ore : vanillaOres) {
            registerOrePair("blaze_stone_" + ore + "_ore", "deep_blaze_stone_" + ore + "_ore",
                    blazeStoneProperties(), deepBlazeStoneProperties());
            registerOrePair("weathered_sandstone_" + ore + "_ore", "deep_weathered_sandstone_" + ore + "_ore",
                    weatheredSandstoneProperties(), deepWeatheredSandstoneProperties());
        }
        registerOrePair("blaze_stone_ember_crystal_ore", "deep_blaze_stone_ember_crystal_ore",
                blazeStoneProperties(), deepBlazeStoneProperties());
        registerOrePair("weathered_sandstone_sand_crystal_ore", "deep_weathered_sandstone_sand_crystal_ore",
                weatheredSandstoneProperties(), deepWeatheredSandstoneProperties());
    }

    // --- 焦火木 / Scorchwood (blaze biome tree family) -----------------------------------------
    public static final DeferredBlock<FrostLogBlock> SCORCHWOOD_LOG = BLOCKS.registerBlock("scorchwood_log", FrostLogBlock::new, blazeWoodPillarProperties());
    public static final DeferredItem<BlockItem> SCORCHWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem("scorchwood_log", SCORCHWOOD_LOG);
    public static final DeferredBlock<FrostLogBlock> SCORCHWOOD_WOOD = BLOCKS.registerBlock("scorchwood_wood", FrostLogBlock::new, blazeWoodPillarProperties());
    public static final DeferredItem<BlockItem> SCORCHWOOD_WOOD_ITEM = ITEMS.registerSimpleBlockItem("scorchwood_wood", SCORCHWOOD_WOOD);
    public static final DeferredBlock<FrostLogBlock> STRIPPED_SCORCHWOOD_LOG = BLOCKS.registerBlock("stripped_scorchwood_log", FrostLogBlock::new, blazeWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_SCORCHWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_scorchwood_log", STRIPPED_SCORCHWOOD_LOG);
    public static final DeferredBlock<FrostLogBlock> STRIPPED_SCORCHWOOD_WOOD = BLOCKS.registerBlock("stripped_scorchwood_wood", FrostLogBlock::new, blazeWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_SCORCHWOOD_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_scorchwood_wood", STRIPPED_SCORCHWOOD_WOOD);

    public static final DeferredBlock<Block> SCORCHWOOD_PLANKS = BLOCKS.registerSimpleBlock("scorchwood_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> SCORCHWOOD_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("scorchwood_planks", SCORCHWOOD_PLANKS);

    public static final DeferredBlock<FrostLeavesBlock> SCORCHWOOD_LEAVES = BLOCKS.registerBlock("scorchwood_leaves", FrostLeavesBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> SCORCHWOOD_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("scorchwood_leaves", SCORCHWOOD_LEAVES);

    public static final DeferredBlock<ScorchwoodSaplingBlock> SCORCHWOOD_SAPLING = BLOCKS.registerBlock("scorchwood_sapling", ScorchwoodSaplingBlock::new, blazePlantProperties());
    public static final DeferredItem<BlockItem> SCORCHWOOD_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("scorchwood_sapling", SCORCHWOOD_SAPLING);

    // --- 风化木 / Weathered wood (arid biome tree family) --------------------------------------
    public static final DeferredBlock<FrostLogBlock> WEATHERED_LOG = BLOCKS.registerBlock("weathered_log", FrostLogBlock::new, weatheredWoodPillarProperties());
    public static final DeferredItem<BlockItem> WEATHERED_LOG_ITEM = ITEMS.registerSimpleBlockItem("weathered_log", WEATHERED_LOG);
    public static final DeferredBlock<FrostLogBlock> WEATHERED_WOOD = BLOCKS.registerBlock("weathered_wood", FrostLogBlock::new, weatheredWoodPillarProperties());
    public static final DeferredItem<BlockItem> WEATHERED_WOOD_ITEM = ITEMS.registerSimpleBlockItem("weathered_wood", WEATHERED_WOOD);
    public static final DeferredBlock<FrostLogBlock> STRIPPED_WEATHERED_LOG = BLOCKS.registerBlock("stripped_weathered_log", FrostLogBlock::new, weatheredWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_WEATHERED_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_weathered_log", STRIPPED_WEATHERED_LOG);
    public static final DeferredBlock<FrostLogBlock> STRIPPED_WEATHERED_WOOD = BLOCKS.registerBlock("stripped_weathered_wood", FrostLogBlock::new, weatheredWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_WEATHERED_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_weathered_wood", STRIPPED_WEATHERED_WOOD);

    public static final DeferredBlock<Block> WEATHERED_PLANKS = BLOCKS.registerSimpleBlock("weathered_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> WEATHERED_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("weathered_planks", WEATHERED_PLANKS);

    public static final DeferredBlock<FrostLeavesBlock> WEATHERED_LEAVES = BLOCKS.registerBlock("weathered_leaves", FrostLeavesBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> WEATHERED_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("weathered_leaves", WEATHERED_LEAVES);

    public static final DeferredBlock<WeatheredSaplingBlock> WEATHERED_SAPLING = BLOCKS.registerBlock("weathered_sapling", WeatheredSaplingBlock::new, weatheredPlantProperties());
    public static final DeferredItem<BlockItem> WEATHERED_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("weathered_sapling", WEATHERED_SAPLING);

    // --- 植被 / Vegetation ---------------------------------------------------------------------
    public static final DeferredBlock<BlazePlantBlock> EMBER_GRASS = BLOCKS.registerBlock("ember_grass", BlazePlantBlock::new, blazePlantProperties());
    public static final DeferredItem<BlockItem> EMBER_GRASS_ITEM = ITEMS.registerSimpleBlockItem("ember_grass", EMBER_GRASS);
    public static final DeferredBlock<BlazePlantBlock> FLAME_FLOWER = BLOCKS.registerBlock("flame_flower", BlazePlantBlock::new, blazePlantProperties());
    public static final DeferredItem<BlockItem> FLAME_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("flame_flower", FLAME_FLOWER);
    public static final DeferredBlock<WeatheredPlantBlock> DESERT_GRASS = BLOCKS.registerBlock("desert_grass", WeatheredPlantBlock::new, weatheredPlantProperties());
    public static final DeferredItem<BlockItem> DESERT_GRASS_ITEM = ITEMS.registerSimpleBlockItem("desert_grass", DESERT_GRASS);
    public static final DeferredBlock<WeatheredPlantBlock> DESERT_BLOOM = BLOCKS.registerBlock("desert_bloom", WeatheredPlantBlock::new, weatheredPlantProperties());
    public static final DeferredItem<BlockItem> DESERT_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("desert_bloom", DESERT_BLOOM);
```

- [ ] **Step 8: 把新内容加入创造模式标签**

在 `WeatherRealm.java` 的 `EXAMPLE_TAB` 里，定位到 `output.accept(FROST_RASPBERRY.get());` 这一行，在它**之后**、`}).build());` **之前**插入：

```java
                output.accept(BLAZE_STONE_ITEM.get());
                output.accept(DEEP_BLAZE_STONE_ITEM.get());
                output.accept(WEATHERED_SANDSTONE_ITEM.get());
                output.accept(DEEP_WEATHERED_SANDSTONE_ITEM.get());
                output.accept(EMBER_CRYSTAL.get());
                output.accept(EMBER_CRYSTAL_BLOCK_ITEM.get());
                output.accept(SAND_CRYSTAL.get());
                output.accept(SAND_CRYSTAL_BLOCK_ITEM.get());
                output.accept(SCORCHWOOD_LOG_ITEM.get());
                output.accept(SCORCHWOOD_WOOD_ITEM.get());
                output.accept(STRIPPED_SCORCHWOOD_LOG_ITEM.get());
                output.accept(STRIPPED_SCORCHWOOD_WOOD_ITEM.get());
                output.accept(SCORCHWOOD_PLANKS_ITEM.get());
                output.accept(SCORCHWOOD_LEAVES_ITEM.get());
                output.accept(SCORCHWOOD_SAPLING_ITEM.get());
                output.accept(WEATHERED_LOG_ITEM.get());
                output.accept(WEATHERED_WOOD_ITEM.get());
                output.accept(STRIPPED_WEATHERED_LOG_ITEM.get());
                output.accept(STRIPPED_WEATHERED_WOOD_ITEM.get());
                output.accept(WEATHERED_PLANKS_ITEM.get());
                output.accept(WEATHERED_LEAVES_ITEM.get());
                output.accept(WEATHERED_SAPLING_ITEM.get());
                output.accept(EMBER_GRASS_ITEM.get());
                output.accept(FLAME_FLOWER_ITEM.get());
                output.accept(DESERT_GRASS_ITEM.get());
                output.accept(DESERT_BLOOM_ITEM.get());
                GENERATED_ORE_ITEMS.forEach(item -> output.accept(item.get()));
```

- [ ] **Step 9: 编译验证（自动化门禁）**

Run:
```powershell
.\gradlew.bat compileJava
```

Expected：`BUILD SUCCESSFUL`；无 `cannot find symbol` / `incompatible types` / `method does not override`。常见错因：`codec()` 返回类型没写 `MapCodec<? extends BlazePlantBlock>`（泛型不可协变）；`BonemealableBlock` 的方法名拼写错误（应为 `isBonemealSuccess`）；漏 import。

- [ ] **Step 10: 提交**

```powershell
git add src/main/java/com/example/weather_realm/block/BlazePlantBlock.java `
        src/main/java/com/example/weather_realm/block/WeatheredPlantBlock.java `
        src/main/java/com/example/weather_realm/block/ScorchwoodSaplingBlock.java `
        src/main/java/com/example/weather_realm/block/WeatheredSaplingBlock.java `
        src/main/java/com/example/weather_realm/world/ModTreeBuilder.java `
        src/main/java/com/example/weather_realm/WeatherRealm.java
git commit -m "feat: register blazing/arid biome blocks, ores, woods and plants"
```

---

## Task 2: 更新 `ModSurfaceRules.java` 地表材质

**Files:**
- Modify: `src/main/java/com/example/weather_realm/world/ModSurfaceRules.java:63-81`

**Interfaces:**
- Consumes: Task 1 产出的 `WeatherRealm.BLAZE_STONE` / `DEEP_BLAZE_STONE` / `WEATHERED_SANDSTONE` / `DEEP_WEATHERED_SANDSTONE`（`DeferredBlock<Block>`，`.get()` → `Block`）。
- Produces: 更新后的 `wrapOverworld(SurfaceRules.RuleSource)`（签名不变，由既有 `SurfaceRuleDataMixin` 委托调用）。

- [ ] **Step 1: 替换 `createBlazingPlainsRules()` 与 `createAridWastelandRules()`**

把 `ModSurfaceRules.java` 中这两个方法整体替换为：

```java
    private static SurfaceRules.RuleSource createBlazingPlainsRules() {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(BLAZING_PLAINS),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(WeatherRealm.BLAZE_STONE.get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.state(WeatherRealm.DEEP_BLAZE_STONE.get().defaultBlockState()))));
    }

    private static SurfaceRules.RuleSource createAridWastelandRules() {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ARID_WASTELAND),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(WeatherRealm.WEATHERED_SANDSTONE.get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.state(WeatherRealm.DEEP_WEATHERED_SANDSTONE.get().defaultBlockState()))));
    }
```

替换后 `Blocks` 这个 import 可能变成未使用（`createCrystalPlainsRules` 已全部用 `WeatherRealm.*`，三处分支都不再用 `Blocks.MAGMA_BLOCK` / `Blocks.SAND` / `Blocks.SANDSTONE`）。检查该文件是否还有 `net.minecraft.world.level.block.Blocks` 的其它引用：

Run:
```powershell
Select-String -Path .\src\main\java\com\example\weather_realm\world\ModSurfaceRules.java -Pattern "Blocks\."
```

Expected：**无输出**。若确无输出，删除 `import net.minecraft.world.level.block.Blocks;` 这一行，避免 IDE 未使用告警（非致命）。

- [ ] **Step 2: 编译验证**

Run:
```powershell
.\gradlew.bat compileJava
```

Expected：`BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```powershell
git add src/main/java/com/example/weather_realm/world/ModSurfaceRules.java
git commit -m "feat: assign blaze stone / weathered sandstone surface materials per biome"
```

---

## Task 3: Python 脚本生成方块资源（贴图 / 模型 / 战利品 / 标签 / 语言）

**Files:**
- Create: `tools/biome_data.py`
- Create: `tools/gen_block_assets.py`

**Interfaces:**
- Consumes: 原版 1.21.1 客户端 jar（`C:\Users\31087\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar`，或 PCL 的 `.minecraft\libraries\net\minecraft\client\**\*extra.jar`），以及仓库现有 `assets/weather_realm/lang/*.json`、`data/minecraft/tags/**`、`data/weather_realm/tags/**`。
- Produces（供 Task 4 脚本 `import`）：
  - `biome_data.THEMES`（`list[dict]`，2 个元素，键 `key/base/deep/crystal/crystal_block/wood/grass/flower/...`）。
  - `biome_data.ORE_ORDER/ORE_EN/ORE_ZH/ORE_COUNT/ORE_MAX_Y`。
  - `biome_data.wood_block_names(theme)`、`shallow_ore(theme, ore)`、`deep_ore(theme, ore)`、`shallow_crystal_ore(theme)`、`deep_crystal_ore(theme)`。
  - 资源断言：60 个 blockstate / 68 个 block model / 62 个 item model / 60 张 block PNG / 2 张 item PNG / 60 张 loot_table。

- [ ] **Step 1: 确认 Pillow 可用**

Run:
```powershell
python --version
python -c "import PIL; print(PIL.__version__)"
```

Expected：`Python 3.11.x`；`Pillow 11.x`。若第二条报 `ModuleNotFoundError`，执行 `python -m pip install Pillow`。

- [ ] **Step 2: 创建共享数据模块 `tools/biome_data.py`**

Create `tools/biome_data.py` with exactly:

```python
"""Shared data tables for the Weather Realm biome-content generators.

The two entry-point scripts import this module:

    python tools/gen_block_assets.py        # textures + client/datapack resources
    python tools/gen_worldgen_features.py   # tree / plant / ore feature JSONs

Everything here is plain data so the two generators stay in sync (DRY).
"""

MODID = "weather_realm"

# --- Vanilla ore analogues ---------------------------------------------------
ORE_ORDER = ["coal", "copper", "iron", "gold", "redstone", "emerald", "lapis", "diamond"]

ORE_EN = {
    "coal": "Coal", "copper": "Copper", "iron": "Iron", "gold": "Gold",
    "redstone": "Redstone", "emerald": "Emerald", "lapis": "Lapis", "diamond": "Diamond",
}
ORE_ZH = {
    "coal": "煤", "copper": "铜", "iron": "铁", "gold": "金",
    "redstone": "红石", "emerald": "绿宝石", "lapis": "青金石", "diamond": "钻石",
}

# ore -> (drop_item_id, min_count, max_count, fortune_formula, bonus_multiplier_or_None)
ORE_DROPS = {
    "coal": ("minecraft:coal", 1, 1, "minecraft:ore_drops", None),
    "copper": ("minecraft:raw_copper", 2, 5, "minecraft:ore_drops", None),
    "iron": ("minecraft:raw_iron", 1, 1, "minecraft:ore_drops", None),
    "gold": ("minecraft:raw_gold", 1, 1, "minecraft:ore_drops", None),
    "redstone": ("minecraft:redstone", 4, 5, "minecraft:uniform_bonus_count", 1),
    "emerald": ("minecraft:emerald", 1, 1, "minecraft:ore_drops", None),
    "lapis": ("minecraft:lapis_lazuli", 4, 9, "minecraft:ore_drops", None),
    "diamond": ("minecraft:diamond", 1, 1, "minecraft:ore_drops", None),
}

# vein count per chunk (rare gems are deliberately sparse)
ORE_COUNT = {"coal": 8, "copper": 8, "iron": 10, "gold": 2,
             "redstone": 6, "emerald": 3, "lapis": 2, "diamond": 1}

# max_inclusive Y for the trapezoid height range (min is always -64)
ORE_MAX_Y = {"coal": 80, "copper": 80, "iron": 80, "gold": 40,
             "redstone": 40, "emerald": 40, "lapis": 40, "diamond": 40}

# --- Theme definitions -------------------------------------------------------
# Every entry is self-contained: names, vanilla source textures, recolor
# profiles and worldgen knobs for one climate biome.
THEMES = [
    {
        "key": "blaze",
        "base": "blaze_stone",
        "base_en": "Blaze Stone",
        "base_zh": "火石",
        "deep": "deep_blaze_stone",
        "deep_en": "Deep Blaze Stone",
        "deep_zh": "深层火石",
        "base_src": "stone",
        "deep_src": "deepslate",
        "stone_profile": dict(target_hue=0.045, sat_floor=0.35, sat_mul=1.0, val_mul=0.85, protect_sat=0.55, speck_hue_shift=0.0),
        "deep_profile": dict(target_hue=0.020, sat_floor=0.35, sat_mul=1.0, val_mul=0.70, protect_sat=0.55, speck_hue_shift=0.0),
        "crystal": "ember_crystal",
        "crystal_en": "Ember Crystal",
        "crystal_zh": "炎火晶石",
        "crystal_block": "ember_crystal_block",
        "crystal_block_en": "Ember Crystal Block",
        "crystal_block_zh": "炎火晶石块",
        "crystal_src": "item/amethyst_shard",
        "crystal_block_src": "amethyst_block",
        "crystal_ore_src": "diamond_ore",
        "deep_crystal_ore_src": "deepslate_diamond_ore",
        "crystal_profile": dict(target_hue=0.06, sat_floor=0.50, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "crystal_ore_profile": dict(target_hue=0.05, sat_floor=0.35, sat_mul=1.0, val_mul=0.90, protect_sat=0.50, speck_hue_shift=-0.45),
        "deep_crystal_ore_profile": dict(target_hue=0.03, sat_floor=0.35, sat_mul=1.0, val_mul=0.75, protect_sat=0.50, speck_hue_shift=-0.45),
        "replaceable": "blaze_stone_ore_replaceables",
        "deep_replaceable": "deep_blaze_stone_ore_replaceables",
        "wood": "scorchwood",
        "wood_src": "oak",
        "wood_profile": dict(target_hue=0.030, sat_floor=0.22, sat_mul=1.0, val_mul=0.35, protect_sat=None, speck_hue_shift=0.0),
        "planks_profile": dict(target_hue=0.040, sat_floor=0.25, sat_mul=1.0, val_mul=0.55, protect_sat=None, speck_hue_shift=0.0),
        "leaves_profile": dict(target_hue=0.055, sat_floor=0.45, sat_mul=1.0, val_mul=0.95, protect_sat=None, speck_hue_shift=0.0),
        "sapling_profile": dict(target_hue=0.055, sat_floor=0.45, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "grass": "ember_grass",
        "grass_en": "Ember Grass",
        "grass_zh": "火绒草",
        "flower": "flame_flower",
        "flower_en": "Flame Flower",
        "flower_zh": "烈焰花",
        "grass_src": "short_grass",
        "flower_src": "dandelion",
        "grass_profile": dict(target_hue=0.040, sat_floor=0.50, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "flower_profile": dict(target_hue=0.000, sat_floor=0.60, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "tree_placer": "spruce",
        "tree_rarity": 12,
        "wood_names_en": {
            "log": "Scorchwood Log", "wood": "Scorchwood",
            "stripped_log": "Stripped Scorchwood Log", "stripped_wood": "Stripped Scorchwood",
            "planks": "Scorchwood Planks", "leaves": "Scorchwood Leaves", "sapling": "Scorchwood Sapling",
        },
        "wood_names_zh": {
            "log": "焦火原木", "wood": "焦火木",
            "stripped_log": "去皮焦火原木", "stripped_wood": "去皮焦火木",
            "planks": "焦火木板", "leaves": "焦火树叶", "sapling": "焦火树苗",
        },
    },
    {
        "key": "weathered",
        "base": "weathered_sandstone",
        "base_en": "Weathered Sandstone",
        "base_zh": "风化砂石",
        "deep": "deep_weathered_sandstone",
        "deep_en": "Deep Weathered Sandstone",
        "deep_zh": "深层风化砂石",
        "base_src": "sandstone",
        "deep_src": "sandstone",
        "stone_profile": dict(target_hue=0.11, sat_floor=0.28, sat_mul=0.80, val_mul=1.00, protect_sat=0.55, speck_hue_shift=0.0),
        "deep_profile": dict(target_hue=0.09, sat_floor=0.30, sat_mul=0.85, val_mul=0.62, protect_sat=0.55, speck_hue_shift=0.0),
        "crystal": "sand_crystal",
        "crystal_en": "Sand Crystal",
        "crystal_zh": "风沙晶石",
        "crystal_block": "sand_crystal_block",
        "crystal_block_en": "Sand Crystal Block",
        "crystal_block_zh": "风沙晶石块",
        "crystal_src": "item/amethyst_shard",
        "crystal_block_src": "amethyst_block",
        "crystal_ore_src": "diamond_ore",
        "deep_crystal_ore_src": "deepslate_diamond_ore",
        "crystal_profile": dict(target_hue=0.12, sat_floor=0.45, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "crystal_ore_profile": dict(target_hue=0.11, sat_floor=0.28, sat_mul=1.0, val_mul=1.00, protect_sat=0.50, speck_hue_shift=-0.38),
        "deep_crystal_ore_profile": dict(target_hue=0.10, sat_floor=0.30, sat_mul=1.0, val_mul=0.70, protect_sat=0.50, speck_hue_shift=-0.38),
        "replaceable": "weathered_sandstone_ore_replaceables",
        "deep_replaceable": "deep_weathered_sandstone_ore_replaceables",
        "wood": "weathered",
        "wood_src": "birch",
        "wood_profile": dict(target_hue=0.090, sat_floor=0.22, sat_mul=1.0, val_mul=1.00, protect_sat=None, speck_hue_shift=0.0),
        "planks_profile": dict(target_hue=0.100, sat_floor=0.24, sat_mul=1.0, val_mul=1.00, protect_sat=None, speck_hue_shift=0.0),
        "leaves_profile": dict(target_hue=0.130, sat_floor=0.35, sat_mul=1.0, val_mul=0.95, protect_sat=None, speck_hue_shift=0.0),
        "sapling_profile": dict(target_hue=0.130, sat_floor=0.40, sat_mul=1.0, val_mul=1.00, protect_sat=None, speck_hue_shift=0.0),
        "grass": "desert_grass",
        "grass_en": "Desert Grass",
        "grass_zh": "风沙草",
        "flower": "desert_bloom",
        "flower_en": "Desert Bloom",
        "flower_zh": "砂兰",
        "grass_src": "fern",
        "flower_src": "dandelion",
        "grass_profile": dict(target_hue=0.130, sat_floor=0.40, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "flower_profile": dict(target_hue=0.130, sat_floor=0.55, sat_mul=1.0, val_mul=1.0, protect_sat=None, speck_hue_shift=0.0),
        "tree_placer": "blob",
        "tree_rarity": 12,
        "wood_names_en": {
            "log": "Weathered Log", "wood": "Weathered Wood",
            "stripped_log": "Stripped Weathered Log", "stripped_wood": "Stripped Weathered Wood",
            "planks": "Weathered Planks", "leaves": "Weathered Leaves", "sapling": "Weathered Sapling",
        },
        "wood_names_zh": {
            "log": "风化原木", "wood": "风化木",
            "stripped_log": "去皮风化原木", "stripped_wood": "去皮风化木",
            "planks": "风化木板", "leaves": "风化树叶", "sapling": "风化树苗",
        },
    },
]


def wood_block_names(theme):
    """Return the seven generated block IDs of a theme's tree family."""
    w = theme["wood"]
    return {
        "log": f"{w}_log",
        "wood": f"{w}_wood",
        "stripped_log": f"stripped_{w}_log",
        "stripped_wood": f"stripped_{w}_wood",
        "planks": f"{w}_planks",
        "leaves": f"{w}_leaves",
        "sapling": f"{w}_sapling",
    }


def shallow_ore(theme, ore):
    return f"{theme['base']}_{ore}_ore"


def deep_ore(theme, ore):
    return f"deep_{theme['base']}_{ore}_ore"


def shallow_crystal_ore(theme):
    return f"{theme['base']}_{theme['crystal']}_ore"


def deep_crystal_ore(theme):
    return f"deep_{theme['base']}_{theme['crystal']}_ore"


def ore_loot(block, ore):
    """Silk-touch / fortune aware loot table for a vanilla-ore analogue."""
    item, lo, hi, formula, mult = ORE_DROPS[ore]
    funcs = []
    if (lo, hi) != (1, 1):
        funcs.append({
            "add": False,
            "count": {"type": "minecraft:uniform", "max": float(hi), "min": float(lo)},
            "function": "minecraft:set_count",
        })
    bonus = {
        "enchantment": "minecraft:fortune",
        "formula": formula,
        "function": "minecraft:apply_bonus",
    }
    if mult is not None:
        bonus["parameters"] = {"bonusMultiplier": mult}
    funcs.append(bonus)
    funcs.append({"function": "minecraft:explosion_decay"})
    return _ore_loot(block, item, funcs)


def crystal_ore_loot(block, crystal):
    """Silk-touch / fortune aware loot table dropping a mod crystal item."""
    funcs = [
        {"enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops", "function": "minecraft:apply_bonus"},
        {"function": "minecraft:explosion_decay"},
    ]
    return _ore_loot(block, f"{MODID}:{crystal}", funcs)


def _ore_loot(block, drop_item, functions):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [
                    {
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item",
                                "name": f"{MODID}:{block}",
                                "conditions": [
                                    {
                                        "condition": "minecraft:match_tool",
                                        "predicate": {
                                            "predicates": {
                                                "minecraft:enchantments": [
                                                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                                                ]
                                            }
                                        },
                                    }
                                ],
                            },
                            {"type": "minecraft:item", "name": drop_item, "functions": functions},
                        ],
                    }
                ],
            }
        ],
        "random_sequence": f"{MODID}:blocks/{block}",
    }


def self_loot(block):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{block}"}],
            }
        ],
        "random_sequence": f"{MODID}:blocks/{block}",
    }


def leaves_loot(block, sapling):
    """Vanilla-style leaf loot: shears/silk-touch drop the leaf, else sapling + stick."""
    fortune = {
        "function": "minecraft:set_count",
        "conditions": [
            {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
             "chances": [0.05, 0.0625, 0.083333336, 0.1]}
        ],
        "count": 1.0,
    }
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [
                    {
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item",
                                "conditions": [
                                    {
                                        "condition": "minecraft:any_of",
                                        "terms": [
                                            {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
                                            {
                                                "condition": "minecraft:match_tool",
                                                "predicate": {
                                                    "predicates": {
                                                        "minecraft:enchantments": [
                                                            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                                                        ]
                                                    }
                                                },
                                            },
                                        ],
                                    }
                                ],
                                "name": f"{MODID}:{block}",
                            },
                            {
                                "type": "minecraft:item",
                                "conditions": [{"condition": "minecraft:survives_explosion"}],
                                "name": f"{MODID}:{sapling}",
                                "functions": [dict(fortune)],
                            },
                        ],
                    }
                ],
            },
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [
                    {
                        "type": "minecraft:item",
                        "conditions": [{"condition": "minecraft:survives_explosion"}],
                        "name": "minecraft:stick",
                        "functions": [dict(fortune)],
                    }
                ],
            },
        ],
        "random_sequence": f"{MODID}:blocks/{block}",
    }
```

- [ ] **Step 3: 创建资源生成器 `tools/gen_block_assets.py`**

Create `tools/gen_block_assets.py` with exactly:

```python
#!/usr/bin/env python3
"""Generate textures + client/datapack resources for the New Realm biome blocks.

Usage (from the repo root, after `pip install Pillow`):

    python tools/gen_block_assets.py
    python tools/gen_block_assets.py --root . --client-jar <path to client jar>

16x16 textures are recoloured from the vanilla 1.21.1 client jar, so the mod
ships no hand-drawn art. Datapack JSON (loot tables, tags) and the lang files are
*merged* into the existing resources, never overwritten wholesale.
"""
from __future__ import annotations

import argparse
import colorsys
import glob
import io
import json
import os
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("Pillow is required:  pip install Pillow")

MODID = bd.MODID


# --- vanilla jar / texture helpers ------------------------------------------
def find_client_jar(root: Path) -> Path:
    override = os.environ.get("MC_CLIENT_JAR")
    candidates = []
    if override:
        candidates.append(Path(override))
    home = Path.home()
    candidates += [Path(p) for p in glob.glob(str(home / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"))]
    for base in (root, root.parent, home):
        candidates += [Path(p) for p in glob.glob(str(base / ".minecraft/libraries/net/minecraft/client/**/*extra.jar"), recursive=True)]
    for c in candidates:
        if c.is_file():
            return c
    sys.exit("Could not find a vanilla client jar; pass --client-jar or set MC_CLIENT_JAR.")


def read_png(zf: zipfile.ZipFile, rel: str) -> Image.Image:
    path = f"assets/minecraft/textures/{rel}.png"
    with zf.open(path) as fh:
        return Image.open(io.BytesIO(fh.read())).convert("RGBA")


def recolor(img: Image.Image, profile: dict) -> Image.Image:
    """Hue/saturation/value recolor. Saturated 'speck' pixels are protected."""
    out = img.copy().convert("RGBA")
    px = out.load()
    target_hue = profile.get("target_hue")
    sat_floor = profile.get("sat_floor", 0.0)
    sat_mul = profile.get("sat_mul", 1.0)
    val_mul = profile.get("val_mul", 1.0)
    protect_sat = profile.get("protect_sat")
    speck_shift = profile.get("speck_hue_shift", 0.0)
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if protect_sat is not None and ss >= protect_sat:
                hh = (hh + speck_shift) % 1.0
                vv = min(1.0, vv * val_mul)
            else:
                hh = target_hue if target_hue is not None else hh
                ss = min(1.0, max(ss, sat_floor) * sat_mul)
                vv = min(1.0, vv * val_mul)
            nr, ng, nb = colorsys.hsv_to_rgb(hh, ss, vv)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def merge_tag(path: Path, values) -> None:
    data = {"replace": False, "values": []}
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        data.setdefault("replace", False)
        data.setdefault("values", [])
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write_json(path, data)


# --- block specs -------------------------------------------------------------
def block_specs(theme):
    """Return the list of block descriptors generated for one theme."""
    specs = []
    base, deep = theme["base"], theme["deep"]

    specs.append(dict(
        name=base, model="cube_all", en=theme["base_en"], zh=theme["base_zh"], loot=("self",),
        textures=[(base, f"block/{theme['base_src']}", theme["stone_profile"])],
    ))
    specs.append(dict(
        name=deep, model="cube_all", en=theme["deep_en"], zh=theme["deep_zh"], loot=("self",),
        textures=[(deep, f"block/{theme['deep_src']}", theme["deep_profile"])],
    ))
    specs.append(dict(
        name=theme["crystal_block"], model="cube_all",
        en=theme["crystal_block_en"], zh=theme["crystal_block_zh"], loot=("self",),
        textures=[(theme["crystal_block"], f"block/{theme['crystal_block_src']}", theme["crystal_profile"])],
    ))

    for ore in bd.ORE_ORDER:
        shallow = bd.shallow_ore(theme, ore)
        d = bd.deep_ore(theme, ore)
        specs.append(dict(
            name=shallow, model="cube_all",
            en=f"{theme['base_en']} {bd.ORE_EN[ore]} Ore", zh=f"{theme['base_zh']}{bd.ORE_ZH[ore]}矿",
            loot=("ore", ore),
            textures=[(shallow, f"block/{ore}_ore", theme["stone_profile"])],
        ))
        specs.append(dict(
            name=d, model="cube_all",
            en=f"{theme['deep_en']} {bd.ORE_EN[ore]} Ore", zh=f"{theme['deep_zh']}{bd.ORE_ZH[ore]}矿",
            loot=("ore", ore),
            textures=[(d, f"block/deepslate_{ore}_ore", theme["deep_profile"])],
        ))

    cs = bd.shallow_crystal_ore(theme)
    cd = bd.deep_crystal_ore(theme)
    specs.append(dict(
        name=cs, model="cube_all",
        en=f"{theme['base_en']} {theme['crystal_en']} Ore", zh=f"{theme['base_zh']}{theme['crystal_zh']}矿",
        loot=("crystal_ore", theme["crystal"]),
        textures=[(cs, f"block/{theme['crystal_ore_src']}", theme["crystal_ore_profile"])],
    ))
    specs.append(dict(
        name=cd, model="cube_all",
        en=f"{theme['deep_en']} {theme['crystal_en']} Ore", zh=f"{theme['deep_zh']}{theme['crystal_zh']}矿",
        loot=("crystal_ore", theme["crystal"]),
        textures=[(cd, f"block/{theme['deep_crystal_ore_src']}", theme["deep_crystal_ore_profile"])],
    ))

    wn = bd.wood_block_names(theme)
    wsrc = theme["wood_src"]
    specs.append(dict(
        name=wn["log"], model="pillar", en=theme["wood_names_en"]["log"], zh=theme["wood_names_zh"]["log"],
        loot=("self",),
        textures=[(f"{theme['wood']}_log", f"block/{wsrc}_log", theme["wood_profile"]),
                  (f"{theme['wood']}_log_top", f"block/{wsrc}_log_top", theme["wood_profile"])],
        pillar_top=f"{theme['wood']}_log_top", pillar_side=f"{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["wood"], model="pillar", en=theme["wood_names_en"]["wood"], zh=theme["wood_names_zh"]["wood"],
        loot=("self",),
        textures=[(f"{theme['wood']}_log", f"block/{wsrc}_log", theme["wood_profile"])],
        pillar_top=f"{theme['wood']}_log", pillar_side=f"{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["stripped_log"], model="pillar", en=theme["wood_names_en"]["stripped_log"],
        zh=theme["wood_names_zh"]["stripped_log"], loot=("self",),
        textures=[(f"stripped_{theme['wood']}_log", f"block/stripped_{wsrc}_log", theme["wood_profile"]),
                  (f"stripped_{theme['wood']}_log_top", f"block/stripped_{wsrc}_log_top", theme["wood_profile"])],
        pillar_top=f"stripped_{theme['wood']}_log_top", pillar_side=f"stripped_{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["stripped_wood"], model="pillar", en=theme["wood_names_en"]["stripped_wood"],
        zh=theme["wood_names_zh"]["stripped_wood"], loot=("self",),
        textures=[(f"stripped_{theme['wood']}_log", f"block/stripped_{wsrc}_log", theme["wood_profile"])],
        pillar_top=f"stripped_{theme['wood']}_log", pillar_side=f"stripped_{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["planks"], model="cube_all", en=theme["wood_names_en"]["planks"],
        zh=theme["wood_names_zh"]["planks"], loot=("self",),
        textures=[(f"{theme['wood']}_planks", f"block/{wsrc}_planks", theme["planks_profile"])],
    ))
    specs.append(dict(
        name=wn["leaves"], model="leaves", en=theme["wood_names_en"]["leaves"],
        zh=theme["wood_names_zh"]["leaves"], loot=("leaves", wn["sapling"]),
        textures=[(f"{theme['wood']}_leaves", f"block/{wsrc}_leaves", theme["leaves_profile"])],
    ))
    specs.append(dict(
        name=wn["sapling"], model="cross", en=theme["wood_names_en"]["sapling"],
        zh=theme["wood_names_zh"]["sapling"], loot=("self",),
        textures=[(f"{theme['wood']}_sapling", f"block/{wsrc}_sapling", theme["sapling_profile"])],
    ))

    specs.append(dict(
        name=theme["grass"], model="cross", en=theme["grass_en"], zh=theme["grass_zh"], loot=("self",),
        textures=[(theme["grass"], f"block/{theme['grass_src']}", theme["grass_profile"])],
    ))
    specs.append(dict(
        name=theme["flower"], model="cross", en=theme["flower_en"], zh=theme["flower_zh"], loot=("self",),
        textures=[(theme["flower"], f"block/{theme['flower_src']}", theme["flower_profile"])],
    ))
    return specs


# --- resource writers --------------------------------------------------------
def write_block_client(root: Path, spec) -> None:
    assets = root / "src/main/resources/assets" / MODID
    name = spec["name"]
    (assets / "textures/block").mkdir(parents=True, exist_ok=True)
    for out_tex, src_tex, profile in spec["textures"]:
        img = recolor(read_png(ZIP, src_tex), profile)
        img.save(assets / "textures/block" / f"{out_tex}.png")
    if spec["model"] == "pillar":
        side, top = spec["pillar_side"], spec["pillar_top"]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"},
        })
        write_json(assets / "models/block" / f"{name}_horizontal.json", {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"},
        })
        write_json(assets / "blockstates" / f"{name}.json", {
            "variants": {
                "axis=x": {"model": f"{MODID}:block/{name}_horizontal", "x": 90, "y": 90},
                "axis=y": {"model": f"{MODID}:block/{name}"},
                "axis=z": {"model": f"{MODID}:block/{name}_horizontal", "x": 90},
            },
        })
    elif spec["model"] == "cross":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MODID}:block/{texture}"},
        })
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif spec["model"] == "leaves":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
            "textures": {"all": f"{MODID}:block/{texture}"},
        })
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    else:  # cube_all
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"{MODID}:block/{texture}"},
        })
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})

    if spec["model"] == "cross":
        write_json(assets / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{MODID}:block/{spec['textures'][0][0]}"},
        })
    else:
        write_json(assets / "models/item" / f"{name}.json", {"parent": f"{MODID}:block/{name}"})


def write_block_loot(root: Path, spec) -> None:
    data = root / "src/main/resources/data" / MODID
    name = spec["name"]
    when = spec["loot"]
    if when[0] == "self":
        table = bd.self_loot(name)
    elif when[0] == "ore":
        table = bd.ore_loot(name, when[1])
    elif when[0] == "crystal_ore":
        table = bd.crystal_ore_loot(name, when[1])
    elif when[0] == "leaves":
        table = bd.leaves_loot(name, when[1])
    else:  # pragma: no cover
        raise ValueError(when)
    write_json(data / "loot_table/blocks" / f"{name}.json", table)


def write_crystal_item(root: Path, theme) -> None:
    assets = root / "src/main/resources/assets" / MODID
    (assets / "textures/item").mkdir(parents=True, exist_ok=True)
    img = recolor(read_png(ZIP, theme["crystal_src"]), theme["crystal_profile"])
    img.save(assets / "textures/item" / f"{theme['crystal']}.png")
    write_json(assets / "models/item" / f"{theme['crystal']}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MODID}:item/{theme['crystal']}"},
    })


# --- tags / lang -------------------------------------------------------------
def write_tags(root: Path, specs, themes) -> None:
    mc_tags = root / "src/main/resources/data/minecraft/tags"
    mod_tags = root / "src/main/resources/data" / MODID / "tags"

    pickaxe, axe, logs, leaves, small_flowers, flowers = [], [], [], [], [], []
    needs_stone, needs_iron, needs_diamond = [], [], []
    for theme in themes:
        names = [s["name"] for s in specs[theme["key"]]]
        pickaxe += [n for n in names if n.endswith("_ore") or n in (theme["base"], theme["deep"], theme["crystal_block"])]
        wn = bd.wood_block_names(theme)
        axe += [wn["log"], wn["wood"], wn["stripped_log"], wn["stripped_wood"], wn["planks"]]
        logs += [wn["log"], wn["wood"], wn["stripped_log"], wn["stripped_wood"]]
        leaves.append(wn["leaves"])
        small_flowers.append(theme["flower"])
        flowers.append(theme["flower"])
        for ore in ("copper", "lapis"):
            needs_stone += [bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)]
        for ore in ("gold", "redstone", "emerald", "diamond"):
            needs_iron += [bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)]
        needs_diamond += [bd.shallow_crystal_ore(theme), bd.deep_crystal_ore(theme)]

        merge_tag(mod_tags / "block" / f"{theme['replaceable']}.json", [f"{MODID}:{theme['base']}"])
        merge_tag(mod_tags / "block" / f"{theme['deep_replaceable']}.json", [f"{MODID}:{theme['deep']}"])

    merge_tag(mc_tags / "block/mineable/pickaxe.json", [f"{MODID}:{n}" for n in pickaxe])
    merge_tag(mc_tags / "block/mineable/axe.json", [f"{MODID}:{n}" for n in axe])
    merge_tag(mc_tags / "block/logs.json", [f"{MODID}:{n}" for n in logs])
    merge_tag(mc_tags / "block/leaves.json", [f"{MODID}:{n}" for n in leaves])
    merge_tag(mc_tags / "block/small_flowers.json", [f"{MODID}:{n}" for n in small_flowers])
    merge_tag(mc_tags / "block/flowers.json", [f"{MODID}:{n}" for n in flowers])
    merge_tag(mc_tags / "block/needs_stone_tool.json", [f"{MODID}:{n}" for n in needs_stone])
    merge_tag(mc_tags / "block/needs_iron_tool.json", [f"{MODID}:{n}" for n in needs_iron])
    merge_tag(mc_tags / "block/needs_diamond_tool.json", [f"{MODID}:{n}" for n in needs_diamond])
    merge_tag(mc_tags / "item/logs.json", [f"{MODID}:{n}" for n in logs])
    merge_tag(mc_tags / "item/planks.json", [f"{MODID}:{n}" for n in axe if n.endswith("_planks")])


def write_lang(root: Path, specs, themes) -> None:
    lang_dir = root / "src/main/resources/assets" / MODID / "lang"
    en, zh = {}, {}
    for theme in themes:
        for spec in specs[theme["key"]]:
            en[f"block.{MODID}.{spec['name']}"] = spec["en"]
            zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]
        en[f"item.{MODID}.{theme['crystal']}"] = theme["crystal_en"]
        zh[f"item.{MODID}.{theme['crystal']}"] = theme["crystal_zh"]

    for filename, table in (("en_us.json", en), ("zh_cn.json", zh)):
        path = lang_dir / filename
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        data.update(table)
        write_json(path, data)


# --- entry point -------------------------------------------------------------
def run(root: Path) -> None:
    global ZIP
    jar = find_client_jar(root)
    print(f"[gen] vanilla client jar: {jar}")
    ZIP = zipfile.ZipFile(jar)

    specs = {theme["key"]: block_specs(theme) for theme in bd.THEMES}
    total = 0
    for theme in bd.THEMES:
        for spec in specs[theme["key"]]:
            write_block_client(root, spec)
            write_block_loot(root, spec)
            total += 1
        write_crystal_item(root, theme)
    write_tags(root, specs, bd.THEMES)
    write_lang(root, specs, bd.THEMES)
    ZIP.close()
    print(f"[gen] wrote textures + resources for {total} blocks "
          f"({2 * total} block textures) under {root}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    ap.add_argument("--client-jar", default=None)
    args = ap.parse_args()
    if args.client_jar:
        os.environ["MC_CLIENT_JAR"] = args.client_jar
    run(Path(args.root).resolve())


ZIP: zipfile.ZipFile | None = None

if __name__ == "__main__":
    main()
```

- [ ] **Step 4: 运行资源生成器**

Run:
```powershell
python tools/gen_block_assets.py
```

Expected：
```
[gen] vanilla client jar: C:\...\minecraft_1.21.1_client.jar
[gen] wrote textures + resources for 60 blocks (120 block textures) under C:\...\ModDevelopGamejam
```
若报 `Could not find a vanilla client jar`，改跑 `python tools/gen_block_assets.py --client-jar "C:\Users\31087\Desktop\mc\.minecraft\libraries\net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-extra.jar"`。

- [ ] **Step 5: 校验产物数量与 JSON 合法性**

Run:
```powershell
python -c "import json,glob; [json.load(open(p,encoding='utf-8')) for p in glob.glob('src/main/resources/**/*.json',recursive=True)]; print('all json ok')"
(Get-ChildItem .\src\main\resources\assets\weather_realm\blockstates\*.json).Count
(Get-ChildItem .\src\main\resources\assets\weather_realm\models\block\*.json).Count
(Get-ChildItem .\src\main\resources\assets\weather_realm\models\item\*.json).Count
(Get-ChildItem .\src\main\resources\assets\weather_realm\textures\block\*.png).Count
```

Expected：第一行 `all json ok`；计数分别 **≥ 60、≥ 68、≥ 62、≥ 60**（含原有资源，只会更多）。

- [ ] **Step 6: 视觉抽查贴图**

Run（PowerShell here-string 管道给 `python -`）：
```powershell
@'
from PIL import Image
from pathlib import Path
b = Path("src/main/resources/assets/weather_realm/textures/block")
names = ["blaze_stone", "blaze_stone_iron_ore", "deep_blaze_stone",
         "scorchwood_log", "scorchwood_leaves", "ember_grass", "flame_flower", "ember_crystal_block",
         "weathered_sandstone", "deep_weathered_sandstone", "weathered_sandstone_iron_ore",
         "weathered_log", "weathered_leaves", "desert_grass", "desert_bloom", "sand_crystal_block"]
cols, size = 6, 48
rows = (len(names) + cols - 1) // cols
canvas = Image.new("RGBA", (cols * size, rows * size), (30, 30, 30, 255))
for i, n in enumerate(names):
    tile = Image.open(b / (n + ".png")).convert("RGBA").resize((size, size), Image.NEAREST)
    canvas.paste(tile, ((i % cols) * size, (i // cols) * size), tile)
canvas.save("texture_preview.png")
print("saved texture_preview.png")
'@ | python -
```

Expected：生成 `texture_preview.png`。人工确认：火石系为暖橙 / 深红，风化砂石系为浅棕黄，焦火木偏焦黑、风化木偏浅黄，烈焰花为红、砂兰为黄。可用 `browser.preview` 打开查看（该预览图**不**提交，检查完删除即可）。

- [ ] **Step 7: 提交生成产物**

```powershell
Remove-Item .\texture_preview.png -ErrorAction SilentlyContinue
git add tools/biome_data.py tools/gen_block_assets.py `
        src/main/resources/assets src/main/resources/data
git commit -m "feat: generate recoloured textures, models, loot and tags for new biome blocks"
```

---

## Task 4: 树木 / 植物 / 矿石地物 JSON 与群系注入

**Files:**
- Create: `tools/gen_worldgen_features.py`
- Modify: `src/main/resources/data/weather_realm/worldgen/placed_feature/frost_tree.json`
- Modify: `src/main/resources/data/weather_realm/worldgen/biome/blazing_plains.json`
- Modify: `src/main/resources/data/weather_realm/worldgen/biome/arid_wasteland.json`

**Interfaces:**
- Consumes: Task 1 的方块 ID；Task 3 的 `tools/biome_data.py`（`THEMES`、`ORE_ORDER`、`ORE_COUNT`、`ORE_MAX_Y`、`shallow_ore`、`deep_ore`、`shallow_crystal_ore`、`deep_crystal_ore`、`wood_block_names`）。
- Produces: `weather_realm:{scorchwood_tree,weathered_tree,ember_grass,flame_flower,desert_grass,desert_bloom}` 的 configured + placed feature；`weather_realm:ore_blaze_stone_*` / `ore_weathered_sandstone_*` 各 9 组（8 原版矿 + 1 专属矿）。

- [ ] **Step 1: 创建地物生成器 `tools/gen_worldgen_features.py`**

Create `tools/gen_worldgen_features.py` with exactly:

```python
#!/usr/bin/env python3
"""Generate tree / plant / ore worldgen feature JSONs for the New Realm biomes.

Usage (from the repo root):

    python tools/gen_worldgen_features.py
    python tools/gen_worldgen_features.py --root .

Writes configured + placed features under
src/main/resources/data/weather_realm/worldgen/. The biome feature arrays are
edited by hand (see the implementation plan, Task 4).
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402

MODID = bd.MODID


def write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# --- trees -------------------------------------------------------------------
def leaf_provider(block):
    return {
        "type": "minecraft:simple_state_provider",
        "state": {
            "Name": f"{MODID}:{block}",
            "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"},
        },
    }


def tree_configured(theme):
    wn = bd.wood_block_names(theme)
    if theme["tree_placer"] == "spruce":
        foliage = {
            "type": "minecraft:spruce_foliage_placer",
            "offset": {"type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 0},
            "radius": {"type": "minecraft:uniform", "max_inclusive": 3, "min_inclusive": 2},
            "trunk_height": {"type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 1},
        }
        minimum_size = {"type": "minecraft:two_layers_feature_size", "limit": 2, "lower_size": 0, "upper_size": 2}
        trunk = {"type": "minecraft:straight_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 1}
    else:
        foliage = {"type": "minecraft:blob_foliage_placer", "height": 3, "radius": 2, "offset": 0}
        minimum_size = {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1}
        trunk = {"type": "minecraft:straight_trunk_placer", "base_height": 4, "height_rand_a": 1, "height_rand_b": 1}
    return {
        "type": "minecraft:tree",
        "config": {
            "decorators": [],
            "dirt_provider": {
                "type": "minecraft:simple_state_provider",
                "state": {"Name": f"{MODID}:{theme['base']}"},
            },
            "foliage_placer": foliage,
            "foliage_provider": leaf_provider(wn["leaves"]),
            "force_dirt": False,
            "ignore_vines": True,
            "minimum_size": minimum_size,
            "trunk_placer": trunk,
            "trunk_provider": {
                "type": "minecraft:simple_state_provider",
                "state": {"Name": f"{MODID}:{wn['log']}", "Properties": {"axis": "y"}},
            },
        },
    }


def tree_placed(theme):
    return {
        "feature": f"{MODID}:{theme['wood']}_tree",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": theme["tree_rarity"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
            {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": "minecraft:biome"},
        ],
    }


# --- plants ------------------------------------------------------------------
def patch_configured(block, tries, feature_type="minecraft:random_patch"):
    return {
        "type": feature_type,
        "config": {
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {
                        "to_place": {
                            "type": "minecraft:simple_state_provider",
                            "state": {"Name": f"{MODID}:{block}"},
                        }
                    },
                },
                "placement": [
                    {
                        "type": "minecraft:block_predicate_filter",
                        "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                    }
                ],
            },
            "tries": tries,
            "xz_spread": 7,
            "y_spread": 3,
        },
    }


def grass_placed(block):
    return {
        "feature": f"{MODID}:{block}",
        "placement": [
            {"type": "minecraft:count", "count": 4},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:biome"},
        ],
    }


def flower_placed(block):
    return {
        "feature": f"{MODID}:{block}",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 8},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:biome"},
        ],
    }


# --- ores --------------------------------------------------------------------
def ore_configured(theme, shallow_name, deep_name, size=5):
    return {
        "type": "minecraft:ore",
        "config": {
            "discard_chance_on_air_exposure": 0.0,
            "size": size,
            "targets": [
                {
                    "state": {"Name": f"{MODID}:{shallow_name}"},
                    "target": {"predicate_type": "minecraft:tag_match", "tag": f"{MODID}:{theme['replaceable']}"},
                },
                {
                    "state": {"Name": f"{MODID}:{deep_name}"},
                    "target": {"predicate_type": "minecraft:tag_match", "tag": f"{MODID}:{theme['deep_replaceable']}"},
                },
            ],
        },
    }


def ore_placed(feature, count, max_y):
    return {
        "feature": f"{MODID}:{feature}",
        "placement": [
            {"type": "minecraft:count", "count": count},
            {"type": "minecraft:in_square"},
            {
                "type": "minecraft:height_range",
                "height": {
                    "type": "minecraft:trapezoid",
                    "min_inclusive": {"absolute": -64},
                    "max_inclusive": {"absolute": max_y},
                },
            },
            {"type": "minecraft:biome"},
        ],
    }


def run(root: Path) -> None:
    wg = root / "src/main/resources/data" / MODID / "worldgen"
    configured = wg / "configured_feature"
    placed = wg / "placed_feature"
    count = 0

    for theme in bd.THEMES:
        tree = f"{theme['wood']}_tree"
        write_json(configured / f"{tree}.json", tree_configured(theme))
        write_json(placed / f"{tree}.json", tree_placed(theme))
        count += 2

        write_json(configured / f"{theme['grass']}.json", patch_configured(theme["grass"], 32))
        write_json(placed / f"{theme['grass']}.json", grass_placed(theme["grass"]))
        write_json(configured / f"{theme['flower']}.json", patch_configured(theme["flower"], 64, "minecraft:flower"))
        write_json(placed / f"{theme['flower']}.json", flower_placed(theme["flower"]))
        count += 4

        for ore in bd.ORE_ORDER:
            feature = f"ore_{theme['base']}_{ore}"
            write_json(configured / f"{feature}.json",
                       ore_configured(theme, bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)))
            write_json(placed / f"{feature}.json",
                       ore_placed(feature, bd.ORE_COUNT[ore], bd.ORE_MAX_Y[ore]))
            count += 2

        feature = f"ore_{theme['base']}_{theme['crystal']}"
        write_json(configured / f"{feature}.json",
                   ore_configured(theme, bd.shallow_crystal_ore(theme), bd.deep_crystal_ore(theme)))
        write_json(placed / f"{feature}.json", ore_placed(feature, 3, 40))
        count += 2

    print(f"[gen] wrote {count} worldgen feature JSONs under {wg}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    args = ap.parse_args()
    run(Path(args.root).resolve())


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行地物生成器**

Run:
```powershell
python tools/gen_worldgen_features.py
```

Expected：`[gen] wrote 48 worldgen feature JSONs under C:\...\src\main\resources\data\weather_realm\worldgen`。

- [ ] **Step 3: 调低极寒树木频次**

把 `src/main/resources/data/weather_realm/worldgen/placed_feature/frost_tree.json` 的 `count` 从 `3` 改为 `1`。修改后文件应为：

```json
{
  "feature": "weather_realm:frost_tree",
  "placement": [
    {
      "type": "minecraft:count",
      "count": 1
    },
    {
      "type": "minecraft:in_square"
    },
    {
      "type": "minecraft:surface_water_depth_filter",
      "max_water_depth": 0
    },
    {
      "type": "minecraft:heightmap",
      "heightmap": "OCEAN_FLOOR"
    },
    {
      "type": "minecraft:biome"
    }
  ]
}
```

- [ ] **Step 4: 用注入后的内容覆盖 `blazing_plains.json`**

把 `src/main/resources/data/weather_realm/worldgen/biome/blazing_plains.json` **整体替换**为：

```json
{
  "has_precipitation": false,
  "temperature": 2.0,
  "downfall": 0.0,
  "effects": {
    "sky_color": 11141120,
    "fog_color": 16724530,
    "water_color": 16744230,
    "water_fog_color": 11153408
  },
  "spawners": {},
  "spawn_costs": {},
  "carvers": {},
  "features": [
    [],
    [],
    [],
    [],
    [],
    [],
    [
      "weather_realm:ore_blaze_stone_coal",
      "weather_realm:ore_blaze_stone_copper",
      "weather_realm:ore_blaze_stone_iron",
      "weather_realm:ore_blaze_stone_gold",
      "weather_realm:ore_blaze_stone_redstone",
      "weather_realm:ore_blaze_stone_emerald",
      "weather_realm:ore_blaze_stone_lapis",
      "weather_realm:ore_blaze_stone_diamond",
      "weather_realm:ore_blaze_stone_ember_crystal"
    ],
    [],
    [
      "weather_realm:scorchwood_tree",
      "weather_realm:ember_grass",
      "weather_realm:flame_flower"
    ],
    []
  ]
}
```

- [ ] **Step 5: 用注入后的内容覆盖 `arid_wasteland.json`**

把 `src/main/resources/data/weather_realm/worldgen/biome/arid_wasteland.json` **整体替换**为：

```json
{
  "has_precipitation": false,
  "temperature": 1.5,
  "downfall": 0.0,
  "effects": {
    "sky_color": 15784042,
    "fog_color": 14467710,
    "water_color": 4418698,
    "water_fog_color": 3426678
  },
  "spawners": {},
  "spawn_costs": {},
  "carvers": {},
  "features": [
    [],
    [],
    [],
    [],
    [],
    [],
    [
      "weather_realm:ore_weathered_sandstone_coal",
      "weather_realm:ore_weathered_sandstone_copper",
      "weather_realm:ore_weathered_sandstone_iron",
      "weather_realm:ore_weathered_sandstone_gold",
      "weather_realm:ore_weathered_sandstone_redstone",
      "weather_realm:ore_weathered_sandstone_emerald",
      "weather_realm:ore_weathered_sandstone_lapis",
      "weather_realm:ore_weathered_sandstone_diamond",
      "weather_realm:ore_weathered_sandstone_sand_crystal"
    ],
    [],
    [
      "weather_realm:weathered_tree",
      "weather_realm:desert_grass",
      "weather_realm:desert_bloom"
    ],
    []
  ]
}
```

- [ ] **Step 6: 校验地物引用闭合（每个 placed feature 都指向存在的 configured feature）**

Run:
```powershell
python -c "import json,glob,os; base='src/main/resources/data/weather_realm/worldgen'; cfg={os.path.basename(p) for p in glob.glob(base+'/configured_feature/*.json')}; miss=[p for p in glob.glob(base+'/placed_feature/*.json') if os.path.basename(json.load(open(p,encoding='utf-8'))['feature'].split(':',1)[1])+'.json' not in cfg]; print('dangling placed->configured:', miss if miss else 'none')"
python -c "import json; [json.load(open('src/main/resources/data/weather_realm/worldgen/biome/'+n,encoding='utf-8')) for n in ['blazing_plains.json','arid_wasteland.json','crystal_plains.json']]; print('biomes ok')"
```

Expected：`dangling placed->configured: none` 与 `biomes ok`。

- [ ] **Step 7: 覆盖两种矿石替换标签自检**

Run:
```powershell
Get-Content .\src\main\resources\data\weather_realm\tags\block\blaze_stone_ore_replaceables.json
Get-Content .\src\main\resources\data\weather_realm\tags\block\deep_weathered_sandstone_ore_replaceables.json
```

Expected：前者 `values` 含 `weather_realm:blaze_stone`；后者含 `weather_realm:deep_weathered_sandstone`。

- [ ] **Step 8: 提交**

```powershell
git add tools/gen_worldgen_features.py `
        src/main/resources/data/weather_realm/worldgen `
        src/main/resources/data/weather_realm/tags
git commit -m "feat: add scorchwood/weathered trees, biome plants and ore features; thin out frost trees"
```

---

## Task 5: 构建部署到 PCL 客户端并校验 jar

**Files:**
- Read-only 依赖: `deploy.ps1`
- Verify: `build/libs/weather_realm-1.21.1-*.jar` 含新类与资源

**Interfaces:**
- Consumes: Task 1–4 的全部产出。
- Produces: 注入到 `C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\` 的可联调 jar。

- [ ] **Step 1: 一键构建并注入**

Run:
```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\deploy.ps1
```

Expected：脚本输出 `[deploy] JAVA_HOME OK`（或切换到 JDK 21）、`BUILD SUCCESSFUL`、`[deploy] ===== DEPLOY OK =====`，并打印 jar 名 / 大小 / 目标目录。任一环节非零退出码会被脚本中止透传，**不得**带着报错继续。

- [ ] **Step 2: 核查 jar 内含新类与新资源**

Run:
```powershell
$jar = Get-ChildItem .\build\libs\weather_realm-*.jar |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" -and $_.Name -notlike "*-dev.jar" } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
Write-Host "Inspecting: $($jar.Name)"
& "$env:JAVA_HOME\bin\jar.exe" tf $jar.FullName | Select-String -Pattern `
    "BlazePlantBlock.class","WeatheredPlantBlock.class","ScorchwoodSaplingBlock.class","WeatheredSaplingBlock.class","ModTreeBuilder.class","blaze_stone.png","scorchwood_log.json","blazing_plains.json","ore_blaze_stone_ember_crystal.json"
```

Expected：至少命中 5 个 `.class` 与 4 个资源路径。若缺 `.class`，检查包路径 / `src/main/java`；若缺资源，检查是否写进了 `src/main/resources`（不是 `generated`）。

- [ ] **Step 3: 独立服务端洁净启动（双端隔离门禁）**

Run（启动后看到 `Done` 即按 `Ctrl+C` 停止）：
```powershell
.\gradlew.bat runServer
```

Expected：干净启动到 `Done (x.xxxs)!`，无 `ClassNotFoundException` / `NoClassDefFoundError`（证明 Common 代码未引用 `net.minecraft.client.*`）。

- [ ] **Step 4: 游戏内功能验收清单（人工）**

在 PCL 客户端内**重启**后，用 `/locate biome weather_realm:blazing_plains` 与 `weather_realm:arid_wasteland` 定位，然后逐项核对：

| # | 操作 | 预期结果 |
| :- | :- | :- |
| 1 | 进入 `blazing_plains` | 地表为橙红色 `blaze_stone`，其下为暗红 `deep_blaze_stone`（表层规则生效，需重进存档） |
| 2 | 进入 `arid_wasteland` | 地表为浅棕黄 `weathered_sandstone`，其下为较深 `deep_weathered_sandstone` |
| 3 | 在炎热 / 风沙群系 Y≈-64~40 挖矿 | 生成 `blaze_stone_*_ore` / `weathered_sandstone_*_ore` 与专属晶石矿；铁矿掉 `raw_iron`、红石掉 4–5 个 `redstone`、专属矿掉 `ember_crystal` / `sand_crystal` |
| 4 | 观察植被 | 稀疏散布 `scorchwood_tree` / `weathered_tree`（每 ~12 区块 1 棵）、`ember_grass` / `flame_flower` / `desert_grass` / `desert_bloom` |
| 5 | 手持骨粉右键 `scorchwood_sapling` / `weathered_sapling` | 长出锥形焦火木 / 风化木 |
| 6 | 走到极寒 `crystal_plains` | `frost_tree` 明显比之前稀疏（count 3 → 1） |
| 7 | 破坏 `scorchwood_leaves` | 无剪刀时概率掉 `scorchwood_sapling` + `stick`；有剪刀 / 精准采集掉叶片本体 |
| 8 | 创造模式标签 | 手工列出的 26 项（4 地质 + 2 晶石块 + 2 晶石物品 + 14 木质 + 4 植被）+ 36 个矿石物品均可取用 |
| 9 | 语言切换 en_us / zh_cn | 方块与物品显示 `Blaze Stone` / `火石`、`Scorchwood` / `焦火木` 等（切换语言或 `F3+T` 生效） |

- [ ] **Step 5: 提交（如产物有变动）**

```powershell
git status --short
```

Expected：`build/` 已被 `.gitignore` 忽略；本 Task 通常无源码改动。若 Step 4 暴露出问题：贴图 / 模型 / 语言问题回到 Task 3 改脚本重跑（`F3+T`）；worldgen / 地物问题回到 Task 4（重进存档）；Java 逻辑问题回到 Task 1 改方法体（普通类可 JBR HotSwap，但改注册项必须重启）。

---

## Self-Review

**1. 规格覆盖：**

- 目标（火石 / 风化砂石基础方块、矿物、树木、植被、晶石物品）→ Task 1 注册（4 基材、18 对矿石、2 晶石物品 + 2 晶石块、2 套木质家族、4 植被）。
- 调低所有群系树木概率（极寒降频、炎热 / 风沙更低更稀疏）→ Task 4 Step 3（`frost_tree` count 3→1）+ Step 2 的 `tree_rarity=12`（`rarity_filter`）。
- 原版换色生成模型贴图 → Task 3 脚本从客户端 jar 读取并 HSV 换色，产出 60+2 张贴图。
- 注入群系地貌 → Task 2 地表规则 + Task 4 地物 + 群系 JSON 注入。
- Python 生成 blockstates / models / loot_tables / tags / 中英文 lang → Task 3 Step 2–4。
- 树 / 植物 / 矿物地物 JSON → Task 4 Step 1–5。
- 编译部署到 PCL 并校验 jar → Task 5。

**2. 占位符扫描：** 无 `TBD` / `TODO` / “类似上文” / “补充错误处理” 等表述；每个代码步骤均给出完整可编译 / 可运行内容；命令均给出精确命令与预期输出；两个 Python 脚本已在本机以真实客户端 jar 跑通（60 方块 / 120 贴图 / 48 地物 / 全部 JSON 可解析 / 贴图视觉抽查通过）。

**3. 类型一致性：**

- Java 侧符号 `BLAZE_STONE` / `DEEP_BLAZE_STONE` / `WEATHERED_SANDSTONE` / `DEEP_WEATHERED_SANDSTONE` 在 Task 1 定义、Task 2 使用，拼写一致。
- `ModTreeBuilder.grow(ServerLevel, BlockPos, RandomSource, Supplier<Block>, Supplier<Block>)` 定义与两处调用一致；`WeatherRealm.SCORCHWOOD_LOG::get` / `WEATHERED_LOG::get` 兼容 `Supplier<Block>`。
- `codec()` 覆写统一用 `MapCodec<? extends 基类>`，避免泛型协变编译错误；`BonemealableBlock` 方法名统一为 `isValidBonemealTarget` / `isBonemealSuccess` / `performBonemeal`。
- Python 侧 `biome_data` 的 `THEMES` 键（`base/deep/crystal/crystal_block/wood/grass/flower/replaceable/deep_replaceable/wood_src/wood_profile/...`）与 `gen_block_assets.py`、`gen_worldgen_features.py` 的读取键逐一对应；`shallow_ore(theme, ore)` = `{base}_{ore}_ore`、`deep_ore` = `deep_{base}_{ore}_ore`，与 Task 1 的注册名、Task 4 的群系注入 ID、矿石替换标签完全对齐。
- 地物命名：`{wood}_tree`、`{grass}`、`{flower}`、`ore_{base}_{ore}`、`ore_{base}_{crystal}` 在生成器与两个群系 JSON 中一致。
