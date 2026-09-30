package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 坚冰木 / Glacial Log - an axis-oriented pillar block (axe mineable).
 */
public class FrostLogBlock extends RotatedPillarBlock {
    public static final MapCodec<FrostLogBlock> CODEC = simpleCodec(FrostLogBlock::new);

    public FrostLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FrostLogBlock> codec() {
        return CODEC;
    }
}
