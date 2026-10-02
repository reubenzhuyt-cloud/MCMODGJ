package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风化木木板 / Arid (weathered) planks.
 *
 * <p>Same reasoning as {@link AridLogBlock}: the weathered planks sprite carries {@code alpha=0}
 * holes, so the block is registered with {@code noOcclusion()} to stop neighbour-face culling and
 * remove the see-through artefact. {@link #getLightBlock} puts the solid-cube light attenuation
 * back to 15, which {@code noOcclusion()} alone would drop to 1.</p>
 */
public class AridPlanksBlock extends Block {
    public static final MapCodec<AridPlanksBlock> CODEC = simpleCodec(AridPlanksBlock::new);

    public AridPlanksBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AridPlanksBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getMaxLightLevel();
    }
}
