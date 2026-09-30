package com.example.weather_realm;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 粒子类型注册 / Particle type registration.
 */
public final class ModParticles {
    private ModParticles() {
    }

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, WeatherRealm.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLIZZARD_SNOW =
            PARTICLE_TYPES.register("blizzard_snow", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
