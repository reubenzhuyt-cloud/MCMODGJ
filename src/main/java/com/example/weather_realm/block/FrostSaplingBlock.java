package com.example.weather_realm.block;

import com.example.weather_realm.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 坚冰木树苗 / Glacial Sapling - grows a tall glacial spruce (6-8 log trunk with a conical,
 * tiered canopy) when bonemealed, only ever placing into air / replaceable blocks. The canopy
 * mirrors {@code minecraft:spruce_foliage_placer}'s pagoda silhouette.
 */
public class FrostSaplingBlock extends FrostPlantBlock implements BonemealableBlock {
    public static final MapCodec<FrostSaplingBlock> CODEC = simpleCodec(FrostSaplingBlock::new);
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public FrostSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FrostSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        growGlacialTree(level, pos, random);
    }

    private void growGlacialTree(ServerLevel level, BlockPos pos, RandomSource random) {
        int height = 6 + random.nextInt(3); // 6, 7 or 8 logs tall

        // The sapling sits at the trunk base, so only blocks above it need to be clear.
        for (int y = 1; y <= height + 2; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (!above.isAir() && !above.canBeReplaced()) {
                return;
            }
        }

        // Trunk: y = 0 .. height - 1.
        for (int y = 0; y < height; y++) {
            level.setBlock(pos.above(y), ModBlocks.FROST_LOG.get().defaultBlockState(), 3);
        }

        // Conical, tiered spruce canopy relative to the block just above the trunk tip.
        BlockPos top = pos.above(height);
        placeLeafIfAir(level, top.above());                 // top + 1 : single spire
        crossLayer(level, top, 1);                          // top     : cross (r1)
        squareLayer(level, top.below(), 1, true);           // top - 1 : square skirt (r1, corners trimmed)
        squareLayer(level, top.below(2), 2, true);          // top - 2 : expanded skirt (r2, corners trimmed)
        crossLayer(level, top.below(3), 1);                 // top - 3 : stepped collar (r1 cross)
        squareLayer(level, top.below(4), 2, true);          // top - 4 : expanded skirt (r2, corners trimmed)
        if (height >= 7) {
            crossLayer(level, top.below(5), 2);             // top - 5 : base cross skirt (r2) for tall trees
        }
    }

    /** Places a leaf only where the current block is air or replaceable. */
    private void placeLeafIfAir(ServerLevel level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, ModBlocks.FROST_LEAVES.get().defaultBlockState(), 3);
        }
    }

    /** Fills a square footprint; optionally trims the four corners. */
    private void squareLayer(ServerLevel level, BlockPos center, int radius, boolean trimCorners) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                placeLeafIfAir(level, center.offset(dx, 0, dz));
            }
        }
    }

    /** Fills a diamond (plus-derived) footprint of the given radius. */
    private void crossLayer(ServerLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius) {
                    continue;
                }
                placeLeafIfAir(level, center.offset(dx, 0, dz));
            }
        }
    }
}
