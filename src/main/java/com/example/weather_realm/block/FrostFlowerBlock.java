package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 冰晶花 / Frost Flower - a cross-model flower that only grows on snow / ice.
 * Bonemeal duplicates it onto a nearby valid snow block (or drops the item as a fallback).
 */
public class FrostFlowerBlock extends FrostPlantBlock implements BonemealableBlock {
    public static final MapCodec<FrostFlowerBlock> CODEC = simpleCodec(FrostFlowerBlock::new);
    protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);

    public FrostFlowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FrostFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
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
        // Try to duplicate onto an adjacent valid snow block.
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - 1, random.nextInt(3) - 1);
            if (target.equals(pos)) {
                continue;
            }
            if (canPlantAt(level, target)) {
                level.setBlock(target, WeatherRealm.FROST_FLOWER.get().defaultBlockState(), 3);
                return;
            }
        }
        // Fallback: drop a frost flower item.
        Block.popResource(level, pos, new ItemStack(WeatherRealm.FROST_FLOWER_ITEM.get()));
    }
}
