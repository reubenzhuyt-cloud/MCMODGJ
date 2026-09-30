package com.example.weather_realm.block;

import com.example.weather_realm.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for the frost plants. Restricts placement to snow / ice blocks
 * (either the {@code weather_realm:frost_plantable_on} tag or a full snow layer).
 */
public abstract class FrostPlantBlock extends BushBlock {
    protected FrostPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isFrostPlantable(state);
    }

    /** A block is plantable-on if it is tagged or a full (8-layer) snow layer. */
    protected static boolean isFrostPlantable(BlockState state) {
        if (state.is(ModTags.Blocks.FROST_PLANTABLE_ON)) {
            return true;
        }
        return state.is(Blocks.SNOW)
                && state.hasProperty(SnowLayerBlock.LAYERS)
                && state.getValue(SnowLayerBlock.LAYERS) == 8;
    }

    /** Whether a frost plant may be placed at {@code pos} (empty space with plantable ground below). */
    protected static boolean canPlantAt(LevelReader level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        return isFrostPlantable(level.getBlockState(pos.below()));
    }
}
