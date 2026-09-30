package com.example.weather_realm.item;

import net.minecraft.world.item.Item;

/**
 * 气候碎片 / Climate Shard.
 *
 * <p>Harvested by shattering a {@code weather_altar_core}. The shard is the ritual key for the
 * weather gate: thrown into a validated 2x2 climate pool it condenses into a living storm portal.
 * This class is intentionally inert - the pool detection lives in
 * {@code com.example.weather_realm.portal.ClimatePortalHandler} so the gameplay can be hot-swapped
 * without touching the item class.</p>
 */
public class ClimateShardItem extends Item {
    public ClimateShardItem(Properties properties) {
        super(properties);
    }
}
