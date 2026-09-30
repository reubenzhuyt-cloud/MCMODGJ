package com.example.weather_realm;

import com.example.weather_realm.block.WeatherAltarCoreBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体注册 / Block entity type registration.
 */
public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WeatherRealm.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeatherAltarCoreBlockEntity>> WEATHER_ALTAR_CORE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("weather_altar_core",
                    () -> BlockEntityType.Builder.of(WeatherAltarCoreBlockEntity::new, ModBlocks.WEATHER_ALTAR_CORE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
