package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 坚冰木树叶 / Glacial Leaves - vanilla leaf decay behaviour (distance + persistent).
 */
public class FrostLeavesBlock extends LeavesBlock {
    public static final MapCodec<FrostLeavesBlock> CODEC = simpleCodec(FrostLeavesBlock::new);

    public FrostLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FrostLeavesBlock> codec() {
        return CODEC;
    }
}
