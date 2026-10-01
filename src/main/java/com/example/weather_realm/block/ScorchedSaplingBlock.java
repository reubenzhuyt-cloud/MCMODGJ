package com.example.weather_realm.block;

import com.example.weather_realm.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 焦木树苗 / Scorched Sapling - a squat, heavy tree: a 1x1 trunk 4-6 logs tall with a reinforced
 * two-block base flare and a wide, dense crown, evoking a charred oak. Grows on volcanic ash,
 * dirt-family blocks and frost-plantable ground.
 */
public class ScorchedSaplingBlock extends ThemeSaplingBlock {
    public static final MapCodec<ScorchedSaplingBlock> CODEC = simpleCodec(ScorchedSaplingBlock::new);

    public ScorchedSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties, ModBlocks.SCORCHED_LOG, ModBlocks.SCORCHED_LEAVES, ModBlocks.VOLCANIC_ASH,
                4, 6, ScorchedSaplingBlock::growCanopy);
    }

    @Override
    public MapCodec<ScorchedSaplingBlock> codec() {
        return CODEC;
    }

    /** Reinforced base + wide dense crown (up to a trimmed 7x7 skirt on the tallest trees). */
    private static void growCanopy(ServerLevel level, BlockPos base, int height, Block log,
                                   Block leaves, RandomSource random) {
        for (int y = 1; y <= 2; y++) {
            for (int[] dir : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                placeLogIfAir(level, base.offset(dir[0], y, dir[1]), log);
            }
        }
        BlockPos top = base.above(height);
        placeLeafIfAir(level, top.above(), leaves);
        squareLayer(level, top, 1, false, leaves);
        squareLayer(level, top.below(1), 2, true, leaves);
        squareLayer(level, top.below(2), 2, true, leaves);
        if (height >= 6) {
            squareLayer(level, top.below(3), 3, true, leaves);
        }
    }

    /** Fills a square footprint; optionally trims the four corners. */
    private static void squareLayer(ServerLevel level, BlockPos center, int radius, boolean trimCorners,
                                    Block leaves) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                placeLeafIfAir(level, center.offset(dx, 0, dz), leaves);
            }
        }
    }
}
