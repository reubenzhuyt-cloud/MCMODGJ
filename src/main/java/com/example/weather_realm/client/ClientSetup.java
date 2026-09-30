package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化 / Client-only mod setup.
 *
 * <p>Lives outside the common mod class so that no {@code net.minecraft.client.*} type is ever
 * referenced from common code (AGENTS.md §5.3); the dedicated server therefore never loads this
 * class. Behaviour is identical to the previously nested {@code ClientModEvents}.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        WeatherRealm.LOGGER.info("HELLO FROM CLIENT SETUP");
        WeatherRealm.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
