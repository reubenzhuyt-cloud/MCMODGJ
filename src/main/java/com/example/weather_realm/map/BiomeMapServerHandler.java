package com.example.weather_realm.map;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.weather_realm.ModDimensions;
import com.example.weather_realm.ModItems;
import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.item.BiomeMapItem;
import com.example.weather_realm.network.BiomeMapGridPayload;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 天象图服务端调度 / Server-side driver for the biome map.
 *
 * <p>Common code (never references any client-only type). Every server tick it walks the
 * player list and, for players holding the map inside the crystal realm, advances that player's
 * {@link BiomeMapServerSampler.State}. Sampling is budgeted by the sampler, so a full 16384-pixel
 * repaint is spread across several ticks; once a window is complete and differs from the last one
 * delivered, the grid is sent with {@link PacketDistributor#sendToPlayer}.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BiomeMapServerHandler {
    /** 空闲重估间隔 / Idle re-evaluation interval in ticks once a grid is complete. */
    private static final int IDLE_THROTTLE_TICKS = 20;

    private static final Map<UUID, BiomeMapServerSampler.State> STATES = new HashMap<>();
    private static final Map<UUID, Integer> CLIENT_TIERS = new HashMap<>();

    private BiomeMapServerHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            if (!level.dimension().equals(ModDimensions.CRYSTAL_REALM) || !holdingMap(player)) {
                // Dropping into another dimension (or holstering the map) invalidates the cache.
                STATES.remove(player.getUUID());
                continue;
            }

            BiomeMapServerSampler.State state =
                    STATES.computeIfAbsent(player.getUUID(), uuid -> new BiomeMapServerSampler.State());
            int tier = tierFor(player.getUUID());

            if (!state.initialized) {
                applyTarget(player, state, tier);
                BiomeMapServerSampler.advance(level, state);
            } else if (!state.complete) {
                BiomeMapServerSampler.advance(level, state);
            } else {
                state.idleTicks++;
                if (state.idleTicks < IDLE_THROTTLE_TICKS) {
                    continue;
                }
                state.idleTicks = 0;
                applyTarget(player, state, tier);
                BiomeMapServerSampler.advance(level, state);
            }

            if (state.complete && state.sentVersion != state.version) {
                state.sentVersion = state.version;
                PacketDistributor.sendToPlayer(player, new BiomeMapGridPayload(
                        state.originPixelX, state.originPixelZ, state.blocksPerPixel, state.tier,
                        state.colors.clone()));
            }
        }
    }

    /** 记录档位 / Records the tier requested by the client, clamped to the legal range. */
    public static void setTier(ServerPlayer player, int tier) {
        CLIENT_TIERS.put(player.getUUID(), BiomeMapItem.clampTier(tier));
    }

    /** 登出清理 / Forgets all per-player state when a player disconnects. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        STATES.remove(id);
        CLIENT_TIERS.remove(id);
    }

    private static void applyTarget(ServerPlayer player, BiomeMapServerSampler.State state, int tier) {
        int blocksPerPixel = BiomeMapItem.blocksPerPixelForTier(tier);
        int pixelX = Math.floorDiv(player.getBlockX(), blocksPerPixel);
        int pixelZ = Math.floorDiv(player.getBlockZ(), blocksPerPixel);
        BiomeMapServerSampler.setTarget(state, pixelX - BiomeMapServerSampler.HALF,
                pixelZ - BiomeMapServerSampler.HALF, blocksPerPixel, tier);
    }

    private static int tierFor(UUID id) {
        Integer tier = CLIENT_TIERS.get(id);
        return tier == null ? BiomeMapItem.TIER_MACRO : tier;
    }

    private static boolean holdingMap(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.BIOME_MAP.get())
                || player.getOffhandItem().is(ModItems.BIOME_MAP.get());
    }
}
