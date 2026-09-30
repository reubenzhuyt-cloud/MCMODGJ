package com.example.weather_realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 音效注册 / Sound event registration.
 */
public final class ModSounds {
    private ModSounds() {
    }

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, WeatherRealm.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CRYSTAL_PLAINS = SOUND_EVENTS.register(
            "music.biome.crystal_plains",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "music.biome.crystal_plains")));

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
