package com.example.weather_realm;

import com.example.weather_realm.entity.FrostCat;
import com.example.weather_realm.entity.FrostCow;
import com.example.weather_realm.entity.FrostPig;
import com.example.weather_realm.entity.FrostSheep;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity type registration for the mod.
 */
public final class ModEntities {
    private ModEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WeatherRealm.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FrostSheep>> FROST_SHEEP =
            ENTITY_TYPES.register("frost_sheep",
                    () -> EntityType.Builder.of(FrostSheep::new, MobCategory.CREATURE)
                            .sized(0.9F, 1.3F)
                            .build("frost_sheep"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostCow>> FROST_COW =
            ENTITY_TYPES.register("frost_cow",
                    () -> EntityType.Builder.of(FrostCow::new, MobCategory.CREATURE)
                            .sized(0.9F, 1.4F)
                            .build("frost_cow"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostPig>> FROST_PIG =
            ENTITY_TYPES.register("frost_pig",
                    () -> EntityType.Builder.of(FrostPig::new, MobCategory.CREATURE)
                            .sized(0.9F, 0.9F)
                            .build("frost_pig"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostCat>> FROST_CAT =
            ENTITY_TYPES.register("frost_cat",
                    () -> EntityType.Builder.of(FrostCat::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.7F)
                            .build("frost_cat"));
}
