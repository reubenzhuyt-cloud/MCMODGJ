package com.example.weather_realm.block;

import com.example.weather_realm.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 气候群系植被共享基类 / Shared base for the climate-biome plants.
 *
 * <p>The blazing and arid plants only differed in their {@link #mayPlaceOn} rule, so they now share
 * this single implementation and select the rule through the {@link Ground} family passed at
 * registration time. The family is carried through the block codec so a round-trip preserves the
 * placement rule; the resulting block ids are unchanged.</p>
 */
public class BiomePlantBlock extends BushBlock {
    /** Which climate geology the plant may be planted on. */
    public enum Ground {
        /** 炎热群系 / Blazing: fire stone / deep fire stone / volcanic ash. */
        BLAZING,
        /** 风沙群系 / Arid: weathered sandstone / deep weathered sandstone / dry turf. */
        ARID
    }

    public static final MapCodec<BiomePlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            Codec.BOOL.fieldOf("arid").forGetter(block -> block.ground == Ground.ARID)
    ).apply(instance, (properties, arid) -> new BiomePlantBlock(properties, arid ? Ground.ARID : Ground.BLAZING)));

    private final Ground ground;

    public BiomePlantBlock(BlockBehaviour.Properties properties, Ground ground) {
        super(properties);
        this.ground = ground;
    }

    @Override
    public MapCodec<BiomePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (ground) {
            case BLAZING -> state.is(ModBlocks.FIRE_STONE.get())
                    || state.is(ModBlocks.DEEP_FIRE_STONE.get())
                    || state.is(ModBlocks.VOLCANIC_ASH.get());
            case ARID -> state.is(ModBlocks.WEATHERED_SANDSTONE.get())
                    || state.is(ModBlocks.DEEP_WEATHERED_SANDSTONE.get())
                    || state.is(ModBlocks.DRY_TURF.get());
        };
    }
}
