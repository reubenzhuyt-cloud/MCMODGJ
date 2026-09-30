package com.example.weather_realm.client.map;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * 客户端探索状态 / Client-side exploration state for the Glacial Realm.
 *
 * <p>Purely local: as the player walks around, the client marks every already-loaded chunk within a
 * radius as explored. The map screen then reveals exactly those chunks and shrouds the rest in fog,
 * so nothing has to be sampled or synchronised from the server.</p>
 */
public final class BiomeMapExplorationState {
    private BiomeMapExplorationState() {
    }

    /** Stable {@link ChunkPos#asLong(int, int)} keys of every chunk the player has uncovered. */
    private static final LongSet EXPLORED_CHUNKS = new LongOpenHashSet();

    /**
     * Marks every loaded chunk within {@code chunkRadius} of the player's chunk as explored.
     *
     * @param level       the client level to query for loaded chunks
     * @param playerPos   the player's current position
     * @param chunkRadius radius in chunks (e.g. 12~16)
     */
    public static void updateExploration(ClientLevel level, BlockPos playerPos, int chunkRadius) {
        if (level == null || playerPos == null) {
            return;
        }
        int centerX = playerPos.getX() >> 4;
        int centerZ = playerPos.getZ() >> 4;
        int radius = Math.max(0, chunkRadius);
        for (int dz = -radius; dz <= radius; dz++) {
            int cz = centerZ + dz;
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = centerX + dx;
                if (level.getChunkSource().hasChunk(cx, cz)) {
                    EXPLORED_CHUNKS.add(ChunkPos.asLong(cx, cz));
                }
            }
        }
    }

    /** Returns whether the chunk at {@code (cx, cz)} has already been uncovered. */
    public static boolean isExplored(int cx, int cz) {
        return EXPLORED_CHUNKS.contains(ChunkPos.asLong(cx, cz));
    }

    /** Wipes all exploration progress. */
    public static void clear() {
        EXPLORED_CHUNKS.clear();
    }
}
