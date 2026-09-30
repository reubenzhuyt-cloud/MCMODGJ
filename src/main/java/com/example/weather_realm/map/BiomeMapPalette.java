package com.example.weather_realm.map;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.MapColor;

/**
 * 天象图调色板 / Shared biome-to-colour palette for the biome map.
 *
 * <p>Common code: {@link MapColor} and {@link Biome} live in the common half of the game, so both
 * the server-side sampler (which paints the grid) and the client-side map data (which selects the
 * initial neutral colour) can share one definition instead of duplicating the constants.</p>
 */
public final class BiomeMapPalette {
    /** 中性底色 / Neutral placeholder colour shown before a pixel has ever been sampled. */
    public static final byte UNKNOWN_COLOR = MapColor.STONE.getPackedId(MapColor.Brightness.NORMAL);
    /** 水晶平原 / Crystal plains. */
    public static final byte CRYSTAL_COLOR = MapColor.ICE.getPackedId(MapColor.Brightness.NORMAL);
    /** 炽热平原 / Blazing plains. */
    public static final byte BLAZING_COLOR = MapColor.FIRE.getPackedId(MapColor.Brightness.NORMAL);
    /** 风沙荒地 / Arid wasteland. */
    public static final byte ARID_COLOR = MapColor.SAND.getPackedId(MapColor.Brightness.NORMAL);

    private static final ResourceLocation CRYSTAL_PLAINS =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "crystal_plains");
    private static final ResourceLocation BLAZING_PLAINS =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "blazing_plains");
    private static final ResourceLocation ARID_WASTELAND =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "arid_wasteland");

    private BiomeMapPalette() {
    }

    /** Resolves the map colour of a biome holder, falling back to {@link #UNKNOWN_COLOR}. */
    public static byte colorFor(Holder<Biome> biome) {
        if (biome == null) {
            return UNKNOWN_COLOR;
        }
        return colorFor(biome.unwrapKey().map(key -> key.location()).orElse(null));
    }

    /** Resolves the map colour of a biome id, falling back to {@link #UNKNOWN_COLOR}. */
    public static byte colorFor(ResourceLocation biomeId) {
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
