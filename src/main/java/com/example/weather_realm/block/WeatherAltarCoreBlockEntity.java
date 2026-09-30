package com.example.weather_realm.block;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 天气祭坛核心方块实体 / Block entity backing the weather altar core.
 *
 * <p>Intentionally data-free. The core exists only so the client can attach
 * {@code WeatherAltarCoreRenderer}, which draws the translucent shell and the floating, rotating
 * crystal. Right-clicking a core that sits on a tuning pedestal opens the weather control screen via
 * the client-only {@code WeatherAltarInteractionHandler}; all other transport is handled by the
 * climate portal system, so no server-side ritual state is kept here.</p>
 */
public class WeatherAltarCoreBlockEntity extends BlockEntity {
    public WeatherAltarCoreBlockEntity(BlockPos pos, BlockState state) {
        super(WeatherRealm.WEATHER_ALTAR_CORE_BLOCK_ENTITY.get(), pos, state);
    }
}
