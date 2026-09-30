package com.example.weather_realm;

import com.example.weather_realm.world.FrostVillageStructure;
import com.example.weather_realm.world.FrostWoodProcessor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 结构类型注册 / Structure type and structure processor registration.
 */
public final class ModStructureTypes {
    private ModStructureTypes() {
    }

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, WeatherRealm.MODID);

    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, WeatherRealm.MODID);

    // 坚冰木村庄结构类型 / Custom jigsaw structure that frost-ifies the snowy village.
    public static final DeferredHolder<StructureType<?>, StructureType<FrostVillageStructure>> FROST_VILLAGE_STRUCTURE =
            STRUCTURE_TYPES.register("frost_village", () -> () -> FrostVillageStructure.CODEC);

    // 坚冰木替换结构处理器 / Structure processor swapping spruce blocks for frost wood.
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FrostWoodProcessor>> FROST_WOOD_PROCESSOR =
            STRUCTURE_PROCESSORS.register("frost_wood_replace", () -> () -> FrostWoodProcessor.CODEC);

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PROCESSORS.register(bus);
    }
}
