package com.example.weather_realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Registers attributes and spawn placements for the mod's entities (mod event bus).
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FROST_SHEEP.get(), Sheep.createAttributes().build());
        event.put(ModEntities.FROST_COW.get(), Cow.createAttributes().build());
        event.put(ModEntities.FROST_PIG.get(), Pig.createAttributes().build());
        event.put(ModEntities.FROST_CAT.get(), Cat.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.FROST_SHEEP.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntityEvents::frostSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FROST_COW.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntityEvents::frostSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FROST_PIG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntityEvents::frostSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FROST_CAT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntityEvents::frostSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Allows spawning on the frost-plantable blocks (snow/ice) or any solid block. */
    private static <T extends Entity> boolean frostSpawnRules(EntityType<T> entityType, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        return state.is(ModTags.Blocks.FROST_PLANTABLE_ON) || state.isFaceSturdy(level, below, Direction.UP);
    }
}
