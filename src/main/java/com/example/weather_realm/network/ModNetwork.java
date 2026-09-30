package com.example.weather_realm.network;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.config.WeatherRealmConfig;
import com.example.weather_realm.map.BiomeMapServerHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络注册 / Payload registration and the server-side weather handler.
 *
 * <p>Registered on the mod bus. Common code only: the handler operates on {@link ServerLevel} and
 * {@link ServerPlayer}, and the client-bound bridge goes through a common sink, never a client type.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(SetWeatherPayload.TYPE, SetWeatherPayload.STREAM_CODEC, ModNetwork::handleSetWeather);
        registrar.playToClient(BiomeMapGridPayload.TYPE, BiomeMapGridPayload.STREAM_CODEC, ModNetwork::handleBiomeMapGrid);
        registrar.playToServer(BiomeMapTierPayload.TYPE, BiomeMapTierPayload.STREAM_CODEC, ModNetwork::handleBiomeMapTier);
    }

    private static void handleSetWeather(SetWeatherPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            WeatherMode mode = payload.mode();
            int clearTime = (mode == WeatherMode.CLEAR) ? WeatherRealmConfig.WEATHER_CLEAR_TIME.getAsInt() : 0;
            int weatherTime = (mode != WeatherMode.CLEAR) ? WeatherRealmConfig.WEATHER_RAIN_TIME.getAsInt() : 0;
            level.setWeatherParameters(clearTime, weatherTime, mode.raining(), mode.thundering());
            player.displayClientMessage(Component.translatable(mode.messageKey()).withStyle(ChatFormatting.AQUA), false);
        });
    }

    private static void handleBiomeMapGrid(BiomeMapGridPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BiomeMapGridPayload.deliverToClient(payload));
    }

    private static void handleBiomeMapTier(BiomeMapTierPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BiomeMapServerHandler.setTier(player, payload.tier());
            }
        });
    }
}
