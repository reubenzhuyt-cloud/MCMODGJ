package com.example.weather_realm;

import java.util.ArrayList;
import java.util.List;

import com.example.weather_realm.block.AridLogBlock;
import com.example.weather_realm.block.AridSaplingBlock;
import com.example.weather_realm.block.BiomePlantBlock;
import com.example.weather_realm.block.FrostFlowerBlock;
import com.example.weather_realm.block.FrostGrassBlock;
import com.example.weather_realm.block.FrostLeavesBlock;
import com.example.weather_realm.block.FrostLogBlock;
import com.example.weather_realm.block.FrostRaspberryBushBlock;
import com.example.weather_realm.block.FrostSaplingBlock;
import com.example.weather_realm.block.FrostSproutBlock;
import com.example.weather_realm.block.GlacierBloomBlock;
import com.example.weather_realm.block.ScorchedSaplingBlock;
import com.example.weather_realm.block.TallFrostFlowerBlock;
import com.example.weather_realm.block.TallFrostGrassBlock;
import com.example.weather_realm.block.WeatherAltarCoreBlock;
import com.example.weather_realm.block.WeatherPortalBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
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
 * 方块注册 / Block registration.
 *
 * <p>Extracted from the former monolithic mod class; every block id is unchanged and the
 * declarations keep their original order. Shared property recipes live in {@link ModBlockProperties}
 * and the two climate-plant ids now share {@link BiomePlantBlock}. The matching {@code BlockItem}s
 * live in {@link ModItems}.</p>
 */
