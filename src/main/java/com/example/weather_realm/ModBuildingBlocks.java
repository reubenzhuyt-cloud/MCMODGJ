package com.example.weather_realm;

import java.util.ArrayList;
import java.util.List;

import com.example.weather_realm.block.FrostLogBlock;
import com.example.weather_realm.block.WeatherSpikeBlock;

import net.minecraft.world.level.block.AmethystClusterBlock;
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
import net.minecraft.world.level.block.SnowLayerBlock;
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

    public record EcoSet(String prefix, DeferredBlock<AmethystClusterBlock> cluster,
                         DeferredBlock<SnowLayerBlock> layer, DeferredBlock<WeatherSpikeBlock> spike) {
    }

    public static final List<StoneFamily> STONE_FAMILIES = new ArrayList<>();
    public static final List<WoodFamily> WOOD_FAMILIES = new ArrayList<>();
    public static final List<LanternSet> LANTERNS = new ArrayList<>();
    public static final List<DecorationSet> DECORATIONS = new ArrayList<>();
    public static final List<EcoSet> ECO_ITEMS = new ArrayList<>();

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

        ECO_ITEMS.add(ecoSet("frost", MapColor.ICE, 7, SoundType.SNOW, 1.5F));
        ECO_ITEMS.add(ecoSet("blaze", MapColor.COLOR_ORANGE, 3, SoundType.SAND, 1.5F));
        ECO_ITEMS.add(ecoSet("wind", MapColor.SAND, 0, SoundType.SAND, 1.5F));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
