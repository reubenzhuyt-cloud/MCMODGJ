package com.example.weather_realm.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Parameterised shrine for the two non-frost biome saplings ({@code arid_sapling} /
 * {@code scorched_sapling}). Everything the old {@link FrostSaplingBlock} hard-coded is now
 * injected per theme: the log / leaves / supporting topsoil blocks plus the trunk height range and
 * a {@link Canopy} strategy that stamps the crown silhouette. Growth is server-only and only ever
 * writes into air / replaceable blocks, so a failed grow leaves the world untouched.
 */
public abstract class ThemeSaplingBlock extends FrostPlantBlock implements BonemealableBlock {
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    /** Builds the canopy after the 1x1 trunk has been placed. */
    @FunctionalInterface
    public interface Canopy {
        void place(ServerLevel level, BlockPos base, int height, Block log, Block leaves, RandomSource random);
    }

    private final Supplier<? extends Block> log;
    private final Supplier<? extends Block> leaves;
    private final Supplier<? extends Block> ground;
    private final int minHeight;
    private final int maxHeight;
    private final Canopy canopy;

    protected ThemeSaplingBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> log,
                                Supplier<? extends Block> leaves, Supplier<? extends Block> ground,
                                int minHeight, int maxHeight, Canopy canopy) {
        super(properties);
        this.log = log;
        this.leaves = leaves;
        this.ground = ground;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.canopy = canopy;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isSaplingPlantable(state, ground.get());
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int height = minHeight + random.nextInt(maxHeight - minHeight + 1);

        // Bounds: never let the crown poke past the build limit.
        if (pos.getY() + height + 2 > level.getMaxBuildHeight()) {
            return;
        }
        // The sapling column must be clear above the base (allowing replaceables).
        for (int y = 1; y <= height + 2; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (!above.isAir() && !above.canBeReplaced()) {
                return;
            }
        }

        for (int y = 0; y < height; y++) {
            level.setBlock(pos.above(y), log.get().defaultBlockState(), 3);
        }
        canopy.place(level, pos, height, log.get(), leaves.get(), random);
    }

    /** Places a leaf only where the current block is air or replaceable. */
    protected static void placeLeafIfAir(ServerLevel level, BlockPos pos, Block leaves) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, leaves.defaultBlockState(), 3);
        }
    }

    /** Places a leaf with the given probability (deterministic given the level's RNG state). */
    protected static void placeLeafIfAir(ServerLevel level, BlockPos pos, Block leaves,
                                         RandomSource random, float chance) {
        if (random.nextFloat() < chance) {
            placeLeafIfAir(level, pos, leaves);
        }
    }

    /** Places a log only where the current block is air or replaceable (never eats terrain). */
    protected static void placeLogIfAir(ServerLevel level, BlockPos pos, Block log) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, log.defaultBlockState(), 3);
        }
    }
}
