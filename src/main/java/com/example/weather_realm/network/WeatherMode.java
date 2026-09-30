package com.example.weather_realm.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 天气模式 / Weather mode target selected from the control screen.
 *
 * <p>Pure data + network codec, deliberately free of any client-only reference so it can be shared
 * between both sides.</p>
 */
public enum WeatherMode {
    /** 晴空万里 / Clear skies. */
    CLEAR(false, false, "message.weather_realm.weather.clear"),
    /** 甘霖骤降 / Gentle rain. */
    RAIN(true, false, "message.weather_realm.weather.rain"),
    /** 狂雷暴雨 / Raging thunderstorm. */
    THUNDER(true, true, "message.weather_realm.weather.thunder");

    public static final StreamCodec<RegistryFriendlyByteBuf, WeatherMode> STREAM_CODEC = StreamCodec.of(
            (buffer, mode) -> buffer.writeByte(mode.ordinal()),
            buffer -> values()[buffer.readByte() & 0xFF]);

    private final boolean raining;
    private final boolean thundering;
    private final String messageKey;

    WeatherMode(boolean raining, boolean thundering, String messageKey) {
        this.raining = raining;
        this.thundering = thundering;
        this.messageKey = messageKey;
    }

    public boolean raining() {
        return raining;
    }

    public boolean thundering() {
        return thundering;
    }

    /** Translation key of the chat feedback sent to the requesting player. */
    public String messageKey() {
        return messageKey;
    }
}
