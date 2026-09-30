package com.example.weather_realm.client;

import com.example.weather_realm.ModBlockEntities;
import com.example.weather_realm.WeatherRealm;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Client-only hooks: registers the altar core block entity renderer and loads the two custom
 * models it draws as standalone (additionally baked) models.
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientBlockEntityRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WEATHER_ALTAR_CORE_BLOCK_ENTITY.get(), WeatherAltarCoreRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "block/weather_altar_core_shell")));
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "block/weather_altar_core_inner")));
    }
}
