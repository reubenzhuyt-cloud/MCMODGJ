package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.client.gui.WeatherControlScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 客户端右键入口 / Client-only right-click entry point.
 *
 * <p>Skillfully isolated to the client dist: when the local player right-clicks a weather altar
 * core whose block below is a tuning pedestal, the {@link WeatherControlScreen} opens. The physical
 * server never loads this class.</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WeatherAltarInteractionHandler {
    private WeatherAltarInteractionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(WeatherRealm.WEATHER_ALTAR_CORE.get())) {
            return;
        }
        if (!level.getBlockState(pos.below()).is(WeatherRealm.WEATHER_PEDESTAL.get())) {
            return;
        }
        Minecraft.getInstance().setScreen(new WeatherControlScreen());
    }
}
