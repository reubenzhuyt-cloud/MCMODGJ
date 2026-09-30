package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 霜草幼芽 / Frost Sprout - a short frosty grass tuft. Harvest with shears; empty hands get
 * nothing (see the block loot table).
 */
public class FrostSproutBlock extends FrostPlantBlock {
    public static final MapCodec<FrostSproutBlock> CODEC = simpleCodec(FrostSproutBlock::new);
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 9.0, 14.0);

    public FrostSproutBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FrostSproutBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
