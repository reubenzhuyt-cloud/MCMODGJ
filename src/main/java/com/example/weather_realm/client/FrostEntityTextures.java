package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.resources.ResourceLocation;

/**
 * 冰系生物贴图共享 / Shared texture locations for the frost-recoloured vanilla animals.
 *
 * <p>{@link FrostCowRenderer}, {@link FrostCatRenderer} and {@link FrostPigRenderer} only differed by
 * their vanilla superclass (which owns the model and any render layers, e.g. the pig saddle and the
 * cat collar) and by the texture path. The path construction is centralised here; each renderer stays
 * a thin subclass of its vanilla counterpart so models and layers are untouched and the visual result
 * is byte-for-byte the same.</p>
 */
public final class FrostEntityTextures {
    private FrostEntityTextures() {
    }

    public static final ResourceLocation COW = entity("frost_cow");
    public static final ResourceLocation CAT = entity("frost_cat");
    public static final ResourceLocation PIG = entity("frost_pig");

    private static ResourceLocation entity(String name) {
        return ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "textures/entity/" + name + ".png");
    }
}