public final class ModBlocks {
    private ModBlocks() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WeatherRealm.MODID);

    // 坚冰木专属材质组 / Dedicated frost wood material set for the frost wood block family.
    public static final BlockSetType FROST_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType("frost"));
    public static final WoodType FROST_WOOD_TYPE = WoodType.register(new WoodType("frost", FROST_BLOCK_SET_TYPE));

    // --- Frost themed blocks ------------------------------------------------------------------
    // 冰晶花 / Frost Flower
    public static final DeferredBlock<FrostFlowerBlock> FROST_FLOWER = BLOCKS.registerBlock("frost_flower", FrostFlowerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));

    // 冰雪草 / Frost Grass
    public static final DeferredBlock<FrostGrassBlock> FROST_GRASS = BLOCKS.registerBlock("frost_grass", FrostGrassBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    // 冰川兰 / Glacier Bloom - a small frost orchid that also grows on grass / permafrost.
    public static final DeferredBlock<GlacierBloomBlock> GLACIER_BLOOM = BLOCKS.registerBlock("glacier_bloom", GlacierBloomBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));

    // 霜草幼芽 / Frost Sprout - short frost grass, harvested with shears.
    public static final DeferredBlock<FrostSproutBlock> FROST_SPROUT = BLOCKS.registerBlock("frost_sprout", FrostSproutBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    // 高霜冻花 / Tall Frost Flower - two blocks tall, drops itself when broken.
    public static final DeferredBlock<TallFrostFlowerBlock> TALL_FROST_FLOWER = BLOCKS.registerBlock("tall_frost_flower", TallFrostFlowerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    // 高霜草 / Tall Frost Grass - two blocks tall, harvested with shears.
    public static final DeferredBlock<TallFrostGrassBlock> TALL_FROST_GRASS = BLOCKS.registerBlock("tall_frost_grass", TallFrostGrassBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    // 坚冰木树苗 / Glacial Sapling
    public static final DeferredBlock<FrostSaplingBlock> FROST_SAPLING = BLOCKS.registerBlock("frost_sapling", FrostSaplingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));

    // 冰树莓丛 / Frost Raspberry Bush - sweet-berry bush behaviour, drops frost raspberries.
    public static final DeferredBlock<FrostRaspberryBushBlock> FROST_RASPBERRY_BUSH = BLOCKS.registerBlock("frost_raspberry_bush", FrostRaspberryBushBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .randomTicks()
                    .noCollission()
                    .sound(SoundType.SWEET_BERRY_BUSH)
                    .pushReaction(PushReaction.DESTROY));

    // 坚冰原木 / Glacial Log
    public static final DeferredBlock<FrostLogBlock> FROST_LOG = BLOCKS.registerBlock("frost_log", FrostLogBlock::new,
            ModBlockProperties.frostWoodPillar());

    // --- 坚冰木套件 / Frost wood family ---------------------------------------------------------
    // 坚冰木木板 / Frost Planks
    public static final DeferredBlock<Block> FROST_PLANKS = BLOCKS.registerSimpleBlock("frost_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());

    // 坚冰木楼梯 / Frost Stairs
    public static final DeferredBlock<StairBlock> FROST_STAIRS = BLOCKS.register("frost_stairs",
            () -> new StairBlock(FROST_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get())));

    // 坚冰木台阶 / Frost Slab
    public static final DeferredBlock<SlabBlock> FROST_SLAB = BLOCKS.register("frost_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get())));

    // 坚冰木栅栏 / Frost Fence
    public static final DeferredBlock<FenceBlock> FROST_FENCE = BLOCKS.register("frost_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get())));

    // 坚冰木栅栏门 / Frost Fence Gate
    public static final DeferredBlock<FenceGateBlock> FROST_FENCE_GATE = BLOCKS.register("frost_fence_gate",
            () -> new FenceGateBlock(FROST_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get()).forceSolidOn()));

    // 坚冰木门 / Frost Door
    public static final DeferredBlock<DoorBlock> FROST_DOOR = BLOCKS.register("frost_door",
            () -> new DoorBlock(FROST_BLOCK_SET_TYPE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    // 坚冰木活板门 / Frost Trapdoor
    public static final DeferredBlock<TrapDoorBlock> FROST_TRAPDOOR = BLOCKS.register("frost_trapdoor",
            () -> new TrapDoorBlock(FROST_BLOCK_SET_TYPE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .ignitedByLava()));

    // 坚冰木压力板 / Frost Pressure Plate
    public static final DeferredBlock<PressurePlateBlock> FROST_PRESSURE_PLATE = BLOCKS.register("frost_pressure_plate",
            () -> new PressurePlateBlock(FROST_BLOCK_SET_TYPE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .forceSolidOn()
                    .instrument(NoteBlockInstrument.BASS)
                    .noCollission()
                    .strength(0.5F)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    // 坚冰木按钮 / Frost Button
    public static final DeferredBlock<ButtonBlock> FROST_BUTTON = BLOCKS.register("frost_button",
            () -> new ButtonBlock(FROST_BLOCK_SET_TYPE, 30, BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .pushReaction(PushReaction.DESTROY)));

    // 去皮坚冰木原木 / Stripped Frost Log
    public static final DeferredBlock<FrostLogBlock> STRIPPED_FROST_LOG = BLOCKS.registerBlock("stripped_frost_log", FrostLogBlock::new,
            ModBlockProperties.frostWoodPillar());

    // 坚冰木木干（四面树皮）/ Frost Wood
    public static final DeferredBlock<FrostLogBlock> FROST_WOOD = BLOCKS.registerBlock("frost_wood", FrostLogBlock::new,
            ModBlockProperties.frostWoodPillar());

    // 去皮坚冰木木干 / Stripped Frost Wood
    public static final DeferredBlock<FrostLogBlock> STRIPPED_FROST_WOOD = BLOCKS.registerBlock("stripped_frost_wood", FrostLogBlock::new,
            ModBlockProperties.frostWoodPillar());

    // --- 天气祭坛核心 / Weather Altar Core ------------------------------------------------------
    public static final DeferredBlock<WeatherAltarCoreBlock> WEATHER_ALTAR_CORE = BLOCKS.registerBlock("weather_altar_core", WeatherAltarCoreBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    // --- 调谐基座 / Tuning Pedestal -------------------------------------------------------------
    public static final DeferredBlock<Block> WEATHER_PEDESTAL = BLOCKS.registerSimpleBlock("weather_pedestal",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE));

    // --- 气候传送门 / Weather Portal --------------------------------------------------------------
    // 无碰撞、发光的凝缩风暴 / Non-solid, glowing condensed storm; ignited inside a climate pool.
    public static final DeferredBlock<WeatherPortalBlock> WEATHER_PORTAL = BLOCKS.registerBlock("weather_portal",
            WeatherPortalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 12)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK));

    // 坚冰木树叶 / Glacial Leaves
    public static final DeferredBlock<FrostLeavesBlock> FROST_LEAVES = BLOCKS.registerBlock("frost_leaves", FrostLeavesBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));

    // --- Glacial geology blocks ----------------------------------------------------------------
    // 冻土 / Permafrost - hardness 2.25F (1.5 * 1.5), resistance 6.0F
    public static final DeferredBlock<Block> PERMAFROST = BLOCKS.registerSimpleBlock("permafrost",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.25F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());

    // 深层冻土 / Deep Permafrost - hardness 4.5F (1.5 * 3.0), resistance 6.0F
    public static final DeferredBlock<Block> DEEP_PERMAFROST = BLOCKS.registerSimpleBlock("deep_permafrost",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());

    // 冻土铁矿 / Permafrost Iron Ore
    public static final DeferredBlock<Block> PERMAFROST_IRON_ORE = BLOCKS.registerSimpleBlock("permafrost_iron_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.25F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());

    // 深层冻土铁矿 / Deep Permafrost Iron Ore
    public static final DeferredBlock<Block> DEEP_PERMAFROST_IRON_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_iron_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());

    // --- Permafrost / Deep Permafrost vanilla-adapted ores --------------------------------------
    public static final DeferredBlock<Block> PERMAFROST_COAL_ORE = BLOCKS.registerSimpleBlock("permafrost_coal_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_COAL_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_coal_ore", ModBlockProperties.deepPermafrostOre());

    public static final DeferredBlock<Block> PERMAFROST_COPPER_ORE = BLOCKS.registerSimpleBlock("permafrost_copper_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_COPPER_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_copper_ore", ModBlockProperties.deepPermafrostOre());

    public static final DeferredBlock<Block> PERMAFROST_GOLD_ORE = BLOCKS.registerSimpleBlock("permafrost_gold_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_GOLD_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_gold_ore", ModBlockProperties.deepPermafrostOre());

    public static final DeferredBlock<Block> PERMAFROST_REDSTONE_ORE = BLOCKS.registerSimpleBlock("permafrost_redstone_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_REDSTONE_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_redstone_ore", ModBlockProperties.deepPermafrostOre());

    public static final DeferredBlock<Block> PERMAFROST_EMERALD_ORE = BLOCKS.registerSimpleBlock("permafrost_emerald_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_EMERALD_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_emerald_ore", ModBlockProperties.deepPermafrostOre());

    public static final DeferredBlock<Block> PERMAFROST_LAPIS_ORE = BLOCKS.registerSimpleBlock("permafrost_lapis_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_LAPIS_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_lapis_ore", ModBlockProperties.deepPermafrostOre());

    public static final DeferredBlock<Block> PERMAFROST_DIAMOND_ORE = BLOCKS.registerSimpleBlock("permafrost_diamond_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_DIAMOND_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_diamond_ore", ModBlockProperties.deepPermafrostOre());

    // --- Blizzard Crystal suite -----------------------------------------------------------------
    public static final DeferredBlock<Block> BLIZZARD_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("blizzard_crystal_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.5F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> PERMAFROST_BLIZZARD_CRYSTAL_ORE = BLOCKS.registerSimpleBlock("permafrost_blizzard_crystal_ore", ModBlockProperties.permafrostOre());
    public static final DeferredBlock<Block> DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_blizzard_crystal_ore", ModBlockProperties.deepPermafrostOre());

    // --- Frost animal drops ---------------------------------------------------------------------
    public static final DeferredBlock<Block> FROST_WOOL = BLOCKS.registerSimpleBlock("frost_wool",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.8F, 0.8F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava());

    // ============================================================================================
    // 三大群系地表覆盖 / Biome surface covers
    // ============================================================================================
    // 耐寒苔藓 / Frost Moss - the topsoil layer of the frozen biome.
    public static final DeferredBlock<Block> FROST_MOSS = BLOCKS.registerSimpleBlock("frost_moss",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6F)
                    .sound(SoundType.MOSS));

    // 干草坪 / Dry Turf - the topsoil layer of the arid biome.
    public static final DeferredBlock<Block> DRY_TURF = BLOCKS.registerSimpleBlock("dry_turf",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.6F)
                    .sound(SoundType.GRASS));

    // 火山灰 / Volcanic Ash - the topsoil layer of the blazing biome.
    public static final DeferredBlock<Block> VOLCANIC_ASH = BLOCKS.registerSimpleBlock("volcanic_ash",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5F)
                    .sound(SoundType.SAND));

    // ============================================================================================
    // 炎热 / 风沙 群系内容 / Blazing & arid biome content
    // ============================================================================================
    // 火石 / Fire stone geology
    public static final DeferredBlock<Block> FIRE_STONE = BLOCKS.registerSimpleBlock("fire_stone", ModBlockProperties.fireStone());
    public static final DeferredBlock<Block> DEEP_FIRE_STONE = BLOCKS.registerSimpleBlock("deep_fire_stone", ModBlockProperties.deepFireStone());

    // 风化砂石 / Weathered sandstone geology
    public static final DeferredBlock<Block> WEATHERED_SANDSTONE = BLOCKS.registerSimpleBlock("weathered_sandstone", ModBlockProperties.weatheredSandstone());
    public static final DeferredBlock<Block> DEEP_WEATHERED_SANDSTONE = BLOCKS.registerSimpleBlock("deep_weathered_sandstone", ModBlockProperties.deepWeatheredSandstone());

    // 烈火晶石 / 风沙晶石 - decorative blocks (items live in ModItems)
    public static final DeferredBlock<Block> BLAZE_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("blaze_crystal_block", ModBlockProperties.crystalBlock());
    public static final DeferredBlock<Block> WIND_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("wind_crystal_block", ModBlockProperties.crystalBlock());

    // 矿石批量注册 / Generated ore family (shallow + deep); the BlockItems are created in ModItems
    // from this list so the two registries stay free of a load-order cycle.
    public record OreBlock(String name, DeferredBlock<Block> block) {
    }

    public static final List<OreBlock> GENERATED_ORES = new ArrayList<>();

    private static void registerOrePair(String shallowName, String deepName,
                                        BlockBehaviour.Properties shallowProps,
                                        BlockBehaviour.Properties deepProps) {
        GENERATED_ORES.add(new OreBlock(shallowName, BLOCKS.registerSimpleBlock(shallowName, shallowProps)));
        GENERATED_ORES.add(new OreBlock(deepName, BLOCKS.registerSimpleBlock(deepName, deepProps)));
    }

    static {
        String[] vanillaOres = {"coal", "copper", "iron", "gold", "redstone", "emerald", "lapis", "diamond"};
        for (String ore : vanillaOres) {
            registerOrePair("fire_stone_" + ore + "_ore", "deep_fire_stone_" + ore + "_ore",
                    ModBlockProperties.fireStone(), ModBlockProperties.deepFireStone());
            registerOrePair("weathered_sandstone_" + ore + "_ore", "deep_weathered_sandstone_" + ore + "_ore",
                    ModBlockProperties.weatheredSandstone(), ModBlockProperties.deepWeatheredSandstone());
        }
        registerOrePair("fire_stone_blaze_crystal_ore", "deep_fire_stone_blaze_crystal_ore",
                ModBlockProperties.fireStone(), ModBlockProperties.deepFireStone());
        registerOrePair("weathered_sandstone_wind_crystal_ore", "deep_weathered_sandstone_wind_crystal_ore",
                ModBlockProperties.weatheredSandstone(), ModBlockProperties.deepWeatheredSandstone());
    }

    // --- 焦木 / Scorched wood (blaze biome tree family) -----------------------------------------
    public static final DeferredBlock<FrostLogBlock> SCORCHED_LOG = BLOCKS.registerBlock("scorched_log", FrostLogBlock::new, ModBlockProperties.scorchedWoodPillar());
    public static final DeferredBlock<FrostLogBlock> SCORCHED_WOOD = BLOCKS.registerBlock("scorched_wood", FrostLogBlock::new, ModBlockProperties.scorchedWoodPillar());
    public static final DeferredBlock<FrostLogBlock> STRIPPED_SCORCHED_LOG = BLOCKS.registerBlock("stripped_scorched_log", FrostLogBlock::new, ModBlockProperties.scorchedWoodPillar());

    public static final DeferredBlock<FrostLeavesBlock> SCORCHED_LEAVES = BLOCKS.registerBlock("scorched_leaves", FrostLeavesBlock::new,
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

    // 焦木树苗 / Scorched Sapling
    public static final DeferredBlock<ScorchedSaplingBlock> SCORCHED_SAPLING = BLOCKS.registerBlock("scorched_sapling", ScorchedSaplingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));

    // --- 风化木 / Arid wood (arid biome tree family) --------------------------------------------
    public static final DeferredBlock<AridLogBlock> ARID_LOG = BLOCKS.registerBlock("arid_log", AridLogBlock::new, ModBlockProperties.aridWoodPillar());
    public static final DeferredBlock<AridLogBlock> ARID_WOOD = BLOCKS.registerBlock("arid_wood", AridLogBlock::new, ModBlockProperties.aridWoodPillar());
    public static final DeferredBlock<AridLogBlock> STRIPPED_ARID_LOG = BLOCKS.registerBlock("stripped_arid_log", AridLogBlock::new, ModBlockProperties.aridWoodPillar());

    public static final DeferredBlock<FrostLeavesBlock> ARID_LEAVES = BLOCKS.registerBlock("arid_leaves", FrostLeavesBlock::new,
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

    // 风化树苗 / Arid Sapling
    public static final DeferredBlock<AridSaplingBlock> ARID_SAPLING = BLOCKS.registerBlock("arid_sapling", AridSaplingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));

    // --- 植被 / Vegetation ----------------------------------------------------------------------
    public static final DeferredBlock<BiomePlantBlock> CINDER_BLOOM = BLOCKS.registerBlock("cinder_bloom",
            properties -> new BiomePlantBlock(properties, BiomePlantBlock.Ground.BLAZING), ModBlockProperties.blazingPlant());
    public static final DeferredBlock<BiomePlantBlock> FLAME_SPROUT = BLOCKS.registerBlock("flame_sprout",
            properties -> new BiomePlantBlock(properties, BiomePlantBlock.Ground.BLAZING), ModBlockProperties.blazingPlant());
    public static final DeferredBlock<BiomePlantBlock> FIRE_FLOWER = BLOCKS.registerBlock("fire_flower",
            properties -> new BiomePlantBlock(properties, BiomePlantBlock.Ground.BLAZING), ModBlockProperties.blazingPlant());

    public static final DeferredBlock<BiomePlantBlock> DUNE_FLOWER = BLOCKS.registerBlock("dune_flower",
            properties -> new BiomePlantBlock(properties, BiomePlantBlock.Ground.ARID), ModBlockProperties.aridPlant());
    public static final DeferredBlock<BiomePlantBlock> WIND_SPROUT = BLOCKS.registerBlock("wind_sprout",
            properties -> new BiomePlantBlock(properties, BiomePlantBlock.Ground.ARID), ModBlockProperties.aridPlant());
    public static final DeferredBlock<BiomePlantBlock> ARID_BUSH = BLOCKS.registerBlock("arid_bush",
            properties -> new BiomePlantBlock(properties, BiomePlantBlock.Ground.ARID), ModBlockProperties.aridPlant());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
