package com.example.weather_realm.block;

import com.example.weather_realm.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 冰川兰 / Glacier Bloom - a small frost orchid. Unlike the other frost plants it may also be
 * planted on grass / dirt and permafrost, so it can decorate both the Glacial Realm and the
 * frozen overworld. Breaking it always drops itself.
 */
public class GlacierBloomBlock extends FlowerBlock {
    public static final MapCodec<GlacierBloomBlock> CODEC = simpleCodec(GlacierBloomBlock::new);

    public GlacierBloomBlock(BlockBehaviour.Properties properties) {
        super(SuspiciousStewEffects.EMPTY, properties);
    }

    @Override
    public MapCodec<? extends FlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT)
                || state.is(ModBlocks.PERMAFROST.get())
                || state.is(ModBlocks.DEEP_PERMAFROST.get())
                || FrostPlantBlock.isFrostPlantable(state);
    }
}
