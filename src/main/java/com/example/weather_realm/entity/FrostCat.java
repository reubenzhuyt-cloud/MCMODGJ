package com.example.weather_realm.entity;

import org.jetbrains.annotations.Nullable;

import com.example.weather_realm.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;

/**
 * 冰原猫 / Frost Cat - a silver snow-leopard cat. Tamed with raw fish and, like the vanilla cat,
 * keeps creepers and phantoms at bay while inheriting all of the base cat behaviour.
 */
public class FrostCat extends Cat {
    public FrostCat(EntityType<? extends Cat> entityType, Level level) {
        super(entityType, level);
    }

    @Nullable
    @Override
    public FrostCat getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        FrostCat baby = ModEntities.FROST_CAT.get().create(level);
        if (baby != null && otherParent instanceof Cat other) {
            baby.setVariant(this.random.nextBoolean() ? this.getVariant() : other.getVariant());
            if (this.isTame()) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
            }
        }
        return baby;
    }
}
