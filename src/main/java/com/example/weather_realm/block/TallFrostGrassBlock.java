package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 高霜草 / Tall Frost Grass - a two-block-tall frost grass. Harvest with shears; empty hands
 * get nothing (see the block loot table).
 */
public class TallFrostGrassBlock extends DoublePlantBlock {
    public static final MapCodec<TallFrostGrassBlock> CODEC = simpleCodec(TallFrostGrassBlock::new);

    public TallFrostGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TallFrostGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return FrostPlantBlock.isFrostPlantable(state);
    }
}
