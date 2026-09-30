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
 * 冰雪草 / Frost Grass - a grass-like cross plant restricted to snow / ice.
 * Bonemeal spreads {@code frost_grass} over a 5x5 area and occasionally a {@code frost_flower}.
 */
public class FrostGrassBlock extends FrostPlantBlock implements BonemealableBlock {
    public static final MapCodec<FrostGrassBlock> CODEC = simpleCodec(FrostGrassBlock::new);
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public FrostGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FrostGrassBlock> codec() {
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
        // Spread frost_grass / frost_flower over a 5x5 (radius 2) area.
        FrostBonemeal.spreadArea(level, random, pos, 48, 2, 1, 1, 1, 6,
                ModBlocks.FROST_GRASS.get().defaultBlockState(),
                ModBlocks.FROST_FLOWER.get().defaultBlockState());
    }
}
