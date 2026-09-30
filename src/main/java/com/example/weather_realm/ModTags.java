package com.example.weather_realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Mod-wide {@link TagKey} holders.
 */
public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        private Blocks() {
        }

        /** Blocks that frost plants ({@code frost_flower}, {@code frost_grass}, {@code frost_sapling}) can be placed on. */
        public static final TagKey<Block> FROST_PLANTABLE_ON = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "frost_plantable_on"));

        /**
         * 气候传送门框架 / Weather-gate climate frames.
         * The twelve blocks that must ring a 2x2 climate pool before a thrown {@code climate_shard}
         * can condense it into a portal.
         */
        public static final TagKey<Block> CLIMATE_PORTAL_FRAMES = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "climate_portal_frames"));
    }
}
