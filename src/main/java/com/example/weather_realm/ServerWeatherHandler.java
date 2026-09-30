package com.example.weather_realm;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps the Glacial Realm in a permanent blizzard without forcing weather on other biomes.
 * <ul>
 *     <li>Per-biome blizzard rendering is decided on the client, so the server no longer pins the
 *     whole dimension's weather every tick (which would overwrite the arid/scorching biomes).</li>
 *     <li>Immediately pushes {@code START_RAINING} + {@code RAIN_LEVEL_CHANGE(1.0F)} to players
 *     when they log in or change into the realm, so vanilla snow renders without waiting.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ServerWeatherHandler {
    private ServerWeatherHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncWeather(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncWeather(player);
        }
    }

    private static void syncWeather(ServerPlayer player) {
        if (player.level().dimension().equals(ModDimensions.CRYSTAL_REALM)) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));
        }
    }
}
