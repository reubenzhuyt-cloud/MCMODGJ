package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 炎热群系植被基类 / Base for blaze-biome plants.
 *
 * <p>Plants only grow on the blazing geology ({@code fire_stone} / {@code deep_fire_stone}) or on
 * the volcanic-ash surface cover.</p>
 */
public class FirePlantBlock extends BushBlock {
    public static final MapCodec<FirePlantBlock> CODEC = simpleCodec(FirePlantBlock::new);

    public FirePlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FirePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(WeatherRealm.FIRE_STONE.get())
                || state.is(WeatherRealm.DEEP_FIRE_STONE.get())
                || state.is(WeatherRealm.VOLCANIC_ASH.get());
    }
}
