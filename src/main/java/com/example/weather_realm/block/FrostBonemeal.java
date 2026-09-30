package com.example.weather_realm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 骨粉扩散共享助手 / Shared bonemeal spread helpers for the frost plants.
 *
 * <p>Extracted from {@link FrostGrassBlock} and {@link FrostFlowerBlock}, whose loops only differed
 * in their search box and placement rule. The jittered offset is rolled in the same {@code x, y, z}
 * order the original loops used, and the rare roll is only drawn after a cell passed the plantability
 * check, so the consumed randomness and the resulting world edits are identical to the previous
 * per-block implementations.</p>
 */
public final class FrostBonemeal {
    private FrostBonemeal() {
    }

    /**
     * Scatters up to {@code attempts} plants into the box around {@code origin}. Each attempt rolls a
     * jittered offset, skips cells that are not plantable, then rolls
     * {@code rareNumerator}/{@code rareDenominator} to choose between the common and the rare state
     * (the box is {@code radiusXZ} horizontally and {@code [-dyDown, +dyUp]} vertically).
     */
    public static void spreadArea(ServerLevel level, RandomSource random, BlockPos origin,
            int attempts, int radiusXZ, int dyUp, int dyDown,
            int rareNumerator, int rareDenominator, BlockState common, BlockState rare) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos target = randomOffset(random, origin, radiusXZ, dyUp, dyDown);
            if (!FrostPlantBlock.canPlantAt(level, target)) {
                continue;
            }
            BlockState plant = random.nextInt(rareDenominator) < rareNumerator ? rare : common;
            level.setBlock(target, plant, 3);
        }
    }

    /**
     * Tries to place {@code plant} on the first valid neighbouring cell and returns {@code true} once
     * placed. Mirrors the frost-flower duplicate loop, including the rule that the origin cell itself
     * is skipped.
     */
    public static boolean duplicateNearby(ServerLevel level, RandomSource random, BlockPos origin,
            int attempts, int radiusXZ, int dyUp, int dyDown, BlockState plant) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos target = randomOffset(random, origin, radiusXZ, dyUp, dyDown);
            if (target.equals(origin)) {
                continue;
            }
            if (FrostPlantBlock.canPlantAt(level, target)) {
                level.setBlock(target, plant, 3);
                return true;
            }
        }
        return false;
    }

    /** Rolls the jittered offset in the same {@code x, y, z} order the original loops used. */
    private static BlockPos randomOffset(RandomSource random, BlockPos origin, int radiusXZ, int dyUp, int dyDown) {
        return origin.offset(
                random.nextInt(radiusXZ * 2 + 1) - radiusXZ,
                random.nextInt(dyUp + dyDown + 1) - dyDown,
                random.nextInt(radiusXZ * 2 + 1) - radiusXZ);
    }
}
