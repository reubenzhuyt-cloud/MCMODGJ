package com.example.weather_realm.client.map;

import java.util.Arrays;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.item.BiomeMapItem;
import com.example.weather_realm.map.BiomeMapPalette;
import com.example.weather_realm.network.BiomeMapGridPayload;
import com.example.weather_realm.network.BiomeMapTierPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 极域天象图客户端像素数据 / Client-side pixel consumer for the biome map.
 *
 * <p>Owns a single, never-registered {@link MapItemSavedData} whose {@code public byte[] colors}
 * array is now painted <b>by the server</b> from the noise biome source (so the map no longer waits
 * for chunks to load) and delivered as a {@link BiomeMapGridPayload}. The client simply copies the
 * incoming 128x128 row-major texture into {@code colors} and re-uploads it through the vanilla
 * {@code MapRenderer}.</p>
 *
 * <p>The texture is <b>player-centred</b>: pixel {@code (64,64)} is the player. The player arrow is
 * added at data-relative {@code (0,0)} (the data centre) with the player's yaw, which
 * {@code MapRenderer} draws at pixel {@code 64,64} with the vanilla arrow sprite.</p>
 *
 * <p>Everything here is client-only. {@link BiomeMapItem} finds this class without importing it,
 * through a {@link java.util.function.Function} installed by the static initialiser below; the
 * server-to-client grid bridge is installed the same way.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BiomeMapClientData {
    private static final String PLAYER_DECORATION_ID = "player";

    private static MapItemSavedData data;
    private static int reportedTier = -1;
    private static ResourceKey<Level> reportedDimension;

    private BiomeMapClientData() {
    }

    static {
        // Loading this class (NeoForge scans it for @EventBusSubscriber at client startup) installs
        // the provider that BiomeMapItem.getCustomMapData consults - without any common->client ref.
        BiomeMapItem.setClientMapDataProvider(BiomeMapClientData::instance);
        BiomeMapGridPayload.setClientReceiver(BiomeMapClientData::acceptGrid);
    }

    /**
     * O(1) accessor used by {@link BiomeMapItem#getCustomMapData}. The instance is created once and
     * rebuilt only if the dimension changes.
     */
    public static MapItemSavedData instance(Level level) {
        ResourceKey<Level> dimension = level == null ? Level.OVERWORLD : level.dimension();
        if (data == null || !data.dimension.equals(dimension)) {
            data = MapItemSavedData.createForClient((byte) 0, false, dimension);
            // Neutral grey, not the old fog black: the grid arrives from the server shortly.
            Arrays.fill(data.colors, BiomeMapPalette.UNKNOWN_COLOR);
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
        reportTierIfNeeded(level);
    }

    /**
     * 登出清理 / Wipes all client map state when leaving a world so nothing leaks across saves.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        data = null;
        reportedTier = -1;
        reportedDimension = null;
    }

    /**
     * 应用网格 / Copies a server-sampled grid into the map texture and re-uploads it when changed.
     * Invoked on the client main thread through {@link BiomeMapGridPayload#deliverToClient}.
     */
    private static void acceptGrid(BiomeMapGridPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        MapItemSavedData saved = instance(level);
        byte[] incoming = payload.colors();
        if (incoming.length != saved.colors.length) {
            return;
        }
        if (!Arrays.equals(saved.colors, incoming)) {
            System.arraycopy(incoming, 0, saved.colors, 0, saved.colors.length);
            minecraft.gameRenderer.getMapRenderer().update(BiomeMapItem.MAP_ID, saved);
        }
    }

    /**
     * 上报档位 / Tells the server the active zoom tier whenever it or the dimension changes, so the
     * server samples at the matching blocks-per-pixel scale.
     */
    private static void reportTierIfNeeded(ClientLevel level) {
        int tier = BiomeMapItem.getZoomTier();
        ResourceKey<Level> dimension = level.dimension();
        if (tier == reportedTier && dimension.equals(reportedDimension)) {
            return;
        }
        reportedTier = tier;
        reportedDimension = dimension;
        PacketDistributor.sendToServer(new BiomeMapTierPayload(tier));
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
}
