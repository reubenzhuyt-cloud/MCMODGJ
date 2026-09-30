package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.ModDimensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Drives the permanent blizzard on the client while the player is inside the Glacial Realm.
 * <ul>
 *     <li>Drives vanilla rain/snow rendering through a smoothly interpolated rain level.</li>
 *     <li>Spawns ambient blizzard flakes whose density scales with the blizzard strength, and only in
 *     open sky (never underground/roofed).</li>
 * </ul>
 * The blizzard strength fades in/out over roughly 1.25 seconds as the player crosses biome borders,
 * so stepping between the frozen and arid regions transitions the weather and sky box smoothly
 * instead of snapping. Client-only; the server still owns the authoritative rain/thunder levels.
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientBlizzardEffects {
    private ClientBlizzardEffects() {
    }

    private static final int FLAKES_PER_TICK = 40;
    private static final double RADIUS = 16.0;
    private static final double HEIGHT = 12.0;
    /** Per-tick blend step: 0.04F means ~25 ticks (~1.25s) to travel the full 0..1 range. */
    private static final float TRANSITION_STEP = 0.04F;

    /** Smoothed blizzard strength in {@code [0, 1]}, interpolated towards the current biome target. */
    private static float currentBlizzardTransition = 0.0F;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        if (!level.dimension().equals(ModDimensions.CRYSTAL_REALM)) {
            currentBlizzardTransition = 0.0F;
            return;
        }

        // Blizzard effects are confined to the frozen biome; the arid/scorching biomes stay dry.
        // The strength is approached smoothly so biome crossings fade rather than snap.
        boolean inBlizzard = level.getBiome(player.blockPosition())
                .is(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "crystal_plains"));
        float target = inBlizzard ? 1.0F : 0.0F;
        currentBlizzardTransition = Mth.approach(currentBlizzardTransition, target, TRANSITION_STEP);

        // 1. Drive vanilla rain/snow rendering proportionally (LevelRenderer.renderSnowAndRain uses it).
        level.setRainLevel(currentBlizzardTransition);
        if (level.getLevelData() instanceof ClientLevel.ClientLevelData data) {
            data.setRaining(currentBlizzardTransition > 0.02F);
        }

        // 2. Suppress surface flakes when the player is buried (underground or under a roof).
        BlockPos playerPos = player.blockPosition();
        int playerSurfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, playerPos.getX(), playerPos.getZ());
        if (!level.canSeeSky(playerPos) && playerPos.getY() < playerSurfaceY - 2) {
            return;
        }

        // 3. Flake density follows the smoothed transition, tightly around the player & under open sky.
        int activeFlakes = Math.round(FLAKES_PER_TICK * currentBlizzardTransition);
        RandomSource random = level.random;
        for (int i = 0; i < activeFlakes; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * RADIUS;
            double z = player.getZ() + (random.nextDouble() - 0.5) * RADIUS;
            double y = player.getY() + random.nextDouble() * HEIGHT - 2.0;

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            if (y < surfaceY) {
                continue;
            }
            if (!level.canSeeSky(BlockPos.containing(x, y, z))) {
                continue;
            }

            double vx = (random.nextDouble() - 0.5) * 0.1;
            double vy = -0.2 - random.nextDouble() * 0.2;
            double vz = (random.nextDouble() - 0.5) * 0.1;
            level.addParticle(WeatherRealm.BLIZZARD_SNOW.get(), x, y, z, vx, vy, vz);
        }
    }
}
