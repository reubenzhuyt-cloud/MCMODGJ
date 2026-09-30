package com.example.weather_realm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 天象之境配置 / Weather Realm configuration.
 *
 * <p>Two specs are registered from the mod constructor: a {@code COMMON} spec holding the numeric
 * tuning values shared by both sides (weather durations, portal scan/search parameters) and a
 * {@code CLIENT} spec holding the blizzard particle tuning. Client values must only ever be read
 * from client-only code; common code never touches {@link #CLIENT_SPEC}.</p>
 *
 * <p>Every value carries a safe lower bound so a misconfigured {@code 0} cannot cause a divide by
 * zero (e.g. the tick modulo in the portal scanner) or an infinite loop.</p>
 */
public final class WeatherRealmConfig {
    private WeatherRealmConfig() {
    }

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    // --- COMMON ---------------------------------------------------------------------------------

    /** Duration in ticks of a forced clear spell; vanilla default is 12000. */
    public static final ModConfigSpec.IntValue WEATHER_CLEAR_TIME = COMMON_BUILDER
            .comment("Duration in ticks applied when the weather is set to clear")
            .defineInRange("weatherClearTime", 12000, 0, Integer.MAX_VALUE);

    /** Duration in ticks of a forced rain/thunder spell; vanilla default is 12000. */
    public static final ModConfigSpec.IntValue WEATHER_RAIN_TIME = COMMON_BUILDER
            .comment("Duration in ticks applied when the weather is set to rain or thunder")
            .defineInRange("weatherRainTime", 12000, 0, Integer.MAX_VALUE);

    /** How many game ticks between climate-pool scans of a resting climate shard. */
    public static final ModConfigSpec.IntValue PORTAL_SCAN_INTERVAL_TICKS = COMMON_BUILDER
            .comment("Ticks between climate shard pool scans (must be >= 1)")
            .defineInRange("portalScanIntervalTicks", 5, 1, Integer.MAX_VALUE);

    /** Radius (in blocks) searched for the nearest frozen biome when travelling through a portal. */
    public static final ModConfigSpec.IntValue PORTAL_SEARCH_RADIUS = COMMON_BUILDER
            .comment("Biome search radius in blocks for the portal landing spot")
            .defineInRange("portalBiomeSearchRadius", 6400, 1, Integer.MAX_VALUE);

    /** Horizontal step of the portal biome search. */
    public static final ModConfigSpec.IntValue PORTAL_SEARCH_HORIZONTAL_STEP = COMMON_BUILDER
            .comment("Horizontal step of the portal biome search")
            .defineInRange("portalBiomeSearchHorizontalStep", 32, 1, Integer.MAX_VALUE);

    /** Vertical step of the portal biome search. */
    public static final ModConfigSpec.IntValue PORTAL_SEARCH_VERTICAL_STEP = COMMON_BUILDER
            .comment("Vertical step of the portal biome search")
            .defineInRange("portalBiomeSearchVerticalStep", 64, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    // --- CLIENT ---------------------------------------------------------------------------------

    /** Maximum blizzard flakes spawned per tick at full blizzard strength. */
    public static final ModConfigSpec.IntValue BLIZZARD_FLAKES_PER_TICK = CLIENT_BUILDER
            .comment("Blizzard flakes spawned per tick at full strength")
            .defineInRange("blizzardFlakesPerTick", 40, 0, Integer.MAX_VALUE);

    /** Horizontal spawn radius (blocks) of the blizzard flakes around the player. */
    public static final ModConfigSpec.DoubleValue BLIZZARD_RADIUS = CLIENT_BUILDER
            .comment("Horizontal radius of the blizzard flake field")
            .defineInRange("blizzardRadius", 16.0D, 0.0D, Double.MAX_VALUE);

    /** Vertical spawn height (blocks) of the blizzard flakes above the player. */
    public static final ModConfigSpec.DoubleValue BLIZZARD_HEIGHT = CLIENT_BUILDER
            .comment("Vertical span of the blizzard flake field")
            .defineInRange("blizzardHeight", 12.0D, 0.0D, Double.MAX_VALUE);

    /** Per-tick blend step of the blizzard strength (roughly 1/25 means ~1.25s full fade). */
    public static final ModConfigSpec.DoubleValue BLIZZARD_TRANSITION_STEP = CLIENT_BUILDER
            .comment("Per-tick blizzard-strength blend step (0..1)")
            .defineInRange("blizzardTransitionStep", 0.04D, 0.0D, 1.0D);

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
}
