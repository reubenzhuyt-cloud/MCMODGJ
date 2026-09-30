package com.example.weather_realm.entity;

import com.example.weather_realm.ModEntities;
import com.example.weather_realm.WeatherRealm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 冰原猪 / Frost Pig - a pig wrapped in a thick frost mane, bred with carrots and potatoes and
 * dropping frost porkchops and frost pelts instead of the vanilla loot.
 */
public class FrostPig extends Pig {
    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "entities/frost_pig"));

    public FrostPig(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public FrostPig getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.FROST_PIG.get().create(level);
    }

    @Override
    public ResourceKey<LootTable> getDefaultLootTable() {
        return LOOT;
    }
}
