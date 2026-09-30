package com.example.weather_realm.network;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 天气调控请求 / Client-to-server request asking the server to change the weather of the sender's
 * dimension.
 *
 * @param mode the requested {@link WeatherMode}
 */
public record SetWeatherPayload(WeatherMode mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetWeatherPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "set_weather"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetWeatherPayload> STREAM_CODEC =
            StreamCodec.composite(WeatherMode.STREAM_CODEC, SetWeatherPayload::mode, SetWeatherPayload::new);

    @Override
    public Type<SetWeatherPayload> type() {
        return TYPE;
    }
}
