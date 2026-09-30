package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 高霜冻花 / Tall Frost Flower - a two-block-tall frost flower grown on snow / ice.
 * Breaking either half drops a single flower (the other half is removed without drops).
 */
public class TallFrostFlowerBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<TallFrostFlowerBlock> CODEC = simpleCodec(TallFrostFlowerBlock::new);

    public TallFrostFlowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TallFrostFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return FrostPlantBlock.isFrostPlantable(state);
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
        popResource(level, pos, new ItemStack(this));
    }
}
