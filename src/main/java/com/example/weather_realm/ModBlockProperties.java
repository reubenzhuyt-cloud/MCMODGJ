package com.example.weather_realm;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * 方块属性工厂 / Block property factories.
 *
 * <p>Centralises the stone-like and wood-pillar property recipes that used to be duplicated as
 * private methods on the mod class. The generated {@link BlockBehaviour.Properties} are identical
 * to the former per-block helpers (same hardness / resistance / sound / map colour / instrument /
 * tool requirement), so block behaviour is unchanged.</p>
 */
public final class ModBlockProperties {
    private ModBlockProperties() {
    }

    // --- Stone-like geology / ores --------------------------------------------------------------

    /** Common stone-like recipe: map colour, hardness/resistance, sound, correct-tool requirement. */
    public static BlockBehaviour.Properties stoneLike(MapColor mapColor, float hardness, float resistance, SoundType sound) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .strength(hardness, resistance)
                .sound(sound)
                .requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties permafrostOre() {
        return stoneLike(MapColor.STONE, 2.25F, 6.0F, SoundType.DEEPSLATE);
    }

    public static BlockBehaviour.Properties deepPermafrostOre() {
        return stoneLike(MapColor.DEEPSLATE, 4.5F, 6.0F, SoundType.DEEPSLATE);
    }

    public static BlockBehaviour.Properties fireStone() {
        return stoneLike(MapColor.COLOR_ORANGE, 2.0F, 6.0F, SoundType.STONE);
    }

    public static BlockBehaviour.Properties deepFireStone() {
        return stoneLike(MapColor.NETHER, 4.0F, 6.0F, SoundType.DEEPSLATE);
    }

    public static BlockBehaviour.Properties weatheredSandstone() {
        return stoneLike(MapColor.SAND, 1.8F, 6.0F, SoundType.STONE);
    }

    public static BlockBehaviour.Properties deepWeatheredSandstone() {
        return stoneLike(MapColor.TERRACOTTA_ORANGE, 3.6F, 6.0F, SoundType.STONE);
    }

    public static BlockBehaviour.Properties crystalBlock() {
        return stoneLike(MapColor.COLOR_ORANGE, 4.5F, 6.0F, SoundType.AMETHYST);
    }

    // --- Wood pillars ---------------------------------------------------------------------------

    /** Common wood-pillar recipe: map colour, bass instrument, 2.0 hardness, wood sound, flammable. */
    public static BlockBehaviour.Properties woodPillar(MapColor mapColor) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    public static BlockBehaviour.Properties frostWoodPillar() {
        return woodPillar(MapColor.ICE);
    }

    public static BlockBehaviour.Properties scorchedWoodPillar() {
        return woodPillar(MapColor.COLOR_BROWN);
    }

    /**
     * 风化原木/木干 / Arid (weathered) log &amp; wood properties. The weathered sprites carry
     * {@code alpha=0} erosion holes, so the block must not occlude its neighbours (otherwise the
     * culled neighbour face shows through the holes). The light-blocking is restored by
     * {@code AridLogBlock.getLightBlock}.
     */
    public static BlockBehaviour.Properties aridWoodPillar() {
        return woodPillar(MapColor.SAND).noOcclusion();
    }

    /** Common planks recipe shared by every wood family: bass, 2.0 hardness, wood, flammable. */
    public static BlockBehaviour.Properties woodPlanks(MapColor mapColor) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F, 3.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    /**
     * 风化木木板 / Arid (weathered) planks properties. Same as {@link #woodPlanks} plus
     * {@code noOcclusion()}: the sprite carries {@code alpha=0} holes, so the block must not cull
     * its neighbours' faces. {@code AridPlanksBlock.getLightBlock} restores the full-cube
     * light blocking that {@code noOcclusion()} would otherwise drop to 1.
     */
    public static BlockBehaviour.Properties aridPlanks() {
        return woodPlanks(MapColor.SAND).noOcclusion();
    }

    // --- Plants ---------------------------------------------------------------------------------

    public static BlockBehaviour.Properties blazingPlant() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    public static BlockBehaviour.Properties aridPlant() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }
}
