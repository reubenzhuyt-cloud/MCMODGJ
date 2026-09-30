package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Client-only particle provider registration. Kept out of common code so dedicated
 * servers never load client classes.
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientParticleProviders {
    private ClientParticleProviders() {
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WeatherRealm.BLIZZARD_SNOW.get(), BlizzardSnowParticle.Provider::new);
    }
}
