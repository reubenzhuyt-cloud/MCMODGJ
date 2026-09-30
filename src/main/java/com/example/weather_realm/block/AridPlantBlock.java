package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风沙群系植被基类 / Base for arid-biome plants.
 *
 * <p>Plants only grow on the weathered sandstone geology ({@code weathered_sandstone} /
 * {@code deep_weathered_sandstone}) or on the dry-turf surface cover.</p>
 */
public class AridPlantBlock extends BushBlock {
    public static final MapCodec<AridPlantBlock> CODEC = simpleCodec(AridPlantBlock::new);

    public AridPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AridPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(WeatherRealm.WEATHERED_SANDSTONE.get())
                || state.is(WeatherRealm.DEEP_WEATHERED_SANDSTONE.get())
                || state.is(WeatherRealm.DRY_TURF.get());
    }
}
