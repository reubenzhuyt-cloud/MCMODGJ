package com.example.weather_realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Holds {@link ResourceKey}s for the mod's dimensions.
 */
public final class ModDimensions {
    private ModDimensions() {
    }

    /** The Glacial Realm dimension ({@code data/weather_realm/dimension/crystal_realm.json}). */
    public static final ResourceKey<Level> CRYSTAL_REALM = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "crystal_realm"));
}
