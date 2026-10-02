package com.example.weather_realm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风化原木/木干 / Arid (weathered) log &amp; wood.
 *
 * <p>The weathered bark sprite carries {@code alpha=0} erosion holes, so the block is registered
 * with {@code noOcclusion()}: a full opaque cube would otherwise cull the touching neighbour face
 * and the hole would reveal that culled face (the see-through bug). Because
 * {@code BlockBehaviour#getLightBlock} falls back to a 1-per-layer attenuation once
 * {@code canOcclude} is cleared, this class restores the 15 light block a solid full cube has, so
 * a weathered timber wall still blocks light.</p>
 */
public class AridLogBlock extends RotatedPillarBlock {
    public static final MapCodec<AridLogBlock> CODEC = simpleCodec(AridLogBlock::new);

    public AridLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AridLogBlock> codec() {
        return CODEC;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getMaxLightLevel();
    }
}
