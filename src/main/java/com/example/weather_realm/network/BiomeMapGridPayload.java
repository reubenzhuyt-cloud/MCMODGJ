package com.example.weather_realm.network;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 天象图网格下发 / Server-to-client biome grid for the biome map.
 *
 * <p>Carries a full 128x128 row-major colour texture ({@code index = row * SIZE + col}) sampled
 * server-side straight from the noise biome source. The window is the same player-centred grid the
 * client used to derive locally: {@code originPixelX = floorDiv(playerBlockX, blocksPerPixel) - 64}.</p>
 *
 * <p>The byte array is written verbatim (VarInt length + bytes); 16384 bytes stays well under the
 * friendly-buffer byte-array cap, so no extra compression is needed.</p>
 *
 * @param originPixelX  pixel column of texture column 0
 * @param originPixelZ  pixel row of texture row 0
 * @param blocksPerPixel world blocks covered by one pixel
 * @param tier          zoom tier the grid was sampled for
 * @param colors        {@code 128 * 128} row-major palette bytes
 */
public record BiomeMapGridPayload(int originPixelX, int originPixelZ, int blocksPerPixel, int tier, byte[] colors)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BiomeMapGridPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "biome_map_grid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeMapGridPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.originPixelX());
                buffer.writeVarInt(payload.originPixelZ());
                buffer.writeVarInt(payload.blocksPerPixel());
                buffer.writeVarInt(payload.tier());
                buffer.writeByteArray(payload.colors());
            },
            buffer -> new BiomeMapGridPayload(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readByteArray()));

    /**
     * 客户端接收桥 / Static sink installed by client-only code at startup, mirroring the
     * {@code BiomeMapItem.setClientMapDataProvider} bridge so common code never references any
     * client-only type.
     */
    public interface ClientReceiver {
        void accept(BiomeMapGridPayload payload);
    }

    private static volatile ClientReceiver clientReceiver;

    /** Installed once on the physical client; {@code null} on a dedicated server. */
    public static void setClientReceiver(ClientReceiver receiver) {
        clientReceiver = receiver;
    }

    /** Hands the payload to the client sink, if one is installed. */
    public static void deliverToClient(BiomeMapGridPayload payload) {
        ClientReceiver receiver = clientReceiver;
        if (receiver != null) {
            receiver.accept(payload);
        }
    }

    @Override
    public Type<BiomeMapGridPayload> type() {
        return TYPE;
    }
}
