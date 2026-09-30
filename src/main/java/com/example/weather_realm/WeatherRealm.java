package com.example.weather_realm;

import org.slf4j.Logger;

import com.example.weather_realm.config.WeatherRealmConfig;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WeatherRealm.MODID)
public class WeatherRealm {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "weather_realm";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WeatherRealm(IEventBus modEventBus, ModContainer modContainer) {
        // Register every deferred register on the mod event bus, in the exact order the monolith used.
        ModBlocks.register(modEventBus);          // BLOCKS
        ModItems.register(modEventBus);           // ITEMS
        ModCreativeTabs.register(modEventBus);    // CREATIVE_MODE_TABS
        ModParticles.register(modEventBus);       // PARTICLE_TYPES
        ModSounds.register(modEventBus);          // SOUND_EVENTS
        ModItems.registerArmorMaterials(modEventBus); // ARMOR_MATERIALS
        ModEntities.ENTITY_TYPES.register(modEventBus); // ENTITY_TYPES
        ModStructureTypes.register(modEventBus);  // STRUCTURE_TYPES + STRUCTURE_PROCESSORS
        ModBlockEntities.register(modEventBus);   // BLOCK_ENTITY_TYPE

        // Register the item to a creative tab
        modEventBus.addListener(ModCreativeTabs::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config files for us
        modContainer.registerConfig(ModConfig.Type.COMMON, WeatherRealmConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, WeatherRealmConfig.CLIENT_SPEC);
    }
}
