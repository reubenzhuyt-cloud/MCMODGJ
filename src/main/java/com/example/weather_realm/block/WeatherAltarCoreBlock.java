package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 天气祭坛核心 / Weather Altar Core.
 * A glowing, end-crystal-like translucent orb; its shell and inner rotating crystal are drawn
 * by {@code WeatherAltarCoreRenderer} (the block render shape is INVISIBLE).
 *
 * <p>It has no server-side behaviour of its own: right-clicking a tuned core (one resting on a
 * tuning pedestal) opens the weather control UI on the client, and inter-dimensional travel is
 * handled entirely by the climate portal system.</p>
 */
public class WeatherAltarCoreBlock extends BaseEntityBlock {
    public static final MapCodec<WeatherAltarCoreBlock> CODEC = simpleCodec(WeatherAltarCoreBlock::new);

    public WeatherAltarCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeatherAltarCoreBlockEntity(pos, state);
    }
}
