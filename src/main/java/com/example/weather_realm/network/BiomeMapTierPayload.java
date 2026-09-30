package com.example.weather_realm.network;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 缩放档位上报 / Client-to-server notification of the player's active map zoom tier.
 *
 * <p>The zoom tier used to be a purely client-local cosmetic value, but the server now samples the
 * biome grid and therefore has to know how many blocks each pixel covers. The client reports the
 * tier on map init / dimension change and whenever it cycles the zoom.</p>
 *
 * @param tier requested zoom tier; clamped server-side to {@code [TIER_MACRO, TIER_WIDE]}
 */
public record BiomeMapTierPayload(int tier) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BiomeMapTierPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "biome_map_tier"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeMapTierPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.tier()),
            buffer -> new BiomeMapTierPayload(buffer.readVarInt()));

    @Override
    public Type<BiomeMapTierPayload> type() {
        return TYPE;
    }
}
