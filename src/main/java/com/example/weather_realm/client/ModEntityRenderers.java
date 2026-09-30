package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.ModEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Registers entity renderers on the mod event bus, client only.
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FROST_SHEEP.get(), FrostSheepRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_COW.get(), FrostCowRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_PIG.get(), FrostPigRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_CAT.get(), FrostCatRenderer::new);
    }
}
