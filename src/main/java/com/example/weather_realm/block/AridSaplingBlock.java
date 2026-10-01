package com.example.weather_realm.block;

import com.example.weather_realm.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 风化树苗 / Arid Sapling - a wiry, understated tree: a single 1x1 trunk 5-7 logs tall topped by
 * a small, deliberately sparse crown, evoking a wind-sculpted birch. Grows on dry turf, dirt-family
 * blocks and frost-plantable ground.
 */
public class AridSaplingBlock extends ThemeSaplingBlock {
    public static final MapCodec<AridSaplingBlock> CODEC = simpleCodec(AridSaplingBlock::new);

    public AridSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties, ModBlocks.ARID_LOG, ModBlocks.ARID_LEAVES, ModBlocks.DRY_TURF,
                5, 7, AridSaplingBlock::growCanopy);
    }

    @Override
    public MapCodec<AridSaplingBlock> codec() {
        return CODEC;
    }

    /** Thin trunk, small sparse crown: two radius-1 collars and one gappy radius-2 skirt. */
    private static void growCanopy(ServerLevel level, BlockPos base, int height, Block log,
                                   Block leaves, RandomSource random) {
        BlockPos top = base.above(height);
        placeLeafIfAir(level, top.above(), leaves, random, 0.6F);
        sparseLayer(level, top, 1, leaves, random);
        sparseLayer(level, top.below(1), 1, leaves, random);
        sparseLayer(level, top.below(2), 2, leaves, random);
    }

    /** A diamond footprint where each candidate leaf has only a 50% chance of appearing. */
    private static void sparseLayer(ServerLevel level, BlockPos center, int radius, Block leaves,
                                    RandomSource random) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius) {
                    continue;
                }
                placeLeafIfAir(level, center.offset(dx, 0, dz), leaves, random, 0.5F);
            }
        }
    }
}
