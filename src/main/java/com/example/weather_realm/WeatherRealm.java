package com.example.weather_realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.weather_realm.block.AridPlantBlock;
import com.example.weather_realm.block.FirePlantBlock;
import com.example.weather_realm.block.FrostFlowerBlock;
import com.example.weather_realm.block.FrostGrassBlock;
import com.example.weather_realm.block.FrostLeavesBlock;
import com.example.weather_realm.block.FrostLogBlock;
import com.example.weather_realm.block.FrostSaplingBlock;
import com.example.weather_realm.block.FrostSproutBlock;
import com.example.weather_realm.block.GlacierBloomBlock;
import com.example.weather_realm.block.TallFrostFlowerBlock;
import com.example.weather_realm.block.TallFrostGrassBlock;
import com.example.weather_realm.block.WeatherAltarCoreBlock;
import com.example.weather_realm.block.WeatherAltarCoreBlockEntity;
import com.example.weather_realm.block.WeatherPortalBlock;
import com.example.weather_realm.item.AncientWeatherTomeItem;
import com.example.weather_realm.item.BiomeMapItem;
import com.example.weather_realm.item.ClimateShardItem;
import com.example.weather_realm.item.FrostMilkBucketItem;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import com.example.weather_realm.world.FrostVillageStructure;
import com.example.weather_realm.world.FrostWoodProcessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WeatherRealm.MODID)
public class WeatherRealm {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "weather_realm";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "weather_realm" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "weather_realm" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "weather_realm" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold StructureTypes which will all be registered under the "weather_realm" namespace
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    // Create a Deferred Register to hold StructureProcessorTypes which will all be registered under the "weather_realm" namespace
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "weather_realm" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // 坚冰木专属材质组 / Dedicated frost wood material set for the frost wood block family.
    public static final BlockSetType FROST_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType("frost"));
    public static final WoodType FROST_WOOD_TYPE = WoodType.register(new WoodType("frost", FROST_BLOCK_SET_TYPE));

    // 坚冰木村庄结构类型 / Custom jigsaw structure that frost-ifies the snowy village.
    public static final DeferredHolder<StructureType<?>, StructureType<FrostVillageStructure>> FROST_VILLAGE_STRUCTURE =
            STRUCTURE_TYPES.register("frost_village", () -> () -> FrostVillageStructure.CODEC);
    // 坚冰木替换结构处理器 / Structure processor swapping spruce blocks for frost wood.
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FrostWoodProcessor>> FROST_WOOD_PROCESSOR =
            STRUCTURE_PROCESSORS.register("frost_wood_replace", () -> () -> FrostWoodProcessor.CODEC);

    // (The MDK example_block / example_block_item / example_item were removed - this is now the Glacial Realm mod.)

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
    public static final DeferredItem<BlockItem> FROST_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("frost_flower", FROST_FLOWER);

    // 冰雪草 / Frost Grass
    public static final DeferredBlock<FrostGrassBlock> FROST_GRASS = BLOCKS.registerBlock("frost_grass", FrostGrassBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> FROST_GRASS_ITEM = ITEMS.registerSimpleBlockItem("frost_grass", FROST_GRASS);

    // 冰川兰 / Glacier Bloom - a small frost orchid that also grows on grass / permafrost.
    public static final DeferredBlock<GlacierBloomBlock> GLACIER_BLOOM = BLOCKS.registerBlock("glacier_bloom", GlacierBloomBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> GLACIER_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("glacier_bloom", GLACIER_BLOOM);

    // 霜草幼芽 / Frost Sprout - short frost grass, harvested with shears.
    public static final DeferredBlock<FrostSproutBlock> FROST_SPROUT = BLOCKS.registerBlock("frost_sprout", FrostSproutBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> FROST_SPROUT_ITEM = ITEMS.registerSimpleBlockItem("frost_sprout", FROST_SPROUT);

    // 高霜冻花 / Tall Frost Flower - two blocks tall, drops itself when broken.
    public static final DeferredBlock<TallFrostFlowerBlock> TALL_FROST_FLOWER = BLOCKS.registerBlock("tall_frost_flower", TallFrostFlowerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> TALL_FROST_FLOWER_ITEM = ITEMS.register("tall_frost_flower",
            () -> new DoubleHighBlockItem(TALL_FROST_FLOWER.get(), new Item.Properties()));

    // 高霜草 / Tall Frost Grass - two blocks tall, harvested with shears.
    public static final DeferredBlock<TallFrostGrassBlock> TALL_FROST_GRASS = BLOCKS.registerBlock("tall_frost_grass", TallFrostGrassBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> TALL_FROST_GRASS_ITEM = ITEMS.register("tall_frost_grass",
            () -> new DoubleHighBlockItem(TALL_FROST_GRASS.get(), new Item.Properties()));

    // 坚冰木树苗 / Glacial Sapling
    public static final DeferredBlock<FrostSaplingBlock> FROST_SAPLING = BLOCKS.registerBlock("frost_sapling", FrostSaplingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> FROST_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("frost_sapling", FROST_SAPLING);

    // 坚冰原木 / Glacial Log
    public static final DeferredBlock<FrostLogBlock> FROST_LOG = BLOCKS.registerBlock("frost_log", FrostLogBlock::new,
            frostWoodPillarProperties());
    public static final DeferredItem<BlockItem> FROST_LOG_ITEM = ITEMS.registerSimpleBlockItem("frost_log", FROST_LOG);

    // --- 坚冰木套件 / Frost wood family ---------------------------------------------------------
    // 坚冰木木板 / Frost Planks
    public static final DeferredBlock<Block> FROST_PLANKS = BLOCKS.registerSimpleBlock("frost_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredItem<BlockItem> FROST_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("frost_planks", FROST_PLANKS);

    // 坚冰木楼梯 / Frost Stairs
    public static final DeferredBlock<StairBlock> FROST_STAIRS = BLOCKS.register("frost_stairs",
            () -> new StairBlock(FROST_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get())));
    public static final DeferredItem<BlockItem> FROST_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("frost_stairs", FROST_STAIRS);

    // 坚冰木台阶 / Frost Slab
    public static final DeferredBlock<SlabBlock> FROST_SLAB = BLOCKS.register("frost_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get())));
    public static final DeferredItem<BlockItem> FROST_SLAB_ITEM = ITEMS.registerSimpleBlockItem("frost_slab", FROST_SLAB);

    // 坚冰木栅栏 / Frost Fence
    public static final DeferredBlock<FenceBlock> FROST_FENCE = BLOCKS.register("frost_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get())));
    public static final DeferredItem<BlockItem> FROST_FENCE_ITEM = ITEMS.registerSimpleBlockItem("frost_fence", FROST_FENCE);

    // 坚冰木栅栏门 / Frost Fence Gate
    public static final DeferredBlock<FenceGateBlock> FROST_FENCE_GATE = BLOCKS.register("frost_fence_gate",
            () -> new FenceGateBlock(FROST_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(FROST_PLANKS.get()).forceSolidOn()));
    public static final DeferredItem<BlockItem> FROST_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem("frost_fence_gate", FROST_FENCE_GATE);

    // 坚冰木门 / Frost Door
    public static final DeferredBlock<DoorBlock> FROST_DOOR = BLOCKS.register("frost_door",
            () -> new DoorBlock(FROST_BLOCK_SET_TYPE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> FROST_DOOR_ITEM = ITEMS.register("frost_door",
            () -> new DoubleHighBlockItem(FROST_DOOR.get(), new Item.Properties()));

    // 坚冰木活板门 / Frost Trapdoor
    public static final DeferredBlock<TrapDoorBlock> FROST_TRAPDOOR = BLOCKS.register("frost_trapdoor",
            () -> new TrapDoorBlock(FROST_BLOCK_SET_TYPE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .ignitedByLava()));
    public static final DeferredItem<BlockItem> FROST_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem("frost_trapdoor", FROST_TRAPDOOR);

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
    public static final DeferredItem<BlockItem> FROST_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("frost_pressure_plate", FROST_PRESSURE_PLATE);

    // 坚冰木按钮 / Frost Button
    public static final DeferredBlock<ButtonBlock> FROST_BUTTON = BLOCKS.register("frost_button",
            () -> new ButtonBlock(FROST_BLOCK_SET_TYPE, 30, BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> FROST_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("frost_button", FROST_BUTTON);

    // 去皮坚冰木原木 / Stripped Frost Log
    public static final DeferredBlock<FrostLogBlock> STRIPPED_FROST_LOG = BLOCKS.registerBlock("stripped_frost_log", FrostLogBlock::new,
            frostWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_FROST_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_frost_log", STRIPPED_FROST_LOG);

    // 坚冰木木干（四面树皮）/ Frost Wood
    public static final DeferredBlock<FrostLogBlock> FROST_WOOD = BLOCKS.registerBlock("frost_wood", FrostLogBlock::new,
            frostWoodPillarProperties());
    public static final DeferredItem<BlockItem> FROST_WOOD_ITEM = ITEMS.registerSimpleBlockItem("frost_wood", FROST_WOOD);

    // 去皮坚冰木木干 / Stripped Frost Wood
    public static final DeferredBlock<FrostLogBlock> STRIPPED_FROST_WOOD = BLOCKS.registerBlock("stripped_frost_wood", FrostLogBlock::new,
            frostWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_FROST_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_frost_wood", STRIPPED_FROST_WOOD);

    // --- 天气祭坛核心 / Weather Altar Core ------------------------------------------------------
    public static final DeferredBlock<WeatherAltarCoreBlock> WEATHER_ALTAR_CORE = BLOCKS.registerBlock("weather_altar_core", WeatherAltarCoreBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> WEATHER_ALTAR_CORE_ITEM = ITEMS.registerSimpleBlockItem("weather_altar_core", WEATHER_ALTAR_CORE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeatherAltarCoreBlockEntity>> WEATHER_ALTAR_CORE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("weather_altar_core", () -> BlockEntityType.Builder.of(WeatherAltarCoreBlockEntity::new, WEATHER_ALTAR_CORE.get()).build(null));

    // --- 调谐基座 / Tuning Pedestal -------------------------------------------------------------
    public static final DeferredBlock<Block> WEATHER_PEDESTAL = BLOCKS.registerSimpleBlock("weather_pedestal",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> WEATHER_PEDESTAL_ITEM = ITEMS.registerSimpleBlockItem("weather_pedestal", WEATHER_PEDESTAL);

    // --- 气候碎片 / Climate Shard -----------------------------------------------------------------
    // 击碎天气祭坛核心所得 / Harvested by shattering a weather altar core; the weather-gate key.
    public static final DeferredItem<ClimateShardItem> CLIMATE_SHARD = ITEMS.register("climate_shard",
            () -> new ClimateShardItem(new Item.Properties()));

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

    // --- 《古代天气研究手记》/ Ancient Weather Tome ---------------------------------------------
    public static final DeferredItem<AncientWeatherTomeItem> ANCIENT_WEATHER_TOME = ITEMS.register("ancient_weather_tome",
            () -> new AncientWeatherTomeItem(new Item.Properties().stacksTo(1)));

    // --- 群系地图 / Biome Map --------------------------------------------------------------------
    // 极域天象图 / A vanilla-map-derived star-chart: extends MapItem to reuse the two-handed pose
    // and the MapRenderer pixel pipeline (see BiomeMapClientData).
    public static final DeferredItem<BiomeMapItem> BIOME_MAP = ITEMS.register("biome_map",
            () -> new BiomeMapItem(BiomeMapItem.defaultProperties()));

    // --- 群系专属 BGM / Biome background music ---------------------------------------------------
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CRYSTAL_PLAINS = SOUND_EVENTS.register(
            "music.biome.crystal_plains",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "music.biome.crystal_plains")));

    private static BlockBehaviour.Properties frostWoodPillarProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

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
    public static final DeferredItem<BlockItem> FROST_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("frost_leaves", FROST_LEAVES);

    // --- Glacial geology blocks ----------------------------------------------------------------
    // 冻土 / Permafrost - hardness 2.25F (1.5 * 1.5), resistance 6.0F
    public static final DeferredBlock<Block> PERMAFROST = BLOCKS.registerSimpleBlock("permafrost",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.25F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> PERMAFROST_ITEM = ITEMS.registerSimpleBlockItem("permafrost", PERMAFROST);

    // 深层冻土 / Deep Permafrost - hardness 4.5F (1.5 * 3.0), resistance 6.0F
    public static final DeferredBlock<Block> DEEP_PERMAFROST = BLOCKS.registerSimpleBlock("deep_permafrost",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost", DEEP_PERMAFROST);

    // 冻土铁矿 / Permafrost Iron Ore
    public static final DeferredBlock<Block> PERMAFROST_IRON_ORE = BLOCKS.registerSimpleBlock("permafrost_iron_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.25F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> PERMAFROST_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_iron_ore", PERMAFROST_IRON_ORE);

    // 深层冻土铁矿 / Deep Permafrost Iron Ore
    public static final DeferredBlock<Block> DEEP_PERMAFROST_IRON_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_iron_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_iron_ore", DEEP_PERMAFROST_IRON_ORE);

    // --- Permafrost / Deep Permafrost vanilla-adapted ores --------------------------------------
    private static BlockBehaviour.Properties permafrostOreProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.25F, 6.0F)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties deepPermafrostOreProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(4.5F, 6.0F)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<Block> PERMAFROST_COAL_ORE = BLOCKS.registerSimpleBlock("permafrost_coal_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_COAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_coal_ore", PERMAFROST_COAL_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_COAL_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_coal_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_COAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_coal_ore", DEEP_PERMAFROST_COAL_ORE);

    public static final DeferredBlock<Block> PERMAFROST_COPPER_ORE = BLOCKS.registerSimpleBlock("permafrost_copper_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_COPPER_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_copper_ore", PERMAFROST_COPPER_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_COPPER_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_copper_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_COPPER_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_copper_ore", DEEP_PERMAFROST_COPPER_ORE);

    public static final DeferredBlock<Block> PERMAFROST_GOLD_ORE = BLOCKS.registerSimpleBlock("permafrost_gold_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_GOLD_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_gold_ore", PERMAFROST_GOLD_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_GOLD_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_gold_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_GOLD_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_gold_ore", DEEP_PERMAFROST_GOLD_ORE);

    public static final DeferredBlock<Block> PERMAFROST_REDSTONE_ORE = BLOCKS.registerSimpleBlock("permafrost_redstone_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_REDSTONE_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_redstone_ore", PERMAFROST_REDSTONE_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_REDSTONE_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_redstone_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_REDSTONE_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_redstone_ore", DEEP_PERMAFROST_REDSTONE_ORE);

    public static final DeferredBlock<Block> PERMAFROST_EMERALD_ORE = BLOCKS.registerSimpleBlock("permafrost_emerald_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_EMERALD_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_emerald_ore", PERMAFROST_EMERALD_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_EMERALD_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_emerald_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_EMERALD_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_emerald_ore", DEEP_PERMAFROST_EMERALD_ORE);

    public static final DeferredBlock<Block> PERMAFROST_LAPIS_ORE = BLOCKS.registerSimpleBlock("permafrost_lapis_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_LAPIS_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_lapis_ore", PERMAFROST_LAPIS_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_LAPIS_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_lapis_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_LAPIS_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_lapis_ore", DEEP_PERMAFROST_LAPIS_ORE);

    public static final DeferredBlock<Block> PERMAFROST_DIAMOND_ORE = BLOCKS.registerSimpleBlock("permafrost_diamond_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_DIAMOND_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_diamond_ore", PERMAFROST_DIAMOND_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_DIAMOND_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_diamond_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_DIAMOND_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_diamond_ore", DEEP_PERMAFROST_DIAMOND_ORE);

    // --- Blizzard Crystal suite -----------------------------------------------------------------
    public static final DeferredItem<Item> BLIZZARD_CRYSTAL = ITEMS.registerSimpleItem("blizzard_crystal");

    public static final DeferredBlock<Block> BLIZZARD_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("blizzard_crystal_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.5F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> BLIZZARD_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("blizzard_crystal_block", BLIZZARD_CRYSTAL_BLOCK);

    public static final DeferredBlock<Block> PERMAFROST_BLIZZARD_CRYSTAL_ORE = BLOCKS.registerSimpleBlock("permafrost_blizzard_crystal_ore", permafrostOreProperties());
    public static final DeferredItem<BlockItem> PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_blizzard_crystal_ore", PERMAFROST_BLIZZARD_CRYSTAL_ORE);
    public static final DeferredBlock<Block> DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE = BLOCKS.registerSimpleBlock("deep_permafrost_blizzard_crystal_ore", deepPermafrostOreProperties());
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_blizzard_crystal_ore", DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE);

    // Armor material (datapack-backed ARMOR_MATERIAL registry).
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLIZZARD_CRYSTAL_MATERIAL = ARMOR_MATERIALS.register("blizzard_crystal",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 3,
                            ArmorItem.Type.LEGGINGS, 6,
                            ArmorItem.Type.CHESTPLATE, 8,
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.BODY, 9),
                    15,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(BLIZZARD_CRYSTAL.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "blizzard_crystal"))),
                    2.5F,
                    0.1F));

    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_HELMET = ITEMS.register("blizzard_crystal_helmet",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(40))));
    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_CHESTPLATE = ITEMS.register("blizzard_crystal_chestplate",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(40))));
    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_LEGGINGS = ITEMS.register("blizzard_crystal_leggings",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(40))));
    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_BOOTS = ITEMS.register("blizzard_crystal_boots",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(40))));

    // Custom tool tier (diamond-incorrect blocks, 1650 uses, 8.5 speed, +3.5 damage, 16 enchantability).
    public static final Tier BLIZZARD_CRYSTAL_TIER = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1650, 8.5F, 3.5F, 16,
            () -> Ingredient.of(BLIZZARD_CRYSTAL.get()));

    public static final DeferredItem<SwordItem> BLIZZARD_CRYSTAL_SWORD = ITEMS.register("blizzard_crystal_sword",
            () -> new SwordItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> BLIZZARD_CRYSTAL_PICKAXE = ITEMS.register("blizzard_crystal_pickaxe",
            () -> new PickaxeItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(PickaxeItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> BLIZZARD_CRYSTAL_AXE = ITEMS.register("blizzard_crystal_axe",
            () -> new AxeItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(AxeItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> BLIZZARD_CRYSTAL_SHOVEL = ITEMS.register("blizzard_crystal_shovel",
            () -> new ShovelItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(ShovelItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> BLIZZARD_CRYSTAL_HOE = ITEMS.register("blizzard_crystal_hoe",
            () -> new HoeItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(HoeItem.createAttributes(BLIZZARD_CRYSTAL_TIER, -3.0F, 0.0F))));

    // --- Entity spawn eggs ----------------------------------------------------------------------
    public static final DeferredItem<SpawnEggItem> FROST_SHEEP_SPAWN_EGG = ITEMS.register("frost_sheep_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_SHEEP, 0xA0E0F0, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> FROST_COW_SPAWN_EGG = ITEMS.register("frost_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_COW, 0x5F402A, 0xE8F4FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> FROST_PIG_SPAWN_EGG = ITEMS.register("frost_pig_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_PIG, 0xF0B4C0, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> FROST_CAT_SPAWN_EGG = ITEMS.register("frost_cat_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_CAT, 0xE8F0F8, 0x5A6A7A, new Item.Properties()));

    // --- Frost animal drops ---------------------------------------------------------------------
    public static final DeferredBlock<Block> FROST_WOOL = BLOCKS.registerSimpleBlock("frost_wool",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.8F, 0.8F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava());
    public static final DeferredItem<BlockItem> FROST_WOOL_ITEM = ITEMS.registerSimpleBlockItem("frost_wool", FROST_WOOL);

    public static final DeferredItem<Item> FROST_MUTTON = ITEMS.registerSimpleItem("frost_mutton",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_FROST_MUTTON = ITEMS.registerSimpleItem("cooked_frost_mutton",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FROST_BEEF = ITEMS.registerSimpleItem("frost_beef",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_FROST_BEEF = ITEMS.registerSimpleItem("cooked_frost_beef",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FROST_LEATHER = ITEMS.registerSimpleItem("frost_leather");
    public static final DeferredItem<Item> FROST_PORKCHOP = ITEMS.registerSimpleItem("frost_porkchop",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_FROST_PORKCHOP = ITEMS.registerSimpleItem("cooked_frost_porkchop",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FROST_PELT = ITEMS.registerSimpleItem("frost_pelt");

    // 冰树莓 / Frost Raspberry - a frosty berry snack matching vanilla sweet berries.
    public static final DeferredItem<Item> FROST_RASPBERRY = ITEMS.registerSimpleItem("frost_raspberry",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()));

    // 冰牛奶桶 / Frost Milk Bucket - obtained by milking a frost cow with an empty bucket.
    public static final DeferredItem<MilkBucketItem> FROST_MILK_BUCKET = ITEMS.register("frost_milk_bucket",
            () -> new FrostMilkBucketItem(new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ============================================================================================
    // 三大群系地表覆盖 / Biome surface covers
    // ============================================================================================
    // 耐寒苔藓 / Frost Moss - the topsoil layer of the frozen biome.
    public static final DeferredBlock<Block> FROST_MOSS = BLOCKS.registerSimpleBlock("frost_moss",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6F)
                    .sound(SoundType.MOSS));
    public static final DeferredItem<BlockItem> FROST_MOSS_ITEM = ITEMS.registerSimpleBlockItem("frost_moss", FROST_MOSS);

    // 干草坪 / Dry Turf - the topsoil layer of the arid biome.
    public static final DeferredBlock<Block> DRY_TURF = BLOCKS.registerSimpleBlock("dry_turf",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.6F)
                    .sound(SoundType.GRASS));
    public static final DeferredItem<BlockItem> DRY_TURF_ITEM = ITEMS.registerSimpleBlockItem("dry_turf", DRY_TURF);

    // 火山灰 / Volcanic Ash - the topsoil layer of the blazing biome.
    public static final DeferredBlock<Block> VOLCANIC_ASH = BLOCKS.registerSimpleBlock("volcanic_ash",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5F)
                    .sound(SoundType.SAND));
    public static final DeferredItem<BlockItem> VOLCANIC_ASH_ITEM = ITEMS.registerSimpleBlockItem("volcanic_ash", VOLCANIC_ASH);

    // ============================================================================================
    // 炎热 / 风沙 群系内容 / Blazing & arid biome content
    // ============================================================================================

    // 火石 / Blaze (fire) stone geology
    private static BlockBehaviour.Properties fireStoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties deepFireStoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .strength(4.0F, 6.0F)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops();
    }

    // 风化砂石 / Weathered sandstone geology
    private static BlockBehaviour.Properties weatheredSandstoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(1.8F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties deepWeatheredSandstoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_ORANGE)
                .strength(3.6F, 6.0F)
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

    private static BlockBehaviour.Properties scorchedWoodPillarProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties aridWoodPillarProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties firePlantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties aridPlantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    // 火石 / Fire stone geology
    public static final DeferredBlock<Block> FIRE_STONE = BLOCKS.registerSimpleBlock("fire_stone", fireStoneProperties());
    public static final DeferredItem<BlockItem> FIRE_STONE_ITEM = ITEMS.registerSimpleBlockItem("fire_stone", FIRE_STONE);
    public static final DeferredBlock<Block> DEEP_FIRE_STONE = BLOCKS.registerSimpleBlock("deep_fire_stone", deepFireStoneProperties());
    public static final DeferredItem<BlockItem> DEEP_FIRE_STONE_ITEM = ITEMS.registerSimpleBlockItem("deep_fire_stone", DEEP_FIRE_STONE);

    // 风化砂石 / Weathered sandstone geology
    public static final DeferredBlock<Block> WEATHERED_SANDSTONE = BLOCKS.registerSimpleBlock("weathered_sandstone", weatheredSandstoneProperties());
    public static final DeferredItem<BlockItem> WEATHERED_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem("weathered_sandstone", WEATHERED_SANDSTONE);
    public static final DeferredBlock<Block> DEEP_WEATHERED_SANDSTONE = BLOCKS.registerSimpleBlock("deep_weathered_sandstone", deepWeatheredSandstoneProperties());
    public static final DeferredItem<BlockItem> DEEP_WEATHERED_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem("deep_weathered_sandstone", DEEP_WEATHERED_SANDSTONE);

    // 烈火晶石 / 风沙晶石 - items + decorative blocks
    public static final DeferredItem<Item> BLAZE_CRYSTAL = ITEMS.registerSimpleItem("blaze_crystal");
    public static final DeferredBlock<Block> BLAZE_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("blaze_crystal_block", crystalBlockProperties());
    public static final DeferredItem<BlockItem> BLAZE_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("blaze_crystal_block", BLAZE_CRYSTAL_BLOCK);
    public static final DeferredItem<Item> WIND_CRYSTAL = ITEMS.registerSimpleItem("wind_crystal");
    public static final DeferredBlock<Block> WIND_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("wind_crystal_block", crystalBlockProperties());
    public static final DeferredItem<BlockItem> WIND_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("wind_crystal_block", WIND_CRYSTAL_BLOCK);

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
            registerOrePair("fire_stone_" + ore + "_ore", "deep_fire_stone_" + ore + "_ore",
                    fireStoneProperties(), deepFireStoneProperties());
            registerOrePair("weathered_sandstone_" + ore + "_ore", "deep_weathered_sandstone_" + ore + "_ore",
                    weatheredSandstoneProperties(), deepWeatheredSandstoneProperties());
        }
        registerOrePair("fire_stone_blaze_crystal_ore", "deep_fire_stone_blaze_crystal_ore",
                fireStoneProperties(), deepFireStoneProperties());
        registerOrePair("weathered_sandstone_wind_crystal_ore", "deep_weathered_sandstone_wind_crystal_ore",
                weatheredSandstoneProperties(), deepWeatheredSandstoneProperties());
    }

    // --- 焦木 / Scorched wood (blaze biome tree family) -----------------------------------------
    public static final DeferredBlock<FrostLogBlock> SCORCHED_LOG = BLOCKS.registerBlock("scorched_log", FrostLogBlock::new, scorchedWoodPillarProperties());
    public static final DeferredItem<BlockItem> SCORCHED_LOG_ITEM = ITEMS.registerSimpleBlockItem("scorched_log", SCORCHED_LOG);
    public static final DeferredBlock<FrostLogBlock> SCORCHED_WOOD = BLOCKS.registerBlock("scorched_wood", FrostLogBlock::new, scorchedWoodPillarProperties());
    public static final DeferredItem<BlockItem> SCORCHED_WOOD_ITEM = ITEMS.registerSimpleBlockItem("scorched_wood", SCORCHED_WOOD);
    public static final DeferredBlock<FrostLogBlock> STRIPPED_SCORCHED_LOG = BLOCKS.registerBlock("stripped_scorched_log", FrostLogBlock::new, scorchedWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_SCORCHED_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_scorched_log", STRIPPED_SCORCHED_LOG);

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
    public static final DeferredItem<BlockItem> SCORCHED_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("scorched_leaves", SCORCHED_LEAVES);

    // --- 风化木 / Arid wood (arid biome tree family) --------------------------------------------
    public static final DeferredBlock<FrostLogBlock> ARID_LOG = BLOCKS.registerBlock("arid_log", FrostLogBlock::new, aridWoodPillarProperties());
    public static final DeferredItem<BlockItem> ARID_LOG_ITEM = ITEMS.registerSimpleBlockItem("arid_log", ARID_LOG);
    public static final DeferredBlock<FrostLogBlock> ARID_WOOD = BLOCKS.registerBlock("arid_wood", FrostLogBlock::new, aridWoodPillarProperties());
    public static final DeferredItem<BlockItem> ARID_WOOD_ITEM = ITEMS.registerSimpleBlockItem("arid_wood", ARID_WOOD);
    public static final DeferredBlock<FrostLogBlock> STRIPPED_ARID_LOG = BLOCKS.registerBlock("stripped_arid_log", FrostLogBlock::new, aridWoodPillarProperties());
    public static final DeferredItem<BlockItem> STRIPPED_ARID_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_arid_log", STRIPPED_ARID_LOG);

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
    public static final DeferredItem<BlockItem> ARID_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("arid_leaves", ARID_LEAVES);

    // --- 植被 / Vegetation ----------------------------------------------------------------------
    public static final DeferredBlock<FirePlantBlock> CINDER_BLOOM = BLOCKS.registerBlock("cinder_bloom", FirePlantBlock::new, firePlantProperties());
    public static final DeferredItem<BlockItem> CINDER_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("cinder_bloom", CINDER_BLOOM);
    public static final DeferredBlock<FirePlantBlock> FLAME_SPROUT = BLOCKS.registerBlock("flame_sprout", FirePlantBlock::new, firePlantProperties());
    public static final DeferredItem<BlockItem> FLAME_SPROUT_ITEM = ITEMS.registerSimpleBlockItem("flame_sprout", FLAME_SPROUT);
    public static final DeferredBlock<FirePlantBlock> FIRE_FLOWER = BLOCKS.registerBlock("fire_flower", FirePlantBlock::new, firePlantProperties());
    public static final DeferredItem<BlockItem> FIRE_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("fire_flower", FIRE_FLOWER);

    public static final DeferredBlock<AridPlantBlock> DUNE_FLOWER = BLOCKS.registerBlock("dune_flower", AridPlantBlock::new, aridPlantProperties());
    public static final DeferredItem<BlockItem> DUNE_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("dune_flower", DUNE_FLOWER);
    public static final DeferredBlock<AridPlantBlock> WIND_SPROUT = BLOCKS.registerBlock("wind_sprout", AridPlantBlock::new, aridPlantProperties());
    public static final DeferredItem<BlockItem> WIND_SPROUT_ITEM = ITEMS.registerSimpleBlockItem("wind_sprout", WIND_SPROUT);
    public static final DeferredBlock<AridPlantBlock> ARID_BUSH = BLOCKS.registerBlock("arid_bush", AridPlantBlock::new, aridPlantProperties());
    public static final DeferredItem<BlockItem> ARID_BUSH_ITEM = ITEMS.registerSimpleBlockItem("arid_bush", ARID_BUSH);

    // --- Particles ------------------------------------------------------------------------------
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLIZZARD_SNOW =
            PARTICLE_TYPES.register("blizzard_snow", () -> new SimpleParticleType(false));

    // Creates a creative tab with the id "weather_realm:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.weather_realm")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> FROST_FLOWER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FROST_FLOWER_ITEM.get());
                output.accept(FROST_GRASS_ITEM.get());
                output.accept(GLACIER_BLOOM_ITEM.get());
                output.accept(FROST_SPROUT_ITEM.get());
                output.accept(TALL_FROST_FLOWER_ITEM.get());
                output.accept(TALL_FROST_GRASS_ITEM.get());
                output.accept(FROST_SAPLING_ITEM.get());
                output.accept(FROST_LOG_ITEM.get());
                output.accept(FROST_WOOD_ITEM.get());
                output.accept(STRIPPED_FROST_LOG_ITEM.get());
                output.accept(STRIPPED_FROST_WOOD_ITEM.get());
                output.accept(FROST_PLANKS_ITEM.get());
                output.accept(FROST_STAIRS_ITEM.get());
                output.accept(FROST_SLAB_ITEM.get());
                output.accept(FROST_FENCE_ITEM.get());
                output.accept(FROST_FENCE_GATE_ITEM.get());
                output.accept(FROST_DOOR_ITEM.get());
                output.accept(FROST_TRAPDOOR_ITEM.get());
                output.accept(FROST_PRESSURE_PLATE_ITEM.get());
                output.accept(FROST_BUTTON_ITEM.get());
                output.accept(WEATHER_ALTAR_CORE_ITEM.get());
                output.accept(WEATHER_PEDESTAL_ITEM.get());
                output.accept(CLIMATE_SHARD.get());
                output.accept(ANCIENT_WEATHER_TOME.get());
                output.accept(BIOME_MAP.get());
                output.accept(FROST_LEAVES_ITEM.get());
                output.accept(PERMAFROST_ITEM.get());
                output.accept(DEEP_PERMAFROST_ITEM.get());
                output.accept(PERMAFROST_IRON_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_IRON_ORE_ITEM.get());
                output.accept(PERMAFROST_COAL_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_COAL_ORE_ITEM.get());
                output.accept(PERMAFROST_COPPER_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_COPPER_ORE_ITEM.get());
                output.accept(PERMAFROST_GOLD_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_GOLD_ORE_ITEM.get());
                output.accept(PERMAFROST_REDSTONE_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_REDSTONE_ORE_ITEM.get());
                output.accept(PERMAFROST_EMERALD_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_EMERALD_ORE_ITEM.get());
                output.accept(PERMAFROST_LAPIS_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_LAPIS_ORE_ITEM.get());
                output.accept(PERMAFROST_DIAMOND_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_DIAMOND_ORE_ITEM.get());
                output.accept(BLIZZARD_CRYSTAL.get());
                output.accept(BLIZZARD_CRYSTAL_BLOCK_ITEM.get());
                output.accept(PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM.get());
                output.accept(DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM.get());
                output.accept(BLIZZARD_CRYSTAL_HELMET.get());
                output.accept(BLIZZARD_CRYSTAL_CHESTPLATE.get());
                output.accept(BLIZZARD_CRYSTAL_LEGGINGS.get());
                output.accept(BLIZZARD_CRYSTAL_BOOTS.get());
                output.accept(BLIZZARD_CRYSTAL_SWORD.get());
                output.accept(BLIZZARD_CRYSTAL_PICKAXE.get());
                output.accept(BLIZZARD_CRYSTAL_AXE.get());
                output.accept(BLIZZARD_CRYSTAL_SHOVEL.get());
                output.accept(BLIZZARD_CRYSTAL_HOE.get());
                output.accept(FROST_SHEEP_SPAWN_EGG.get());
                output.accept(FROST_COW_SPAWN_EGG.get());
                output.accept(FROST_PIG_SPAWN_EGG.get());
                output.accept(FROST_CAT_SPAWN_EGG.get());
                output.accept(FROST_WOOL_ITEM.get());
                output.accept(FROST_MUTTON.get());
                output.accept(COOKED_FROST_MUTTON.get());
                output.accept(FROST_BEEF.get());
                output.accept(COOKED_FROST_BEEF.get());
                output.accept(FROST_LEATHER.get());
                output.accept(FROST_PORKCHOP.get());
                output.accept(COOKED_FROST_PORKCHOP.get());
                output.accept(FROST_PELT.get());
                output.accept(FROST_MILK_BUCKET.get());
                output.accept(FROST_RASPBERRY.get());
                output.accept(FROST_MOSS_ITEM.get());
                output.accept(DRY_TURF_ITEM.get());
                output.accept(VOLCANIC_ASH_ITEM.get());
                output.accept(FIRE_STONE_ITEM.get());
                output.accept(DEEP_FIRE_STONE_ITEM.get());
                output.accept(WEATHERED_SANDSTONE_ITEM.get());
                output.accept(DEEP_WEATHERED_SANDSTONE_ITEM.get());
                output.accept(BLAZE_CRYSTAL.get());
                output.accept(BLAZE_CRYSTAL_BLOCK_ITEM.get());
                output.accept(WIND_CRYSTAL.get());
                output.accept(WIND_CRYSTAL_BLOCK_ITEM.get());
                output.accept(SCORCHED_LOG_ITEM.get());
                output.accept(SCORCHED_WOOD_ITEM.get());
                output.accept(STRIPPED_SCORCHED_LOG_ITEM.get());
                output.accept(SCORCHED_LEAVES_ITEM.get());
                output.accept(ARID_LOG_ITEM.get());
                output.accept(ARID_WOOD_ITEM.get());
                output.accept(STRIPPED_ARID_LOG_ITEM.get());
                output.accept(ARID_LEAVES_ITEM.get());
                output.accept(CINDER_BLOOM_ITEM.get());
                output.accept(FLAME_SPROUT_ITEM.get());
                output.accept(FIRE_FLOWER_ITEM.get());
                output.accept(DUNE_FLOWER_ITEM.get());
                output.accept(WIND_SPROUT_ITEM.get());
                output.accept(ARID_BUSH_ITEM.get());
                GENERATED_ORE_ITEMS.forEach(item -> output.accept(item.get()));
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WeatherRealm(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so particle types get registered
        PARTICLE_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so sound events get registered
        SOUND_EVENTS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so armor materials get registered
        ARMOR_MATERIALS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entity types get registered
        ModEntities.ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so structure types get registered
        STRUCTURE_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so structure processors get registered
        STRUCTURE_PROCESSORS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BLOCK_ENTITIES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (WeatherRealm) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(PERMAFROST_ITEM);
            event.accept(DEEP_PERMAFROST_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
