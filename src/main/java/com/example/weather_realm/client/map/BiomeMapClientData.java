package com.example.weather_realm.client.map;

import java.util.Arrays;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.item.BiomeMapItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 极域天象图客户端像素数据 / Client-side pixel source for the biome map.
 *
 * <p>Owns a single, never-registered {@link MapItemSavedData} whose {@code public byte[] colors}
 * array is painted from the surrounding biomes. Instead of hand-rolling a renderer, the map is
 * handed to the vanilla {@code MapRenderer}: {@link BiomeMapItem#getCustomMapData} returns this
 * instance, {@code ItemInHandRenderer} renders it with the vanilla two-handed pose, and
 * {@code MapRenderer.MapInstance.updateTexture()} uploads the 128x128 pixels through the exact
 * vanilla palette pipeline ({@code byte = MapColor.getPackedId(Brightness)}).</p>
 *
 * <p>The texture is <b>player-centred</b>: pixel {@code (64,64)} is the player. The player arrow is
 * therefore added at data-relative {@code (0,0)} (the data centre) with the player's yaw, which
 * {@code MapRenderer} draws at pixel {@code 64,64} with the vanilla arrow sprite.</p>
 *
 * <p>Everything here is client-only. {@link BiomeMapItem} finds this class without importing it,
 * through a {@link java.util.function.Function} installed by the static initialiser below.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BiomeMapClientData {
    private static final int SIZE = 128;
    private static final int HALF = SIZE / 2;
    private static final int MAX_IDLE_TICKS = 100;
    private static final String PLAYER_DECORATION_ID = "player";

    private static final byte FOG_COLOR = MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.NORMAL);
    private static final byte CRYSTAL_COLOR = MapColor.ICE.getPackedId(MapColor.Brightness.NORMAL);
    private static final byte BLAZING_COLOR = MapColor.FIRE.getPackedId(MapColor.Brightness.NORMAL);
    private static final byte ARID_COLOR = MapColor.SAND.getPackedId(MapColor.Brightness.NORMAL);
    private static final byte UNKNOWN_COLOR = MapColor.STONE.getPackedId(MapColor.Brightness.NORMAL);

    private static final ResourceLocation CRYSTAL_PLAINS =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "crystal_plains");
    private static final ResourceLocation BLAZING_PLAINS =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "blazing_plains");
    private static final ResourceLocation ARID_WASTELAND =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "arid_wasteland");

    private static MapItemSavedData data;
    private static int idleTicks;
    private static int lastPixelX = Integer.MIN_VALUE;
    private static int lastPixelZ = Integer.MIN_VALUE;
    private static int lastTier = -1;
    private static boolean lastRefreshWasActive;

    /**
     * 每像素上一次成功采样到的颜色 / Last successfully sampled colour per pixel. Used when an
     * already-explored chunk has been unloaded: the biome query would otherwise fall back to the
     * empty-chunk biome and paint the region {@link #UNKNOWN_COLOR} (stone grey), greying out
     * terrain the player has already uncovered. Initialised to fog so a never-sampled pixel stays
     * hidden.
     */
    private static final byte[] CACHED_COLORS = new byte[SIZE * SIZE];

    private BiomeMapClientData() {
    }

    static {
        Arrays.fill(CACHED_COLORS, FOG_COLOR);
        // Loading this class (NeoForge scans it for @EventBusSubscriber at client startup) installs
        // the provider that BiomeMapItem.getCustomMapData consults - without any common->client ref.
        BiomeMapItem.setClientMapDataProvider(BiomeMapClientData::instance);
    }

    /**
     * O(1) accessor used by {@link BiomeMapItem#getCustomMapData}. The instance is created once and
     * rebuilt only if the dimension changes; no sampling happens here.
     */
    public static MapItemSavedData instance(Level level) {
        ResourceKey<Level> dimension = level == null ? Level.OVERWORLD : level.dimension();
        if (data == null || !data.dimension.equals(dimension)) {
            data = MapItemSavedData.createForClient((byte) 0, false, dimension);
            idleTicks = MAX_IDLE_TICKS;
            lastPixelX = Integer.MIN_VALUE;
            lastPixelZ = Integer.MIN_VALUE;
            lastTier = -1;
            Arrays.fill(CACHED_COLORS, FOG_COLOR);
        }
        return data;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.level() instanceof ClientLevel level)) {
            return;
        }
        MapItemSavedData saved = instance(level);
        updatePlayerArrow(level, player, saved);
        refreshIfNeeded(minecraft, level, player, saved);
    }

    /**
     * 登出时清空探索迷雾与所有客户端状态 / Wipes the client exploration fog and every cached bit of
     * map state when leaving a world so progress never leaks across saves.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        data = null;
        lastPixelX = Integer.MIN_VALUE;
        lastPixelZ = Integer.MIN_VALUE;
        lastTier = -1;
        idleTicks = MAX_IDLE_TICKS;
        lastRefreshWasActive = false;
        Arrays.fill(CACHED_COLORS, FOG_COLOR);
        BiomeMapExplorationState.clear();
    }

    /**
     * 玩家箭头 / Adds the player's own decoration at the map centre so {@code MapRenderer} draws the
     * vanilla rotating arrow. The data is player-centred, so the correct map position is the data
     * centre (0,0), not the raw world coordinates.
     */
    private static void updatePlayerArrow(ClientLevel level, LocalPlayer player, MapItemSavedData saved) {
        saved.addDecoration(MapDecorationTypes.PLAYER, level, PLAYER_DECORATION_ID,
                0.0, 0.0, player.getYRot(), null);
    }

    private static void refreshIfNeeded(Minecraft minecraft, ClientLevel level, LocalPlayer player,
                                        MapItemSavedData saved) {
        boolean holding = player.getMainHandItem().is(WeatherRealm.BIOME_MAP.get())
                || player.getOffhandItem().is(WeatherRealm.BIOME_MAP.get());
        if (!holding) {
            lastRefreshWasActive = false;
            return;
        }

        int blocksPerPixel = BiomeMapItem.getChunksPerPixel() * 16;
        int pixelX = Math.floorDiv(player.getBlockX(), blocksPerPixel);
        int pixelZ = Math.floorDiv(player.getBlockZ(), blocksPerPixel);

        idleTicks++;
        boolean becameActive = !lastRefreshWasActive;
        boolean refresh = becameActive || pixelX != lastPixelX || pixelZ != lastPixelZ
                || lastTier != BiomeMapItem.getZoomTier() || idleTicks >= MAX_IDLE_TICKS;
        if (!refresh) {
            return;
        }

        idleTicks = 0;
        lastPixelX = pixelX;
        lastPixelZ = pixelZ;
        lastTier = BiomeMapItem.getZoomTier();
        lastRefreshWasActive = true;

        BiomeMapExplorationState.updateExploration(level, player.blockPosition(), explorationRadiusChunks());

        if (rebuildColors(level, saved, pixelX * blocksPerPixel, pixelZ * blocksPerPixel, blocksPerPixel)) {
            // Only re-upload when the pixels actually changed.
            minecraft.gameRenderer.getMapRenderer().update(BiomeMapItem.MAP_ID, saved);
        }
    }

    /**
     * 探索半径 / Exploration radius in chunks. Follows the client's effective render distance but is
     * at least the map's half-width in chunks (64 pixels x chunks-per-pixel, i.e. 128 for the macro
     * tier and 256 for the wide tier), so a raised view distance genuinely widens the uncovered
     * ring instead of the map staying a small disc of fog-free terrain. Capped at 256 chunks so a
     * very high view distance cannot turn each refresh into an absurd quadratic chunk sweep.
     */
    private static int explorationRadiusChunks() {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        int halfMapChunks = HALF * BiomeMapItem.getChunksPerPixel();
        return Math.max(16, Math.min(256, Math.max(renderDistance + 8, halfMapChunks)));
    }

    /** Repaints the 128x128 colours; returns {@code true} if any byte changed. */
    private static boolean rebuildColors(ClientLevel level, MapItemSavedData saved, int baseX, int baseZ,
                                         int blocksPerPixel) {
        byte[] colors = saved.colors;
        byte[] cache = CACHED_COLORS;
        boolean changed = false;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int pz = 0; pz < SIZE; pz++) {
            int worldZ = baseZ + (pz - HALF) * blocksPerPixel;
            int chunkZ = worldZ >> 4;
            int rowStart = pz * SIZE;
            for (int px = 0; px < SIZE; px++) {
                int worldX = baseX + (px - HALF) * blocksPerPixel;
                int chunkX = worldX >> 4;
                int index = rowStart + px;

                byte color;
                if (!BiomeMapExplorationState.isExplored(chunkX, chunkZ)) {
                    color = FOG_COLOR;
                } else if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    // Explored but currently unloaded: keep this pixel's last sampled colour instead
                    // of querying the empty-chunk biome fallback (which would paint it grey) so
                    // already-uncovered terrain does not fade out as the player walks away.
                    color = cache[index];
                } else {
                    Holder<Biome> biome = level.getBiome(cursor.set(worldX, 64, worldZ));
                    color = colorFor(biome.unwrapKey().map(key -> key.location()).orElse(null));
                    cache[index] = color;
                }

                if (colors[index] != color) {
                    colors[index] = color;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static byte colorFor(ResourceLocation biomeId) {
        if (biomeId == null) {
            return UNKNOWN_COLOR;
        }
        if (CRYSTAL_PLAINS.equals(biomeId)) {
            return CRYSTAL_COLOR;
        }
        if (BLAZING_PLAINS.equals(biomeId)) {
            return BLAZING_COLOR;
        }
        if (ARID_WASTELAND.equals(biomeId)) {
            return ARID_COLOR;
        }
        return UNKNOWN_COLOR;
    }
}
