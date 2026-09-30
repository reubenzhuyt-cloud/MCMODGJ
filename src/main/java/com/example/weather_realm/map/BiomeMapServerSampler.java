package com.example.weather_realm.map;

import java.util.Arrays;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * 服务端天象图采样器 / Server-side biome grid sampler.
 *
 * <p>Paints the 128x128 pixel grid of the biome map directly from the level's <b>noise biome
 * source</b> via {@link ServerLevel#getUncachedNoiseBiome(int, int, int)}. That call is a pure
 * function of the biome source and the world seed, so it works for chunks the server has never
 * loaded - the map no longer depends on chunk loading (unlike the client, whose
 * {@code getUncachedNoiseBiome} always returns {@code PLAINS} for unloaded chunks).</p>
 *
 * <p>The sample height is fixed at {@link #SAMPLE_BLOCK_Y} because this dimension's
 * {@code biome_source} only slices on the temperature axis, and {@code minecraft:overworld}'s
 * temperature is a {@code shifted_noise} with {@code y_scale: 0} / {@code shift_y: 0} - i.e. the
 * result is identical for every Y. No height map or chunk is required.</p>
 *
 * <p>Sampling is budgeted: each {@link #advance(ServerLevel, State)} call performs at most
 * {@link #BUDGET_PER_TICK} biome lookups so a full repaint is spread over roughly 8 ticks and never
 * stalls the server. When the player crosses a pixel boundary the old grid is shifted with
 * {@code System.arraycopy} and only the newly exposed row/column is resampled (128~256 pixels).</p>
 */
public final class BiomeMapServerSampler {
    /** 纹理边长 / Texture edge length in pixels. */
    public static final int SIZE = 128;
    /** 半宽 / Half the texture edge; pixel {@code (64,64)} is the player. */
    public static final int HALF = SIZE / 2;
    /** 采样高度(方块) / Sample height in blocks; the temperature axis is Y-independent. */
    public static final int SAMPLE_BLOCK_Y = 64;
    /** 单 tick 采样上限 / Hard per-tick biome lookup budget for one player. */
    public static final int BUDGET_PER_TICK = 2048;

    private static final int PIXEL_COUNT = SIZE * SIZE;

    private BiomeMapServerSampler() {
    }

    /** 单个玩家的网格缓存 / Per-player grid cache owned by the dispatch handler. */
    public static final class State {
        public int originPixelX;
        public int originPixelZ;
        public int blocksPerPixel;
        public int tier;
        public boolean initialized;
        public boolean complete;
        /** Bumped whenever the requested window (origin/tier) changes. */
        public int version;
        /** Version already delivered to the client, so unchanged windows are not resent. */
        public int sentVersion = -1;
        /** Row-major scan cursor for the incremental repaint. */
        public int cursor;
        /** Ticks spent idle (complete and unchanged) since the last evaluation. */
        public int idleTicks;
        /** 行主序纹理 / Row-major texture bytes: {@code index = row * SIZE + col}. */
        public final byte[] colors = new byte[PIXEL_COUNT];
        /** Which pixels currently hold a valid sample. */
        public final boolean[] sampled = new boolean[PIXEL_COUNT];
        private final byte[] scratch = new byte[PIXEL_COUNT];
        private final boolean[] scratchSampled = new boolean[PIXEL_COUNT];

        State() {
            Arrays.fill(colors, BiomeMapPalette.UNKNOWN_COLOR);
        }
    }

    /**
     * 设定目标窗口 / Points the state at a new window, preserving overlapping pixels.
     *
     * <p>On the first call or a tier change the grid is reset and fully invalidated. Otherwise the
     * existing pixels are shifted by the window delta in pixel units and only the newly exposed
     * strip stays unsampled.</p>
     */
    public static void setTarget(State state, int originPixelX, int originPixelZ, int blocksPerPixel, int tier) {
        boolean changed = false;
        if (!state.initialized || state.tier != tier) {
            Arrays.fill(state.colors, BiomeMapPalette.UNKNOWN_COLOR);
            Arrays.fill(state.sampled, false);
            state.cursor = 0;
            changed = true;
        } else {
            int dx = originPixelX - state.originPixelX;
            int dz = originPixelZ - state.originPixelZ;
            if (dx != 0 || dz != 0) {
                shift(state, dx, dz);
                changed = true;
            }
        }
        state.originPixelX = originPixelX;
        state.originPixelZ = originPixelZ;
        state.blocksPerPixel = blocksPerPixel;
        state.tier = tier;
        state.initialized = true;
        if (changed) {
            state.complete = false;
            state.cursor = 0;
            state.version++;
        }
    }

    /**
     * 平移缓存 / Shifts the grid by {@code (dx, dz)} pixels, mirroring the viewport movement.
     *
     * <p>The texture is player-centred: pixel {@code (col, row)} maps to world
     * {@code (originPixelX + col, originPixelZ + row)}. When the origin moves east/south by a
     * positive delta a fixed world point's pixel index <b>decreases</b> (the terrain scrolls the
     * opposite way), so new pixel {@code (c, r)} shows the same world point as old pixel
     * {@code (c + dx, r + dz)}.</p>
     */
    private static void shift(State state, int dx, int dz) {
        System.arraycopy(state.colors, 0, state.scratch, 0, PIXEL_COUNT);
        System.arraycopy(state.sampled, 0, state.scratchSampled, 0, PIXEL_COUNT);

        Arrays.fill(state.colors, BiomeMapPalette.UNKNOWN_COLOR);
        Arrays.fill(state.sampled, false);

        int colStart = Math.max(0, -dx);
        int colEnd = Math.min(SIZE, SIZE - dx);
        int rowStart = Math.max(0, -dz);
        int rowEnd = Math.min(SIZE, SIZE - dz);
        int width = colEnd - colStart;
        if (width <= 0) {
            return;
        }
        for (int row = rowStart; row < rowEnd; row++) {
            int srcRow = row + dz;
            int srcOffset = srcRow * SIZE + (colStart + dx);
            int dstOffset = row * SIZE + colStart;
            System.arraycopy(state.scratch, srcOffset, state.colors, dstOffset, width);
            System.arraycopy(state.scratchSampled, srcOffset, state.sampled, dstOffset, width);
        }
    }

    /**
     * 推进采样 / Advances the repaint by at most {@link #BUDGET_PER_TICK} biome lookups.
     *
     * <p>Skips already-sampled pixels (they cost no budget) and completes once the whole grid has
     * been covered.</p>
     */
    public static void advance(ServerLevel level, State state) {
        if (state.complete || !state.initialized) {
            return;
        }
        int budget = BUDGET_PER_TICK;
        int index = state.cursor;
        while (index < PIXEL_COUNT && budget > 0) {
            if (!state.sampled[index]) {
                state.colors[index] = sample(level, state, index);
                state.sampled[index] = true;
                budget--;
            }
            index++;
        }
        if (index >= PIXEL_COUNT) {
            state.cursor = 0;
            state.complete = true;
        } else {
            state.cursor = index;
        }
    }

    /** 采样单个像素 / Samples the biome at the centre of pixel {@code index}. */
    private static byte sample(ServerLevel level, State state, int index) {
        int col = index % SIZE;
        int row = index / SIZE;
        int worldX = (state.originPixelX + col) * state.blocksPerPixel + state.blocksPerPixel / 2;
        int worldZ = (state.originPixelZ + row) * state.blocksPerPixel + state.blocksPerPixel / 2;
        Holder<Biome> biome = level.getUncachedNoiseBiome(worldX >> 2, SAMPLE_BLOCK_Y >> 2, worldZ >> 2);
        return BiomeMapPalette.colorFor(biome);
    }
}
