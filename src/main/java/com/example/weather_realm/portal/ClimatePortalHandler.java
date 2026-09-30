package com.example.weather_realm.portal;

import com.example.weather_realm.ModTags;
import com.example.weather_realm.WeatherRealm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 气候传送门事件入口 / Climate portal event entry point.
 *
 * <p>Watches climate shards as they fall and collide. When a shard rests in water the pool
 * validation and ignition run fully inline within this class, so the handler never references an
 * external manager class at runtime. A {@code climate_shard} thrown into a 2x2 pool of source
 * water whose twelve surrounding blocks are all climate frames
 * ({@link ModTags.Blocks#CLIMATE_PORTAL_FRAMES}) condenses the pool into a 2x2
 * {@code weather_portal}. Travel itself is delegated entirely to vanilla's {@code Portal}
 * pipeline via {@link com.example.weather_realm.block.WeatherPortalBlock}. The throttle keeps the
 * per-tick scan negligible while still reacting within a fifth of a second.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ClimatePortalHandler {
    /** Only re-check a given shard every this many ticks. */
    private static final long SCAN_INTERVAL_TICKS = 5L;

    private ClimatePortalHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!(itemEntity.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        if (!stack.is(WeatherRealm.CLIMATE_SHARD.get())) {
            return;
        }
        tryActivate(level, itemEntity, stack);
    }

    /**
     * Attempts to ignite a weather gate around the pool cell holding {@code itemEntity}.
     *
     * @return {@code true} if a pool was validated and condensed.
     */
    private static boolean tryActivate(ServerLevel level, ItemEntity itemEntity, ItemStack stack) {
        if (level == null || itemEntity == null || stack == null || stack.isEmpty()) {
            return false;
        }
        BlockPos inside = itemEntity.blockPosition();
        if (!level.getFluidState(inside).is(FluidTags.WATER)) {
            return false;
        }
        int x = inside.getX();
        int y = inside.getY();
        int z = inside.getZ();
        // The shard may be in any of the four pool cells; try each as the pool's minimum corner.
        int[][] candidateMinCorners = {{x, z}, {x - 1, z}, {x, z - 1}, {x - 1, z - 1}};
        for (int[] corner : candidateMinCorners) {
            BlockPos anchor = new BlockPos(corner[0], y, corner[1]);
            if (isPoolValid(level, anchor) && isRingValid(level, anchor)) {
                ignite(level, anchor, itemEntity, stack);
                return true;
            }
        }
        return false;
    }

    /** All four cells must be source water at the anchor's level. */
    private static boolean isPoolValid(ServerLevel level, BlockPos anchor) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos cell = anchor.offset(dx, 0, dz);
                if (!level.getFluidState(cell).is(FluidTags.WATER) || !level.getFluidState(cell).isSource()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The twelve blocks of the enclosing 4x4 ring must all be climate frames. */
    private static boolean isRingValid(ServerLevel level, BlockPos anchor) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean isPool = dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
                if (isPool) {
                    continue;
                }
                if (!level.getBlockState(anchor.offset(dx, 0, dz)).is(ModTags.Blocks.CLIMATE_PORTAL_FRAMES)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void ignite(ServerLevel level, BlockPos anchor, ItemEntity itemEntity, ItemStack stack) {
        // Consume exactly one climate shard.
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }

        BlockState portal = WeatherRealm.WEATHER_PORTAL.get().defaultBlockState();
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                level.setBlock(anchor.offset(dx, 0, dz), portal, 3);
            }
        }

        double centerX = anchor.getX() + 1.0D;
        double centerY = anchor.getY() + 0.5D;
        double centerZ = anchor.getZ() + 1.0D;

        level.playSound(null, anchor, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.6F, 0.7F);
        level.playSound(null, anchor, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 0.8F, 0.6F);

        // Lightning visual: a crackling column of sparks plus a rising rod of storm light.
        for (int i = 0; i < 48; i++) {
            double ox = (level.getRandom().nextDouble() - 0.5D) * 4.0D;
            double oy = level.getRandom().nextDouble() * 3.0D;
            double oz = (level.getRandom().nextDouble() - 0.5D) * 4.0D;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centerX + ox, centerY + oy, centerZ + oz,
                    2, 0.0D, 0.0D, 0.0D, 0.12D);
        }
        for (int i = 0; i < 8; i++) {
            level.sendParticles(ParticleTypes.END_ROD, centerX, centerY + i * 0.6D, centerZ,
                    4, 0.25D, 0.0D, 0.25D, 0.02D);
        }
        for (int i = 0; i < 24; i++) {
            level.sendParticles(WeatherRealm.BLIZZARD_SNOW.get(),
                    centerX + (level.getRandom().nextDouble() - 0.5D) * 3.0D,
                    centerY + level.getRandom().nextDouble() * 2.0D,
                    centerZ + (level.getRandom().nextDouble() - 0.5D) * 3.0D,
                    1, 0.0D, 0.08D, 0.0D, 0.02D);
        }
    }
}
